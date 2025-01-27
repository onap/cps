/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.impl.inventory.sync;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.LockReasonCategory;
import org.onap.cps.ncmp.impl.data.DmiDataOperations;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleOperationsUtils {

    private final CmHandleQueryService cmHandleQueryService;
    private final DmiDataOperations dmiDataOperations;
    private final JsonObjectMapper jsonObjectMapper;
    private static final String RETRY_ATTEMPT_KEY = "attempt";
    private static final String MODULE_SET_TAG_KEY = "moduleSetTag";
    public static final String MODULE_SET_TAG_MESSAGE_FORMAT = "Upgrade to ModuleSetTag: %s";
    private static final String LOCK_REASON_DETAILS_MSG_FORMAT = " Attempt #%d failed: %s";
    private static final Pattern retryAttemptPattern = Pattern.compile("Attempt #(\\d+) failed:.+");
    private static final Pattern moduleSetTagPattern = Pattern.compile("Upgrade to ModuleSetTag: (\\S+)");
    private static final String CPS_PATH_CM_HANDLES_MODEL_SYNC_FAILED_OR_UPGRADE = """
            //lock-reason[@reason="MODULE_SYNC_FAILED"
            or @reason="MODULE_UPGRADE"
            or @reason="MODULE_UPGRADE_FAILED"]""";

    /**
     * Query data nodes for cm handles with an "ADVISED" cm handle state.
     *
     * @return cm handle ids in ADVISED state (empty list if none found)
     */
    public Collection<String> getAdvisedCmHandleIds() {
        return cmHandleQueryService.queryCmHandleIdsByState(CmHandleState.ADVISED);
    }

    /**
     * First query data nodes for cm handles with CM Handle Operational Sync State in "UNSYNCHRONIZED" and
     * randomly select a CM Handle and query the data nodes for CM Handle State in "READY".
     *
     * @return a randomized yang model cm handle list with State in READY and Operation Sync State in "UNSYNCHRONIZED",
     *         return empty list if not found
     */
    public List<YangModelCmHandle> getUnsynchronizedReadyCmHandles() {
        final Collection<DataNode> unsynchronizedCmHandles = cmHandleQueryService
                .queryCmHandlesByOperationalSyncState(DataStoreSyncState.UNSYNCHRONIZED);

        final List<YangModelCmHandle> yangModelCmHandles = new ArrayList<>();
        for (final DataNode unsynchronizedCmHandle : unsynchronizedCmHandles) {
            final String cmHandleId = unsynchronizedCmHandle.getLeaves().get("id").toString();
            if (cmHandleQueryService.cmHandleHasState(cmHandleId, CmHandleState.READY)) {
                yangModelCmHandles.addAll(convertCmHandlesDataNodesToYangModelCmHandles(
                                Collections.singletonList(unsynchronizedCmHandle)));
            }
        }
        Collections.shuffle(yangModelCmHandles);
        return yangModelCmHandles;
    }

    /**
     * Query data nodes for cm handles with an "LOCKED" cm handle state with reason.
     *
     * @return a random LOCKED yang model cm handle, return null if not found
     */
    public Collection<YangModelCmHandle> getCmHandlesThatFailedModelSyncOrUpgrade() {
        final Collection<DataNode> lockedCmHandlesAsDataNodeList
                = cmHandleQueryService.queryCmHandleAncestorsByCpsPath(CPS_PATH_CM_HANDLES_MODEL_SYNC_FAILED_OR_UPGRADE,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        return convertCmHandlesDataNodesToYangModelCmHandles(lockedCmHandlesAsDataNodeList);
    }

    /**
     * Updates the lock reason message and attempt counter for the provided CompositeState.
     * This method increments the attempt counter and updates the lock reason message,
     * including the module set tag if available.
     *
     * @param compositeState     the composite state of the CM handle
     * @param lockReasonCategory the lock reason category for the CM handle
     * @param errorMessage       the error message to include in the lock reason message
     */
    public void updateLockReasonWithAttempts(final CompositeState compositeState,
                                             final LockReasonCategory lockReasonCategory,
                                             final String errorMessage) {
        final Map<String, String> lockedStateDetails = getLockedCompositeStateDetails(compositeState.getLockReason());
        final int nextAttemptCount = calculateNextAttemptCount(lockedStateDetails);
        final String moduleSetTag = lockedStateDetails.getOrDefault(MODULE_SET_TAG_KEY, "");

        final String lockReasonMessage = buildLockReasonDetails(moduleSetTag, nextAttemptCount, errorMessage);

        compositeState.setLockReason(CompositeState.LockReason.builder()
                .details(lockReasonMessage)
                .lockReasonCategory(lockReasonCategory)
                .build());
    }

    /**
     * Extract lock reason details as key-value pair.
     *
     * @param compositeStateLockReason lock reason having all the details
     * @return a map of lock reason details
     */
    public static Map<String, String> getLockedCompositeStateDetails(final CompositeState.LockReason
                                                                             compositeStateLockReason) {
        if (compositeStateLockReason != null) {
            final Map<String, String> compositeStateDetails = new HashMap<>(2);
            final String lockedCompositeStateReasonDetails = compositeStateLockReason.getDetails();
            final Matcher retryAttemptMatcher = retryAttemptPattern.matcher(lockedCompositeStateReasonDetails);
            if (retryAttemptMatcher.find()) {
                final int attemptsRegexGroupId = 1;
                compositeStateDetails.put(RETRY_ATTEMPT_KEY, retryAttemptMatcher.group(attemptsRegexGroupId));
            }
            final Matcher moduleSetTagMatcher = moduleSetTagPattern.matcher(lockedCompositeStateReasonDetails);
            if (moduleSetTagMatcher.find()) {
                final int moduleSetTagRegexGroupId = 1;
                compositeStateDetails.put(MODULE_SET_TAG_KEY, moduleSetTagMatcher.group(moduleSetTagRegexGroupId));
            }
            return compositeStateDetails;
        }
        return Collections.emptyMap();
    }

    /**
     * Get the Resource Data from Node through DMI Passthrough service.
     *
     * @param cmHandleId cm handle id
     * @return optional string containing the resource data
     */
    public String getResourceData(final String cmHandleId) {
        final ResponseEntity<Object> resourceDataResponseEntity = dmiDataOperations.getAllResourceDataFromDmi(
                cmHandleId, UUID.randomUUID().toString());
        if (resourceDataResponseEntity.getStatusCode().is2xxSuccessful()) {
            return getFirstResource(resourceDataResponseEntity.getBody());
        }
        return null;
    }

    /**
     * Checks if cm handle state module is in upgrade or upgrade failed.
     *
     * @param compositeState current lock reason of  cm handle
     * @return true or false based on lock reason category
     */
    public static boolean inUpgradeOrUpgradeFailed(final CompositeState compositeState) {
        return compositeState.getLockReason() != null
                && (LockReasonCategory.MODULE_UPGRADE.equals(compositeState.getLockReason().getLockReasonCategory())
                || LockReasonCategory.MODULE_UPGRADE_FAILED.equals(compositeState.getLockReason()
                .getLockReasonCategory()));
    }

    public static String getTargetModuleSetTagForUpgrade(final YangModelCmHandle yangModelCmHandle) {
        final CompositeState.LockReason lockReason = yangModelCmHandle.getCompositeState().getLockReason();
        return getTargetModuleSetTagFromLockReason(lockReason);
    }

    private static String getTargetModuleSetTagFromLockReason(final CompositeState.LockReason lockReason) {
        return getLockedCompositeStateDetails(lockReason).getOrDefault(MODULE_SET_TAG_KEY, "");
    }

    private String getFirstResource(final Object responseBody) {
        final String jsonObjectAsString = jsonObjectMapper.asJsonString(responseBody);
        final JsonNode overallJsonNode = jsonObjectMapper.convertToJsonNode(jsonObjectAsString);
        final Iterator<Map.Entry<String, JsonNode>> overallJsonTreeMap = overallJsonNode.fields();
        final Map.Entry<String, JsonNode> firstElement = overallJsonTreeMap.next();
        return jsonObjectMapper.asJsonString(Map.of(firstElement.getKey(), firstElement.getValue()));
    }

    private Collection<YangModelCmHandle> convertCmHandlesDataNodesToYangModelCmHandles(
            final Collection<DataNode> cmHandlesAsDataNodeList) {
        return cmHandlesAsDataNodeList.stream().map(YangDataConverter::toYangModelCmHandle).toList();
    }

    private int calculateNextAttemptCount(final Map<String, String> compositeStateDetails) {
        return compositeStateDetails.containsKey(RETRY_ATTEMPT_KEY)
                ? 1 + Integer.parseInt(compositeStateDetails.get(RETRY_ATTEMPT_KEY))
                : 1;
    }

    private String buildLockReasonDetails(final String moduleSetTag, final int attempt, final String errorMessage) {
        if (moduleSetTag.isEmpty()) {
            return String.format(LOCK_REASON_DETAILS_MSG_FORMAT, attempt, errorMessage);
        }
        return String.format(MODULE_SET_TAG_MESSAGE_FORMAT + " " + LOCK_REASON_DETAILS_MSG_FORMAT,
                moduleSetTag, attempt, errorMessage);
    }

}
