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

package org.onap.cps.ncmp.impl.inventory.trustlevel;

import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.impl.KafkaHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper;
import org.onap.cps.ncmp.api.inventory.models.TrustLevel;
import org.onap.cps.ncmp.events.trustlevel.DeviceTrustLevel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceTrustLevelMessageConsumer {

    private static final String CLOUD_EVENT_ID_HEADER_NAME = "ce_id";
    private final TrustLevelManager trustLevelManager;

    /**
     * Listening the device heartbeats.
     *
     * @param deviceHeartbeatConsumerRecord Device Heartbeat record.
     */
    @KafkaListener(topics = "${app.dmi.device-heartbeat.topic}",
        containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void heartbeatListener(final ConsumerRecord<String, CloudEvent> deviceHeartbeatConsumerRecord) {

        final String cmHandleId = KafkaHeaders.getParsedKafkaHeader(deviceHeartbeatConsumerRecord.headers(),
            CLOUD_EVENT_ID_HEADER_NAME);

        final DeviceTrustLevel deviceTrustLevel =
            CloudEventMapper.toTargetEvent(deviceHeartbeatConsumerRecord.value(), DeviceTrustLevel.class);
        final TrustLevel newDeviceTrustLevel = TrustLevel.valueOf(deviceTrustLevel.getData().getTrustLevel());
        trustLevelManager.handleUpdateOfDeviceTrustLevel(cmHandleId, newDeviceTrustLevel);

    }

}

