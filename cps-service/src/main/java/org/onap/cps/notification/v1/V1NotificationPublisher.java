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

package org.onap.cps.notification.v1;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.event.model.v1.CpsDataUpdatedEvent;
import org.onap.cps.notification.NotificationPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class V1NotificationPublisher extends NotificationPublisher<CpsDataUpdatedEvent> {

    public V1NotificationPublisher(
            final KafkaTemplate<String, CpsDataUpdatedEvent> kafkaTemplate,
            final @Value("${notification.data-updated.topic}") String topicName) {
        super(kafkaTemplate, topicName);
    }

    /**
     * Send event to Kafka with correct message key.
     *
     * @param cpsDataUpdatedEvent event to be sent to kafka
     */
    protected void sendNotification(@NonNull final CpsDataUpdatedEvent cpsDataUpdatedEvent) {
        final var messageKey = cpsDataUpdatedEvent.getContent().getDataspaceName() + ","
                                   + cpsDataUpdatedEvent.getContent().getAnchorName();
        log.debug("Data Updated event is being sent with messageKey: '{}' & body : {} ",
            messageKey, cpsDataUpdatedEvent);
        super.getKafkaTemplate().send(super.getTopicName(), messageKey, cpsDataUpdatedEvent);
    }

}
