/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.ALTERNATE_ID_ALREADY_ASSOCIATED;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_FOUND;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_READY;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_ALREADY_EXIST;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_INVALID_ID;
import static org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory.MODULE_UPGRADE;
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.utils.RestQueryParametersValidator.validateCmHandleQueryParameters;

import com.google.common.collect.Lists;
import com.hazelcast.map.IMap;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.events.lcm.LcmEventsCmHandleStateHandler;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState;
import org.onap.cps.ncmp.api.impl.inventory.CompositeState;
import org.onap.cps.ncmp.api.impl.inventory.CompositeStateBuilder;
import org.onap.cps.ncmp.api.impl.inventory.CompositeStateUtils;
import org.onap.cps.ncmp.api.impl.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.inventory.sync.ModuleOperationsUtils;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevelManager;
import org.onap.cps.ncmp.api.impl.utils.AlternateIdChecker;
import org.onap.cps.ncmp.api.impl.utils.CmHandleQueryConditions;
import org.onap.cps.ncmp.api.impl.utils.InventoryQueryConditions;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private static final int DELETE_BATCH_SIZE = 100;
    private final JsonObjectMapper jsonObjectMapper;
    private final DmiDataOperations dmiDataOperations;
    private final NetworkCmProxyDataServicePropertyHandler networkCmProxyDataServicePropertyHandler;
    private final InventoryPersistence inventoryPersistence;
    private final CmHandleQueries cmHandleQueries;
    private final NetworkCmProxyCmHandleQueryService networkCmProxyCmHandleQueryService;
    private final LcmEventsCmHandleStateHandler lcmEventsCmHandleStateHandler;
    private final CpsDataService cpsDataService;
    private final IMap<String, Object> moduleSyncStartedOnCmHandles;
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;
    private final TrustLevelManager trustLevelManager;
    private final AlternateIdChecker alternateIdChecker;

    @Override
    public DmiPluginRegistrationResponse updateDmiRegistrationAndSyncModule(
        final DmiPluginRegistration dmiPluginRegistration) {

        dmiPluginRegistration.validateDmiPluginRegistration();
        final DmiPluginRegistrationResponse dmiPluginRegistrationResponse = new DmiPluginRegistrationResponse();

        setTrustLevelPerDmiPlugin(dmiPluginRegistration);

        processRemovedCmHandles(dmiPluginRegistration, dmiPluginRegistrationResponse);

        processCreatedCmHandles(dmiPluginRegistration, dmiPluginRegistrationResponse);

        processUpdatedCmHandles(dmiPluginRegistration, dmiPluginRegistrationResponse);

        processUpgradedCmHandles(dmiPluginRegistration, dmiPluginRegistrationResponse);

        return dmiPluginRegistrationResponse;
    }

    @Override
    public Object getResourceDataForCmHandle(final String datastoreName,
                                             final String cmHandleId,
                                             final String resourceIdentifier,
                                             final String optionsParamInQuery,
                                             final String topicParamInQuery,
                                             final String requestId,
                                             final String authorization) {
        final ResponseEntity<?> responseEntity = dmiDataOperations.getResourceDataFromDmi(datastoreName, cmHandleId,
            resourceIdentifier,
            optionsParamInQuery,
            topicParamInQuery,
            requestId,
            authorization);
        return responseEntity.getBody();
    }

    @Override
    public Object getResourceDataForCmHandle(final String datastoreName,
                                             final String cmHandleId,
                                             final String resourceIdentifier,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNodes(datastoreName, cmHandleId, resourceIdentifier,
            fetchDescendantsOption).iterator().next();
    }

    @Override
    public void executeDataOperationForCmHandles(final String topicParamInQuery,
                                                 final DataOperationRequest dataOperationRequest,
                                                 final String requestId,
                                                 final String authorization) {
        dmiDataOperations.requestResourceDataFromDmi(topicParamInQuery, dataOperationRequest, requestId,
                authorization);
    }

    @Override
    public Object writeResourceDataPassThroughRunningForCmHandle(final String cmHandleId,
                                                                 final String resourceIdentifier,
                                                                 final OperationType operationType,
                                                                 final String requestData,
                                                                 final String dataType,
                                                                 final String authorization) {
        return dmiDataOperations.writeResourceDataPassThroughRunningFromDmi(cmHandleId, resourceIdentifier,
            operationType, requestData, dataType, authorization);
    }

    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleId) {
        return inventoryPersistence.getYangResourcesModuleReferences(cmHandleId);
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(final String cmHandleId) {
        return inventoryPersistence.getModuleDefinitionsByCmHandleId(cmHandleId);
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleAndModule(final String cmHandleId,
                                                                                final String moduleName,
                                                                                final String moduleRevision) {
        return inventoryPersistence.getModuleDefinitionsByCmHandleAndModule(cmHandleId, moduleName, moduleRevision);
    }

    /**
     * Retrieve cm handles with details for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @return cm handles with details
     */
    @Override
    public Collection<NcmpServiceCmHandle> executeCmHandleSearch(
        final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
            cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, CmHandleQueryConditions.ALL_CONDITION_NAMES);
        return networkCmProxyCmHandleQueryService.queryCmHandles(cmHandleQueryServiceParameters);
    }

    /**
     * Retrieve cm handle ids for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @return cm handle ids
     */
    @Override
    public Collection<String> executeCmHandleIdSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
            cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, CmHandleQueryConditions.ALL_CONDITION_NAMES);
        return networkCmProxyCmHandleQueryService.queryCmHandleIds(cmHandleQueryServiceParameters);
    }

    /**
     * Set the data sync enabled flag, along with the data sync state
     * based on the data sync enabled boolean for the cm handle id provided.
     *
     * @param cmHandleId                 cm handle id
     * @param dataSyncEnabledTargetValue data sync enabled flag
     */
    @Override
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

    /**
     * Get all cm handle IDs by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @return set of cm handle IDs
     */
    @Override
    public Collection<String> getAllCmHandleIdsByDmiPluginIdentifier(final String dmiPluginIdentifier) {
        return cmHandleQueries.getCmHandleIdsByDmiPluginIdentifier(dmiPluginIdentifier);
    }

    /**
     * Get all cm handle IDs by various properties.
     *
     * @param cmHandleQueryServiceParameters cm handle query parameters
     * @return set of cm handle IDs
     */
    @Override
    public Collection<String> executeCmHandleIdSearchForInventory(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, InventoryQueryConditions.ALL_CONDITION_NAMES);
        return networkCmProxyCmHandleQueryService.queryCmHandleIdsForInventory(cmHandleQueryServiceParameters);
    }

    /**
     * Retrieve cm handle details for a given cm handle.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle details
     */
    @Override
    public NcmpServiceCmHandle getNcmpServiceCmHandle(final String cmHandleId) {
        return YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(
            inventoryPersistence.getYangModelCmHandle(cmHandleId));
    }

    /**
     * Get cm handle public properties for a given cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle public properties
     */
    @Override
    public Map<String, String> getCmHandlePublicProperties(final String cmHandleId) {
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        final List<YangModelCmHandle.Property> yangModelPublicProperties = yangModelCmHandle.getPublicProperties();
        final Map<String, String> cmHandlePublicProperties = new HashMap<>();
        YangDataConverter.asPropertiesMap(yangModelPublicProperties, cmHandlePublicProperties);
        return cmHandlePublicProperties;
    }

    /**
     * Get cm handle composite state for a given cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle state
     */
    @Override
    public CompositeState getCmHandleCompositeState(final String cmHandleId) {
        return inventoryPersistence.getYangModelCmHandle(cmHandleId).getCompositeState();
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
                batchDeleteCmHandlesFromDbAndModuleSyncMap(tobeRemovedCmHandleBatch);
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
        dmiPluginRegistrationResponse.setUpdatedCmHandles(networkCmProxyDataServicePropertyHandler
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
            ncmpServiceCmHandle.getAlternateId());
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
        trustLevelManager.handleInitialRegistrationOfTrustLevels(initialTrustLevelPerCmHandleId);
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
            deleteCmHandleFromDbAndModuleSyncMap(cmHandleId);
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

    private void deleteCmHandleFromDbAndModuleSyncMap(final String cmHandleId) {
        inventoryPersistence.deleteSchemaSetWithCascade(cmHandleId);
        inventoryPersistence.deleteDataNode(NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']");
        removeDeletedCmHandleFromModuleSyncMap(cmHandleId);
    }

    private void batchDeleteCmHandlesFromDbAndModuleSyncMap(final Collection<String> cmHandleIds) {
        inventoryPersistence.deleteSchemaSetsWithCascade(cmHandleIds);
        inventoryPersistence.deleteDataNodes(mapCmHandleIdsToXpaths(cmHandleIds));
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

    private void setTrustLevelPerDmiPlugin(final DmiPluginRegistration dmiPluginRegistration) {
        if (DmiPluginRegistration.isNullEmptyOrBlank(dmiPluginRegistration.getDmiDataPlugin())) {
            trustLevelPerDmiPlugin.put(dmiPluginRegistration.getDmiPlugin(), TrustLevel.COMPLETE);
        } else {
            trustLevelPerDmiPlugin.put(dmiPluginRegistration.getDmiDataPlugin(), TrustLevel.COMPLETE);
        }
    }

}
