/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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

package org.onap.cps.ncmp.events;

import lombok.Getter;

@Getter
public enum NcmpEventDataSchema {

    BATCH_RESPONSE_V1("urn:cps:org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent:1.0.0"),
    SUBSCRIPTIONS_V1("urn:cps:org.onap.ncmp.events.subscription:1.0.0"),
    MOI_CHANGES_V1("urn:cps:org.onap.cps.ncmp.events.moi-changes:1.0.0"),
    INVENTORY_EVENTS_V1("urn:cps:org.onap.cps.ncmp.events:inventory-event:1.0.0"),
    CM_HANDLE_TRUST_LEVEL_V1("urn:cps:org.onap.cps.ncmp.dmi.events:cm-handle-trust-level:1.0.0");

    private final String dataSchema;

    NcmpEventDataSchema(final String dataSchema) {
        this.dataSchema = dataSchema;
    }


}
