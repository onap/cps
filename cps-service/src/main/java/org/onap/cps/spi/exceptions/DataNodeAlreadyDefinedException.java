/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
 * Data Node already defined exception.
 */

@SuppressWarnings("squid:S110")  // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class DataNodeAlreadyDefinedException extends CpsAdminException {

    private static final long serialVersionUID = 501929839139881112L;

    /**
     * Constructor.
     *
     * @param dataspaceName the name of the dataspace
     * @param anchorName    the anchor name
     */
    public DataNodeAlreadyDefinedException(final String dataspaceName, final String anchorName) {
        super("Data node already defined", String
            .format("Data node already exists in anchor %s and dataspace %s.", anchorName, dataspaceName));
    }
}
