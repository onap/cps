/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021 Nordix Foundation
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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiOperations;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.ncmp.api.models.PersistenceCmHandlesList;
import org.onap.cps.ncmp.api.models.YangResource;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private static final String NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME = "NFP-Operational";

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";

    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private static final OffsetDateTime NO_TIMESTAMP = null;

    private CpsDataService cpsDataService;

    private ObjectMapper objectMapper;

    private CpsQueryService cpsQueryService;

    private DmiDataOperations dmiDataOperations;

    private DmiModelOperations dmiModelOperations;

    private CpsModuleService cpsModuleService;

    private CpsAdminService cpsAdminService;

    /**
     * Constructor Injection for Dependencies.
     * @param dmiDataOperations DMI operation
     * @param cpsDataService Data Service Interface
     * @param cpsQueryService Query Service Interface
     * @param objectMapper Object Mapper
     */
    public NetworkCmProxyDataServiceImpl(final DmiDataOperations dmiDataOperations,
                                         final DmiModelOperations dmiModelOperations,
                                         final CpsModuleService cpsModuleService,
                                         final CpsDataService cpsDataService,
                                         final CpsQueryService cpsQueryService,
                                         final CpsAdminService cpsAdminService,
                                         final ObjectMapper objectMapper) {
        this.dmiDataOperations = dmiDataOperations;
        this.dmiModelOperations = dmiModelOperations;
        this.cpsModuleService = cpsModuleService;
        this.cpsDataService = cpsDataService;
        this.cpsQueryService = cpsQueryService;
        this.cpsAdminService = cpsAdminService;
        this.objectMapper = objectMapper;
    }

    @Override
    public DataNode getDataNode(final String cmHandle, final String xpath,
        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService
            .getDataNode(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle, xpath, fetchDescendantsOption);
    }

    @Override
    public Collection<DataNode> queryDataNodes(final String cmHandle, final String cpsPath,
        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsQueryService
            .queryDataNodes(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle, cpsPath, fetchDescendantsOption);
    }

    @Override
    public void createDataNode(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        if (!StringUtils.hasText(parentNodeXpath) || "/".equals(parentNodeXpath)) {
            cpsDataService.saveData(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle, jsonData, NO_TIMESTAMP);
        } else {
            cpsDataService
                .saveData(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
        }
    }

    @Override
    public void addListNodeElements(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.saveListElements(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData,
            NO_TIMESTAMP);
    }

    @Override
    public void updateNodeLeaves(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService
            .updateNodeLeaves(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData,
                NO_TIMESTAMP);
    }

    @Override
    public void replaceNodeTree(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.replaceNodeTree(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData,
            NO_TIMESTAMP);
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
                deleteSchemaSetWithCascade(cmHandle);
                cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    "/dmi-registry/cm-handles[@id='" + cmHandle + "']", NO_TIMESTAMP);
            } catch (final DataNodeNotFoundException e) {
                log.warn("Datanode {} not deleted message {}", cmHandle, e.getMessage());
            }
        }
    }

    private void deleteSchemaSetWithCascade(final String cmHandle) {
        try {
            cpsModuleService.deleteSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandle,
                CASCADE_DELETE_ALLOWED);
        } catch (final Exception e) {
            log.warn("Schema set {} not deleted message {}", cmHandle, e.getMessage());
        }
    }

    private void syncAndCreateSchemaSet(final PersistenceCmHandle persistenceCmHandle) {

        final List<ModuleReference> moduleReferencesFromCmHandle =
            toModuleReferences(dmiModelOperations.getModuleReferences(persistenceCmHandle));
        final List<ModuleReference> existingModuleReferences = new ArrayList<>();
        final List<ModuleReference> unknownModuleReferences = new ArrayList<>();
        prepareModuleSubsets(moduleReferencesFromCmHandle, existingModuleReferences, unknownModuleReferences);

        final Map<String, String> newYangResourcesModuleNameToContentMap;
        if (unknownModuleReferences.isEmpty()) {
            newYangResourcesModuleNameToContentMap = new HashMap<>();
        } else {
            newYangResourcesModuleNameToContentMap = getNewYangResourcesFromDmi(persistenceCmHandle,
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

    private Map<String, String> getNewYangResourcesFromDmi(final PersistenceCmHandle persistenceCmHandle,
                                                           final List<ModuleReference> unknownModuleReferences) {
        final ResponseEntity<String> responseEntity =
            dmiModelOperations.getNewYangResourcesFromDmi(persistenceCmHandle, unknownModuleReferences);

        final JsonArray moduleResources = new Gson().fromJson(responseEntity.getBody(),
            JsonArray.class);
        final Map<String, String> newYangResourcesModuleNameToContentMap = new HashMap<>();

        for (final JsonElement moduleResource : moduleResources) {
            final YangResource yangResource = toYangResource((JsonObject) moduleResource);
            newYangResourcesModuleNameToContentMap.put(yangResource.getModuleName(), yangResource.getYangSource());
        }
        return newYangResourcesModuleNameToContentMap;
    }

    private static YangResource toYangResource(final JsonObject yangResourceAsJson) {
        final YangResource yangResource = new YangResource();
        yangResource.setModuleName(yangResourceAsJson.get("moduleName").getAsString());
        yangResource.setRevision(yangResourceAsJson.get("revision").getAsString());
        yangResource.setYangSource(yangResourceAsJson.get("yangSource").getAsString());
        return yangResource;
    }

    private static List<ModuleReference> toModuleReferences(
            final ResponseEntity<String> dmiFetchModulesResponseEntity) {
        final List<ModuleReference> moduleReferences = new ArrayList<>();
        final JsonObject bodyAsJsonObject = new Gson().fromJson(dmiFetchModulesResponseEntity.getBody(),
            JsonObject.class);
        final JsonArray moduleReferencesAsJson = bodyAsJsonObject.getAsJsonArray("schemas");
        for (final JsonElement moduleReferenceAsJson : moduleReferencesAsJson) {
            final ModuleReference moduleReference = toModuleReference((JsonObject) moduleReferenceAsJson);
            moduleReferences.add(moduleReference);
        }
        return moduleReferences;
    }

    private static ModuleReference toModuleReference(final JsonObject moduleReferenceAsJson) {
        final ModuleReference moduleReference = new ModuleReference();
        moduleReference.setModuleName(moduleReferenceAsJson.get("moduleName").getAsString());
        moduleReference.setRevision(moduleReferenceAsJson.get("revision").getAsString());
        return moduleReference;
    }
}
