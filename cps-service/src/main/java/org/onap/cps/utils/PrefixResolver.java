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

import static org.onap.cps.cache.AnchorDataCacheConfig.ANCHOR_DATA_CACHE_TTL_SECS;

import com.hazelcast.map.IMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache;
import org.onap.cps.cache.AnchorDataCacheEntry;
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
    private final String CACHE_ENTRY_PROPERTY_NAME = "prefixPerContainerName";

    private final CpsAdminService cpsAdminService;

    private final YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;

    private final IMap<String, AnchorDataCacheEntry> anchorDataCache;

    private static final Pattern TOP_LEVEL_NODE_NAME_FINDER
        = Pattern.compile("\\/([\\w-]*)(\\[@(?!.*\\[).*?])?(\\/.*)?");  //NOSONAR

    /**
     * Get the module prefix for the given xpath for a dataspace and anchor name.
     *
     * @param dataspaceName the name of the dataspace
     * @param anchorName the name of the anchor the xpath belongs to
     * @param xpath the xpath to prefix a prefix for
     * @return the prefix of the module the top level element of given xpath
     */
    public String getPrefix(final String dataspaceName, final String anchorName, final String xpath) {
        final Anchor anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        return getPrefix(anchor, xpath);
    }

    /**
     * Get the module prefix for the given xpath under the given anchor.
     *
     * @param anchor the anchor the xpath belong to
     * @param xpath the xpath to prefix a prefix for
     * @return the prefix of the module the top level element of given xpath
     */
    public String getPrefix(final Anchor anchor, final String xpath) {
        Map<String, String> prefixPerContainerName = getPrefixPerContainerName(anchor);
        return getPrefixForTopContainer(prefixPerContainerName, xpath);
    }

    private Map<String, String> getPrefixPerContainerName(final Anchor anchor) {
        if (anchorDataCache.containsKey(anchor.getName())) {
            AnchorDataCacheEntry anchorDataCacheEntry = anchorDataCache.get(anchorDataCache);
            if (anchorDataCacheEntry.hasProperty(CACHE_ENTRY_PROPERTY_NAME)) {
                return (Map) anchorDataCacheEntry.getProperty(CACHE_ENTRY_PROPERTY_NAME);
            }
        }
        return createAndCachePrefixPerContainerNameMap(anchor);
    }

    private String getPrefixForTopContainer(final Map<String, String> prefixPerContainerName,
                                            final String xpath) {
        final Matcher matcher = TOP_LEVEL_NODE_NAME_FINDER.matcher(xpath);
        if (matcher.matches()) {
            final String topLevelContainerName = matcher.group(1);
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
