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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.exceptions;

import lombok.Getter;

/**
 * Network CM Proxy exception.
 */
public class NcmpException extends RuntimeException {

    private static final long serialVersionUID = 1482619410918497467L;

    @Getter
    final String details;

    /**
     * Constructor.
     *
     * @param message the error message
     * @param details the error details
     */
    public NcmpException(final String message, final String details) {
        this(message, details, null);
    }

    /**
     * Constructor with cause.
     *
     * @param message the error message
     * @param details the error details
     * @param cause the cause of the exception
     */
    public NcmpException(final String message, final String details, final Throwable cause) {
        super(message, cause);
        this.details = details;
    }

}

