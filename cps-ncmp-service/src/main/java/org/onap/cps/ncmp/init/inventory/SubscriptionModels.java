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

package org.onap.cps.ncmp.init.inventory;

import lombok.Getter;

@Getter
public enum SubscriptionModels {
    CURRENT_SUBSCRIPTION_MODEL("cm-data-subscriptions@2023-09-21.yang", "datastores"),
    PREVIOUS_SUBSCRIPTION_MODEL("subscription.yang", "subscription-registry");

    private final String modelFileName;
    private final String topLevelRegistryDatanodeName;

    SubscriptionModels(final String modelFileName, final String topLevelRegistryDatanodeName) {
        this.modelFileName = modelFileName;
        this.topLevelRegistryDatanodeName = topLevelRegistryDatanodeName;
    }
}
