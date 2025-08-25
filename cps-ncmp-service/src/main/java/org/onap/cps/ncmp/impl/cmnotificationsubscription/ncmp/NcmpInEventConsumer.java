/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation
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
 *  SPDX-License-Ident/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless requiredifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.DataJobSubscriptionOperationInEvent;
import org.onap.cps.ncmp.impl.utils.JexParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class NcmpInEventConsumer {

    /**
     * Consume the specified event.
     *
     * @param dataJobSubscriptionOperationInEvent the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.cm-subscription-ncmp-in}",
            containerFactory = "legacyEventConcurrentKafkaListenerContainerFactory", properties = {
                "spring.json.value.default.type="
                    + "org.onap.cps.ncmp.impl.cmnotificationsubscription."
                    + "client_to_ncmp_1_0_0.DataJobSubscriptionOperationInEvent"})
    public void consumeSubscriptionEvent(
            final DataJobSubscriptionOperationInEvent dataJobSubscriptionOperationInEvent) {

        final String eventType = dataJobSubscriptionOperationInEvent.getEventType();
        final String dataNodeSelector = dataJobSubscriptionOperationInEvent.getEvent().getDataJob()
                .getProductionJobDefinition().getTargetSelector().getDataNodeSelector();
        final List<String> fdns = JexParser.extractFdnsFromLocationPaths(dataNodeSelector);
        final String dataJobId = dataJobSubscriptionOperationInEvent.getEvent().getDataJob().getId();
        final String dataTypeId = dataJobSubscriptionOperationInEvent.getEvent().getDataType() != null
                ? dataJobSubscriptionOperationInEvent.getEvent().getDataType().getDataTypeId() : "UNKNOWN";

        log.info("Consumed subscription event with details: | jobId={} | eventType={} | fdns={} | dataType={}",
                dataJobId, eventType, fdns, dataTypeId);
    }
}
