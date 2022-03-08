/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.api.impl;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Map;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.yang.YangTextSchemaSourceSet;
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Provides cached YangTextSchemaSourceSet.
 */
@Service
@CacheConfig(cacheNames = {"yangSchema"})
public class YangTextSchemaSourceSetCache {

    @Autowired
    private CpsModulePersistenceService cpsModulePersistenceService;

    /**
     * Cache YangTextSchemaSourceSet.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @return YangTextSchemaSourceSet
     */
    @Cacheable(key = "#p0.concat('-').concat(#p1)")
    public YangTextSchemaSourceSet get(final String dataspaceName, final String schemaSetName) {
        CpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        final Map<String, String> yangResourceNameToContent =
                cpsModulePersistenceService.getYangSchemaResources(dataspaceName, schemaSetName);
        return YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent);
    }

    /**
     * Updates cache YangTextSchemaSourceSet.
     *
     * @param dataspaceName           dataspace name
     * @param schemaSetName           schema set name
     * @param yangTextSchemaSourceSet yangTextSchemaSourceSet
     * @return YangTextSchemaSourceSet
     */
    @CachePut(key = "#p0.concat('-').concat(#p1)")
    @CanIgnoreReturnValue
    public YangTextSchemaSourceSet updateCache(final String dataspaceName, final String schemaSetName,
            final YangTextSchemaSourceSet yangTextSchemaSourceSet) {
        CpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        return yangTextSchemaSourceSet;
    }


    /**
     * Remove the cached value for the given dataspace and schema-set.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    @CacheEvict(key = "#p0.concat('-').concat(#p1)")
    public void removeFromCache(final String dataspaceName, final String schemaSetName) {
        CpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        // Spring provides implementation for removing object from cache
    }

}
