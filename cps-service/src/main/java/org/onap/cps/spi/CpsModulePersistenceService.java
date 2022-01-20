/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
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

package org.onap.cps.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.model.ModuleReference;

/**
 * Service to manage modules.
 */
public interface CpsModulePersistenceService {

    /**
     * Stores Schema Set.
     *
     * @param dataspaceName                 dataspace name
     * @param schemaSetName                 schema set name
     * @param yangResourcesNameToContentMap YANG resources (files) map where key is a name and value is content
     */
    void storeSchemaSet(@NonNull String dataspaceName, @NonNull String schemaSetName,
        @NonNull Map<String, String> yangResourcesNameToContentMap);

    /**
     * Stores a schema set from new modules and existing modules.
     *
     * @param dataspaceName                          Dataspace name
     * @param schemaSetName                          Schema set name
     * @param newYangResourcesModuleNameToContentMap YANG resources map where key is a module name and value is content
     * @param moduleReferences                    List of YANG resources module references
     */
    void storeSchemaSetFromModules(@NonNull String dataspaceName, @NonNull String schemaSetName,
                                   @NonNull Map<String, String> newYangResourcesModuleNameToContentMap,
                                   @NonNull List<ModuleReference> moduleReferences);

    /**
     * Deletes Schema Set.
     *
     * @param dataspaceName        dataspace name
     * @param schemaSetName        schema set name
     */
    void deleteSchemaSet(@NonNull String dataspaceName, @NonNull String schemaSetName);

    /**
     * Returns YANG resources per specific dataspace / schemaSetName.
     *
     * @param dataspaceName   dataspace name
     * @param schemaSetName schema set name
     * @return YANG resources (files) map where key is a name and value is content
     */
    @NonNull
    Map<String, String> getYangSchemaResources(@NonNull String dataspaceName,
        @NonNull String schemaSetName);

    /**
     * Returns YANG resources per specific dataspace / anchorName.
     *
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @return YANG resources (files) map where key is a name and value is content
     */
    @NonNull
    Map<String, String> getYangSchemaSetResources(@NonNull String dataspaceName,
        @NonNull String anchorName);

    /**
     * Returns YANG resources module references for the given dataspace name.
     *
     * @param dataspaceName dataspace name
     * @return Collection of all YANG resources module information in the database
     */
    Collection<ModuleReference> getYangResourceModuleReferences(String dataspaceName);

    /**
     * Get YANG resource module references for the given anchor name and dataspace name.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @return a collection of module names and revisions
     */
    Collection<ModuleReference> getYangResourceModuleReferences(String dataspaceName, String anchorName);
}
