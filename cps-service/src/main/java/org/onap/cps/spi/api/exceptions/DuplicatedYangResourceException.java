/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.api.exceptions;

import lombok.Getter;

/**
 * Duplicated Yang resource exception. It is raised when trying to persist a duplicated yang resource.
 */
@Getter
public class DuplicatedYangResourceException extends CpsException {

    private static final long serialVersionUID = 9085557087319212380L;

    private final String name;
    private final String checksum;

    /**
     * Constructor.
     * @param cause the exception cause
     */
    public DuplicatedYangResourceException(final String name, final String checksum, final Throwable cause) {
        super(
                String.format("Unexpected duplicated yang resource: '%s' with checksum '%s'", name, checksum),
                "The error could be caused by concurrent requests. Retrying sending the request could be required.",
                cause);
        this.name = name;
        this.checksum = checksum;
    }

}
