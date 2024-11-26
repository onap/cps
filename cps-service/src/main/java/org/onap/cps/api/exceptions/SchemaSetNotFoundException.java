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

package org.onap.cps.api.exceptions;

/**
 * Schema set not found exception. Indicates the schema set is not found in a given dataspace
 */

@SuppressWarnings("squid:S110")  // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class SchemaSetNotFoundException extends CpsAdminException {

    private static final long serialVersionUID = 7422782395935450035L;

    /**
     * Constructor.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     */
    public SchemaSetNotFoundException(final String dataspaceName, final String schemaSetName) {
        super("Schema Set not found.",
                String.format("Schema Set with name %s was not found for dataspace %s.", schemaSetName, dataspaceName));
    }

}
