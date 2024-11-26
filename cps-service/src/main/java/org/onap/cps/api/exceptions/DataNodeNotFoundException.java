/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
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
 * DataNode Not Found Exception. Indicates the requested data being absent.
 */
@SuppressWarnings("squid:S110")  // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class DataNodeNotFoundException extends DataValidationException {

    private static final long serialVersionUID = 7786740001662205407L;
    private static final String DATANODE_NOT_FOUND = "DataNode not found";
    /**
     * Constructor.
     *
     * @param dataspaceName         the name of the dataspace
     * @param anchorName            the anchor name
     * @param xpath                 datanode xpath
     * @param additionalInformation additional information
     */

    public DataNodeNotFoundException(final String dataspaceName, final String anchorName, final String xpath,
                                     final String additionalInformation) {
        super(DATANODE_NOT_FOUND, String
            .format("DataNode with xpath %s was not found for anchor %s and dataspace %s, %s.", xpath,
                anchorName, dataspaceName, additionalInformation));
    }

    /**
     * Constructor.
     *
     * @param dataspaceName the name of the dataspace
     * @param anchorName    the anchor name
     * @param xpath         datanode xpath
     */
    public DataNodeNotFoundException(final String dataspaceName, final String anchorName, final String xpath) {
        super(DATANODE_NOT_FOUND, String
            .format("DataNode with xpath %s was not found for anchor %s and dataspace %s.", xpath,
                anchorName, dataspaceName));
    }

    /**
     * Constructor.
     *
     * @param dataspaceName the name of the dataspace
     * @param anchorName the anchor name
     */
    public DataNodeNotFoundException(final String dataspaceName, final String anchorName) {
        super(DATANODE_NOT_FOUND, String.format(
            "DataNode not found for anchor %s and dataspace %s.", anchorName, dataspaceName));
    }
}
