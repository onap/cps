/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InventoryQueryConditions {

    HAS_ALL_PROPERTIES("hasAllProperties"),
    HAS_ALL_ADDITIONAL_PROPERTIES("hasAllAdditionalProperties"),
    CM_HANDLE_WITH_DMI_PLUGIN("cmHandleWithDmiPlugin"),
    WITH_CPS_PATH("cmHandleWithCpsPath");

    public static final List<String> ALL_CONDITION_NAMES = Arrays.stream(InventoryQueryConditions.values())
        .map(InventoryQueryConditions::getName).collect(Collectors.toList());

    private final String name;

}
