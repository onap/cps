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
import org.apache.kafka.common.header.Headers;
import org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/**
 * Data operation and DMIAsync consumer record filter strategy, which helps to filter the consumer records.
 *
 */
@Configuration
@Slf4j
public class DataOperationAndDmiAsyncRecordFilterStrategy {

    /**
     *  Including the consumer records based on the cloud event type header, It
     *  returns boolean, true means exclude the consumer record and false
     *  means include the consumer record. It includes only DataOperation events.
     * @return boolean value.
     */
    @Bean
    public RecordFilterStrategy<String, CloudEvent> includeDataOperationEventsOnly() {
        return consumedRecord ->
                excludeOrIncludeRecordUsingTypeHeader(consumedRecord.headers(), "DataOperationEvent");
    }

    /**
     *  Including the consumer records based on the cloud event type header, It
     *  returns boolean, true means exclude the consumer record and false
     *  means include the consumer record. It includes only the
     *  DMIAsyncRequestResponseEvents rest all ignored.
     * @return boolean value.
     */
    @Bean
    public RecordFilterStrategy<String, DmiAsyncRequestResponseEvent> includeDmiAsyncRequestResponseEventsOnly() {
        return consumedRecord ->
                excludeOrIncludeRecordUsingTypeHeader(consumedRecord.headers(), "DmiAsyncRequestResponseEvent");
    }

    private boolean excludeOrIncludeRecordUsingTypeHeader(final Headers recordHeaders, final String eventType) {
        final String eventTypeHeaderValue = KafkaHeaders.getParsedKafkaHeader(recordHeaders, "ce_type");
        if (eventTypeHeaderValue == null && eventType.equals("DataOperationEvent")) {
            log.trace("No ce_type header found, possibly a legacy event (ignored)");
            return true;
        } else if (eventTypeHeaderValue == null) {
            log.trace("No ce_type header found, possibly a legacy event (Not ignored)");
            return false;
        }
        return !(eventTypeHeaderValue.contains(eventType));
    }
}
