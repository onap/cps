/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.exception;

import lombok.Getter;

/**
 * Http Request exception for passthrough scenarios.
 */
@Getter
public class HttpRequestException extends RuntimeException {

    private static final long serialVersionUID = 6659897770659834797L;
    final Integer httpStatus;
    final String body;

    /**
     * Constructor to form exception for passthrough scenarios.
     *
     * @param message    message details from NCMP
     * @param httpStatus http status code from DMI
     * @param body       response body from DMI
     */
    public HttpRequestException(final String message, final Integer httpStatus, final String body) {
        super(message);
        this.httpStatus = httpStatus;
        this.body = body;
    }
}
