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

    /**
     * Generates validation error exception for case when requested dataspace is absent.
     *
     * @param dataspaceName dataspace name
     */
    public static CpsException invalidDataspaceException(String dataspaceName) {
        return new CpsValidationException("Dataspace is invalid.",
            String.format("Dataspace with name %s does not exist.", dataspaceName));
    }

    /**
     * Generates validation error exception for case when requested moduleset is absent for existing dataspace.
     *
     * @param dataspaceName dataspace name
     * @param moduleSetName moduleset name
     */
    public static CpsException invalidModuleSetException(String dataspaceName, String moduleSetName) {
        return new CpsValidationException("ModuleSet is invalid.",
            String.format("ModuleSet with name %s was not found for dataspace %s.", moduleSetName, dataspaceName));
    }

    /**
     * Returns validation error exception for case when moduleset contains no modules.
     */
    public static CpsException emptyModuleSetException() {
        return new CpsValidationException("ModuleSet is invalid.", "ModuleSet has no YANG files to persist");
    }

    /**
     * Generates validation error exception for case when moduleset with same name already exists in the dataspace.
     *
     * @param dataspaceName dataspace name
     * @param moduleSetName moduleset name
     */
    public static CpsException duplicateModuleSetException(String dataspaceName, String moduleSetName) {
        return new CpsValidationException("ModuleSet is invalid.",
            String.format("ModuleSet with name %s already exists for dataspace %s.", moduleSetName, dataspaceName));
    }

    /**
     * Generates no data found exception for case when requested dataspace is absent.
     *
     * @param dataspaceName dataspace name
     */
    public static CpsException dataspaceNotFoundException(String dataspaceName) {
        return new CpsNotFoundException("Dataspace was not found.",
            String.format("Dataspace with name %s does not exist.", dataspaceName));
    }

    /**
     * Generates no data found exception for case when requested moduleset is absent for existing dataspace.
     *
     * @param dataspaceName dataspace name
     * @param moduleSetName moduleset name
     */
    public static CpsException moduleSetNotFoundException(String dataspaceName, String moduleSetName) {
        return new CpsNotFoundException("ModuleSet was not found.",
            String.format("ModuleSet with name %s was not found for dataspace %s.", moduleSetName, dataspaceName));
    }

}
