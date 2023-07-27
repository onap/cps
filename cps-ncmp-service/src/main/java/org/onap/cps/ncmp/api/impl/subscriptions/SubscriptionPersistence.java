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

package org.onap.cps.ncmp.api.impl.subscriptions;

import java.util.Collection;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.spi.model.DataNode;

public interface SubscriptionPersistence {

    /**
     * Save subscription Event.
     *
     * @param yangModelSubscriptionEvent subscription Event as Yang Model.
     */
    void saveSubscriptionEvent(YangModelSubscriptionEvent yangModelSubscriptionEvent);

    /**
     * Check if there is an ongoing Active Subscription.
     *
     * @param yangModelSubscriptionEvent subscription Event as Yang Model.
     */
    boolean isOngoingSubscription(YangModelSubscriptionEvent yangModelSubscriptionEvent);

    /**
     * Get data nodes.
     *
     * @return the DataNode as collection.
     */
    Collection<DataNode> getDataNodesForSubscriptionEvent();

    /**
     * Get data nodes by xpath.
     *
     * @return the DataNode as collection.
     */
    Collection<DataNode> getCmHandlesForSubscriptionEvent(String clientId, String subscriptionName);
}
