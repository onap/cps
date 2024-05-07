/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation
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
import org.onap.cps.ncmp.api.NcmpResponseStatus;

/**
 * Http Client Request exception from dmi service.
 */
@Getter
public class HttpClientRequestException extends NcmpException {

    private static final long serialVersionUID = 6659897770659834797L;
    final NcmpResponseStatus ncmpResponseStatus;
    final String message;
    final String responseBodyAsString;
    final int httpStatusCode;

    /**
     * Constructor to form exception for dmi service response.
     *
     * @param ncmpResponseStatus   ncmp status message and code
     * @param message              response message from the dmi client
     * @param responseBodyAsString response body from the dmi client
     * @param httpStatusCode       http response code from the dmi client
     */
    public HttpClientRequestException(final NcmpResponseStatus ncmpResponseStatus,
                                      final String message,
                                      final String responseBodyAsString,
                                      final int httpStatusCode) {
        super(message, responseBodyAsString);
        this.ncmpResponseStatus = ncmpResponseStatus;
        this.message = message;
        this.responseBodyAsString = responseBodyAsString;
        this.httpStatusCode = httpStatusCode;
    }
}
