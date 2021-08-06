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

package org.onap.cps.notification.v0;

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.notification.CpsDataUpdatedEventFactory;
import org.onap.cps.notification.NotificationErrorHandler;
import org.onap.cps.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("v0NotificationService")
@Slf4j
public class V0NotificationService extends NotificationService {

    private final V0NotificationPublisher notificationPublisher;

    /**
     * Create a notification service instance for v1 events.
     */
    public V0NotificationService(
            @Value("${notification.data-updated.enabled}") final boolean dataUpdatedEventNotificationEnabled,
            final V0NotificationPublisher notificationPublisher,
            final CpsDataUpdatedEventFactory cpsDataUpdatedEventFactory,
            final NotificationErrorHandler notificationErrorHandler) {
        super(dataUpdatedEventNotificationEnabled, cpsDataUpdatedEventFactory, notificationErrorHandler);
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    protected void notify(final String dataspaceName, final String anchorName) {
        final var cpsDataUpdatedEvent =
            super.getCpsDataUpdatedEventFactory().createCpsDataUpdatedEventV0(dataspaceName, anchorName);
        log.debug("data updated event to be published {}", cpsDataUpdatedEvent);
        notificationPublisher.sendNotification(cpsDataUpdatedEvent);
    }

}
