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

package org.onap.cps.ncmp.api.impl.async;

import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.impl.KafkaHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/**
 * Data operation record filter strategy, which helps to filter the consumer records.
 *
 */
@Configuration
@Slf4j
public class DataOperationRecordFilterStrategy {

    /**
     *  Filtering the consumer records based on the eventType header, It
     *  returns boolean, true means filter the consumer record and false
     *  means not filter the consumer record.
     * @return boolean value.
     */
    @Bean
    public RecordFilterStrategy<String, CloudEvent> includeDataOperationEventsOnly() {
        return consumedRecord -> {
            final String eventTypeHeaderValue = KafkaHeaders.getParsedKafkaHeader(consumedRecord.headers(), "ce_type");
            if (eventTypeHeaderValue == null) {
                log.trace("No ce_type header found, possibly a legacy event (ignored)");
                return true;
            }
            return !(eventTypeHeaderValue.contains("DataOperationEvent"));
        };
    }
}
