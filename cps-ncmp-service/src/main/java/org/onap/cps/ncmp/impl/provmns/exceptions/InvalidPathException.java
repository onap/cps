/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.provmns.exceptions;

import org.onap.cps.ncmp.api.exceptions.NcmpException;

public class InvalidPathException extends NcmpException {

    private static final String INVALID_PATH_DETAILS_FORMAT =
        "%s not a valid path";

    /**
     * Constructor.
     *
     * @param path provmns uri path
     */
    public InvalidPathException(final String path) {
        super("not a valid path", String.format(INVALID_PATH_DETAILS_FORMAT,
            path));
    }

}
