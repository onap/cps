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

import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.common.header.Header;
import org.onap.cps.ncmp.events.async.BatchDataResponseEventV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/**
 * Batch Record filter strategy, which helps to filter the consumer records.
 *
 */
@Configuration
public class BatchRecordFilterStrategy {

    /**
     *  Filtering the consumer records based on the eventType header, It
     *  returns boolean, true means filter the consumer record and false
     *  means not filter the consumer record.
     * @return boolean value.
     */
    @Bean
    public RecordFilterStrategy<String, BatchDataResponseEventV1> filterBatchDataResponseEvent() {
        return consumedRecord -> {
            final Header eventTypeHeader = consumedRecord.headers().lastHeader("eventType");
            if (eventTypeHeader == null) {
                return false;
            }
            final String eventTypeHeaderValue = SerializationUtils.deserialize(eventTypeHeader.value());
            return !(eventTypeHeaderValue != null
                    && eventTypeHeaderValue.startsWith("org.onap.cps.ncmp.events.async.BatchDataResponseEvent"));
        };
    }
}
