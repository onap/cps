/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation
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

@Getter
public class InvalidDmiResourceUrlException extends RuntimeException {

    private static final long serialVersionUID = 2928476384584894968L;

    private static final String INVALID_DMI_URL = "Invalid dmi resource url";
    final Integer httpStatus;

    public InvalidDmiResourceUrlException(final String details, final Integer httpStatus) {
        super(String.format(INVALID_DMI_URL + ": %s", details));
        this.httpStatus = httpStatus;
    }
}
