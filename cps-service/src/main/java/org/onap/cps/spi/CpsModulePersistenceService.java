/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2025 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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
import java.util.Map;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.api.model.SchemaSet;

/**
 * Service to manage modules.
 */
public interface CpsModulePersistenceService {

    /**
     * Stores Schema Set.
     *
     * @param dataspaceName               dataspace name
     * @param schemaSetName               schema set name
     * @param yangResourceContentPerName a map of YANG resources map where key is a name and value is content
     */
    void createSchemaSet(String dataspaceName, String schemaSetName, Map<String, String> yangResourceContentPerName);

    /**
     * Stores a new schema set from new modules and existing modules.
     *
     * @param dataspaceName                   dataspace name
     * @param schemaSetName                   Schema set name
     * @param newYangResourceContentPerName   a map of only the new YANG resources
     *                                        the key is a name and value is its content
     * @param allModuleReferences             all YANG resources module references
     */
    void createSchemaSetFromNewAndExistingModules(String dataspaceName, String schemaSetName,
                                                  Map<String, String> newYangResourceContentPerName,
                                                  Collection<ModuleReference> allModuleReferences);

    /**
     * Update an existing schema set from new modules and existing modules.
     *
     * @param dataspaceName             dataspace name
     * @param schemaSetName             Schema set name
     * @param newModuleNameToContentMap a map of only the new YANG resources
     *                                  the key is a module name and value is its content
     * @param allModuleReferences       all YANG resources module references
     */
    void updateSchemaSetFromNewAndExistingModules(String dataspaceName, String schemaSetName,
                                                  Map<String, String> newModuleNameToContentMap,
                                                  Collection<ModuleReference> allModuleReferences);

    /**
     * Checks whether a schema set exists in the specified dataspace.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @return {@code true} if the schema set exists in the given dataspace; {@code false} otherwise
     */
    boolean schemaSetExists(String dataspaceName, String schemaSetName);

    /**
     * Get all schema sets for a given dataspace.
     *
     * @param dataspaceName dataspace name.
     * @return List of schema sets
     */
    Collection<SchemaSet> getSchemaSetsByDataspaceName(String dataspaceName);

    /**
     * Deletes Schema Set.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    void deleteSchemaSet(String dataspaceName, String schemaSetName);

    /**
     * Deletes Schema Sets.
     *
     * @param dataspaceName  dataspace name
     * @param schemaSetNames schema set names
     */
    void deleteSchemaSets(String dataspaceName, Collection<String> schemaSetNames);

    /**
     * Returns YANG resources per specific dataspace / schemaSetName.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @return YANG resources (files) map where key is a name and value is content
     */
    Map<String, String> getYangSchemaResources(String dataspaceName, String schemaSetName);

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
     * @return a collection of module reference (moduleName and revision)
     */
    Collection<ModuleReference> getYangResourceModuleReferences(String dataspaceName, String anchorName);

    /**
     * Get YANG resource definitions for the given anchor name and dataspace name.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @return a collection of module definitions (moduleName, revision and yang resource content)
     */
    Collection<ModuleDefinition> getYangResourceDefinitions(String dataspaceName, String anchorName);

    /**
     * Get YANG resource definitions for the given parameters.
     *
     * @param dataspaceName  dataspace name
     * @param anchorName     anchor name
     * @param moduleName     module name
     * @param moduleRevision the revision of the module
     * @return a collection of module definitions (moduleName, revision and yang resource content)
     */
    Collection<ModuleDefinition> getYangResourceDefinitionsByAnchorAndModule(String dataspaceName, String anchorName,
                                                                             String moduleName, String moduleRevision);

    /**
     * Remove any unused Yang Resource Modules and Schema Sets from the given dataspace.
     *
     * @param dataspaceName  dataspace name
     */
    void deleteAllUnusedYangModuleData(String dataspaceName);

    /**
     * Identify new module references from those returned by a node compared to what is in CPS already.
     * The system will ignore the namespace of all module references.
     *
     * @param moduleReferencesToCheck the module references ot check
     * @return Collection of {@link ModuleReference} (namespace will be always blank)
     *
     */
    Collection<ModuleReference> identifyNewModuleReferences(Collection<ModuleReference> moduleReferencesToCheck);

}
