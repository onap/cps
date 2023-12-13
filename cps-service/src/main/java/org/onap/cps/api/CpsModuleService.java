/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2023 Nordix Foundation
 *  Modifications Copyright (C) 2020-2021 Pantheon.tech
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

package org.onap.cps.api;

import java.util.Collection;
import java.util.Map;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.exceptions.DataInUseException;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.spi.model.SchemaSet;

/**
 * Responsible for managing module sets.
 */
public interface CpsModuleService {

    /**
     * Create schema set.
     *
     * @param dataspaceName                 dataspace name
     * @param schemaSetName                 schema set name
     * @param yangResourcesNameToContentMap yang resources (files) as a mep where key is resource name
     *                                      and value is content
     */
    void createSchemaSet(String dataspaceName, String schemaSetName,
                         Map<String, String> yangResourcesNameToContentMap);

    /**
     * Create or upgrade a schema set from new modules and existing modules or only existing modules.
     * @param dataspaceName             Dataspace name
     * @param schemaSetName             schema set name
     * @param newModuleNameToContentMap YANG resources map where key is a module name and value is content
     * @param allModuleReferences       All YANG resource module references
     */
    void createSchemaSetFromModules(String dataspaceName, String schemaSetName,
                                    Map<String, String> newModuleNameToContentMap,
                                    Collection<ModuleReference> allModuleReferences);

    /**
     * Read schema set in the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @return a SchemaSet
     */
    SchemaSet getSchemaSet(String dataspaceName, String schemaSetName);

    /**
     * Retrieve all schema sets in the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @return all SchemaSets
     */
    Collection<SchemaSet> getSchemaSets(String dataspaceName);

    /**
     * Deletes Schema Set.
     *
     * @param dataspaceName        dataspace name
     * @param schemaSetName        schema set name
     * @param cascadeDeleteAllowed indicates the allowance to remove associated anchors and data if exist
     * @throws DataInUseException  if cascadeDeleteAllowed is set to CASCADE_DELETE_PROHIBITED and there
     *                             is associated anchor record exists in database
     */
    void deleteSchemaSet(String dataspaceName, String schemaSetName,
                         CascadeDeleteAllowed cascadeDeleteAllowed);

    /**
     * Deletes Schema Sets with cascade.
     *
     * @param dataspaceName        dataspace name
     * @param schemaSetNames       schema set names
     */
    void deleteSchemaSetsWithCascade(String dataspaceName, Collection<String> schemaSetNames);


    /**
     * upgrade schema sets with existing or new modules.
     *
     * @param dataspaceName             dataspace name
     * @param schemaSetName             schema set name
     * @param newModuleNameToContentMap YANG resources map where key is a module name and value is content
     * @param allModuleReferences       All YANG resource module references
     */
    void upgradeSchemaSetFromModules(final String dataspaceName, final String schemaSetName,
                                     final Map<String, String> newModuleNameToContentMap,
                                     final Collection<ModuleReference> allModuleReferences);

    /**
     * Retrieve module references for the given dataspace name.
     *
     * @param dataspaceName        dataspace name
     * @return a list of ModuleReference objects
     */
    Collection<ModuleReference> getYangResourceModuleReferences(String dataspaceName);

    /**
     * Retrieve module references for the given dataspace name and anchor name.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @return a list of ModuleReference objects
     */
    Collection<ModuleReference> getYangResourcesModuleReferences(String dataspaceName, String anchorName);

    /**
     * Retrieve module definitions for the given dataspace name and anchor name.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @return a collection of module definitions (moduleName, revision, yang resource content)
     */
    Collection<ModuleDefinition> getModuleDefinitionsByAnchorName(String dataspaceName, String anchorName);

    /**
     * Identify previously unknown Yang Resource module references.
     * The system will ignore the namespace of all module references.
     *
     * @param moduleReferencesToCheck the moduleReferencesToCheck
     * @returns collection of module references (namespace will be always blank)
     */
    Collection<ModuleReference> identifyNewModuleReferences(
        Collection<ModuleReference> moduleReferencesToCheck);

}
