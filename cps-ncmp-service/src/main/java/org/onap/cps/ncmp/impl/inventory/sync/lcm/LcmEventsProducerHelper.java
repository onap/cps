/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.DELETED;
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.CREATE;
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.DELETE;
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.UPDATE;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.v1.Event;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.events.lcm.v1.LcmEventHeader;
import org.onap.cps.ncmp.events.lcm.v1.Values;
import org.onap.cps.ncmp.impl.utils.EventDateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * LcmEventsProducerHelper to create LcmEvent based on relevant operation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LcmEventsProducerHelper {

    private final LcmEventHeaderMapper lcmEventHeaderMapper;

    /**
     * Create Lifecycle Management Event.
     *
     * @param cmHandleId                  cm handle identifier
     * @param currentNcmpServiceCmHandle  current ncmp service cmhandle
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @return Populated LcmEvent
     */
    public LcmEvent createLcmEvent(final String cmHandleId,
                                   final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                   final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
            determineEventType(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        final LcmEvent lcmEvent = createLcmEventWithHeaderDetails(cmHandleId, lcmEventType);
        final Event event = new Event();
        event.setCmHandleId(cmHandleId);
        event.setAlternateId(targetNcmpServiceCmHandle.getAlternateId());
        event.setModuleSetTag(targetNcmpServiceCmHandle.getModuleSetTag());
        event.setDataProducerIdentifier(targetNcmpServiceCmHandle.getDataProducerIdentifier());
        final CmHandleValuesHolder cmHandleValuesHolder =
            determineEventValues(lcmEventType, currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        event.setOldValues(cmHandleValuesHolder.getOldValues());
        event.setNewValues(cmHandleValuesHolder.getNewValues());
        lcmEvent.setEvent(event);
        return lcmEvent;
    }

    /**
     * Create Lifecycle Management Event Header.
     *
     * @param cmHandleId                 cm handle identifier
     * @param currentNcmpServiceCmHandle current ncmp service cmhandle
     * @param targetNcmpServiceCmHandle  target ncmp service cmhandle
     * @return Populated LcmEventHeader
     */
    public LcmEventHeader createLcmEventHeader(final String cmHandleId,
                                               final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                               final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
                determineEventType(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        final LcmEvent lcmEventWithHeaderDetails = createLcmEventWithHeaderDetails(cmHandleId, lcmEventType);
        return lcmEventHeaderMapper.toLcmEventHeader(lcmEventWithHeaderDetails);
    }

    private static LcmEventType determineEventType(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                   final NcmpServiceCmHandle targetNcmpServiceCmHandle) {

        if (currentNcmpServiceCmHandle.getCompositeState() == null) {
            return CREATE;
        } else if (targetNcmpServiceCmHandle.getCompositeState().getCmHandleState() == DELETED) {
            return DELETE;
        }
        return UPDATE;
    }

    private static CmHandleValuesHolder determineEventValues(final LcmEventType lcmEventType,
                                                             final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                             final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        if (CREATE == lcmEventType) {
            return determineCreateEventValues(targetNcmpServiceCmHandle);
        } else if (UPDATE == lcmEventType) {
            return determineUpdateEventValues(targetNcmpServiceCmHandle, currentNcmpServiceCmHandle);
        }
        return new CmHandleValuesHolder();

    }

    private LcmEvent createLcmEventWithHeaderDetails(final String eventCorrelationId, final LcmEventType lcmEventType) {
        final LcmEvent lcmEvent = new LcmEvent();
        lcmEvent.setEventId(UUID.randomUUID().toString());
        lcmEvent.setEventCorrelationId(eventCorrelationId);
        lcmEvent.setEventTime(EventDateTimeFormatter.getCurrentIsoFormattedDateTime());
        lcmEvent.setEventSource("org.onap.ncmp");
        lcmEvent.setEventType(lcmEventType.getEventType());
        lcmEvent.setEventSchema("org.onap.ncmp:cmhandle-lcm-event");
        lcmEvent.setEventSchemaVersion("1.0");
        return lcmEvent;
    }


    private static CmHandleValuesHolder determineCreateEventValues(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final CmHandleValuesHolder cmHandleValuesHolder = new CmHandleValuesHolder();
        cmHandleValuesHolder.setNewValues(new Values());
        cmHandleValuesHolder.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(ncmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(ncmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues().setCmHandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return cmHandleValuesHolder;
    }

    private static CmHandleValuesHolder determineUpdateEventValues(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                                final NcmpServiceCmHandle currentNcmpServiceCmHandle) {
        final boolean hasDataSyncFlagEnabledChanged =
                hasDataSyncEnabledFlagChanged(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        final boolean hasCmHandleStateChanged =
                hasCmHandleStateChanged(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        final boolean arePublicCmHandlePropertiesEqual =
                arePublicCmHandlePropertiesEqual(currentNcmpServiceCmHandle.getPublicProperties(),
                    targetNcmpServiceCmHandle.getPublicProperties()
                );

        final CmHandleValuesHolder cmHandleValuesHolder = new CmHandleValuesHolder();

        if (hasDataSyncFlagEnabledChanged || hasCmHandleStateChanged || (!arePublicCmHandlePropertiesEqual)) {
            cmHandleValuesHolder.setOldValues(new Values());
            cmHandleValuesHolder.setNewValues(new Values());
        } else {
            return cmHandleValuesHolder;
        }

        if (hasDataSyncFlagEnabledChanged) {
            setDataSyncEnabledFlag(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle, cmHandleValuesHolder);
        }

        if (hasCmHandleStateChanged) {
            setCmHandleStateChange(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle, cmHandleValuesHolder);
        }

        if (!arePublicCmHandlePropertiesEqual) {
            setPublicCmHandlePropertiesChange(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle,
                cmHandleValuesHolder);
        }

        return cmHandleValuesHolder;

    }

    private static void setDataSyncEnabledFlag(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                               final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                               final CmHandleValuesHolder cmHandleValuesHolder) {
        cmHandleValuesHolder.getOldValues().setDataSyncEnabled(getDataSyncEnabledFlag(currentNcmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(targetNcmpServiceCmHandle));

    }

    private static void setCmHandleStateChange(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                               final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                               final CmHandleValuesHolder cmHandleValuesHolder) {
        cmHandleValuesHolder.getOldValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(currentNcmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(targetNcmpServiceCmHandle));
    }

    private static void setPublicCmHandlePropertiesChange(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                          final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                          final CmHandleValuesHolder cmHandleValuesHolder) {

        final Map<String, Map<String, String>> publicCmHandlePropertiesDifference =
                getPublicCmHandlePropertiesDifference(currentNcmpServiceCmHandle.getPublicProperties(),
                    targetNcmpServiceCmHandle.getPublicProperties()
                );
        cmHandleValuesHolder.getOldValues()
                .setCmHandleProperties(List.of(publicCmHandlePropertiesDifference.get("oldValues")));
        cmHandleValuesHolder.getNewValues()
                .setCmHandleProperties(List.of(publicCmHandlePropertiesDifference.get("newValues")));

    }

    private static Values.CmHandleState mapCmHandleStateToLcmEventCmHandleState(
            final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return Values.CmHandleState.fromValue(ncmpServiceCmHandle.getCompositeState().getCmHandleState().name());
    }

    private static Boolean getDataSyncEnabledFlag(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return ncmpServiceCmHandle.getCompositeState().getDataSyncEnabled();
    }

    private static boolean hasDataSyncEnabledFlagChanged(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                         final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final Boolean currentDataSyncFlag = currentNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled();
        final Boolean targetDataSyncFlag = targetNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled();

        if (targetDataSyncFlag == null) {
            return currentDataSyncFlag != null;
        }

        return !targetDataSyncFlag.equals(currentDataSyncFlag);
    }

    private static boolean hasCmHandleStateChanged(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                   final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        return targetNcmpServiceCmHandle.getCompositeState().getCmHandleState()
                != currentNcmpServiceCmHandle.getCompositeState().getCmHandleState();
    }

    private static boolean arePublicCmHandlePropertiesEqual(final Map<String, String> currentCmHandleProperties,
                                                            final Map<String, String> targetCmHandleProperties) {
        if (targetCmHandleProperties.size() != currentCmHandleProperties.size()) {
            return false;
        }
        return targetCmHandleProperties.equals(currentCmHandleProperties);
    }

    private static Map<String, Map<String, String>> getPublicCmHandlePropertiesDifference(
           final Map<String, String> currentCmHandleProperties,
           final Map<String, String> targetCmHandleProperties) {
        final Map<String, Map<String, String>> oldAndNewPropertiesDifferenceMap = new HashMap<>(2);

        final MapDifference<String, String> cmHandlePropertiesDifference =
                Maps.difference(targetCmHandleProperties, currentCmHandleProperties);

        final Map<String, String> oldValues = new HashMap<>(cmHandlePropertiesDifference.entriesOnlyOnRight());
        final Map<String, String> newValues = new HashMap<>(cmHandlePropertiesDifference.entriesOnlyOnLeft());

        cmHandlePropertiesDifference.entriesDiffering().keySet().forEach(cmHandlePropertyName -> {
            oldValues.put(cmHandlePropertyName, currentCmHandleProperties.get(cmHandlePropertyName));
            newValues.put(cmHandlePropertyName, targetCmHandleProperties.get(cmHandlePropertyName));
        });

        oldAndNewPropertiesDifferenceMap.put("oldValues", oldValues);
        oldAndNewPropertiesDifferenceMap.put("newValues", newValues);

        return oldAndNewPropertiesDifferenceMap;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    static class CmHandleValuesHolder {
        private Values oldValues;
        private Values newValues;
    }
}
