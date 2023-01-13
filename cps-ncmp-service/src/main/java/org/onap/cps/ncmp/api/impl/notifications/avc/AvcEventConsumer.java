/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.notifications.avc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.event.model.AvcEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for AVC events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class AvcEventConsumer {

    private final AvcEventProducer avcEventProducer;

    /**
     * Consume the specified event.
     *
     * @param avcEvent the event to be consumed and produced.
     */
    @KafkaListener(
            topics = "dmi-cm-events",
            properties = {"spring.json.value.default.type=org.onap.cps.ncmp.event.model.AvcEvent"})
    public void consumeAndForward(final AvcEvent avcEvent) {
        log.debug("Consuming AVC event {} ...", avcEvent);
        avcEventProducer.sendMessage(avcEvent);
    }
}
