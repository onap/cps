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
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
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

    private static final String NF_PROXY_DATASPACE_NAME = "NFP-Operational";

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
        return cpsDataService.getDataNode(NF_PROXY_DATASPACE_NAME, cmHandle, xpath, fetchDescendantsOption);
    }

    @Override
    public Collection<DataNode> queryDataNodes(final String cmHandle, final String cpsPath,
        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsQueryService.queryDataNodes(NF_PROXY_DATASPACE_NAME, cmHandle, cpsPath, fetchDescendantsOption);
    }

    @Override
    public void createDataNode(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        if (!StringUtils.hasText(parentNodeXpath) || "/".equals(parentNodeXpath)) {
            cpsDataService.saveData(NF_PROXY_DATASPACE_NAME, cmHandle, jsonData, NO_TIMESTAMP);
        } else {
            cpsDataService.saveData(NF_PROXY_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
        }
    }

    @Override
    public void addListNodeElements(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.saveListNodeData(NF_PROXY_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
    }

    @Override
    public void updateNodeLeaves(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.updateNodeLeaves(NF_PROXY_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
    }

    @Override
    public void replaceNodeTree(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.replaceNodeTree(NF_PROXY_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
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
                                                        final String acceptParam,
                                                        final String fieldsQueryParam,
                                                        final Integer depthQueryParam) {

        final var cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final var dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get(NCMP_DMI_SERVICE_NAME));
        final String dmiRequestBody = getGenericRequestBody(cmHandleDataNode);
        final ResponseEntity<Object> response = dmiOperations.getResourceDataOperationalFromDmi(dmiServiceName,
                cmHandle,
                resourceIdentifier,
                fieldsQueryParam,
                depthQueryParam,
                acceptParam,
                dmiRequestBody);
        return handleResponse(response);
    }

    @Override
    public Object getResourceDataPassThroughRunningForCmHandle(final @NotNull String cmHandle,
                                                               final @NotNull String resourceIdentifier,
                                                               final String acceptParam,
                                                               final String fields,
                                                               final Integer depth) {
        final var cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final var dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get(NCMP_DMI_SERVICE_NAME));
        final String dmiRequestBody = getGenericRequestBody(cmHandleDataNode);
        final ResponseEntity<Object> response = dmiOperations.getResourceDataPassThroughRunningFromDmi(dmiServiceName,
                cmHandle,
                resourceIdentifier,
                fields,
                depth,
                acceptParam,
                dmiRequestBody);
        return handleResponse(response);
    }

    @Override
    public void createResourceDataPassThroughRunningForCmHandle(final @NotNull String cmHandle,
                                                                final @NotNull String resourceIdentifier,
                                                                final @NotNull String requestBody,
                                                                final String contentType) {
        final var cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final var dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get(NCMP_DMI_SERVICE_NAME));
        final Collection<DataNode> cmHandlePropertiesList = cmHandleDataNode.getChildDataNodes();
        final Map<String, String> cmHandlePropertiesMap = getCmHandlePropertiesAsMap(cmHandlePropertiesList);
        final var dmiRequestBodyObject = GenericRequestBody.builder()
                .operation(GenericRequestBody.OperationEnum.CREATE)
                .dataType(contentType)
                .data(requestBody)
                .cmHandleProperties(cmHandlePropertiesMap)
                .build();
        final var dmiRequestBody = prepareOperationBody(dmiRequestBodyObject);
        final ResponseEntity<String> responseEntity = dmiOperations
                .createResourceDataPassThroughRunningFromDmi(dmiServiceName,
                        cmHandle,
                        resourceIdentifier,
                        dmiRequestBody);
        handleResponseForPost(responseEntity);
    }

    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandle) {
        return cpsModuleService.getYangResourcesModuleReferences(NF_PROXY_DATASPACE_NAME, cmHandle);
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

    private static Map<String, String> getCmHandlePropertiesAsMap(final Collection<DataNode> cmHandlePropertiesList) {
        if (cmHandlePropertiesList == null || cmHandlePropertiesList.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, String> cmHandlePropertiesMap = new LinkedHashMap<>();
        for (final var node: cmHandlePropertiesList) {
            cmHandlePropertiesMap.put(String.valueOf(node.getLeaves().get("name")),
                    String.valueOf(node.getLeaves().get("value")));
        }
        return cmHandlePropertiesMap;
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

    private static void handleResponseForPost(final @NotNull ResponseEntity<String> responseEntity) {
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new NcmpException("Not able to create resource data.",
                    "DMI status code: " + responseEntity.getStatusCodeValue()
                            + ", DMI response body: " + responseEntity.getBody());
        }
    }

    private String getGenericRequestBody(final DataNode cmHandleDataNode) {
        final Collection<DataNode> cmHandlePropertiesList = cmHandleDataNode.getChildDataNodes();
        final Map<String, String> cmHandlePropertiesMap = getCmHandlePropertiesAsMap(cmHandlePropertiesList);
        final var requestBodyObject = GenericRequestBody.builder()
                .operation(GenericRequestBody.OperationEnum.READ)
                .cmHandleProperties(cmHandlePropertiesMap)
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
        cpsDataService.saveListNodeData(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, "/dmi-registry",
            cmHandleJsonData, NO_TIMESTAMP);

        for (final PersistenceCmHandle persistenceCmHandle : persistenceCmHandlesList.getPersistenceCmHandles()) {
            createAnchorAndSyncModel(persistenceCmHandle);
        }
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
                cpsDataService.deleteListNodeData(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    "/dmi-registry/cm-handles[@id='" + cmHandle + "']", NO_TIMESTAMP);
            } catch (final DataNodeNotFoundException e) {
                log.warn("Datanode {} not deleted message {}", cmHandle, e.getMessage());
            }
        }
    }

    protected void createAnchorAndSyncModel(final PersistenceCmHandle cmHandle) {
        final var modulesForCmHandle =
            dmiOperations.getResourceFromDmi(cmHandle.getDmiServiceName(), cmHandle.getId(), "modules");
        final List<ModuleReference> moduleReferencesFromDmiForCmHandle =
            getModuleReferences(modulesForCmHandle);
        final var knownModuleReferencesInCps =
            cpsModuleService.getYangResourceModuleReferences(NF_PROXY_DATASPACE_NAME);
        final List<ModuleReference> existingModuleReferences = new ArrayList<>();

        final List<ModuleReference> unknownModuleReferences = new ArrayList<>();
        for (final ModuleReference moduleReferenceFromDmiForCmHandle : moduleReferencesFromDmiForCmHandle) {
            if (knownModuleReferencesInCps.contains(moduleReferenceFromDmiForCmHandle)) {
                existingModuleReferences.add(moduleReferenceFromDmiForCmHandle);
            } else {
                unknownModuleReferences.add(moduleReferenceFromDmiForCmHandle);
            }
        }

        final JsonObject requestBodyAsJson = getRequestBodyAsJson(unknownModuleReferences);

        final Map<String, String> newYangResourcesModuleNameToContentMap =
            getNewYangResources(cmHandle, requestBodyAsJson.toString());

        cpsModuleService.createSchemaSetFromModules(NF_PROXY_DATASPACE_NAME, cmHandle.getId(),
            newYangResourcesModuleNameToContentMap, existingModuleReferences);

        cpsAdminService.createAnchor(NF_PROXY_DATASPACE_NAME, cmHandle.getId(), cmHandle.getId());
    }

    private static JsonObject getRequestBodyAsJson(final List<ModuleReference> unknownModuleReferences) {

        final JsonObject requestBodyAsJson = new JsonObject();
        requestBodyAsJson.addProperty("operation", "read");

        final JsonArray moduleReferencesAsJson = getModuleReferencesAsJson(unknownModuleReferences);

        final JsonObject data = new JsonObject();
        data.add("modules", moduleReferencesAsJson);
        requestBodyAsJson.add("data", data);

        return requestBodyAsJson;
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

    private Map<String, String> getNewYangResources(final PersistenceCmHandle cmHandle, final String jsonData) {
        final var moduleResourcesAsJsonString =  dmiOperations.getResourceFromDmiWithJsonData(
            cmHandle.getDmiServiceName(), jsonData, cmHandle.getId(), "moduleResources");

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

    private static List<ModuleReference> getModuleReferences(final ResponseEntity<String> response) {
        final List<ModuleReference> modulesFromDmiForCmHandle = new ArrayList<>();
        final JsonObject convertedObject = new Gson().fromJson(response.getBody(), JsonObject.class);
        final JsonArray moduleReferencesAsJson = convertedObject.getAsJsonArray("schemas");
        for (final JsonElement moduleReferenceAsJson : moduleReferencesAsJson) {
            final ModuleReference moduleReference =
                toModuleReference((JsonObject) moduleReferenceAsJson);
            modulesFromDmiForCmHandle.add(moduleReference);
        }
        return modulesFromDmiForCmHandle;
    }

    private static ModuleReference toModuleReference(final JsonObject moduleReferenceAsJson) {
        final var moduleReference = new ModuleReference();
        moduleReference.setModuleName(moduleReferenceAsJson.get("moduleName").getAsString());
        moduleReference.setRevision(moduleReferenceAsJson.get("revision").getAsString());
        return moduleReference;
    }
}
