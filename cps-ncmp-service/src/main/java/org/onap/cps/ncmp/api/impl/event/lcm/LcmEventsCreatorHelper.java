/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.event.lcm;

import static org.onap.cps.ncmp.api.impl.event.lcm.LcmEventType.CREATE;
import static org.onap.cps.ncmp.api.impl.event.lcm.LcmEventType.DELETE;
import static org.onap.cps.ncmp.api.impl.event.lcm.LcmEventType.UPDATE;
import static org.onap.cps.ncmp.api.inventory.CmHandleState.DELETED;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.ncmp.cmhandle.event.lcm.Values;

/**
 * LcmEventsCreatorHelper has helper methods to create LcmEvent.
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
    public static LcmEventsCreator.CmHandleValueDifferencePair determineEventValues(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle, final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventType lcmEventType) {

        if (CREATE == lcmEventType) {
            return determineCreateEventValuesDifferencePair(targetNcmpServiceCmHandle);
        } else if (UPDATE == lcmEventType) {
            return determineUpdateEventValuesDifferencePair(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        }
        return new LcmEventsCreator.CmHandleValueDifferencePair();

    }

    private static LcmEventsCreator.CmHandleValueDifferencePair determineCreateEventValuesDifferencePair(
            final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final LcmEventsCreator.CmHandleValueDifferencePair cmHandleValueDifferencePair =
                new LcmEventsCreator.CmHandleValueDifferencePair();
        cmHandleValueDifferencePair.setNewValues(new Values());
        cmHandleValueDifferencePair.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(ncmpServiceCmHandle));
        cmHandleValueDifferencePair.getNewValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(ncmpServiceCmHandle));
        cmHandleValueDifferencePair.getNewValues()
                .setCmHandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return cmHandleValueDifferencePair;
    }

    private static LcmEventsCreator.CmHandleValueDifferencePair determineUpdateEventValuesDifferencePair(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        final LcmEventsCreator.CmHandleValueDifferencePair cmHandleValueDifferencePair =
                new LcmEventsCreator.CmHandleValueDifferencePair();
        cmHandleValueDifferencePair.setOldValues(new Values());
        cmHandleValueDifferencePair.setNewValues(new Values());

        if (hasDataSyncEnabledFlagChanged(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)) {
            setDataSyncEnabledFlagUsingTargetAndExistingCmHandle(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle,
                    cmHandleValueDifferencePair);
        }

        if (hasCmHandleStateChanged(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)) {
            setCmHandleStateChangeUsingTargetAndExistingCmHandle(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle,
                    cmHandleValueDifferencePair);
        }

        if (!publicCmHandlePropertiesAreEqual(targetNcmpServiceCmHandle.getPublicProperties(),
                existingNcmpServiceCmHandle.getPublicProperties())) {
            setPublicCmHandlePropertiesChangeUsingTargetAndExistingCmHandle(targetNcmpServiceCmHandle,
                    existingNcmpServiceCmHandle, cmHandleValueDifferencePair);

        }

        return cmHandleValueDifferencePair;

    }

    private static void setDataSyncEnabledFlagUsingTargetAndExistingCmHandle(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle, final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventsCreator.CmHandleValueDifferencePair cmHandleValueDifferencePair) {

        cmHandleValueDifferencePair.getOldValues()
                .setDataSyncEnabled(getDataSyncEnabledFlag(existingNcmpServiceCmHandle));
        cmHandleValueDifferencePair.getNewValues()
                .setDataSyncEnabled(getDataSyncEnabledFlag(targetNcmpServiceCmHandle));

    }

    private static void setCmHandleStateChangeUsingTargetAndExistingCmHandle(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle, final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventsCreator.CmHandleValueDifferencePair cmHandleValueDifferencePair) {
        cmHandleValueDifferencePair.getOldValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(existingNcmpServiceCmHandle));
        cmHandleValueDifferencePair.getNewValues()
                .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(targetNcmpServiceCmHandle));
    }

    private static void setPublicCmHandlePropertiesChangeUsingTargetAndExistingCmHandle(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle, final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventsCreator.CmHandleValueDifferencePair cmHandleValueDifferencePair) {

        final Map<String, Map<String, String>> publicCmHandlePropertiesDifference =
                publicCmHandlePropertiesDifference(targetNcmpServiceCmHandle.getPublicProperties(),
                        existingNcmpServiceCmHandle.getPublicProperties());
        cmHandleValueDifferencePair.getOldValues()
                .setCmHandleProperties(List.of(publicCmHandlePropertiesDifference.get("oldValues")));
        cmHandleValueDifferencePair.getNewValues()
                .setCmHandleProperties(List.of(publicCmHandlePropertiesDifference.get("newValues")));

    }

    private static Values.CmHandleState mapCmHandleStateToLcmEventCmHandleState(
            final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return Values.CmHandleState.fromValue(ncmpServiceCmHandle.getCompositeState().getCmHandleState().name());
    }

    private static boolean getDataSyncEnabledFlag(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return ncmpServiceCmHandle.getCompositeState().getDataSyncEnabled();
    }

    private static boolean hasDataSyncEnabledFlagChanged(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        return !targetNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled()
                .equals(existingNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled());
    }

    private static boolean hasCmHandleStateChanged(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {

        return targetNcmpServiceCmHandle.getCompositeState().getCmHandleState()
                       != existingNcmpServiceCmHandle.getCompositeState().getCmHandleState();
    }

    private static boolean publicCmHandlePropertiesAreEqual(final Map<String, String> targetCmHandleProperties,
            final Map<String, String> existingCmHandleProperties) {
        if (targetCmHandleProperties.size() != existingCmHandleProperties.size()) {
            return false;
        }

        return targetCmHandleProperties.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(existingCmHandleProperties.get(entry.getKey())));
    }

    private static Map<String, Map<String, String>> publicCmHandlePropertiesDifference(
            final Map<String, String> targetCmHandleProperties, final Map<String, String> existingCmHandleProperties) {
        final Map<String, Map<String, String>> oldAndNewPropertiesDifferenceMap = new HashMap<>(2);

        final MapDifference<String, String> cmHandlePropertiesDifference =
                Maps.difference(targetCmHandleProperties, existingCmHandleProperties);

        final Map<String, String> newValues = new HashMap<>(cmHandlePropertiesDifference.entriesOnlyOnLeft());
        final Map<String, String> oldValues = new HashMap<>(cmHandlePropertiesDifference.entriesOnlyOnRight());

        cmHandlePropertiesDifference.entriesDiffering().keySet().stream().forEach(cmHandlePropertyName -> {
            oldValues.put(cmHandlePropertyName, existingCmHandleProperties.get(cmHandlePropertyName));
            newValues.put(cmHandlePropertyName, targetCmHandleProperties.get(cmHandlePropertyName));
        });

        oldAndNewPropertiesDifferenceMap.put("oldValues", oldValues);
        oldAndNewPropertiesDifferenceMap.put("newValues", newValues);

        return oldAndNewPropertiesDifferenceMap;
    }

}
