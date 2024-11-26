/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.cps.spi.api.exceptions;

/**
 * Dataspace Not Found Exception. Indicates the requested data being absent.
 */

@SuppressWarnings("squid:S110")  // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class DataspaceNotFoundException extends CpsAdminException {

    private static final long serialVersionUID = -1852996415384288431L;

    /**
     * Constructor.
     *
     * @param dataspaceName the name of the dataspace
     */

    public DataspaceNotFoundException(final String dataspaceName) {
        super("Dataspace not found", String.format("Dataspace with name %s does not exist.", dataspaceName));
    }
}
