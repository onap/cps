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

package org.onap.cps.ncmp.api.impl.trustlevel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.collection.ISet;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import io.cloudevents.kafka.impl.KafkaHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceHeartbeatListener {

    private final ISet<String> untrustworthyCmHandlesSet;
    private final ObjectMapper objectMapper;

    /**
     * Listening the device heartbeats.
     *
     * @param deviceHeartbeatConsumerRecord Device Heartbeat record.
     */
    @KafkaListener(topics = "${app.dmi.device-heartbeat.topic}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void heartbeatListener(final ConsumerRecord<String, CloudEvent> deviceHeartbeatConsumerRecord) {

        final DeviceTrustLevel deviceTrustLevel =
                toConcreteEvent(deviceHeartbeatConsumerRecord.value(), DeviceTrustLevel.class);
        if (deviceTrustLevel.getTrustLevel().equals(TrustLevel.NONE)) {
            final String cmHandleId =
                    KafkaHeaders.getParsedKafkaHeader(deviceHeartbeatConsumerRecord.headers(), "ce_id");
            untrustworthyCmHandlesSet.add(cmHandleId);
            log.info("Adding cmHandleId to untrustworthy set: {}", cmHandleId);
        }
    }

    /**
     * Generic method to convert any cloudevent to provided object.
     *
     * @param cloudEvent  input cloud event
     * @param targetClass target class
     * @param <T>         target object type
     * @return target object
     */
    public <T> T toConcreteEvent(final CloudEvent cloudEvent, final Class<T> targetClass) {
        PojoCloudEventData<T> deserializedCloudEvent = null;

        try {
            deserializedCloudEvent =
                    CloudEventUtils.mapData(cloudEvent, PojoCloudEventDataMapper.from(objectMapper, targetClass));

        } catch (final Exception e) {
            log.warn("Unable to transform cloud event to target type : {} with cause : {}", targetClass,
                    e.getMessage());
        }

        return deserializedCloudEvent == null ? null : deserializedCloudEvent.getValue();
    }

}

