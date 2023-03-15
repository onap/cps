/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.spi.exceptions;

/**
 * Operation Not Yet Supported Exception.
 * Indicates the operation is not supported and has intention to be supported in the future.
 */

public class OperationNotYetSupportedException extends CpsException {

    private static final long serialVersionUID = 1517903069236383746L;

    /**
     * Constructor.
     *
     * @param details reason for the exception
     */
    public OperationNotYetSupportedException(final String details) {
        super("Operation Not Yet Supported Exception", details);
    }
}
