/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.model.SchemaSet;
import org.onap.cps.yang.YangTextSchemaSourceSet;
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("CpsModuleServiceImpl")
public class CpsModuleServiceImpl implements CpsModuleService {
    private final LoadingCache<YangTextSchemaSourceSetKey, YangTextSchemaSourceSet> yangSchemaResourcesLoader
        = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public YangTextSchemaSourceSet load(final YangTextSchemaSourceSetKey key) {
                    return getYangTextSchemaSourceSet(key.getDataspaceName(), key.getSchemaSetName());
                }
            });
    @Autowired
    private CpsModulePersistenceService cpsModulePersistenceService;

    @Override
    public void createSchemaSet(final String dataspaceName, final String schemaSetName,
                                final Map<String, String> yangResourcesNameToContentMap) {

        final YangTextSchemaSourceSet yangTextSchemaSourceSet =
                YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap);
        final YangTextSchemaSourceSetKey key = new YangTextSchemaSourceSetKey(dataspaceName, schemaSetName);
        yangSchemaResourcesLoader.put(key, yangTextSchemaSourceSet);
        cpsModulePersistenceService
            .storeSchemaSet(dataspaceName, schemaSetName, yangResourcesNameToContentMap);
    }

    @Override
    public SchemaSet getSchemaSet(final String dataspaceName, final String schemaSetName) {
        final YangTextSchemaSourceSetKey key = new YangTextSchemaSourceSetKey(dataspaceName, schemaSetName);
        final YangTextSchemaSourceSet yangTextSchemaSourceSet;
        try {
            yangTextSchemaSourceSet = yangSchemaResourcesLoader.getUnchecked(key);
        } catch (final Exception e) {
            throw new CpsException("Failed to load YangTextSchemaSourceSet.",
                    String.format("Exception occurred on loading cached YangTextSchemaSourceSet %s.", key), e);
        }
        return SchemaSet.builder()
                       .name(schemaSetName)
                       .dataspaceName(dataspaceName)
                       .moduleReferences(yangTextSchemaSourceSet.getModuleReferences())
                .build();
    }

    private YangTextSchemaSourceSet getYangTextSchemaSourceSet(final String dataspaceName, final String schemaSetName) {
        final Map<String, String> yangResourceNameToContent =
                cpsModulePersistenceService.getYangSchemaResources(dataspaceName, schemaSetName);
        return YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent);
    }

    @Getter
    @EqualsAndHashCode
    @AllArgsConstructor
    private static class YangTextSchemaSourceSetKey {
        private final String dataspaceName;
        private final String schemaSetName;
    }
}
