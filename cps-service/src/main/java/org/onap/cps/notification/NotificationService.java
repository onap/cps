/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.notification;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public abstract class NotificationService {

    private boolean dataUpdatedEventNotificationEnabled;
    @Getter(AccessLevel.PROTECTED)
    private CpsDataUpdatedEventFactory cpsDataUpdatedEventFactory;
    private NotificationErrorHandler notificationErrorHandler;

    /**
     * Create an instance of Notification Subscriber.
     *
     * @param dataUpdatedEventNotificationEnabled   notification can be enabled by setting
     *                                              'notification.data-updated.enabled=true' in application properties
     * @param cpsDataUpdatedEventFactory            to create CPSDataUpdatedEvent
     * @param notificationErrorHandler              error handler
     */
    @Autowired
    protected NotificationService(
        @Value("${notification.data-updated.enabled}") final boolean dataUpdatedEventNotificationEnabled,
        final CpsDataUpdatedEventFactory cpsDataUpdatedEventFactory,
        final NotificationErrorHandler notificationErrorHandler) {
        this.dataUpdatedEventNotificationEnabled = dataUpdatedEventNotificationEnabled;
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
                notify(dataspaceName, anchorName);
            }
        } catch (final Exception exception) {
            /* All the exceptions are handled to not to propagate it to caller.
               CPS operation should not fail if sending event fails for any reason.
             */
            notificationErrorHandler.onException("Failed to process cps-data-updated-event.",
                exception, dataspaceName, anchorName);
        }
    }

    protected abstract void notify(String dataspaceName, String anchorName);

    /*
        Add more complex rules based on dataspace and anchor later
     */
    private boolean shouldSendNotification() {
        return dataUpdatedEventNotificationEnabled;
    }

}
