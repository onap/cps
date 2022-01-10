/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021-2022 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.notification;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    private static final String ROOT_NODE_XPATH = "/";

    private NotificationProperties notificationProperties;
    private NotificationPublisher notificationPublisher;
    private CpsDataUpdatedEventFactory cpsDataUpdatedEventFactory;
    private NotificationErrorHandler notificationErrorHandler;
    private List<Pattern> dataspacePatterns;

    /**
     * Create an instance of Notification Subscriber.
     *
     * @param notificationProperties     properties for notification
     * @param notificationPublisher      notification Publisher
     * @param cpsDataUpdatedEventFactory to create CPSDataUpdatedEvent
     * @param notificationErrorHandler   error handler
     */
    public NotificationService(
        final NotificationProperties notificationProperties,
        final NotificationPublisher notificationPublisher,
        final CpsDataUpdatedEventFactory cpsDataUpdatedEventFactory,
        final NotificationErrorHandler notificationErrorHandler) {
        log.info("Notification Properties {}", notificationProperties);
        this.notificationProperties = notificationProperties;
        this.notificationPublisher = notificationPublisher;
        this.cpsDataUpdatedEventFactory = cpsDataUpdatedEventFactory;
        this.notificationErrorHandler = notificationErrorHandler;
        this.dataspacePatterns = getDataspaceFilterPatterns(notificationProperties);
    }

    private List<Pattern> getDataspaceFilterPatterns(final NotificationProperties notificationProperties) {
        if (notificationProperties.isEnabled()) {
            return Arrays.stream(notificationProperties.getFilters()
                .getOrDefault("enabled-dataspaces", "")
                .split(","))
                .map(filterPattern -> Pattern.compile(filterPattern, Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Process Data Updated Event and publishes the notification.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param observedTimestamp observedTimestamp
     * @param xpath             xpath of changed data node
     * @param operation         operation
     * @return future
     */
    @Async("notificationExecutor")
    public Future<Void> processDataUpdatedEvent(final String dataspaceName, final String anchorName,
                                                final OffsetDateTime observedTimestamp,
                                                final String xpath, final Operation operation) {
        log.debug("process data updated event for dataspace '{}' & anchor '{}'", dataspaceName, anchorName);
        try {
            if (shouldSendNotification(dataspaceName)) {
                final var cpsDataUpdatedEvent =
                    cpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(dataspaceName, anchorName,
                        observedTimestamp, getRootNodeOperation(xpath, operation));
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
        return CompletableFuture.completedFuture(null);
    }

    /*
        Add more complex rules based on dataspace and anchor later
     */
    private boolean shouldSendNotification(final String dataspaceName) {

        return notificationProperties.isEnabled()
            && dataspacePatterns.stream()
            .anyMatch(pattern -> pattern.matcher(dataspaceName).find());
    }

    private Operation getRootNodeOperation(final String xpath, final Operation operation) {
        return ROOT_NODE_XPATH.equals(xpath) ? operation : Operation.UPDATE;
    }

}
