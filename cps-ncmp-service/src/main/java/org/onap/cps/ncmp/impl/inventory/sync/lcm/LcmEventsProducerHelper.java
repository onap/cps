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
     * Populate Lifecycle Management Event.
     *
     * @param cmHandleId                  cm handle identifier
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @param existingNcmpServiceCmHandle existing ncmp service cmhandle
     * @return Populated LcmEvent
     */
    public LcmEvent populateLcmEvent(final String cmHandleId, final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        return createLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
    }

    /**
     * Populate Lifecycle Management Event Version 2.
     *
     * @param cmHandleId                  cm handle identifier
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @param existingNcmpServiceCmHandle existing ncmp service cmhandle
     * @return Populated LcmEvent
     */
    public org.onap.cps.ncmp.events.lcm.v2.LcmEvent populateLcmEventV2(final String cmHandleId,
                                     final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                     final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        return createLcmEventV2(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
    }

    /**
     * Populate Lifecycle Management Event Header.
     *
     * @param cmHandleId                  cm handle identifier
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @param existingNcmpServiceCmHandle existing ncmp service cmhandle
     * @return Populated LcmEventHeader
     */
    public LcmEventHeader populateLcmEventHeader(final String cmHandleId,
            final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        return createLcmEventHeader(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
    }

    /**
     * Populate Lifecycle Management Event Header Version 2.
     *
     * @param cmHandleId                  cm handle identifier
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @param existingNcmpServiceCmHandle existing ncmp service cmhandle
     * @return Populated LcmEventHeader
     */
    public org.onap.cps.ncmp.events.lcm.v2.LcmEventHeader populateLcmEventHeaderV2(final String cmHandleId,
                                                   final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                   final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        return createLcmEventHeaderV2(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
    }

    private LcmEvent createLcmEvent(final String cmHandleId, final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
                determineEventType(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final LcmEvent lcmEvent = lcmEventHeader(cmHandleId, lcmEventType);
        lcmEvent.setEvent(
                lcmEventPayload(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle, lcmEventType));
        return lcmEvent;
    }

    private org.onap.cps.ncmp.events.lcm.v2.LcmEvent createLcmEventV2(final String cmHandleId,
                                    final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                    final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
            determineEventType(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final org.onap.cps.ncmp.events.lcm.v2.LcmEvent lcmEvent = lcmEventHeaderV2(cmHandleId, lcmEventType);
        lcmEvent.setEvent(
            lcmEventPayloadV2(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle, lcmEventType));
        return lcmEvent;
    }

    private LcmEventHeader createLcmEventHeader(final String cmHandleId,
            final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
                determineEventType(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final LcmEvent lcmEventWithHeaderInformation = lcmEventHeader(cmHandleId, lcmEventType);
        return lcmEventHeaderMapper.toLcmEventHeader(lcmEventWithHeaderInformation);
    }

    private org.onap.cps.ncmp.events.lcm.v2.LcmEventHeader createLcmEventHeaderV2(final String cmHandleId,
                                                final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
            determineEventType(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final org.onap.cps.ncmp.events.lcm.v2.LcmEvent lcmEventWithHeaderInformation
            = lcmEventHeaderV2(cmHandleId, lcmEventType);
        return lcmEventHeaderMapper.toLcmEventHeaderV2(lcmEventWithHeaderInformation);
    }

    private static LcmEventType determineEventType(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                   final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        if (existingNcmpServiceCmHandle.getCompositeState() == null) {
            return CREATE;
        } else if (targetNcmpServiceCmHandle.getCompositeState().getCmHandleState() == DELETED) {
            return DELETE;
        }
        return UPDATE;
    }

    private static CmHandleValuesHolder determineEventValues(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle, final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventType lcmEventType) {

        if (CREATE == lcmEventType) {
            return determineCreateEventValues(targetNcmpServiceCmHandle);
        } else if (UPDATE == lcmEventType) {
            return determineUpdateEventValues(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        }
        return new CmHandleValuesHolder();

    }

    private Event lcmEventPayload(final String eventCorrelationId, final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle, final LcmEventType lcmEventType) {
        final Event event = new Event();
        event.setCmHandleId(eventCorrelationId);
        event.setAlternateId(targetNcmpServiceCmHandle.getAlternateId());
        event.setModuleSetTag(targetNcmpServiceCmHandle.getModuleSetTag());
        event.setDataProducerIdentifier(targetNcmpServiceCmHandle.getDataProducerIdentifier());
        final CmHandleValuesHolder cmHandleValuesHolder =
                determineEventValues(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle,
                        lcmEventType);
        event.setOldValues(cmHandleValuesHolder.getOldValues());
        event.setNewValues(cmHandleValuesHolder.getNewValues());

        return event;
    }
    
    private org.onap.cps.ncmp.events.lcm.v2.Event lcmEventPayloadV2(final String eventCorrelationId,
                                  final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                  final NcmpServiceCmHandle existingNcmpServiceCmHandle,
                                  final LcmEventType lcmEventType) {
        final org.onap.cps.ncmp.events.lcm.v2.Event event = new org.onap.cps.ncmp.events.lcm.v2.Event();
        event.setCmHandleId(eventCorrelationId);
        //TODO Populate event V2 schema
        return event;
    }

    private LcmEvent lcmEventHeader(final String eventCorrelationId, final LcmEventType lcmEventType) {
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

    private org.onap.cps.ncmp.events.lcm.v2.LcmEvent lcmEventHeaderV2(final String eventCorrelationId,
                                                                    final LcmEventType lcmEventType) {
        final org.onap.cps.ncmp.events.lcm.v2.LcmEvent lcmEvent = new org.onap.cps.ncmp.events.lcm.v2.LcmEvent();
        lcmEvent.setEventId(UUID.randomUUID().toString());
        lcmEvent.setEventCorrelationId(eventCorrelationId);
        lcmEvent.setEventTime(EventDateTimeFormatter.getCurrentIsoFormattedDateTime());
        lcmEvent.setEventSource("org.onap.ncmp");
        lcmEvent.setEventType(lcmEventType.getEventType());
        lcmEvent.setEventSchema("org.onap.ncmp:cmhandle-lcm-event");
        lcmEvent.setEventSchemaVersion("1.0");
        return lcmEvent;
    }


    private static CmHandleValuesHolder determineCreateEventValues(
            final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final CmHandleValuesHolder cmHandleValuesHolder = new CmHandleValuesHolder();
        cmHandleValuesHolder.setNewValues(new Values());
        cmHandleValuesHolder.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(ncmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(ncmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues().setCmHandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return cmHandleValuesHolder;
    }

    private static CmHandleValuesHolder determineUpdateEventValues(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        final boolean hasDataSyncFlagEnabledChanged =
                hasDataSyncEnabledFlagChanged(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final boolean hasCmHandleStateChanged =
                hasCmHandleStateChanged(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final boolean arePublicCmHandlePropertiesEqual =
                arePublicCmHandlePropertiesEqual(targetNcmpServiceCmHandle.getPublicProperties(),
                        existingNcmpServiceCmHandle.getPublicProperties());

        final CmHandleValuesHolder cmHandleValuesHolder = new CmHandleValuesHolder();

        if (hasDataSyncFlagEnabledChanged || hasCmHandleStateChanged || (!arePublicCmHandlePropertiesEqual)) {
            cmHandleValuesHolder.setOldValues(new Values());
            cmHandleValuesHolder.setNewValues(new Values());
        } else {
            return cmHandleValuesHolder;
        }

        if (hasDataSyncFlagEnabledChanged) {
            setDataSyncEnabledFlag(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle, cmHandleValuesHolder);
        }

        if (hasCmHandleStateChanged) {
            setCmHandleStateChange(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle, cmHandleValuesHolder);
        }

        if (!arePublicCmHandlePropertiesEqual) {
            setPublicCmHandlePropertiesChange(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle,
                    cmHandleValuesHolder);
        }

        return cmHandleValuesHolder;

    }

    private static void setDataSyncEnabledFlag(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                               final NcmpServiceCmHandle existingNcmpServiceCmHandle,
                                               final CmHandleValuesHolder cmHandleValuesHolder) {

        cmHandleValuesHolder.getOldValues().setDataSyncEnabled(getDataSyncEnabledFlag(existingNcmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(targetNcmpServiceCmHandle));

    }

    private static void setCmHandleStateChange(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                               final NcmpServiceCmHandle existingNcmpServiceCmHandle,
                                               final CmHandleValuesHolder cmHandleValuesHolder) {
        cmHandleValuesHolder.getOldValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(existingNcmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(targetNcmpServiceCmHandle));
    }

    private static void setPublicCmHandlePropertiesChange(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                          final NcmpServiceCmHandle existingNcmpServiceCmHandle,
                                                          final CmHandleValuesHolder cmHandleValuesHolder) {

        final Map<String, Map<String, String>> publicCmHandlePropertiesDifference =
                getPublicCmHandlePropertiesDifference(targetNcmpServiceCmHandle.getPublicProperties(),
                        existingNcmpServiceCmHandle.getPublicProperties());
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

    private static boolean hasDataSyncEnabledFlagChanged(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                         final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        final Boolean targetDataSyncFlag = targetNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled();
        final Boolean existingDataSyncFlag = existingNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled();

        if (targetDataSyncFlag == null) {
            return existingDataSyncFlag != null;
        }

        return !targetDataSyncFlag.equals(existingDataSyncFlag);
    }

    private static boolean hasCmHandleStateChanged(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                   final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        return targetNcmpServiceCmHandle.getCompositeState().getCmHandleState()
                != existingNcmpServiceCmHandle.getCompositeState().getCmHandleState();
    }

    private static boolean arePublicCmHandlePropertiesEqual(final Map<String, String> targetCmHandleProperties,
                                                            final Map<String, String> existingCmHandleProperties) {
        if (targetCmHandleProperties.size() != existingCmHandleProperties.size()) {
            return false;
        }

        return targetCmHandleProperties.equals(existingCmHandleProperties);
    }

    private static Map<String, Map<String, String>> getPublicCmHandlePropertiesDifference(
            final Map<String, String> targetCmHandleProperties, final Map<String, String> existingCmHandleProperties) {
        final Map<String, Map<String, String>> oldAndNewPropertiesDifferenceMap = new HashMap<>(2);

        final MapDifference<String, String> cmHandlePropertiesDifference =
                Maps.difference(targetCmHandleProperties, existingCmHandleProperties);

        final Map<String, String> newValues = new HashMap<>(cmHandlePropertiesDifference.entriesOnlyOnLeft());
        final Map<String, String> oldValues = new HashMap<>(cmHandlePropertiesDifference.entriesOnlyOnRight());

        cmHandlePropertiesDifference.entriesDiffering().keySet().forEach(cmHandlePropertyName -> {
            oldValues.put(cmHandlePropertyName, existingCmHandleProperties.get(cmHandlePropertyName));
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
