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


public interface CmSubscriptionValidationService {

    /**
     * Validate subscription ID uniqueness against existing subscriptions.
     *
     * @param subscriptionId CM Subscription ID
     * @return true if subscriptionId is not used in active subscriptions, otherwise false
     */
    boolean isValidSubscriptionId(final String subscriptionId);

    /**
     * Validate against the allowed datastores.
     *
     * @param incomingDatastore Datastore from the incoming CmSubscription event from client
     * @return true if valid datastore , otherwise false
     */
    boolean isValidDataStore(final String incomingDatastore);

}
