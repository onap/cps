/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory.models;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public enum NorthboundCmHandleQuerySupportedConditions {
    HAS_ALL_MODULES("hasAllModules"),
    HAS_ALL_PROPERTIES("hasAllProperties"),
    WITH_CPS_PATH("cmHandleWithCpsPath"),
    WITH_TRUST_LEVEL("cmHandleWithTrustLevel");

    public static final Collection<String> CONDITION_NAMES =
        Arrays.stream(NorthboundCmHandleQuerySupportedConditions.values())
        .map(NorthboundCmHandleQuerySupportedConditions::getConditionName).collect(Collectors.toList());

    private final String conditionName;

}
