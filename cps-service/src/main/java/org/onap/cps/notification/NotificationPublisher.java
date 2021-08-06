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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j

public abstract class NotificationPublisher<V> {

    @Getter(AccessLevel.PROTECTED)
    private final KafkaTemplate<String, V> kafkaTemplate;

    @Getter(AccessLevel.PROTECTED)
    private final String topicName;

    /**
     * Create an instance of Notification Publisher.
     *
     * @param kafkaTemplate kafkaTemplate is send event using kafka
     * @param topicName     topic, to which cpsDataUpdatedEvent is sent, is provided by setting
     *                      'notification.data-updated.topic' in the application properties
     */
    @Autowired
    protected NotificationPublisher(
        final KafkaTemplate<String, V> kafkaTemplate,
        final @Value("${notification.data-updated.topic}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    protected abstract void sendNotification(@NonNull final V event);
}
