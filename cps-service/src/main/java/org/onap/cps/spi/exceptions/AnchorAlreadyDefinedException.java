/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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
 * Anchor already defined exception. Indicates the an anchor with same name already exists in the same dataspace
 */

@SuppressWarnings("squid:S110")  // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class AnchorAlreadyDefinedException extends CpsAdminException {

    private static final long serialVersionUID = 5744381546778730691L;

    /**
     * Constructor.
     *
     * @param dataspaceName the name dataspace
     * @param anchorName    the name of the schema set
     * @param cause         the cause of the exception
     */
    public AnchorAlreadyDefinedException(final String dataspaceName, final String anchorName, final Throwable cause) {
        super("Duplicate Anchor",
            String.format("Anchor with name %s already exists for dataspace %s.", anchorName, dataspaceName), cause);
    }
}
