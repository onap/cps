/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
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

package org.onap.cps.exceptions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class.
 * Serves error message consistency for same error cases occurred in different CPS modules.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CpsExceptionBuilder {

    private static final String SCHEMA_SET_IS_INVALID = "Schema Set is invalid.";

    /**
     * Generates validation error exception for case when requested schema set is absent for existing dataspace.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    public static CpsException invalidSchemaSetException(final String dataspaceName, final String schemaSetName) {
        return new CpsValidationException(SCHEMA_SET_IS_INVALID,
            String.format("Schema Set with name %s was not found for dataspace %s.", schemaSetName, dataspaceName));
    }

    /**
     * Returns validation error exception for case when SchemaSet contains no files.
     */
    public static CpsException emptySchemaSetException() {
        return new CpsValidationException(SCHEMA_SET_IS_INVALID, "Schema Set has no YANG resources to store");
    }

    /**
     * Generates validation error exception for case when SchemaSet with same name already exists in the dataspace.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    public static CpsException duplicateSchemaSetException(final String dataspaceName, final String schemaSetName) {
        return new CpsValidationException(SCHEMA_SET_IS_INVALID,
            String.format("Schema Set with name %s already exists for dataspace %s.", schemaSetName, dataspaceName));
    }

    /**
     * Generates an exception for case when requested dataspace is absent.
     *
     * @param dataspaceName dataspace name
     */
    public static CpsException dataspaceNotFoundException(final String dataspaceName) {
        return new DataspaceNotFoundException(
            String.format("Dataspace with name %s does not exist.", dataspaceName));
    }

    /**
     * Generates no data found exception for case when requested SchemaSet is absent for existing dataspace.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    public static CpsException schemaSetNotFoundException(final String dataspaceName, final String schemaSetName) {
        return new CpsNotFoundException("Schema Set was not found.",
            String.format("Schema Set with name %s was not found for dataspace %s.", schemaSetName, dataspaceName));
    }

    /**
     * Generates validation exception for case when inserting anchor for schema set while there is existing
     * anchor referencing same schema set.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    public static CpsException anchorExistsForSchemaSetException(final String dataspaceName,
                                                                 final String schemaSetName) {
        return new CpsValidationException("Anchor already exists for schema set.",
            String.format("Anchor for Schema Set with name %s already exists in dataspace %s.",
                schemaSetName, dataspaceName));
    }

    /**
     * Generates validation exception for case when inserting anchor with name which already taken
     * (within requested dataspace).
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     */
    public static CpsException anchorNameConflictException(final String dataspaceName, final String anchorName) {
        return new CpsValidationException("Anchor name already taken.",
            String.format("Anchor with name %s already exists in dataspace %s.", anchorName, dataspaceName));
    }

}
