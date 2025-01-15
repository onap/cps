/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.v1.Values;

/**
 * LcmEventsCreatorHelper has helper methods to create LcmEvent.
 * Determine the lcm event type i.e create,update and delete.
 * Based on lcm event type create the LcmEvent payload.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LcmEventsCreatorHelper {

    /**
     * Determining the event type based on the composite state.
     *
     * @param targetNcmpServiceCmHandle   target ncmpServiceCmHandle
     * @param existingNcmpServiceCmHandle existing ncmpServiceCmHandle
     * @return Event Type
     */
    public static LcmEventType determineEventType(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        if (existingNcmpServiceCmHandle.getCompositeState() == null) {
            return CREATE;
        } else if (targetNcmpServiceCmHandle.getCompositeState().getCmHandleState() == DELETED) {
            return DELETE;
        }
        return UPDATE;
    }

    /**
     * Determine the cmhandle value difference pair.Contains the difference in the form of oldValues and newValues.
     *
     * @param targetNcmpServiceCmHandle   target ncmpServiceCmHandle
     * @param existingNcmpServiceCmHandle existing ncmpServiceCmHandle
     * @param lcmEventType                lcm event type
     * @return Lcm Event Value difference pair
     */
    public static LcmEventsCreator.CmHandleValuesHolder determineEventValues(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle, final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventType lcmEventType) {

        if (CREATE == lcmEventType) {
            return determineCreateEventValues(targetNcmpServiceCmHandle);
        } else if (UPDATE == lcmEventType) {
            return determineUpdateEventValues(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        }
        return new LcmEventsCreator.CmHandleValuesHolder();

    }

    private static LcmEventsCreator.CmHandleValuesHolder determineCreateEventValues(
            final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final LcmEventsCreator.CmHandleValuesHolder cmHandleValuesHolder = new LcmEventsCreator.CmHandleValuesHolder();
        cmHandleValuesHolder.setNewValues(new Values());
        cmHandleValuesHolder.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(ncmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(ncmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues().setCmHandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return cmHandleValuesHolder;
    }

    private static LcmEventsCreator.CmHandleValuesHolder determineUpdateEventValues(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        final boolean hasDataSyncFlagEnabledChanged =
                hasDataSyncEnabledFlagChanged(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final boolean hasCmHandleStateChanged =
                hasCmHandleStateChanged(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final boolean arePublicCmHandlePropertiesEqual =
                arePublicCmHandlePropertiesEqual(targetNcmpServiceCmHandle.getPublicProperties(),
                        existingNcmpServiceCmHandle.getPublicProperties());

        final LcmEventsCreator.CmHandleValuesHolder cmHandleValuesHolder = new LcmEventsCreator.CmHandleValuesHolder();

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
            final LcmEventsCreator.CmHandleValuesHolder cmHandleValuesHolder) {

        cmHandleValuesHolder.getOldValues().setDataSyncEnabled(getDataSyncEnabledFlag(existingNcmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(targetNcmpServiceCmHandle));

    }

    private static void setCmHandleStateChange(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventsCreator.CmHandleValuesHolder cmHandleValuesHolder) {
        cmHandleValuesHolder.getOldValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(existingNcmpServiceCmHandle));
        cmHandleValuesHolder.getNewValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(targetNcmpServiceCmHandle));
    }

    private static void setPublicCmHandlePropertiesChange(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventsCreator.CmHandleValuesHolder cmHandleValuesHolder) {

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

}
