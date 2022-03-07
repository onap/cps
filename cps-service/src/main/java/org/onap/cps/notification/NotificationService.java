/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021-2022 Bell Canada.
 * Modifications Copyright (C) 2022 Nordix Foundation
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
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.model.Anchor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProperties notificationProperties;
    private final NotificationPublisher notificationPublisher;
    private final CpsDataUpdatedEventFactory cpsDataUpdatedEventFactory;
    private final NotificationErrorHandler notificationErrorHandler;
    private List<Pattern> dataspacePatterns;

    @PostConstruct
    public void init() {
        log.info("Notification Properties {}", notificationProperties);
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
     * @param anchor            anchor
     * @param observedTimestamp observedTimestamp
     * @param xpath             xpath of changed data node
     * @param operation         operation
     * @return future
     */
    @Async("notificationExecutor")
    public Future<Void> processDataUpdatedEvent(final Anchor anchor, final OffsetDateTime observedTimestamp,
                                                final String xpath, final Operation operation) {
        log.debug("process data updated event for anchor '{}'", anchor);
        try {
            if (shouldSendNotification(anchor.getDataspaceName())) {
                final var cpsDataUpdatedEvent =
                    cpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(anchor,
                        observedTimestamp, getRootNodeOperation(xpath, operation));
                log.debug("data updated event to be published {}", cpsDataUpdatedEvent);
                notificationPublisher.sendNotification(cpsDataUpdatedEvent);
            }
        } catch (final Exception exception) {
            /* All the exceptions are handled to not to propagate it to caller.
               CPS operation should not fail if sending event fails for any reason.
             */
            notificationErrorHandler.onException("Failed to process cps-data-updated-event.",
                exception, anchor, xpath, operation);
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
        return isRootXpath(xpath) || isRootContainerNodeXpath(xpath) ? operation : Operation.UPDATE;
    }

    private static boolean isRootXpath(final String xpath) {
        return "/".equals(xpath) || "".equals(xpath);
    }

    private static boolean isRootContainerNodeXpath(final String xpath) {
        return 0 == xpath.lastIndexOf('/');
    }

}
