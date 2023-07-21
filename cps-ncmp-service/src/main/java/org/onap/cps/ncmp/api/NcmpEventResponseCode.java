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

package org.onap.cps.ncmp.api;

import lombok.Getter;

@Getter
public enum NcmpEventResponseCode {

    SUCCESS("0", "Successfully applied changes"),
    SUCCESSFULLY_APPLIED_SUBSCRIPTION("1", "successfully applied subscription"),
    CM_HANDLES_NOT_FOUND("100", "cm handle id(s) not found"),
    CM_HANDLES_NOT_READY("101", "cm handle(s) not ready"),
    DMI_SERVICE_NOT_RESPONDING("102", "dmi plugin service is not responding"),
    UNABLE_TO_READ_RESOURCE_DATA("103", "dmi plugin service is not able to read resource data"),
    PARTIALLY_APPLIED_SUBSCRIPTION("104", "partially applied subscription"),
    SUBSCRIPTION_NOT_APPLICABLE("105", "subscription not applicable for all cm handles"),
    SUBSCRIPTION_PENDING("106", "subscription pending for all cm handles");

    private final String statusCode;
    private final String statusMessage;

    NcmpEventResponseCode(final String statusCode, final String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }
}
