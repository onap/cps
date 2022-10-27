/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.utils;

import com.hazelcast.map.IMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache;
import org.onap.cps.cache.AnchorDataCacheConfig;
import org.onap.cps.cache.AnchorDataCacheEntry;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.yang.YangTextSchemaSourceSet;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrefixResolver {
    private final CpsAdminService cpsAdminService;

    private final YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;

    private final IMap<String, AnchorDataCacheEntry> anchorDataCache;

    private static final Pattern TOP_LEVEL_NODE_NAME_FINDER
        = Pattern.compile("\\/([\\w-]*)(\\[@(?!.*\\[).*?])?(\\/.*)?");  //NOSONAR

    /**
     * Retrieve top level prefix using schemaContext.
     *
     * @param dataNode the target DataNode
     * @return the prefix of the top level container
     */
    public String getPrefix(final DataNode dataNode) {
        final Anchor anchor = cpsAdminService.getAnchor(dataNode.getDataspace(), dataNode.getAnchorName());
        final YangTextSchemaSourceSet yangTextSchemaSourceSet =
            yangTextSchemaSourceSetCache.get(dataNode.getDataspace(), anchor.getSchemaSetName());
        final SchemaContext schemaContext = yangTextSchemaSourceSet.getSchemaContext();
        return getModulePrefix(dataNode.getAnchorName(), schemaContext, dataNode.getXpath());
    }

    private String getModulePrefix(final String anchorName, final SchemaContext schemaContext, final String xpath) {
        final Map<QNameModule, String> prefixPerQNameModule = new HashMap<>(schemaContext.getModules().size());
        for (final Module module : schemaContext.getModules()) {
            prefixPerQNameModule.put(module.getQNameModule(), module.getPrefix());
        }
        final Map<String, String> prefixPerContainerName = new HashMap<>();
        for (final DataSchemaNode dataSchemaNode : schemaContext.getChildNodes()) {
            if (dataSchemaNode instanceof DataNodeContainer) {
                final String containerName = dataSchemaNode.getQName().getLocalName();
                final String prefix = prefixPerQNameModule.get(dataSchemaNode.getQName().getModule());
                prefixPerContainerName.put(containerName, prefix);
            }
        }
        final Matcher matcher = TOP_LEVEL_NODE_NAME_FINDER.matcher(xpath);
        if (matcher.matches()) {
            final String topLevelContainerName = matcher.group(1);
            if (prefixPerContainerName.containsKey(topLevelContainerName)) {
                final String prefix = prefixPerContainerName.get(topLevelContainerName);
                final AnchorDataCacheEntry anchorDataCacheEntry = new AnchorDataCacheEntry();
                anchorDataCacheEntry.setProperty("prefix", prefix);
                anchorDataCache.put(anchorName, anchorDataCacheEntry, AnchorDataCacheConfig.ANCHOR_DATA_CACHE_TTL_SECS,
                    TimeUnit.SECONDS);
                return prefix;
            }
        }
        return "";
    }


}
