
/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    private boolean dataUpdatedEventNotificationEnabled;
    private NotificationPublisher notificationPublisher;
    private CpsDataUpdatedEventFactory cpsDataUpdatedEventFactory;
    private NotificationErrorHandler notificationErrorHandler;

    /**
     * Create an instance of Notification Subscriber.
     *
     * @param dataUpdatedEventNotificationEnabled   notification can be enabled by setting
     *                                              'notification.data-updated.enabled=true' in application properties
     * @param notificationPublisher                 notification Publisher
     * @param cpsDataUpdatedEventFactory            to create CPSDataUpdatedEvent
     * @param notificationErrorHandler              error handler
     */
    @Autowired
    public NotificationService(
        @Value("${notification.data-updated.enabled}") final boolean dataUpdatedEventNotificationEnabled,
        final NotificationPublisher notificationPublisher,
        final CpsDataUpdatedEventFactory cpsDataUpdatedEventFactory,
        final NotificationErrorHandler notificationErrorHandler) {
        this.dataUpdatedEventNotificationEnabled = dataUpdatedEventNotificationEnabled;
        this.notificationPublisher = notificationPublisher;
        this.cpsDataUpdatedEventFactory = cpsDataUpdatedEventFactory;
        this.notificationErrorHandler = notificationErrorHandler;
    }

    /**
     * Process Data Updated Event and publishes the notification.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     */
    public void processDataUpdatedEvent(final String dataspaceName, final String anchorName) {
        log.debug("process data updated event for dataspace '{}' & anchor '{}'", dataspaceName, anchorName);
        try {
            if (shouldSendNotification()) {
                final var cpsDataUpdatedEvent =
                    cpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(dataspaceName, anchorName);
                log.debug("data updated event to be published {}", cpsDataUpdatedEvent);
                notificationPublisher.sendNotification(cpsDataUpdatedEvent);
            }
        } catch (final Exception exception) {
            /* All the exceptions are handled to not to propagate it to caller.
               CPS operation should not fail if sending event fails for any reason.
             */
            notificationErrorHandler.onException("Failed to process cps-data-updated-event.",
                exception, dataspaceName, anchorName);
        }
    }

    /*
        Add more complex rules based on dataspace and anchor later
     */
    private boolean shouldSendNotification() {
        return dataUpdatedEventNotificationEnabled;
    }

}
