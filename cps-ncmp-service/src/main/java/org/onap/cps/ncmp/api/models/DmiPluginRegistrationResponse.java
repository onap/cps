/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.models;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DmiPluginRegistrationResponse {
    private List<CmHandleRegistrationResponse> createdCmHandles = Collections.emptyList();
    private List<CmHandleRegistrationResponse> updatedCmHandles = Collections.emptyList();
    private List<CmHandleRegistrationResponse> removedCmHandles = Collections.emptyList();
    private List<CmHandleRegistrationResponse> upgradedCmHandles = Collections.emptyList();
}