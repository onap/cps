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

package org.onap.cps.exceptions;


/**
 * Dataspace Not Found Exception. Indicates the requested data being absent.
 */
public class DataspaceNotFoundException extends CpsAdminException {

    private static final long serialVersionUID = -1852996415384288431L;

    /**
     * Constructor.
     *
     * @param cause the cause of the exception
     */
    public DataspaceNotFoundException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }

    /**
     * Constructor.
     *
     * @param message the error message
     * @param cause   the cause of the exception
     */
    public DataspaceNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     *
     * @param details the error details
     */
    public DataspaceNotFoundException(final String details) {
        super("Dataspace Not Found", details);
    }
}
