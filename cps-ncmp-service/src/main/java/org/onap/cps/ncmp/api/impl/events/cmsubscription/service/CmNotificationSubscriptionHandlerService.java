/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.service;

import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.CmNotificationSubscriptionNcmpInEvent;

public interface CmNotificationSubscriptionHandlerService {

    /**
     * Check if subscription is unique.
     *
     * @param cmNotificationSubscriptionNcmpInEvent valid cmNotificationSubscriptionNcmpInEvent
     */
    void processCreateSubscriptionRequest(
        final CmNotificationSubscriptionNcmpInEvent cmNotificationSubscriptionNcmpInEvent);

}
