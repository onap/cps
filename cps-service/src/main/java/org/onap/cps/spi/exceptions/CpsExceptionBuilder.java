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

package org.onap.cps.spi.exceptions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class.
 * Serves error message consistency for same error cases occurred in different CPS modules.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CpsExceptionBuilder {

    /**
     * Generates model validation exception for case when SchemaSet with same name already exists in the dataspace.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    public static ModelValidationException duplicateSchemaSetException(final String dataspaceName,
                                                                       final String schemaSetName) {
        return new ModelValidationException("Duplicate Schema Set",
                String.format("Schema Set with name %s already exists for dataspace %s.",
                        schemaSetName, dataspaceName));
    }

    /**
     * Generates no data found exception for case when requested dataspace is absent.
     *
     * @param dataspaceName dataspace name
     */
    public static DataspaceNotFoundException dataspaceNotFoundException(final String dataspaceName) {
        return new DataspaceNotFoundException(String.format("Dataspace with name %s does not exist.", dataspaceName));
    }


}
