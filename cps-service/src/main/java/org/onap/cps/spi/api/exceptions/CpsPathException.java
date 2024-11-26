/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.spi.api.exceptions;

public class CpsPathException extends CpsException {

    private static final long serialVersionUID = 1006899957127327791L;

    private static final String ERROR_MESSAGE = "Error while parsing cpsPath expression";

    /**
     * Constructor.
     *
     * @param details the error details
     */
    public CpsPathException(final String details) {
        super(ERROR_MESSAGE, details);
    }
}
