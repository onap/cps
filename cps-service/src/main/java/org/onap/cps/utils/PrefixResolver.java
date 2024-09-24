/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache;
import org.onap.cps.cache.AnchorDataCacheEntry;
import org.onap.cps.cpspath.parser.CpsPathPrefixType;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.spi.model.Anchor;
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
    private static final long ANCHOR_DATA_CACHE_TTL_SECS = TimeUnit.HOURS.toSeconds(1);

    private static final String CACHE_ENTRY_PROPERTY_NAME = "prefixPerContainerName";

    private final CpsAnchorService cpsAnchorService;

    private final YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;

    private final IMap<String, AnchorDataCacheEntry> anchorDataCache;

    /**
     * Get the module prefix for the given xpath under the given anchor.
     *
     * @param anchor the anchor the xpath belong to
     * @param xpath the xpath to prefix a prefix for
     * @return the prefix of the module the top level element of given xpath
     */
    public String getPrefix(final Anchor anchor, final String xpath) {
        final Map<String, String> prefixPerContainerName = getPrefixPerContainerName(anchor);
        return getPrefixForTopContainer(prefixPerContainerName, xpath);
    }

    private Map<String, String> getPrefixPerContainerName(final Anchor anchor) {
        if (anchorDataCache.containsKey(anchor.getName())) {
            final AnchorDataCacheEntry anchorDataCacheEntry = anchorDataCache.get(anchor.getName());
            if (anchorDataCacheEntry.hasProperty(CACHE_ENTRY_PROPERTY_NAME)) {
                return (Map) anchorDataCacheEntry.getProperty(CACHE_ENTRY_PROPERTY_NAME);
            }
        }
        return createAndCachePrefixPerContainerNameMap(anchor);
    }

    private String getPrefixForTopContainer(final Map<String, String> prefixPerContainerName,
                                            final String xpath) {
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(xpath);
        if (cpsPathQuery.getCpsPathPrefixType() == CpsPathPrefixType.ABSOLUTE) {
            final String topLevelContainerName = cpsPathQuery.getContainerNames().get(0);
            if (prefixPerContainerName.containsKey(topLevelContainerName)) {
                return prefixPerContainerName.get(topLevelContainerName);
            }
        }
        return "";
    }

    private Map<String, String> createAndCachePrefixPerContainerNameMap(final Anchor anchor) {
        final YangTextSchemaSourceSet yangTextSchemaSourceSet =
            yangTextSchemaSourceSetCache.get(anchor.getDataspaceName(), anchor.getSchemaSetName());
        final SchemaContext schemaContext = yangTextSchemaSourceSet.getSchemaContext();
        final Map<QNameModule, String> prefixPerQNameModule = new HashMap<>(schemaContext.getModules().size());
        for (final Module module : schemaContext.getModules()) {
            prefixPerQNameModule.put(module.getQNameModule(), module.getPrefix());
        }
        final HashMap<String, String> prefixPerContainerName = new HashMap<>();
        for (final DataSchemaNode dataSchemaNode : schemaContext.getChildNodes()) {
            if (dataSchemaNode instanceof DataNodeContainer) {
                final String containerName = dataSchemaNode.getQName().getLocalName();
                final String prefix = prefixPerQNameModule.get(dataSchemaNode.getQName().getModule());
                prefixPerContainerName.put(containerName, prefix);
            }
        }
        cachePrefixPerContainerNameMap(anchor.getName(), prefixPerContainerName);
        return prefixPerContainerName;
    }

    private void cachePrefixPerContainerNameMap(final String anchorName,
                                                final Serializable prefixPerContainerName) {
        final AnchorDataCacheEntry anchorDataCacheEntry = new AnchorDataCacheEntry();
        anchorDataCacheEntry.setProperty(CACHE_ENTRY_PROPERTY_NAME, prefixPerContainerName);
        anchorDataCache.put(anchorName, anchorDataCacheEntry, ANCHOR_DATA_CACHE_TTL_SECS, TimeUnit.SECONDS);
    }

}
