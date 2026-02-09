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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.PayloadV1;
import org.onap.cps.ncmp.events.lcm.PayloadV2;
import org.onap.cps.ncmp.events.lcm.Values;

/**
 * Utility class for examining and identifying changes in CM handle properties.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PayloadFactory {

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
        newProperties.putAll(ncmpServiceCmHandle.getPublicProperties());
        payload.setNewValues(newProperties);
        return payload;
    }

    static PayloadV2 identifyChanges(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                     final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final Boolean currentDataSync = currentNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled();
        final Boolean targetDataSync = targetNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled();
        final boolean dataSyncChanged = !java.util.Objects.equals(currentDataSync, targetDataSync);

        final boolean stateChanged = currentNcmpServiceCmHandle.getCompositeState().getCmHandleState()
            != targetNcmpServiceCmHandle.getCompositeState().getCmHandleState();

        final boolean propertiesChanged = !currentNcmpServiceCmHandle.getPublicProperties()
            .equals(targetNcmpServiceCmHandle.getPublicProperties());

        final PayloadV2 payload = new PayloadV2();
        if (!dataSyncChanged && !stateChanged && !propertiesChanged) {
            return payload;
        }

        final Map<String, Object> oldProperties = new HashMap<>();
        final Map<String, Object> newProperties = new HashMap<>();

        if (dataSyncChanged) {
            oldProperties.put("dataSyncEnabled", currentDataSync);
            newProperties.put("dataSyncEnabled", targetDataSync);
        }

        if (stateChanged) {
            oldProperties.put("cmHandleState",
                currentNcmpServiceCmHandle.getCompositeState().getCmHandleState().name());
            newProperties.put("cmHandleState",
                targetNcmpServiceCmHandle.getCompositeState().getCmHandleState().name());
        }

        if (propertiesChanged) {
            final MapDifference<String, String> mapDifference = Maps.difference(
                targetNcmpServiceCmHandle.getPublicProperties(),
                currentNcmpServiceCmHandle.getPublicProperties());

            oldProperties.putAll(mapDifference.entriesOnlyOnRight());
            newProperties.putAll(mapDifference.entriesOnlyOnLeft());

            mapDifference.entriesDiffering().forEach((key, valueDifference) -> {
                oldProperties.put(key, valueDifference.rightValue());
                newProperties.put(key, valueDifference.leftValue());
            });
        }
        payload.setOldValues(oldProperties);
        payload.setNewValues(newProperties);
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
        properties.forEach((key, value) -> {
            if (!"dataSyncEnabled".equals(key) && !"cmHandleState".equals(key)) {
                publicProperties.put(key, (String) value);
            }
        });
        if (!publicProperties.isEmpty()) {
            values.setCmHandleProperties(List.of(publicProperties));
        }
        return values;
    }

}
