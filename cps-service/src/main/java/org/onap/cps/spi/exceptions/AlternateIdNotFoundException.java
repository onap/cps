/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.spi.exceptions;

import java.io.Serial;

public class AlternateIdNotFoundException extends CpsException {

    @Serial
    private static final long serialVersionUID = -2412915490233422945L;
    private static final String ALTERNATE_ID_NOT_FOUND = "No match found for requested FDN path";

    /**
     * Constructor.
     *
     * @param dataspaceName the name of the dataspace
     * @param anchorName    the anchor name
     * @param cpsPath         datanode cpsPath
     */
    public AlternateIdNotFoundException(final String dataspaceName, final String anchorName, final String cpsPath) {
        super(ALTERNATE_ID_NOT_FOUND, String
            .format("DataNode with cps path %s do not have any match for anchor %s and dataspace %s.", cpsPath,
                anchorName, dataspaceName));
    }
}
