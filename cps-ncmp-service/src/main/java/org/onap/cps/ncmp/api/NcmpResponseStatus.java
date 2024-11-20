/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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
public enum NcmpResponseStatus {

    SUCCESS("0", "Successfully applied changes"),
    CM_DATA_SUBSCRIPTION_ACCEPTED("1", "ACCEPTED"),
    CM_HANDLES_NOT_FOUND("100", "cm handle id(s) not found"),
    CM_HANDLES_NOT_READY("101", "cm handle(s) not ready"),
    DMI_SERVICE_NOT_RESPONDING("102", "dmi plugin service is not responding"),
    UNABLE_TO_READ_RESOURCE_DATA("103", "dmi plugin service is not able to read resource data"),
    CM_DATA_SUBSCRIPTION_REJECTED("104", "REJECTED"),
    UNKNOWN_ERROR("108", "Unknown error"),
    CM_HANDLE_ALREADY_EXIST("109", "cm-handle already exists"),
    CM_HANDLE_INVALID_ID("110", "cm-handle has an invalid character(s) in id"),
    ALTERNATE_ID_ALREADY_ASSOCIATED("111", "alternate id already associated"),
    MESSAGE_TOO_LARGE("112", "message too large");

    private final String code;
    private final String message;

    NcmpResponseStatus(final String code, final String message) {
        this.code = code;
        this.message = message;
    }
}
