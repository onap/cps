/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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

package org.onap.cps.api;

import java.util.List;
import java.util.Map;

public interface CpsNotificationService {

    /**
     * Creates a notification subscription for a given xpath. The dataspacename and anchor details are provided as part
     * of the notification subscription JSON.
     *
     * @param notificationSubscriptionAsJson  notification subscription in JSON format
     * @param xpath                           the location where the notification subscription is to be created
     */
    void createNotificationSubscription(String notificationSubscriptionAsJson, String xpath);

    /**
     * Deletes a notification subscription for a given xpath.
     *
     * @param xpath the location where the notification subscription is to be deleted
     */
    void deleteNotificationSubscription(String xpath);

    /**
     * Checks if notification is enabled for a given dataspace and anchor.
     *
     * @param dataspaceName the dataspace name
     * @param anchorName    the anchor name
     * @return true if notification is enabled, false otherwise
     */
    boolean isNotificationEnabled(String dataspaceName, String anchorName);

    /**
     * Retrieves notification subscription for a given xpath.
     *
     * @param xpath the location where the notification subscription is to be retrieved
     * @return list of notification subscriptions as map
     */
    List<Map<String, Object>> getNotificationSubscription(String xpath);
}
