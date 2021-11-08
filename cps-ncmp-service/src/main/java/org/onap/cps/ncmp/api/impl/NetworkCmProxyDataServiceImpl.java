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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.onap.cps.ncmp.api.impl.operation.DmiOperations;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.GenericRequestBody;
import org.onap.cps.ncmp.api.models.GenericRequestBody.OperationEnum;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle.AdditionalProperty;
import org.onap.cps.ncmp.api.models.PersistenceCmHandlesList;
import org.onap.cps.ncmp.api.models.YangResource;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.http.HttpStatus;
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

    private static  final String NCMP_DMI_SERVICE_NAME = "dmi-service-name";

    private  static final String REVISION = "revision";

    private CpsDataService cpsDataService;

    private ObjectMapper objectMapper;

    private CpsQueryService cpsQueryService;

    private DmiOperations dmiOperations;

    private CpsModuleService cpsModuleService;

    private CpsAdminService cpsAdminService;

    /**
     * Constructor Injection for Dependencies.
     * @param dmiOperations DMI operation
     * @param cpsDataService Data Service Interface
     * @param cpsQueryService Query Service Interface
     * @param objectMapper Object Mapper
     */
    public NetworkCmProxyDataServiceImpl(final DmiOperations dmiOperations,
        final CpsModuleService cpsModuleService,
        final CpsDataService cpsDataService,
        final CpsQueryService cpsQueryService,
        final CpsAdminService cpsAdminService,
        final ObjectMapper objectMapper) {
        this.dmiOperations = dmiOperations;
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
        try {
            if (dmiPluginRegistration.getCreatedCmHandles() != null) {
                parseAndCreateCmHandlesInDmiRegistrationAndSyncModule(dmiPluginRegistration);
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
    public Object getResourceDataOperationalForCmHandle(final @NotNull String cmHandle,
                                                        final @NotNull String resourceIdentifier,
                                                        final String acceptParamInHeader,
                                                        final String optionsParamInQuery) {

        final DataNode cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final String dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get(NCMP_DMI_SERVICE_NAME));
        final String dmiRequestBody = getGenericRequestBody(cmHandleDataNode);
        final ResponseEntity<Object> response = dmiOperations.getResourceDataOperationalFromDmi(dmiServiceName,
                cmHandle,
                resourceIdentifier,
                optionsParamInQuery,
                acceptParamInHeader,
                dmiRequestBody);
        return handleResponse(response);
    }

    @Override
    public Object getResourceDataPassThroughRunningForCmHandle(final @NotNull String cmHandle,
                                                               final @NotNull String resourceIdentifier,
                                                               final String acceptParamInHeader,
                                                               final String optionsParamInQuery) {
        final DataNode cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final String dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get(NCMP_DMI_SERVICE_NAME));
        final String dmiRequestBody = getGenericRequestBody(cmHandleDataNode);
        final ResponseEntity<Object> response = dmiOperations.getResourceDataPassThroughRunningFromDmi(dmiServiceName,
                cmHandle,
                resourceIdentifier,
                optionsParamInQuery,
                acceptParamInHeader,
                dmiRequestBody);
        return handleResponse(response);
    }

    @Override
    public void createResourceDataPassThroughRunningForCmHandle(final @NotNull String cmHandle,
                                                                final @NotNull String resourceIdentifier,
                                                                final @NotNull String requestBody,
                                                                final String contentType) {
        final DataNode cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final String dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get(NCMP_DMI_SERVICE_NAME));
        final Collection<DataNode> cmHandlePropertiesAsDataNodes     = cmHandleDataNode.getChildDataNodes();
        final Map<String, String> cmHandlePropertiesAsMap = getCmHandlePropertiesAsMap(cmHandlePropertiesAsDataNodes);
        final GenericRequestBody dmiRequestBodyObject = GenericRequestBody.builder()
                .operation(GenericRequestBody.OperationEnum.CREATE)
                .dataType(contentType)
                .data(requestBody)
                .cmHandleProperties(cmHandlePropertiesAsMap)
                .build();
        final String dmiRequestBody = prepareOperationBody(dmiRequestBodyObject);
        final ResponseEntity<String> responseEntity = dmiOperations
                .createResourceDataPassThroughRunningFromDmi(dmiServiceName,
                        cmHandle,
                        resourceIdentifier,
                        dmiRequestBody);
        handleResponseFromDmi(responseEntity, "Not able to create resource data.");
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
     * Replace resource data for data store pass-through running using dmi for given cm-handle.
     *
     * @param cmHandle           cm handle
     * @param resourceIdentifier resource identifier
     * @param requestBody        request body to create resource
     * @param contentType        content type in body
     */
    @Override
    public void updateResourceDataPassThroughRunningForCmHandle(final String cmHandle, final String resourceIdentifier,
        final String requestBody, final String contentType) {
        final DataNode cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final String dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get(NCMP_DMI_SERVICE_NAME));
        final Collection<DataNode> cmHandlePropertiesAsDataNodes = cmHandleDataNode.getChildDataNodes();
        final Map<String, String> cmHandlePropertiesAsMap = getCmHandlePropertiesAsMap(cmHandlePropertiesAsDataNodes);
        final GenericRequestBody dmiRequestBodyObject = GenericRequestBody.builder()
            .operation(OperationEnum.UPDATE)
            .dataType(contentType)
            .data(requestBody)
            .cmHandleProperties(cmHandlePropertiesAsMap)
            .build();
        final String dmiRequestBody = prepareOperationBody(dmiRequestBodyObject);
        final ResponseEntity<String> responseEntity = dmiOperations
            .updateResourceDataPassThroughRunningFromDmi(dmiServiceName,
                cmHandle,
                resourceIdentifier,
                dmiRequestBody);
        handleResponseFromDmi(responseEntity, "Unable to replace resource data.");
    }

    private DataNode fetchDataNodeFromDmiRegistryForCmHandle(final String cmHandle) {
        final String xpathForDmiRegistryToFetchCmHandle = "/dmi-registry/cm-handles[@id='" + cmHandle + "']";
        return cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
                NCMP_DMI_REGISTRY_ANCHOR,
                xpathForDmiRegistryToFetchCmHandle,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }

    private String prepareOperationBody(final GenericRequestBody requestBodyObject) {
        try {
            return objectMapper.writeValueAsString(requestBodyObject);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON.");
            throw new NcmpException("Parsing error occurred while converting given object to JSON.",
                e.getMessage());
        }
    }

    private static Map<String, String> getCmHandlePropertiesAsMap(
            final Collection<DataNode> cmHandlePropertiesAsDataNode) {
        if (cmHandlePropertiesAsDataNode.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, String> cmHandlePropertiesAsMap = new LinkedHashMap<>();
        for (final DataNode dataNode: cmHandlePropertiesAsDataNode) {
            cmHandlePropertiesAsMap.put(String.valueOf(dataNode.getLeaves().get("name")),
                    String.valueOf(dataNode.getLeaves().get("value")));
        }
        return cmHandlePropertiesAsMap;
    }

    private static Map<String, String> getCmHandlePropertiesAsMap(
            final List<AdditionalProperty> cmHandlePropertiesAsList) {
        if (cmHandlePropertiesAsList == null || cmHandlePropertiesAsList.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, String> cmHandlePropertiesAsMap = new LinkedHashMap<>();
        for (final AdditionalProperty additionalProperty: cmHandlePropertiesAsList) {
            cmHandlePropertiesAsMap.put(additionalProperty.getName(),
                    additionalProperty.getValue());
        }
        return cmHandlePropertiesAsMap;
    }

    private static Object handleResponse(final @NotNull ResponseEntity<Object> responseEntity) {
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        } else {
            throw new NcmpException("Not able to get resource data.",
                    "DMI status code: " + responseEntity.getStatusCodeValue()
                            + ", DMI response body: " + responseEntity.getBody());
        }
    }

    private static void handleResponseFromDmi(final @NotNull ResponseEntity<String> responseEntity,
        final String exceptionMessage) {
        if (!HttpStatus.valueOf(responseEntity.getStatusCodeValue()).is2xxSuccessful()) {
            throw new NcmpException(exceptionMessage,
                "DMI status code: " + responseEntity.getStatusCodeValue()
                    + ", DMI response body: " + responseEntity.getBody());
        }
    }

    private String getGenericRequestBody(final DataNode cmHandleDataNode) {
        final Collection<DataNode> cmHandlePropertiesAsDataNodes = cmHandleDataNode.getChildDataNodes();
        final Map<String, String> cmHandlePropertiesAsMap = getCmHandlePropertiesAsMap(cmHandlePropertiesAsDataNodes);
        final GenericRequestBody requestBodyObject = GenericRequestBody.builder()
                .operation(GenericRequestBody.OperationEnum.READ)
                .cmHandleProperties(cmHandlePropertiesAsMap)
                .build();
        return prepareOperationBody(requestBodyObject);
    }

    private void parseAndUpdateCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration)
        throws JsonProcessingException {
        final PersistenceCmHandlesList updatedPersistenceCmHandlesList = toPersistenceCmHandlesList(
            dmiPluginRegistration.getDmiPlugin(),
            dmiPluginRegistration.getUpdatedCmHandles());
        final String cmHandlesAsJson = objectMapper.writeValueAsString(updatedPersistenceCmHandlesList);
        cpsDataService.updateNodeLeavesAndExistingDescendantLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                "/dmi-registry", cmHandlesAsJson, NO_TIMESTAMP);
    }

    private void parseAndCreateCmHandlesInDmiRegistrationAndSyncModule(
        final DmiPluginRegistration dmiPluginRegistration) throws JsonProcessingException {
        final PersistenceCmHandlesList createdPersistenceCmHandlesList = toPersistenceCmHandlesList(
            dmiPluginRegistration.getDmiPlugin(),
            dmiPluginRegistration.getCreatedCmHandles());
        registerAndSyncNewCmHandles(createdPersistenceCmHandlesList);
    }

    private static PersistenceCmHandlesList toPersistenceCmHandlesList(final String dmiPlugin,
                                                                       final Collection<CmHandle> cmHandles) {
        final PersistenceCmHandlesList persistenceCmHandlesList = new PersistenceCmHandlesList();
        for (final CmHandle cmHandle : cmHandles) {
            final PersistenceCmHandle persistenceCmHandle = toPersistenceCmHandle(dmiPlugin, cmHandle);
            persistenceCmHandlesList.add(persistenceCmHandle);
        }
        return persistenceCmHandlesList;
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
        fetchAndSyncModules(persistenceCmHandle);
        createAnchor(persistenceCmHandle);
    }

    private static PersistenceCmHandle toPersistenceCmHandle(final String dmiPluginService,
                                                             final CmHandle cmHandle) {
        final PersistenceCmHandle persistenceCmHandle = new PersistenceCmHandle();
        persistenceCmHandle.setDmiServiceName(dmiPluginService);
        persistenceCmHandle.setId(cmHandle.getCmHandleID());
        if (cmHandle.getCmHandleProperties() == null) {
            persistenceCmHandle.setAdditionalProperties(Collections.emptyMap());
        } else {
            persistenceCmHandle.setAdditionalProperties(cmHandle.getCmHandleProperties());
        }
        return persistenceCmHandle;
    }

    private void parseAndRemoveCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        for (final String cmHandle : dmiPluginRegistration.getRemovedCmHandles()) {
            try {
                cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    "/dmi-registry/cm-handles[@id='" + cmHandle + "']", NO_TIMESTAMP);
            } catch (final DataNodeNotFoundException e) {
                log.warn("Datanode {} not deleted message {}", cmHandle, e.getMessage());
            }
        }
    }

    private void fetchAndSyncModules(final PersistenceCmHandle persistenceCmHandle) {
        final Map<String, String> cmHandlePropertiesAsMap = getCmHandlePropertiesAsMap(
            persistenceCmHandle.getAdditionalProperties());

        final List<ModuleReference> moduleReferencesFromCmHandle =
            fetchModuleReferencesFromDmi(persistenceCmHandle, cmHandlePropertiesAsMap);
        final List<ModuleReference> existingModuleReferences = new ArrayList<>();
        final List<ModuleReference> unknownModuleReferences = new ArrayList<>();
        prepareModuleSubsets(moduleReferencesFromCmHandle, existingModuleReferences, unknownModuleReferences);

        final Map<String, String> newYangResourcesModuleNameToContentMap;
        if (unknownModuleReferences.isEmpty()) {
            newYangResourcesModuleNameToContentMap = new HashMap<>();
        } else {
            newYangResourcesModuleNameToContentMap = getNewYangResourcesFromDmi(persistenceCmHandle,
                unknownModuleReferences, cmHandlePropertiesAsMap);
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

    private List<ModuleReference> fetchModuleReferencesFromDmi(final PersistenceCmHandle persistenceCmHandle,
                                                               final Map<String, String> cmHandlePropertiesAsMap) {
        final GenericRequestBody genericRequestBody = GenericRequestBody.builder()
                .cmHandleProperties(cmHandlePropertiesAsMap)
                .build();
        final String jsonBodyWithOnlyCmHandleProperties = prepareOperationBody(genericRequestBody);
        final ResponseEntity<String> dmiFetchModulesResponseEntity =
            dmiOperations.getResourceFromDmiWithJsonData(persistenceCmHandle.getDmiServiceName(),
                    jsonBodyWithOnlyCmHandleProperties, persistenceCmHandle.getId(), "modules");
        return toModuleReferences(dmiFetchModulesResponseEntity);
    }

    private void createAnchor(final PersistenceCmHandle persistenceCmHandle) {
        cpsAdminService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, persistenceCmHandle.getId(),
            persistenceCmHandle.getId());
    }

    private String getRequestBodyToFetchYangResourceFromDmi(final List<ModuleReference> unknownModuleReferences,
                                                            final Map<String, String> cmHandlePropertiesAsMap) {
        final JsonArray moduleReferencesAsJson = getModuleReferencesAsJson(unknownModuleReferences);
        final JsonObject data = new JsonObject();
        data.add("modules", moduleReferencesAsJson);
        final JsonObject jsonRequestObject = new JsonObject();
        jsonRequestObject.add("data", data);
        final Gson gson = new Gson();
        jsonRequestObject.add("cmHandleProperties", gson.toJsonTree(cmHandlePropertiesAsMap));
        return jsonRequestObject.toString();
    }

    private static JsonArray getModuleReferencesAsJson(final List<ModuleReference> unknownModuleReferences) {
        final JsonArray moduleReferences = new JsonArray();

        for (final ModuleReference moduleReference : unknownModuleReferences) {
            final JsonObject moduleReferenceAsJson = new JsonObject();
            moduleReferenceAsJson.addProperty("name", moduleReference.getModuleName());
            moduleReferenceAsJson.addProperty(REVISION, moduleReference.getRevision());
            moduleReferences.add(moduleReferenceAsJson);
        }
        return moduleReferences;
    }

    private Map<String, String> getNewYangResourcesFromDmi(final PersistenceCmHandle persistenceCmHandle,
                                                           final List<ModuleReference> unknownModuleReferences,
                                                           final Map<String, String> cmHandlePropertiesAsMap) {
        final String jsonDataWithDataAndCmHandleProperties = getRequestBodyToFetchYangResourceFromDmi(
                unknownModuleReferences, cmHandlePropertiesAsMap);

        final ResponseEntity<String> moduleResourcesAsJsonString =  dmiOperations.getResourceFromDmiWithJsonData(
                persistenceCmHandle.getDmiServiceName(),
                jsonDataWithDataAndCmHandleProperties,
                persistenceCmHandle.getId(),
                "moduleResources");

        final JsonArray moduleResources = new Gson().fromJson(moduleResourcesAsJsonString.getBody(),
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
        yangResource.setRevision(yangResourceAsJson.get(REVISION).getAsString());
        final String yangSourceJson = yangResourceAsJson.get("yangSource").getAsString();

        String yangSource = JsonUtils.removeWrappingTokens(yangSourceJson);
        yangSource = JsonUtils.removeRedundantEscapeCharacters(yangSource);
        yangResource.setYangSource(yangSource);

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
        moduleReference.setRevision(moduleReferenceAsJson.get(REVISION).getAsString());
        return moduleReference;
    }
}
