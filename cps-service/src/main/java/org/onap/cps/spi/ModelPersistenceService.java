/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.cps.spi;

import java.util.List;
import org.onap.cps.spi.model.SchemaSet;

/**
 * Defines methods to access and manipulate data using the chosen database solution.
 */
public interface ModelPersistenceService {

    /**
     * Store the module from a yang model in the database.
     *
     * @param namespace     module namespace
     * @param moduleContent module content
     * @param revision      module revision
     * @param dataspaceName the name of the dataspace the module is associated with
     */
    void storeModule(final String namespace, final String moduleContent, final String revision,
        final String dataspaceName);


    /**
     * Stores the moduleset.
     *
     * @param schemaSet the data descriptor including dataspace name, moduleset name and modules descriptors
     */
    void storeSchemaSet(SchemaSet schemaSet);

    /**
     * Deletes the ModuleSet and associated modules.
     *
     * @param dataspaceName the dataspace name
     * @param moduleSetName the moduleset name
     */
    void deleteSchemaSet(String dataspaceName, String moduleSetName);

    /**
     * Retrieves the ModuleSet by dataspace and name.
     *
     * @param dataspaceName the dataspace name
     * @param moduleSetName the moduleset name
     */
    SchemaSet getSchemaSet(String dataspaceName, String moduleSetName);

    /**
     * Retrieves list of modulesets by dataspace. Moduleset names only returned, module content is omitted
     *
     * @param dataspaceName the dataspace name
     */
    List<SchemaSet> getAllSchemaSets(String dataspaceName);
}
