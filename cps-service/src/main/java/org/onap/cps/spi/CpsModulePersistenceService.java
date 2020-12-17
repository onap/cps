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

import java.util.Collection;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.model.ModuleReference;

/**
 * Service to manage modules.
 */
public interface CpsModulePersistenceService {

    /**
     * TODO
     * clean up method to conform with spi proposal - https://jira.onap.org/browse/CPS-103
     * Store the module from a yang model in the database.
     * @deprecated
     *
     * @param namespace     module namespace
     * @param moduleContent module content
     * @param revision      module revision
     * @param dataspaceName the name of the dataspace the module is associated with
     */
    @Deprecated(forRemoval = true)
    void storeModule(final String namespace, final String moduleContent, final String revision,
                     final String dataspaceName);


    /**
     * Stores Schema Set.
     *
     * @param dataspaceName                 dataspace name
     * @param schemaSetName                 schema set name
     * @param yangResourcesNameToContentMap YANG resources (files) map where key is a name and value is content
     */
    void storeSchemaSet(String dataspaceName, String schemaSetName, Map<String, String> yangResourcesNameToContentMap);

    /**
     * Returns Modules references per specific namespace / schemaSetName.
     *
     * @param namespace     module namespace
     * @param schemaSetName schema set name
     * @return collection of ModuleRef
     */
    @NonNull
    Collection<ModuleReference> getModuleReferences(@NonNull String namespace, @NonNull String schemaSetName);
}
