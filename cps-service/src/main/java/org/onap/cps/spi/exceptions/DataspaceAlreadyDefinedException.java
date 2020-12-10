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

/**
 * Dataspace already defined exception. Indicates the dataspace with same name already exists.
 */
public class DataspaceAlreadyDefinedException extends CpsAdminException {

    private static final long serialVersionUID = -5813793951842079228L;

    /**
     * Constructor.
     *
     * @param dataspaceName dataspace name
     * @param cause         the cause of this exception
     */
    public DataspaceAlreadyDefinedException(final String dataspaceName, final Throwable cause) {
        super("Duplicate Dataspace.",
            String.format("Dataspace with name %s already exists.", dataspaceName),
            cause);
    }
}
