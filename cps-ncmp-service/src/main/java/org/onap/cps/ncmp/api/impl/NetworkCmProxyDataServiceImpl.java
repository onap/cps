/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada
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

import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum;
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiOperations;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.ncmp.api.models.PersistenceCmHandlesList;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private static final String NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME = "NFP-Operational";

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";

    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private static final OffsetDateTime NO_TIMESTAMP = null;

    private CpsDataService cpsDataService;

    private ObjectMapper objectMapper;

    private DmiDataOperations dmiDataOperations;

    private DmiModelOperations dmiModelOperations;

    private CpsModuleService cpsModuleService;

    private CpsAdminService cpsAdminService;

    /**
     * Constructor Injection for Dependencies.
     * @param dmiDataOperations DMI operation
     * @param cpsDataService Data Service Interface
     * @param objectMapper Object Mapper
     */
    public NetworkCmProxyDataServiceImpl(final DmiDataOperations dmiDataOperations,
                                         final DmiModelOperations dmiModelOperations,
                                         final CpsModuleService cpsModuleService,
                                         final CpsDataService cpsDataService,
                                         final CpsAdminService cpsAdminService,
                                         final ObjectMapper objectMapper) {
        this.dmiDataOperations = dmiDataOperations;
        this.dmiModelOperations = dmiModelOperations;
        this.cpsModuleService = cpsModuleService;
        this.cpsDataService = cpsDataService;
        this.cpsAdminService = cpsAdminService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void updateDmiRegistrationAndSyncModule(final DmiPluginRegistration dmiPluginRegistration) {
        dmiPluginRegistration.validateDmiPluginRegistration();
        try {
            if (dmiPluginRegistration.getCreatedCmHandles() != null) {
                parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(dmiPluginRegistration);
            }
            if (dmiPluginRegistration.getUpdatedCmHandles() != null) {
                parseAndUpdateCmHandlesInDmiRegistration(dmiPluginRegistration);
            }
            if (dmiPluginRegistration.getRemovedCmHandles() != null) {
                parseAndRemoveCmHandlesInDmiRegistration(dmiPluginRegistration);
            }
        } catch (final JsonProcessingException e) {
            handleJsonProcessingException(dmiPluginRegistration, e);
        }
    }

    @Override
    public Object getResourceDataOperationalForCmHandle(final String cmHandle,
                                                        final String resourceIdentifier,
                                                        final String acceptParamInHeader,
                                                        final String optionsParamInQuery) {
        return handleResponse(dmiDataOperations.getResourceDataFromDmi(
            cmHandle,
            resourceIdentifier,
            optionsParamInQuery,
            acceptParamInHeader,
            DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL), "Not able to get resource data.");
    }

    @Override
    public Object getResourceDataPassThroughRunningForCmHandle(final String cmHandle,
                                                               final String resourceIdentifier,
                                                               final String acceptParamInHeader,
                                                               final String optionsParamInQuery) {
        return handleResponse(dmiDataOperations.getResourceDataFromDmi(
            cmHandle,
            resourceIdentifier,
            optionsParamInQuery,
            acceptParamInHeader,
            DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING), "Not able to get resource data.");
    }

    @Override
    public void writeResourceDataPassThroughRunningForCmHandle(final String cmHandle,
                                                               final String resourceIdentifier,
                                                               final OperationEnum operation,
                                                               final String requestData,
                                                               final String dataType) {
        handleResponse(
            dmiDataOperations.writeResourceDataPassThroughRunningFromDmi(
                cmHandle, resourceIdentifier, operation, requestData, dataType),
            "Not able to " + operation + " resource data.");
    }


    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandle) {
        return cpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle);
    }

    /**
     * Retrieve cm handle identifiers for the given list of module names.
     *
     * @param moduleNames module names.
     * @return a collection of anchor identifiers
     */
    @Override
    public Collection<String> executeCmHandleHasAllModulesSearch(final Collection<String> moduleNames) {
        return cpsAdminService.queryAnchorNames(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, moduleNames);
    }

    /**
     * THis method registers a cm handle and intiates modules sync.
     *
     * @param dmiPluginRegistration dmi plugin registration information.
     * @throws JsonProcessingException thrown if json is malformed or missing.
     */
    public void parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(
        final DmiPluginRegistration dmiPluginRegistration) throws JsonProcessingException {
        final PersistenceCmHandlesList createdPersistenceCmHandlesList =
            getUpdatedPersistenceCmHandlesList(dmiPluginRegistration, dmiPluginRegistration.getCreatedCmHandles());
        registerAndSyncNewCmHandles(createdPersistenceCmHandlesList);
    }

    private static Object handleResponse(final ResponseEntity<?> responseEntity,
                                         final String exceptionMessage) {
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        } else {
            throw new NcmpException(exceptionMessage,
                    "DMI status code: " + responseEntity.getStatusCodeValue()
                            + ", DMI response body: " + responseEntity.getBody());
        }
    }

    private void parseAndUpdateCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration)
        throws JsonProcessingException {
        final PersistenceCmHandlesList updatedPersistenceCmHandlesList =
            getUpdatedPersistenceCmHandlesList(dmiPluginRegistration, dmiPluginRegistration.getUpdatedCmHandles());
        final String cmHandlesAsJson = objectMapper.writeValueAsString(updatedPersistenceCmHandlesList);
        cpsDataService.updateNodeLeavesAndExistingDescendantLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                "/dmi-registry", cmHandlesAsJson, NO_TIMESTAMP);
    }

    private PersistenceCmHandlesList getUpdatedPersistenceCmHandlesList(
        final DmiPluginRegistration dmiPluginRegistration,
        final List<CmHandle> updatedCmHandles) {
        return PersistenceCmHandlesList.toPersistenceCmHandlesList(
            dmiPluginRegistration.getDmiPlugin(),
            dmiPluginRegistration.getDmiDataPlugin(),
            dmiPluginRegistration.getDmiModelPlugin(),
            updatedCmHandles);
    }

    private static void handleJsonProcessingException(final DmiPluginRegistration dmiPluginRegistration,
                                                      final JsonProcessingException e) {
        final String message = "Parsing error occurred while processing DMI Plugin Registration"
            + dmiPluginRegistration;
        log.error(message);
        throw new DataValidationException(message, e.getMessage(), e);
    }

    private void registerAndSyncNewCmHandles(final PersistenceCmHandlesList persistenceCmHandlesList)
        throws JsonProcessingException  {
        final String cmHandleJsonData = objectMapper.writeValueAsString(persistenceCmHandlesList);
        cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, "/dmi-registry",
            cmHandleJsonData, NO_TIMESTAMP);

        for (final PersistenceCmHandle persistenceCmHandle : persistenceCmHandlesList.getPersistenceCmHandles()) {
            syncModulesAndCreateAnchor(persistenceCmHandle);
        }
    }

    protected void syncModulesAndCreateAnchor(final PersistenceCmHandle persistenceCmHandle) {
        syncAndCreateSchemaSet(persistenceCmHandle);
        createAnchor(persistenceCmHandle);
    }

    private void parseAndRemoveCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        for (final String cmHandle : dmiPluginRegistration.getRemovedCmHandles()) {
            try {
                attemptToDeleteSchemaSetWithCascade(cmHandle);
                cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    "/dmi-registry/cm-handles[@id='" + cmHandle + "']", NO_TIMESTAMP);
            } catch (final DataNodeNotFoundException e) {
                log.warn("Datanode {} not deleted message {}", cmHandle, e.getMessage());
            }
        }
    }

    private void attemptToDeleteSchemaSetWithCascade(final String schemaSetName) {
        try {
            cpsModuleService.deleteSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                CASCADE_DELETE_ALLOWED);
        } catch (final Exception e) {
            log.warn("Schema set {} delete failed, reason {}", schemaSetName, e.getMessage());
        }
    }

    private void syncAndCreateSchemaSet(final PersistenceCmHandle persistenceCmHandle) {
        final List<ModuleReference> moduleReferencesFromCmHandle =
            dmiModelOperations.getModuleReferences(persistenceCmHandle);
        final List<ModuleReference> existingModuleReferences = new ArrayList<>();
        final List<ModuleReference> unknownModuleReferences = new ArrayList<>();
        prepareModuleSubsets(moduleReferencesFromCmHandle, existingModuleReferences, unknownModuleReferences);

        final Map<String, String> newYangResourcesModuleNameToContentMap;
        if (unknownModuleReferences.isEmpty()) {
            newYangResourcesModuleNameToContentMap = new HashMap<>();
        } else {
            newYangResourcesModuleNameToContentMap = dmiModelOperations.getNewYangResourcesFromDmi(persistenceCmHandle,
                unknownModuleReferences);
        }
        cpsModuleService
            .createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, persistenceCmHandle.getId(),
                newYangResourcesModuleNameToContentMap, existingModuleReferences);
    }

    private void prepareModuleSubsets(final List<ModuleReference> moduleReferencesFromCmHandle,
                                      final List<ModuleReference> existingModuleReferences,
                                      final List<ModuleReference> unknownModuleReferences) {

        final Collection<ModuleReference> knownModuleReferencesInCps =
            cpsModuleService.getYangResourceModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME);

        for (final ModuleReference moduleReferenceFromDmiForCmHandle : moduleReferencesFromCmHandle) {
            if (knownModuleReferencesInCps.contains(moduleReferenceFromDmiForCmHandle)) {
                existingModuleReferences.add(moduleReferenceFromDmiForCmHandle);
            } else {
                unknownModuleReferences.add(moduleReferenceFromDmiForCmHandle);
            }
        }
    }

    private void createAnchor(final PersistenceCmHandle persistenceCmHandle) {
        cpsAdminService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, persistenceCmHandle.getId(),
            persistenceCmHandle.getId());
    }



}
