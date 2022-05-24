/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.api.impl.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.event.model.NcmpAsyncRequestResponseEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NcmpAsyncRequestResponseEventProducer {

    private final KafkaTemplate<String, NcmpAsyncRequestResponseEvent> kafkaTemplate;


    /**
     * Sends message to the configured topic with a message key.
     *
     * @param eventId message key
     * @param ncmpAsyncRequestResponseEvent    message payload
     */
    public void sendMessage(final String eventId, final NcmpAsyncRequestResponseEvent ncmpAsyncRequestResponseEvent) {
        kafkaTemplate.send(ncmpAsyncRequestResponseEvent.getEventTarget(), eventId, ncmpAsyncRequestResponseEvent);
    }
}
