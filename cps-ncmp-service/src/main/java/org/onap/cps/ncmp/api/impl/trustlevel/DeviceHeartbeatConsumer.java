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

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent;

import com.hazelcast.map.IMap;
import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.impl.KafkaHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.events.trustlevel.DeviceTrustLevel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceHeartbeatConsumer {

    private final IMap<String, TrustLevel> trustLevelPerCmHandle;

    /**
     * Listening the device heartbeats.
     *
     * @param deviceHeartbeatConsumerRecord Device Heartbeat record.
     */
    @KafkaListener(topics = "${app.dmi.device-heartbeat.topic}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void heartbeatListener(final ConsumerRecord<String, CloudEvent> deviceHeartbeatConsumerRecord) {

        final String cmHandleId = KafkaHeaders.getParsedKafkaHeader(deviceHeartbeatConsumerRecord.headers(),
                "ce_id");

        final DeviceTrustLevel deviceTrustLevel =
                toTargetEvent(deviceHeartbeatConsumerRecord.value(), DeviceTrustLevel.class);

        if (deviceTrustLevel != null) {
            final String trustLevel = deviceTrustLevel.getData().getTrustLevel();
            trustLevelPerCmHandle.put(cmHandleId, TrustLevel.fromString(trustLevel));
            log.debug("Added cmHandleId to trustLevelPerCmHandle map as {}:{}", cmHandleId, trustLevel);
        }
    }

}

