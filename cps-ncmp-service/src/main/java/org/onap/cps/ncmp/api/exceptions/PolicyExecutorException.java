/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.api.exceptions;

import java.io.Serial;
import lombok.Getter;

/**
 * Exception to be used when policy execution fails or does not allow to proceed.
 */
@Getter
public class PolicyExecutorException extends NcmpException {

    @Serial
    private static final long serialVersionUID = 6659897770659834798L;

    /**
     * Constructor to form exception for policy executor responses.
     *
     * @param message response message
     * @param details response details
     * @param cause   the cause of the exception
     */
    public PolicyExecutorException(final String message, final String details, final Throwable cause) {
        super(message, details, cause);
    }
}
