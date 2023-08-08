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

package org.onap.cps.ncmp.api.impl.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.CmSubscriptionDmiOutEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventResponseCloudMapper {

    private final ObjectMapper objectMapper;

    /**
     * Maps CloudEvent object to SubscriptionEventResponse.
     *
     * @param cloudEvent object
     * @return CmSubscriptionDmiOutEvent deserialized
     */
    public CmSubscriptionDmiOutEvent toCmSubscriptionDmiOutEvent(final CloudEvent cloudEvent) {
        final PojoCloudEventData<CmSubscriptionDmiOutEvent> deserializedCloudEvent = CloudEventUtils
                .mapData(cloudEvent, PojoCloudEventDataMapper.from(objectMapper, CmSubscriptionDmiOutEvent.class));
        if (deserializedCloudEvent == null) {
            log.debug("No data found in the consumed subscription response event");
            return null;
        } else {
            final CmSubscriptionDmiOutEvent cmSubscriptionDmiOutEvent = deserializedCloudEvent.getValue();
            log.debug("Consuming subscription response event {}", cmSubscriptionDmiOutEvent);
            return cmSubscriptionDmiOutEvent;
        }
    }
}
