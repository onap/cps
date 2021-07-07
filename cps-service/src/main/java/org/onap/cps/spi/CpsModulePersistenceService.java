/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada.
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

import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.exceptions.DataInUseException;

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
}
