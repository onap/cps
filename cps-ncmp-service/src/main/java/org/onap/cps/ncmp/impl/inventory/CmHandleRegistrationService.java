/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ncmp.impl.inventory;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.ALTERNATE_ID_ALREADY_ASSOCIATED;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_FOUND;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_READY;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_ALREADY_EXIST;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_INVALID_ID;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;
import static org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory.MODULE_UPGRADE;

import com.google.common.collect.Lists;
import com.hazelcast.map.IMap;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.api.inventory.models.TrustLevel;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.inventory.sync.ModuleOperationsUtils;
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandler;
import org.onap.cps.ncmp.impl.inventory.trustlevel.TrustLevelManager;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmHandleRegistrationService {

    private static final int DELETE_BATCH_SIZE = 100;

    private final CmHandleRegistrationServicePropertyHandler cmHandleRegistrationServicePropertyHandler;
    private final InventoryPersistence inventoryPersistence;
    private final CpsDataService cpsDataService;
    private final LcmEventsCmHandleStateHandler lcmEventsCmHandleStateHandler;
    @Qualifier("moduleSyncStartedOnCmHandles")
    private final IMap<String, Object> moduleSyncStartedOnCmHandles;
    private final TrustLevelManager trustLevelManager;
    private final AlternateIdChecker alternateIdChecker;

    /**
     * Registration of Created, Removed, Updated or Upgraded CM Handles.
     *
     * @param dmiPluginRegistration Dmi Plugin Registration details
     * @return dmiPluginRegistrationResponse
     */
    public DmiPluginRegistrationResponse updateDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {

        dmiPluginRegistration.validateDmiPluginRegistration();
        final DmiPluginRegistrationResponse dmiPluginRegistrationResponse = new DmiPluginRegistrationResponse();

        trustLevelManager.registerDmiPlugin(dmiPluginRegistration);

        processRemovedCmHandles(dmiPluginRegistration, dmiPluginRegistrationResponse);

        processCreatedCmHandles(dmiPluginRegistration, dmiPluginRegistrationResponse);

        processUpdatedCmHandles(dmiPluginRegistration, dmiPluginRegistrationResponse);

        processUpgradedCmHandles(dmiPluginRegistration, dmiPluginRegistrationResponse);

        return dmiPluginRegistrationResponse;
    }

    /**
     * Set the data sync enabled flag, along with the data sync state
     * based on the data sync enabled boolean for the cm handle id provided.
     *
     * @param cmHandleId                 cm handle id
     * @param dataSyncEnabledTargetValue data sync enabled flag
     */
    public void setDataSyncEnabled(final String cmHandleId, final Boolean dataSyncEnabledTargetValue) {
        final CompositeState compositeState = inventoryPersistence.getCmHandleState(cmHandleId);
        if (dataSyncEnabledTargetValue.equals(compositeState.getDataSyncEnabled())) {
            log.info("Data-Sync Enabled flag is already: {} ", dataSyncEnabledTargetValue);
            return;
        }
        if (CmHandleState.READY.equals(compositeState.getCmHandleState())) {
            final DataStoreSyncState dataStoreSyncState = compositeState.getDataStores()
                .getOperationalDataStore().getDataStoreSyncState();
            if (Boolean.FALSE.equals(dataSyncEnabledTargetValue)
                && DataStoreSyncState.SYNCHRONIZED.equals(dataStoreSyncState)) {
                // TODO : This is hard-coded for onap dmi that need to be addressed
                cpsDataService.deleteDataNode(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
                    "/netconf-state", OffsetDateTime.now());
            }
            CompositeStateUtils.setDataSyncEnabledFlagWithDataSyncState(dataSyncEnabledTargetValue, compositeState);
            inventoryPersistence.saveCmHandleState(cmHandleId, compositeState);
        } else {
            throw new CpsException("State mismatch exception.", "Cm-Handle not in READY state. Cm handle state is: "
                + compositeState.getCmHandleState());
        }
    }

    protected void processRemovedCmHandles(final DmiPluginRegistration dmiPluginRegistration,
                                           final DmiPluginRegistrationResponse dmiPluginRegistrationResponse) {
        final List<String> tobeRemovedCmHandleIds = dmiPluginRegistration.getRemovedCmHandles();
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponses =
            new ArrayList<>(tobeRemovedCmHandleIds.size());
        final Collection<YangModelCmHandle> yangModelCmHandles =
            inventoryPersistence.getYangModelCmHandles(tobeRemovedCmHandleIds);
        updateCmHandleStateBatch(yangModelCmHandles, CmHandleState.DELETING);

        final Set<String> notDeletedCmHandles = new HashSet<>();
        for (final List<String> tobeRemovedCmHandleBatch : Lists.partition(tobeRemovedCmHandleIds, DELETE_BATCH_SIZE)) {
            try {
                batchDeleteCmHandlesFromDbAndCaches(tobeRemovedCmHandleBatch);
                tobeRemovedCmHandleBatch.forEach(cmHandleId ->
                    cmHandleRegistrationResponses.add(CmHandleRegistrationResponse.createSuccessResponse(cmHandleId)));

            } catch (final RuntimeException batchException) {
                log.error("Unable to de-register cm-handle batch, retrying on each cm handle");
                for (final String cmHandleId : tobeRemovedCmHandleBatch) {
                    final CmHandleRegistrationResponse cmHandleRegistrationResponse =
                        deleteCmHandleAndGetCmHandleRegistrationResponse(cmHandleId);
                    cmHandleRegistrationResponses.add(cmHandleRegistrationResponse);
                    if (cmHandleRegistrationResponse.getStatus() != CmHandleRegistrationResponse.Status.SUCCESS) {
                        notDeletedCmHandles.add(cmHandleId);
                    }
                }
            }
        }
        yangModelCmHandles.removeIf(yangModelCmHandle -> notDeletedCmHandles.contains(yangModelCmHandle.getId()));
        updateCmHandleStateBatch(yangModelCmHandles, CmHandleState.DELETED);
        dmiPluginRegistrationResponse.setRemovedCmHandles(cmHandleRegistrationResponses);
    }

    protected void processCreatedCmHandles(final DmiPluginRegistration dmiPluginRegistration,
                                           final DmiPluginRegistrationResponse dmiPluginRegistrationResponse) {
        final List<NcmpServiceCmHandle> ncmpServiceCmHandles = dmiPluginRegistration.getCreatedCmHandles();
        final List<CmHandleRegistrationResponse> failedCmHandleRegistrationResponses = new ArrayList<>();

        try {
            final Collection<String> rejectedCmHandleIds
                = checkAlternateIds(ncmpServiceCmHandles, failedCmHandleRegistrationResponses);

            final Collection<String> succeededCmHandleIds = persistCmHandlesWithState(dmiPluginRegistration,
                dmiPluginRegistrationResponse, ncmpServiceCmHandles, rejectedCmHandleIds);

            processTrustLevels(ncmpServiceCmHandles, succeededCmHandleIds);

        } catch (final AlreadyDefinedException alreadyDefinedException) {
            failedCmHandleRegistrationResponses.addAll(CmHandleRegistrationResponse.createFailureResponsesFromXpaths(
                alreadyDefinedException.getAlreadyDefinedObjectNames(), CM_HANDLE_ALREADY_EXIST));
        } catch (final Exception exception) {
            final Collection<String> cmHandleIds =
                ncmpServiceCmHandles.stream().map(NcmpServiceCmHandle::getCmHandleId).collect(Collectors.toList());
            failedCmHandleRegistrationResponses.addAll(CmHandleRegistrationResponse
                .createFailureResponses(cmHandleIds, exception));
        }
        final List<CmHandleRegistrationResponse> mergedCmHandleRegistrationResponses
            = new ArrayList<>(failedCmHandleRegistrationResponses);
        mergedCmHandleRegistrationResponses.addAll(dmiPluginRegistrationResponse.getCreatedCmHandles());

        dmiPluginRegistrationResponse.setCreatedCmHandles(mergedCmHandleRegistrationResponses);
    }

    protected void processUpdatedCmHandles(final DmiPluginRegistration dmiPluginRegistration,
                                           final DmiPluginRegistrationResponse dmiPluginRegistrationResponse) {
        dmiPluginRegistrationResponse.setUpdatedCmHandles(cmHandleRegistrationServicePropertyHandler
            .updateCmHandleProperties(dmiPluginRegistration.getUpdatedCmHandles()));
    }

    protected void processUpgradedCmHandles(
        final DmiPluginRegistration dmiPluginRegistration,
        final DmiPluginRegistrationResponse dmiPluginRegistrationResponse) {

        final List<String> cmHandleIds = dmiPluginRegistration.getUpgradedCmHandles().getCmHandles();
        final String upgradedModuleSetTag = dmiPluginRegistration.getUpgradedCmHandles().getModuleSetTag();
        final Map<YangModelCmHandle, CmHandleState> acceptedCmHandleStatePerCmHandle
            = new HashMap<>(cmHandleIds.size());
        final List<CmHandleRegistrationResponse> cmHandleUpgradeResponses = new ArrayList<>(cmHandleIds.size());

        for (final String cmHandleId : cmHandleIds) {
            try {
                final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
                if (yangModelCmHandle.getCompositeState().getCmHandleState() == CmHandleState.READY) {
                    if (moduleUpgradeCanBeSkipped(yangModelCmHandle, upgradedModuleSetTag)) {
                        cmHandleUpgradeResponses.add(CmHandleRegistrationResponse.createSuccessResponse(cmHandleId));
                    } else {
                        updateYangModelCmHandleForUpgrade(yangModelCmHandle, upgradedModuleSetTag);
                        acceptedCmHandleStatePerCmHandle.put(yangModelCmHandle, CmHandleState.LOCKED);
                    }
                } else {
                    cmHandleUpgradeResponses.add(
                        CmHandleRegistrationResponse.createFailureResponse(cmHandleId, CM_HANDLES_NOT_READY));
                }
            } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
                log.error("Unable to find data node for cm handle id : {} , caused by : {}",
                    cmHandleId, dataNodeNotFoundException.getMessage());
                cmHandleUpgradeResponses.add(
                    CmHandleRegistrationResponse.createFailureResponse(cmHandleId, CM_HANDLES_NOT_FOUND));
            } catch (final DataValidationException dataValidationException) {
                log.error("Unable to upgrade cm handle id: {}, caused by : {}",
                    cmHandleId, dataValidationException.getMessage());
                cmHandleUpgradeResponses.add(
                    CmHandleRegistrationResponse.createFailureResponse(cmHandleId, CM_HANDLE_INVALID_ID));
            }
        }
        cmHandleUpgradeResponses.addAll(upgradeCmHandles(acceptedCmHandleStatePerCmHandle));
        dmiPluginRegistrationResponse.setUpgradedCmHandles(cmHandleUpgradeResponses);
    }

    private void processTrustLevels(final Collection<NcmpServiceCmHandle> cmHandlesToBeCreated,
                                    final Collection<String> succeededCmHandleIds) {
        final Map<String, TrustLevel> initialTrustLevelPerCmHandleId = new HashMap<>(cmHandlesToBeCreated.size());
        for (final NcmpServiceCmHandle ncmpServiceCmHandle: cmHandlesToBeCreated) {
            if (succeededCmHandleIds.contains(ncmpServiceCmHandle.getCmHandleId())) {
                initialTrustLevelPerCmHandleId.put(ncmpServiceCmHandle.getCmHandleId(),
                    ncmpServiceCmHandle.getRegistrationTrustLevel());
            }
        }
        trustLevelManager.registerCmHandles(initialTrustLevelPerCmHandleId);
    }

    private static boolean moduleUpgradeCanBeSkipped(final YangModelCmHandle yangModelCmHandle,
                                                     final String upgradedModuleSetTag) {
        if (StringUtils.isBlank(upgradedModuleSetTag)) {
            return false;
        }
        return yangModelCmHandle.getModuleSetTag().equals(upgradedModuleSetTag);
    }

    private static void updateYangModelCmHandleForUpgrade(final YangModelCmHandle yangModelCmHandle,
                                                          final String upgradedModuleSetTag) {
        final String lockReasonWithModuleSetTag = String.format(ModuleOperationsUtils.MODULE_SET_TAG_MESSAGE_FORMAT,
            upgradedModuleSetTag);
        yangModelCmHandle.setCompositeState(new CompositeStateBuilder().withCmHandleState(CmHandleState.READY)
            .withLockReason(MODULE_UPGRADE, lockReasonWithModuleSetTag).build());
    }

    private CmHandleRegistrationResponse deleteCmHandleAndGetCmHandleRegistrationResponse(final String cmHandleId) {
        try {
            deleteCmHandleFromDbAndCaches(cmHandleId);
            return CmHandleRegistrationResponse.createSuccessResponse(cmHandleId);
        } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
            log.error("Unable to find dataNode for cmHandleId : {} , caused by : {}",
                cmHandleId, dataNodeNotFoundException.getMessage());
            return CmHandleRegistrationResponse.createFailureResponse(cmHandleId, CM_HANDLES_NOT_FOUND);
        } catch (final DataValidationException dataValidationException) {
            log.error("Unable to de-register cm-handle id: {}, caused by: {}",
                cmHandleId, dataValidationException.getMessage());
            return CmHandleRegistrationResponse.createFailureResponse(cmHandleId, CM_HANDLE_INVALID_ID);
        } catch (final Exception exception) {
            log.error("Unable to de-register cm-handle id : {} , caused by : {}", cmHandleId, exception.getMessage());
            return CmHandleRegistrationResponse.createFailureResponse(cmHandleId, exception);
        }
    }

    private void updateCmHandleStateBatch(final Collection<YangModelCmHandle> yangModelCmHandles,
                                          final CmHandleState cmHandleState) {
        final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle = new HashMap<>(yangModelCmHandles.size());
        yangModelCmHandles.forEach(yangModelCmHandle -> cmHandleStatePerCmHandle.put(yangModelCmHandle, cmHandleState));
        lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
    }

    private void deleteCmHandleFromDbAndCaches(final String cmHandleId) {
        inventoryPersistence.deleteSchemaSetWithCascade(cmHandleId);
        inventoryPersistence.deleteDataNode(NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']");
        trustLevelManager.removeCmHandles(Collections.singleton(cmHandleId));
        removeDeletedCmHandleFromModuleSyncMap(cmHandleId);
    }

    private void batchDeleteCmHandlesFromDbAndCaches(final Collection<String> cmHandleIds) {
        inventoryPersistence.deleteSchemaSetsWithCascade(cmHandleIds);
        inventoryPersistence.deleteDataNodes(mapCmHandleIdsToXpaths(cmHandleIds));
        trustLevelManager.removeCmHandles(cmHandleIds);
        cmHandleIds.forEach(this::removeDeletedCmHandleFromModuleSyncMap);
    }

    private Collection<String> mapCmHandleIdsToXpaths(final Collection<String> cmHandles) {
        return cmHandles.stream()
            .map(cmHandleId -> NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']")
            .collect(Collectors.toSet());
    }

    // CPS-1239 Robustness cleaning of in progress cache
    private void removeDeletedCmHandleFromModuleSyncMap(final String cmHandleId) {
        if (moduleSyncStartedOnCmHandles.remove(cmHandleId) != null) {
            log.debug("{} removed from in progress map", cmHandleId);
        }
    }

    private List<CmHandleRegistrationResponse> upgradeCmHandles(final Map<YangModelCmHandle, CmHandleState>
                                                                    cmHandleStatePerCmHandle) {
        final List<String> cmHandleIds = getCmHandleIds(cmHandleStatePerCmHandle);
        log.info("Moving cm handles : {} into locked (for upgrade) state.", cmHandleIds);
        try {
            lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
            return CmHandleRegistrationResponse.createSuccessResponses(cmHandleIds);
        } catch (final Exception e) {
            log.error("Unable to update cmHandleIds : {} , caused by : {}", cmHandleIds, e.getMessage());
            return CmHandleRegistrationResponse.createFailureResponses(cmHandleIds, e);
        }
    }

    private static List<String> getCmHandleIds(final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle) {
        return cmHandleStatePerCmHandle.keySet().stream().map(YangModelCmHandle::getId).toList();
    }

    private Collection<String> checkAlternateIds(
        final List<NcmpServiceCmHandle> cmHandlesToBeCreated,
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponses) {
        final Collection<String> rejectedCmHandleIds = alternateIdChecker
            .getIdsOfCmHandlesWithRejectedAlternateId(cmHandlesToBeCreated, AlternateIdChecker.Operation.CREATE);
        cmHandleRegistrationResponses.addAll(CmHandleRegistrationResponse.createFailureResponses(
            rejectedCmHandleIds, ALTERNATE_ID_ALREADY_ASSOCIATED));
        return rejectedCmHandleIds;
    }

    private List<String> persistCmHandlesWithState(final DmiPluginRegistration dmiPluginRegistration,
                                                   final DmiPluginRegistrationResponse dmiPluginRegistrationResponse,
                                                   final List<NcmpServiceCmHandle> cmHandlesToBeCreated,
                                                   final Collection<String> rejectedCmHandleIds) {
        final List<String> succeededCmHandleIds = new ArrayList<>(cmHandlesToBeCreated.size());
        final List<YangModelCmHandle> yangModelCmHandlesToRegister = new ArrayList<>(cmHandlesToBeCreated.size());
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponses =
            new ArrayList<>(cmHandlesToBeCreated.size());
        for (final NcmpServiceCmHandle ncmpServiceCmHandle: cmHandlesToBeCreated) {
            if (!rejectedCmHandleIds.contains(ncmpServiceCmHandle.getCmHandleId())) {
                yangModelCmHandlesToRegister.add(getYangModelCmHandle(dmiPluginRegistration, ncmpServiceCmHandle));
                cmHandleRegistrationResponses.add(
                    CmHandleRegistrationResponse.createSuccessResponse(ncmpServiceCmHandle.getCmHandleId()));
                succeededCmHandleIds.add(ncmpServiceCmHandle.getCmHandleId());
            }
        }
        lcmEventsCmHandleStateHandler.initiateStateAdvised(yangModelCmHandlesToRegister);
        dmiPluginRegistrationResponse.setCreatedCmHandles(cmHandleRegistrationResponses);
        return succeededCmHandleIds;
    }

    private YangModelCmHandle getYangModelCmHandle(final DmiPluginRegistration dmiPluginRegistration,
                                                   final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return YangModelCmHandle.toYangModelCmHandle(
            dmiPluginRegistration.getDmiPlugin(),
            dmiPluginRegistration.getDmiDataPlugin(),
            dmiPluginRegistration.getDmiModelPlugin(),
            ncmpServiceCmHandle,
            ncmpServiceCmHandle.getModuleSetTag(),
            ncmpServiceCmHandle.getAlternateId(),
            ncmpServiceCmHandle.getDataProducerIdentifier());
    }


}
