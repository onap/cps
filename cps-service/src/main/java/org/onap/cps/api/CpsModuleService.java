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

package org.onap.cps.api;

import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.exceptions.DataInUseException;
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
    void createSchemaSet(@NonNull String dataspaceName, @NonNull String schemaSetName,
                         @NonNull Map<String, String> yangResourcesNameToContentMap);

    /**
     * Read schema set in the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @return a SchemaSet
     */
    SchemaSet getSchemaSet(@NonNull String dataspaceName, @NonNull String schemaSetName);

    /**
     * Deletes Schema Set.
     *
     * @param dataspaceName        dataspace name
     * @param schemaSetName        schema set name
     * @param cascadeDeleteAllowed indicates the allowance to remove associated anchors and data if exist
     * @throws DataInUseException if cascadeDeleteAllowed is set to CASCADE_DELETE_PROHIBITED and there
     *                           is associated anchor record exists in database
     */
    void deleteSchemaSet(@NonNull String dataspaceName, @NonNull String schemaSetName,
        @NonNull CascadeDeleteAllowed cascadeDeleteAllowed);
}
