/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.api.exceptions;

/**
 * Runtime exception. Thrown when schema set record rejected to be deleted because it has anchor records associated.
 */
@SuppressWarnings("squid:S110")  // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class SchemaSetInUseException extends DataInUseException {

    private static final long serialVersionUID = -3729328573253023683L;

    /**
     * Constructor.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    public SchemaSetInUseException(final String dataspaceName, final String schemaSetName) {
        super("Schema Set is being used.",
            String.format("Schema Set with name %s in dataspace %s is having Anchor records associated.",
                schemaSetName, dataspaceName)
        );
    }
}
