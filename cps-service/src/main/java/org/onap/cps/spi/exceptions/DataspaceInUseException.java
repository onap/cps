/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

/**
 * Runtime exception.
 * Thrown when given dataspace name is rejected to be deleted because it has anchor or schemasets associated.
 */

public class DataspaceInUseException extends DataInUseException {

    private static final long serialVersionUID = 4531370947720760347L;

    /**
     * Constructor.
     *
     * @param dataspaceName dataspace name
     * @param details error message details
     */
    public DataspaceInUseException(final String dataspaceName, final String details) {
        super(String.format("Dataspace with name %s is being used.", dataspaceName), details);
    }
}
