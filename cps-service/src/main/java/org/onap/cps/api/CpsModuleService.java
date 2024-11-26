/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2024 Nordix Foundation
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
import org.onap.cps.api.exceptions.DataInUseException;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.api.model.SchemaSet;
import org.onap.cps.api.parameters.CascadeDeleteAllowed;

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
     * Retrieve module definitions for the given parameters.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param moduleName        module name
     * @param moduleRevision    the revision of the module
     * @return a collection of module definitions (moduleName, revision, yang resource content)
     */
    Collection<ModuleDefinition> getModuleDefinitionsByAnchorAndModule(String dataspaceName, String anchorName,
                                                                  String moduleName, String moduleRevision);

    /**
     * Identify previously unknown Yang Resource module references.
     * The system will ignore the namespace of all module references.
     *
     * @param moduleReferencesToCheck the moduleReferencesToCheck
     * @returns collection of module references (namespace will be always blank)
     */
    Collection<ModuleReference> identifyNewModuleReferences(
        Collection<ModuleReference> moduleReferencesToCheck);

    /**
     * Retrieves module references based on the provided dataspace name, anchor name and attribute filters
     * for both parent and child fragments.

     * This method constructs and executes a SQL query to find module references from a database, using
     * the specified `dataspaceName`, `anchorName` and two sets of attribute filters: one for parent fragments
     * and one for child fragments. The method applies these filters to identify the appropriate fragments
     * and schema sets, and then retrieves the corresponding module references.

     * The SQL query is dynamically built based on the provided attribute filters:
     * - The `parentAttributes` map is used to filter the parent fragments. The entries in this map are
     * converted into a WHERE clause for the parent fragments.
     * - The `childAttributes` map is used to filter the child fragments. This is applied to the child fragments
     * after filtering the parent fragments.
     *
     * @param dataspaceName    the name of the dataspace to filter on. It is used to locate the relevant dataspace
     *                         in the database.
     * @param anchorName       the name of the anchor to filter on. It is used to locate the relevant anchor within
     *                         the dataspace.
     * @param parentAttributes a map of attributes to filter parent fragments. Each entry in this map represents
     *                         an attribute key-value pair used in the WHERE clause for parent fragments.
     * @param childAttributes  a map of attributes to filter child fragments. Each entry in this map represents
     *                         an attribute key-value pair used in the WHERE clause for child fragments.
     * @return a collection of {@link ModuleReference} objects that match the given criteria.
     *     Each {@code ModuleReference} contains information about a module's name and revision.
     * @implNote The method assumes that both `parentAttributes` and `childAttributes` maps contain at least
     *     one entry. The first entry from `parentAttributes` is used to filter parent fragments,
     *     and the first entry from `childAttributes` is used to filter child fragments.
     */
    Collection<ModuleReference> getModuleReferencesByAttribute(final String dataspaceName, final String anchorName,
                                                               final Map<String, String> parentAttributes,
                                                               final Map<String, String> childAttributes);

    /**
     * Remove any Yang Resource Modules from the DB that are no longer referenced by any schema set.
     */
    void deleteUnusedYangResourceModules();

}
