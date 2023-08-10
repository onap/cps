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

package org.onap.cps.ncmp.api.impl.utils;

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
public class CloudEventMapperUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generic method to convert any cloudevent to provided object.
     *
     * @param cloudEvent  input cloud event
     * @param targetClass target class
     * @param <T>         target object type
     * @return target object
     */
    public static <T> T toTargetEvent(final CloudEvent cloudEvent, final Class<T> targetClass) {
        PojoCloudEventData<T> deserializedCloudEvent = null;

        try {
            deserializedCloudEvent =
                    CloudEventUtils.mapData(cloudEvent, PojoCloudEventDataMapper.from(objectMapper, targetClass));

        } catch (final Exception e) {
            log.warn("Unable to transform cloud event to target type : {} with cause : {}", targetClass,
                    e.getMessage());
        }

        return deserializedCloudEvent == null ? null : deserializedCloudEvent.getValue();
    }

}
