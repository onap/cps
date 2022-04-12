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

package org.onap.cps.notification.updatedevents;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CpsUpdatedEventProducerListener<K, V> implements ProducerListener<K, V> {

    private CpsUpdatedEventNotificationErrorHandler cpsUpdatedEventNotificationErrorHandler;

    public CpsUpdatedEventProducerListener(
        final CpsUpdatedEventNotificationErrorHandler cpsUpdatedEventNotificationErrorHandler) {
        this.cpsUpdatedEventNotificationErrorHandler = cpsUpdatedEventNotificationErrorHandler;
    }

    @Override
    public void onSuccess(final ProducerRecord<K, V> producerRecord, final RecordMetadata recordMetadata) {
        log.debug("Message sent to event-bus topic :'{}' with body : {} ", producerRecord.topic(),
            producerRecord.value());
    }

    @Override
    public void onError(final ProducerRecord<K, V> producerRecord,
            final RecordMetadata recordMetadata,
            final Exception exception) {
        cpsUpdatedEventNotificationErrorHandler.onException("Failed to send message to message bus",
            exception, producerRecord, recordMetadata);
    }

}
