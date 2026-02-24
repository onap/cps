/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory.sync.lcm;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.PayloadV1;
import org.onap.cps.ncmp.events.lcm.PayloadV2;
import org.onap.cps.ncmp.events.lcm.Values;

/**
 * Utility class for examining and identifying changes in CM handle properties.
 */
@SuppressWarnings("java:S1192")  // Ignore repetition warning for string literals like "alternateId"
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PayloadFactory {

    private static final Set<String> PROPERTIES_HANDLED_SEPERATELY_FOR_V1 =
        Set.of("dataSyncEnabled", "cmHandleState", "alternateId", "moduleSetTag", "dataProducerIdentifier");

    static PayloadV1 createPayloadV1(final LcmEventType lcmEventType,
                                     final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                     final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final PayloadV2 payloadV2 = createPayloadV2(lcmEventType, currentNcmpServiceCmHandle,
                                                                  targetNcmpServiceCmHandle);
        final PayloadV1 payloadV1 = toV1Format(payloadV2);
        payloadV1.setAlternateId(targetNcmpServiceCmHandle.getAlternateId());
        payloadV1.setModuleSetTag(targetNcmpServiceCmHandle.getModuleSetTag());
        payloadV1.setDataProducerIdentifier(targetNcmpServiceCmHandle.getDataProducerIdentifier());
        return payloadV1;
    }

    static PayloadV2 createPayloadV2(final LcmEventType lcmEventType,
                                     final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                     final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final PayloadV2 payloadV2 = switch (lcmEventType) {
            case CREATE -> getPayLoadForCreate(targetNcmpServiceCmHandle);
            case UPDATE -> identifyChanges(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
            default -> new PayloadV2();
        };
        payloadV2.setCmHandleId(targetNcmpServiceCmHandle.getCmHandleId());
        return payloadV2;
    }

    static PayloadV2 getPayLoadForCreate(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final PayloadV2 payload = new PayloadV2();
        final Map<String, Object> newProperties = new HashMap<>();
        newProperties.put("dataSyncEnabled", ncmpServiceCmHandle.getCompositeState().getDataSyncEnabled());
        newProperties.put("cmHandleState", ncmpServiceCmHandle.getCompositeState().getCmHandleState().name());
        newProperties.put("alternateId", ncmpServiceCmHandle.getAlternateId());
        newProperties.put("moduleSetTag", ncmpServiceCmHandle.getModuleSetTag());
        newProperties.put("dataProducerIdentifier", ncmpServiceCmHandle.getDataProducerIdentifier());
        newProperties.putAll(ncmpServiceCmHandle.getPublicProperties());
        payload.setNewValues(newProperties);
        return payload;
    }

    static PayloadV2 identifyChanges(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                     final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final Map<String, Object> oldProperties = new HashMap<>();
        final Map<String, Object> newProperties = new HashMap<>();

        trackChange(oldProperties, newProperties, "dataSyncEnabled",
                currentNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled(),
                targetNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled());
        trackChange(oldProperties, newProperties, "cmHandleState",
                currentNcmpServiceCmHandle.getCompositeState().getCmHandleState().name(),
                targetNcmpServiceCmHandle.getCompositeState().getCmHandleState().name());
        trackChange(oldProperties, newProperties, "alternateId",
            currentNcmpServiceCmHandle.getAlternateId(), targetNcmpServiceCmHandle.getAlternateId());
        trackChange(oldProperties, newProperties, "moduleSetTag",
            currentNcmpServiceCmHandle.getModuleSetTag(), targetNcmpServiceCmHandle.getModuleSetTag());
        trackChange(oldProperties, newProperties, "dataProducerIdentifier",
            currentNcmpServiceCmHandle.getDataProducerIdentifier(),
            targetNcmpServiceCmHandle.getDataProducerIdentifier());

        final Map<String, String> currentPublicProperties = currentNcmpServiceCmHandle.getPublicProperties();
        final Map<String, String> targetPublicProperties = targetNcmpServiceCmHandle.getPublicProperties();
        trackPublicPropertiesChanges(oldProperties, newProperties, currentPublicProperties, targetPublicProperties);

        final PayloadV2 payload = new PayloadV2();
        if (!oldProperties.isEmpty()) {
            payload.setOldValues(oldProperties);
            payload.setNewValues(newProperties);
        }
        return payload;
    }

    private static PayloadV1 toV1Format(final PayloadV2 payloadV2) {
        final PayloadV1 payloadV1 = new PayloadV1();
        payloadV1.setCmHandleId(payloadV2.getCmHandleId());
        if (payloadV2.getOldValues() != null) {
            payloadV1.setOldValues(mapToValues(payloadV2.getOldValues()));
        }
        if (payloadV2.getNewValues() != null) {
            payloadV1.setNewValues(mapToValues(payloadV2.getNewValues()));
        }
        return payloadV1;
    }

    private static Values mapToValues(final Map<String, Object> properties) {
        final Values values = new Values();
        if (properties.containsKey("dataSyncEnabled")) {
            values.setDataSyncEnabled((Boolean) properties.get("dataSyncEnabled"));
        }
        if (properties.containsKey("cmHandleState")) {
            values.setCmHandleState(Values.CmHandleState.fromValue((String) properties.get("cmHandleState")));
        }
        final Map<String, String> publicProperties = new HashMap<>();
        for (final Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!PROPERTIES_HANDLED_SEPERATELY_FOR_V1.contains(entry.getKey())) {
                publicProperties.put(entry.getKey(), (String) entry.getValue());
            }
        }
        if (!publicProperties.isEmpty()) {
            values.setCmHandleProperties(List.of(publicProperties));
        }
        return values;
    }

    private static void trackChange(final Map<String, Object> oldProperties,
                                    final Map<String, Object> newProperties,
                                    final String propertyName,
                                    final Object oldValue,
                                    final Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            oldProperties.put(propertyName, oldValue);
            newProperties.put(propertyName, newValue);
        }
    }

    private static void trackPublicPropertiesChanges(final Map<String, Object> oldProperties,
                                                     final Map<String, Object> newProperties,
                                                     final Map<String, String> currentPublicProperties,
                                                     final Map<String, String> targetPublicProperties) {
        if (!Objects.equals(currentPublicProperties, targetPublicProperties)) {
            final MapDifference<String, String> mapDifference = Maps.difference(
                    targetPublicProperties, currentPublicProperties);
            oldProperties.putAll(mapDifference.entriesOnlyOnRight());
            newProperties.putAll(mapDifference.entriesOnlyOnLeft());
            mapDifference.entriesDiffering().forEach((key, valueDifference) -> {
                oldProperties.put(key, valueDifference.rightValue());
                newProperties.put(key, valueDifference.leftValue());
            });
        }
    }

}
