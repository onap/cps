/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.impl.data.async;

import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.impl.KafkaHeaders;
import java.io.Serializable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/**
 * Record filter strategies, which helps to filter the consumer records based on some conditions.
 *
 */
@Configuration
@Slf4j
public class RecordFilterStrategies {

    private static final boolean EXCLUDE_EVENT = true;

    /**
     *  Include only DataOperation events based on the cloud event type header, It
     *  returns boolean, true means exclude the consumer record and false
     *  means include the consumer record.
     * @return boolean value.
     */
    @Bean
    public RecordFilterStrategy<String, CloudEvent> includeDataOperationEventsOnly() {
        return consumerRecord ->
                isNotCloudEventOfType(consumerRecord.headers(), "DataOperationEvent");
    }

    /**
     *  Includes the consumer records based on the cloud event type header, It  returns boolean,
     *  true means exclude the consumer record and false means include the consumer record.
     *  It includes only the legacy events i.e. non-cloud events
     * @return boolean value.
     */
    @Bean
    public RecordFilterStrategy<String, Serializable> includeNonCloudEventsOnly() {
        return consumerRecord -> isCloudEvent(consumerRecord.headers());
    }

    private boolean isCloudEvent(final Headers headers) {
        return headers.lastHeader("ce_type") != null;
    }

    private boolean isNotCloudEventOfType(final Headers headers, final String requiredEventType) {
        final String eventTypeHeaderValue = KafkaHeaders.getParsedKafkaHeader(headers, "ce_type");
        if (eventTypeHeaderValue == null) {
            log.trace("No ce_type header found, possibly a legacy event (ignored)");
            return EXCLUDE_EVENT;
        }
        return !(eventTypeHeaderValue.contains(requiredEventType));
    }
}
