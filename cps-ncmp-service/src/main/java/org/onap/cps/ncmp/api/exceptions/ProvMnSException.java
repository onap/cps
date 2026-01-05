/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.api.exceptions;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
@Getter
@Setter
public class ProvMnSException extends RuntimeException {

    private String httpMethodName;
    private HttpStatus httpStatus;
    private String title;
    private String badOp;

    /**
     * Constructor.
     *
     * @param httpMethodName  original REST method
     * @param httpStatus      http status to be reported for this exception
     * @param title           3GPP error title (detail)
     * @param badOp           nullable string to describe operation type in patch operations
     */
    public ProvMnSException(final String httpMethodName,
                            final HttpStatus httpStatus,
                            final String title,
                            final String badOp) {
        super(httpMethodName + " failed");
        this.httpMethodName = httpMethodName;
        this.httpStatus = httpStatus;
        this.title = title;
        this.badOp = badOp;
    }

}
