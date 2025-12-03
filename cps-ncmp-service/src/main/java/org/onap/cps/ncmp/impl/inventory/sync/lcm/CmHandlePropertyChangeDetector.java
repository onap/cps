/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.ncmp.events.lcm.v1.Values;

/**
 * Utility class for examining and determining changes in CM handle properties.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CmHandlePropertyChangeDetector {

    /**
     * Determines property (update) values for creating a new CM handle.
     *
     * @param ncmpServiceCmHandle the CM handle being created
     * @return CmHandlePropertyUpdates containing new values for the created CM handle
     */
    static CmHandlePropertyUpdates determineUpdatesForCreate(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final CmHandlePropertyUpdates cmHandlePropertyUpdates = new CmHandlePropertyUpdates();
        cmHandlePropertyUpdates.setNewValues(new Values());
        cmHandlePropertyUpdates.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(ncmpServiceCmHandle));
        cmHandlePropertyUpdates.getNewValues()
            .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(ncmpServiceCmHandle));
        cmHandlePropertyUpdates.getNewValues()
            .setCmHandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return cmHandlePropertyUpdates;
    }

    /**
     * Determines property updates between current and target CM handle states.
     *
     * @param currentNcmpServiceCmHandle the current CM handle state
     * @param targetNcmpServiceCmHandle  the target CM handle state
     * @return CmHandlePropertyUpdates containing old and new values for changed properties
     */
    static CmHandlePropertyUpdates determineUpdates(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                           final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final boolean hasDataSyncFlagEnabledChanged =
            hasDataSyncEnabledFlagChanged(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        final boolean hasCmHandleStateChanged =
            hasCmHandleStateChanged(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        final boolean arePublicCmHandlePropertiesEqual =
            arePublicCmHandlePropertiesEqual(currentNcmpServiceCmHandle.getPublicProperties(),
                targetNcmpServiceCmHandle.getPublicProperties()
            );

        final CmHandlePropertyUpdates cmHandlePropertyUpdates = new CmHandlePropertyUpdates();

        if (hasDataSyncFlagEnabledChanged || hasCmHandleStateChanged || (!arePublicCmHandlePropertiesEqual)) {
            cmHandlePropertyUpdates.setOldValues(new Values());
            cmHandlePropertyUpdates.setNewValues(new Values());
        } else {
            return cmHandlePropertyUpdates;
        }

        if (hasDataSyncFlagEnabledChanged) {
            setDataSyncEnabledFlag(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle, cmHandlePropertyUpdates);
        }

        if (hasCmHandleStateChanged) {
            setCmHandleStateChange(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle, cmHandlePropertyUpdates);
        }

        if (!arePublicCmHandlePropertiesEqual) {
            setPublicCmHandlePropertiesChange(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle,
                cmHandlePropertyUpdates);
        }

        return cmHandlePropertyUpdates;

    }

    private static void setDataSyncEnabledFlag(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                               final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                               final CmHandlePropertyUpdates cmHandlePropertyUpdates) {
        cmHandlePropertyUpdates.getOldValues().setDataSyncEnabled(getDataSyncEnabledFlag(currentNcmpServiceCmHandle));
        cmHandlePropertyUpdates.getNewValues().setDataSyncEnabled(getDataSyncEnabledFlag(targetNcmpServiceCmHandle));

    }

    private static void setCmHandleStateChange(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                               final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                               final CmHandlePropertyUpdates cmHandlePropertyUpdates) {
        cmHandlePropertyUpdates.getOldValues()
            .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(currentNcmpServiceCmHandle));
        cmHandlePropertyUpdates.getNewValues()
            .setCmHandleState(mapCmHandleStateToLcmEventCmHandleState(targetNcmpServiceCmHandle));
    }

    private static void setPublicCmHandlePropertiesChange(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                          final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                                          final CmHandlePropertyUpdates cmHandlePropertyUpdates) {

        final Map<String, Map<String, String>> publicCmHandlePropertiesDifference =
            getPublicCmHandlePropertiesDifference(currentNcmpServiceCmHandle.getPublicProperties(),
                targetNcmpServiceCmHandle.getPublicProperties()
            );
        cmHandlePropertyUpdates.getOldValues()
            .setCmHandleProperties(List.of(publicCmHandlePropertiesDifference.get("oldValues")));
        cmHandlePropertyUpdates.getNewValues()
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

}
