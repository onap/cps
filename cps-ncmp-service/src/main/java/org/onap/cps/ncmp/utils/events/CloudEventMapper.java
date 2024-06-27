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

package org.onap.cps.ncmp.utils.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CloudEventMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generic method to map cloud event data to target event class object.
     *
     * @param cloudEvent       input cloud event
     * @param targetEventClass target event class
     * @param <T>              target event class type
     * @return mapped target event
     */
    public static <T> T toTargetEvent(final CloudEvent cloudEvent, final Class<T> targetEventClass) {
        PojoCloudEventData<T> mappedCloudEvent = null;

        try {
            mappedCloudEvent =
                    CloudEventUtils.mapData(cloudEvent, PojoCloudEventDataMapper.from(objectMapper, targetEventClass));

        } catch (final RuntimeException runtimeException) {
            log.error("Unable to map cloud event to target event class type : {} with cause : {}", targetEventClass,
                    runtimeException.getMessage());
        }

        return mappedCloudEvent == null ? null : mappedCloudEvent.getValue();
    }

}
