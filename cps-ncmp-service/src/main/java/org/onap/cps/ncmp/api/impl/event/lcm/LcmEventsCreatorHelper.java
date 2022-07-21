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
     * Determining the event type based on the changes.
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
        } else {
            return UPDATE;
        }
    }

    /**
     * Determine the ncmp cmhandle value difference pair.
     *
     * @param targetNcmpServiceCmHandle   target ncmpServiceCmHandle
     * @param existingNcmpServiceCmHandle existing ncmpServiceCmHandle
     * @param lcmEventType                lcm event type
     * @return Lcm Event Value difference pair
     */
    public static LcmEventsCreator.ValueDifferencePair determineEventValues(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle, final NcmpServiceCmHandle existingNcmpServiceCmHandle,
            final LcmEventType lcmEventType) {

        if (CREATE == lcmEventType) {
            return determineCreateEventValuesDifferencePair(targetNcmpServiceCmHandle);
        } else if (UPDATE == lcmEventType) {
            return determineUpdateEventValuesDifferencePair(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        } else {
            return new LcmEventsCreator.ValueDifferencePair();
        }

    }

    private static LcmEventsCreator.ValueDifferencePair determineCreateEventValuesDifferencePair(
            final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final LcmEventsCreator.ValueDifferencePair valueDifferencePair = new LcmEventsCreator.ValueDifferencePair();
        final Values newValues = new Values();
        newValues.setDataSyncEnabled(ncmpServiceCmHandle.getCompositeState().getDataSyncEnabled());
        newValues.setCmHandleState(
                Values.CmHandleState.fromValue(ncmpServiceCmHandle.getCompositeState().getCmHandleState().name()));
        newValues.setCmHandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        valueDifferencePair.setNewValues(newValues);
        return valueDifferencePair;
    }

    private static LcmEventsCreator.ValueDifferencePair determineUpdateEventValuesDifferencePair(
            final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmhandle) {

        final LcmEventsCreator.ValueDifferencePair valueDifferencePair = new LcmEventsCreator.ValueDifferencePair();
        final Values oldValues = new Values();
        final Values newValues = new Values();

        if (hasDataSyncEnabledFlagChanged(targetNcmpServiceCmHandle, existingNcmpServiceCmhandle)) {
            oldValues.setDataSyncEnabled(existingNcmpServiceCmhandle.getCompositeState().getDataSyncEnabled());
            newValues.setDataSyncEnabled(targetNcmpServiceCmHandle.getCompositeState().getDataSyncEnabled());
        }

        if (hasCmHandleStateChanged(targetNcmpServiceCmHandle, existingNcmpServiceCmhandle)) {
            oldValues.setCmHandleState(Values.CmHandleState.fromValue(
                    existingNcmpServiceCmhandle.getCompositeState().getCmHandleState().name()));
            newValues.setCmHandleState(Values.CmHandleState.fromValue(
                    targetNcmpServiceCmHandle.getCompositeState().getCmHandleState().name()));
        }

        if (!publicCmHandlePropertiesAreEqual(targetNcmpServiceCmHandle.getPublicProperties(),
                existingNcmpServiceCmhandle.getPublicProperties())) {
            final Map<String, Map<String, String>> publicCmHandlePropertiesDifference =
                    publicCmHandlePropertiesDifference(targetNcmpServiceCmHandle.getPublicProperties(),
                            existingNcmpServiceCmhandle.getPublicProperties());
            oldValues.setCmHandleProperties(List.of(publicCmHandlePropertiesDifference.get("oldValues")));
            newValues.setCmHandleProperties(List.of(publicCmHandlePropertiesDifference.get("newValues")));

        }

        valueDifferencePair.setOldValues(oldValues);
        valueDifferencePair.setNewValues(newValues);

        return valueDifferencePair;

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
        final Map<String, Map<String, String>> differenceMap = new HashMap<>(2);
        final Map<String, String> oldValues = new HashMap<>();
        final Map<String, String> newValues = new HashMap<>();

        final MapDifference<String, String> difference =
                Maps.difference(targetCmHandleProperties, existingCmHandleProperties);

        difference.entriesDiffering().keySet().stream().forEach(key -> {
            oldValues.put(key, existingCmHandleProperties.get(key));
            newValues.put(key, targetCmHandleProperties.get(key));
        });

        differenceMap.put("oldValues", oldValues);
        differenceMap.put("newValues", newValues);

        return differenceMap;
    }

}
