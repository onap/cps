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

package org.onap.cps.ncmp.exceptions;

import java.io.Serial;
import org.onap.cps.ncmp.api.exceptions.NcmpException;

public class NoAlternateIdMatchFoundException extends NcmpException {

    @Serial
    private static final long serialVersionUID = -2412915490233422945L;
    private static final String ALTERNATE_ID_NOT_FOUND = "No matching cm handle found using alternate ids";

    /**
     * Constructor.
     *
     * @param cpsPath datanode cpsPath
     */
    public NoAlternateIdMatchFoundException(final String cpsPath) {
        super(ALTERNATE_ID_NOT_FOUND, String.format("cannot find a datanode with alternate id %s", cpsPath));
    }
}
