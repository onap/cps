/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.inventory.sync;

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState;
import org.onap.cps.ncmp.api.impl.inventory.CompositeState;
import org.onap.cps.ncmp.api.impl.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncUtils {

    private final CmHandleQueries cmHandleQueries;
    private final DmiDataOperations dmiDataOperations;
    private final JsonObjectMapper jsonObjectMapper;
    private static final Pattern retryAttemptPattern = Pattern.compile("^Attempt #(\\d+) failed:");

    /**
     * Query data nodes for cm handles with an "ADVISED" cm handle state.
     *
     * @return cm handles (data nodes) in ADVISED state (empty list if none found)
     */
    public List<DataNode> getAdvisedCmHandles() {
        final List<DataNode> advisedCmHandlesAsDataNodes = cmHandleQueries.queryCmHandlesByState(CmHandleState.ADVISED);
        log.debug("Total number of fetched advised cm handle(s) is (are) {}", advisedCmHandlesAsDataNodes.size());
        return advisedCmHandlesAsDataNodes;
    }

    /**
     * First query data nodes for cm handles with CM Handle Operational Sync State in "UNSYNCHRONIZED" and
     * randomly select a CM Handle and query the data nodes for CM Handle State in "READY".
     *
     * @return a randomized yang model cm handle list with State in READY and Operation Sync State in "UNSYNCHRONIZED",
     *         return empty list if not found
     */
    public List<YangModelCmHandle> getUnsynchronizedReadyCmHandles() {
        final List<DataNode> unsynchronizedCmHandles = cmHandleQueries
                .queryCmHandlesByOperationalSyncState(DataStoreSyncState.UNSYNCHRONIZED);

        final List<YangModelCmHandle> yangModelCmHandles = new ArrayList<>();
        for (final DataNode unsynchronizedCmHandle : unsynchronizedCmHandles) {
            final String cmHandleId = unsynchronizedCmHandle.getLeaves().get("id").toString();
            if (cmHandleQueries.cmHandleHasState(cmHandleId, CmHandleState.READY)) {
                yangModelCmHandles.addAll(
                        convertCmHandlesDataNodesToYangModelCmHandles(
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
    public List<YangModelCmHandle> getCmHandlesThatFailedModelSyncOrUpgrade() {
        final List<DataNode> lockedCmHandlesAsDataNodeList
                = cmHandleQueries.queryCmHandleAncestorsByCpsPath(
                "//lock-reason[@reason=\"MODULE_SYNC_FAILED\" or @reason=\"MODULE_UPGRADE\"]",
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        return convertCmHandlesDataNodesToYangModelCmHandles(lockedCmHandlesAsDataNodeList);
    }

    /**
     * Update Composite State attempts counter and set new lock reason and details.
     *
     * @param lockReasonCategory lock reason category
     * @param errorMessage       error message
     */
    public void updateLockReasonDetailsAndAttempts(final CompositeState compositeState,
                                                   final LockReasonCategory lockReasonCategory,
                                                   final String errorMessage) {
        int attempt = 1;
        if (compositeState.getLockReason() != null) {
            final Matcher matcher = retryAttemptPattern.matcher(compositeState.getLockReason().getDetails());
            if (matcher.find()) {
                attempt = 1 + Integer.parseInt(matcher.group(1));
            }
        }
        compositeState.setLockReason(CompositeState.LockReason.builder()
                .details(String.format("Attempt #%d failed: %s", attempt, errorMessage))
                .lockReasonCategory(lockReasonCategory).build());
    }


    /**
     * Check if a module sync retry is needed.
     *
     * @param compositeState the composite state currently in the locked state
     * @return if the retry mechanism should be attempted
     */
    public boolean needsModuleSyncRetry(final CompositeState compositeState) {
        final OffsetDateTime time = OffsetDateTime.parse(compositeState.getLastUpdateTime(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        final Matcher matcher = retryAttemptPattern.matcher(compositeState.getLockReason().getDetails());
        final boolean failedDuringModuleSync = LockReasonCategory.MODULE_SYNC_FAILED
                == compositeState.getLockReason().getLockReasonCategory();
        if (!failedDuringModuleSync) {
            log.info("Locked for other reason");
            return false;
        }
        final int timeInMinutesUntilNextAttempt;
        if (matcher.find()) {
            timeInMinutesUntilNextAttempt = (int) Math.pow(2, Integer.parseInt(matcher.group(1)));
        } else {
            timeInMinutesUntilNextAttempt = 1;
            log.info("First Attempt: no current attempts found.");
        }
        final int timeSinceLastAttempt = (int) Duration.between(time, OffsetDateTime.now()).toMinutes();
        if (timeInMinutesUntilNextAttempt >= timeSinceLastAttempt) {
            log.info("Time until next attempt is {} minutes: ", timeInMinutesUntilNextAttempt - timeSinceLastAttempt);
            return false;
        }
        log.info("Retry due now");
        return true;
    }

    /**
     * Get the Resourece Data from Node through DMI Passthrough service.
     *
     * @param cmHandleId cm handle id
     * @return optional string containing the resource data
     */
    public String getResourceData(final String cmHandleId) {
        final ResponseEntity<Object> resourceDataResponseEntity = dmiDataOperations.getResourceDataFromDmi(
                PASSTHROUGH_OPERATIONAL.getDatastoreName(),
                cmHandleId,
                UUID.randomUUID().toString());
        if (resourceDataResponseEntity.getStatusCode().is2xxSuccessful()) {
            return getFirstResource(resourceDataResponseEntity.getBody());
        }
        return null;
    }

    private String getFirstResource(final Object responseBody) {
        final String jsonObjectAsString = jsonObjectMapper.asJsonString(responseBody);
        final JsonNode overallJsonNode = jsonObjectMapper.convertToJsonNode(jsonObjectAsString);
        final Iterator<Map.Entry<String, JsonNode>> overallJsonTreeMap = overallJsonNode.fields();
        final Map.Entry<String, JsonNode> firstElement = overallJsonTreeMap.next();
        return jsonObjectMapper.asJsonString(Map.of(firstElement.getKey(), firstElement.getValue()));
    }

    private static List<YangModelCmHandle> convertCmHandlesDataNodesToYangModelCmHandles(
            final List<DataNode> cmHandlesAsDataNodeList) {
        return cmHandlesAsDataNodeList.stream()
                .map(cmHandle -> YangDataConverter.convertCmHandleToYangModel(cmHandle,
                        cmHandle.getLeaves().get("id").toString())).collect(Collectors.toList());
    }
}
