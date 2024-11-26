/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
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
 * Runtime exception. Thrown when data record rejected to be deleted because it's being referenced by other data.
 */
public class DataInUseException extends CpsException {

    private static final long serialVersionUID = 5011830482789788314L;

    /**
     * Constructor.
     *
     * @param message error message
     * @param details error details
     */
    public DataInUseException(final String message, final String details) {
        super(message, details);
    }
}
