/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.events;

import java.io.Serial;
import org.onap.cps.api.exceptions.CpsException;

/**
 * Exception thrown when a batch of events fails to send to Kafka.
 */
public class EventBatchSendException extends CpsException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param message the error message
     * @param details the error details
     */
    public EventBatchSendException(final String message, final String details) {
        super(message, details);
    }

    /**
     * Constructor.
     *
     * @param message the error message
     * @param details the error details
     * @param cause   the cause of the exception
     */
    public EventBatchSendException(final String message, final String details, final Throwable cause) {
        super(message, details, cause);
    }
}
