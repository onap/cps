/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration.DmiProperties;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.onap.cps.ncmp.api.impl.operation.DmiOperations;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.GenericRequestBody;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.ncmp.api.models.PersistenceCmHandlesList;
import org.onap.cps.ncmp.api.models.YangResource;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Slf4j
@Service
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private static final String NF_PROXY_DATASPACE_NAME = "NFP-Operational";

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";

    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private CpsDataService cpsDataService;

    private ObjectMapper objectMapper;

    private CpsQueryService cpsQueryService;

    private DmiOperations dmiOperations;

    private CpsModuleService cpsModuleService;

    private RestTemplate restTemplate;

    private DmiProperties dmiProperties;

    /**
     * Constructor Injection for Dependencies.
     * @param dmiProperties dmi properties
     * @param dmiOperations dmi operation
     * @param cpsModuleService cps module service
     * @param cpsDataService Data Service Interface
     * @param cpsQueryService Query Service Interface
     * @param objectMapper Object Mapper
     * @param restTemplate rest template
     */
    public NetworkCmProxyDataServiceImpl(final DmiProperties dmiProperties,
                                         final DmiOperations dmiOperations,
                                         final CpsModuleService cpsModuleService,
                                         final CpsDataService cpsDataService,
                                         final CpsQueryService cpsQueryService,
                                         final ObjectMapper objectMapper,
                                         final RestTemplate restTemplate) {
        this.dmiProperties = dmiProperties;
        this.dmiOperations = dmiOperations;
        this.cpsModuleService = cpsModuleService;
        this.cpsDataService = cpsDataService;
        this.cpsQueryService = cpsQueryService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
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
            cpsDataService.saveData(NF_PROXY_DATASPACE_NAME, cmHandle, jsonData);
        } else {
            cpsDataService.saveData(NF_PROXY_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData);
        }
    }

    @Override
    public void addListNodeElements(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.saveListNodeData(NF_PROXY_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData);
    }

    @Override
    public void updateNodeLeaves(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.updateNodeLeaves(NF_PROXY_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData);
    }

    @Override
    public void replaceNodeTree(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.replaceNodeTree(NF_PROXY_DATASPACE_NAME, cmHandle, parentNodeXpath, jsonData);
    }

    @Override
    public void updateDmiPluginRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        if (dmiPluginRegistration.getCreatedCmHandles() != null) {
            parseAndCreateCmHandlesInDmiRegistration(dmiPluginRegistration);
        }
        if (dmiPluginRegistration.getUpdatedCmHandles() != null) {
            parseAndUpdateCmHandlesInDmiRegistration(dmiPluginRegistration);
        }
    }

    private void parseAndCreateCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        try {
            final var persistenceCmHandles = new PersistenceCmHandlesList();
            for (final CmHandle cmHandle : dmiPluginRegistration.getCreatedCmHandles()) {
                final PersistenceCmHandle persistenceCmHandle = toPersistenceCmHandle(dmiPluginRegistration, cmHandle);
                persistenceCmHandles.add(persistenceCmHandle);
                modelSync(persistenceCmHandle.getId());
            }

            final String cmHandleJsonData = objectMapper.writeValueAsString(persistenceCmHandles);
            cpsDataService.saveListNodeData(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, "/dmi-registry",
                cmHandleJsonData);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON for Dmi Registry.");
            throw new DataValidationException(
                "Parsing error occurred while processing DMI Plugin Registration" + dmiPluginRegistration, e
                .getMessage(), e);
        }
    }

    private void parseAndUpdateCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        try {
            final PersistenceCmHandlesList persistenceCmHandles = new PersistenceCmHandlesList();
            for (final CmHandle cmHandle : dmiPluginRegistration.getUpdatedCmHandles()) {
                final PersistenceCmHandle persistenceCmHandle = toPersistenceCmHandle(dmiPluginRegistration, cmHandle);
                persistenceCmHandles.add(persistenceCmHandle);
            }

            final String cmHandlesJsonData = objectMapper.writeValueAsString(persistenceCmHandles);
            cpsDataService.updateNodeLeavesAndExistingDescendantLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                "/dmi-registry", cmHandlesJsonData);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON Dmi Registry.");
            throw new DataValidationException(
                "Parsing error occurred while processing DMI Plugin Registration" + dmiPluginRegistration, e
                .getMessage(), e);
        }
    }

    @Override
    public Object getResourceDataOperationalForCmHandle(final @NotNull String cmHandle,
        final @NotNull String resourceIdentifier,
        final String acceptParam,
        final String fieldsQueryParam,
        final Integer depthQueryParam) {

        final var dataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final var dmiServiceName = String.valueOf(dataNode.getLeaves().get("dmi-service-name"));
        final Collection<DataNode> additionalPropsList = dataNode.getChildDataNodes();
        final var jsonBody = prepareOperationBody(GenericRequestBody.OperationEnum.READ, additionalPropsList);
        final ResponseEntity<Object> response = dmiOperations.getResouceDataOperationalFromDmi(dmiServiceName,
            cmHandle,
            resourceIdentifier,
            fieldsQueryParam,
            depthQueryParam,
            acceptParam,
            jsonBody);
        return handleResponse(response);
    }

    @Override
    public Object getResourceDataPassThroughRunningForCmHandle(final @NotNull String cmHandle,
        final @NotNull String resourceIdentifier,
        final String accept,
        final String fields,
        final Integer depth) {
        final var cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);
        final var dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get("dmi-service-name"));
        final Collection<DataNode> additionalPropsList = cmHandleDataNode.getChildDataNodes();
        final var dmiRequesBody = prepareOperationBody(GenericRequestBody.OperationEnum.READ, additionalPropsList);
        final ResponseEntity<Object> response = dmiOperations.getResouceDataPassThroughRunningFromDmi(dmiServiceName,
            cmHandle,
            resourceIdentifier,
            fields,
            depth,
            accept,
            dmiRequesBody);
        return handleResponse(response);
    }

    private DataNode fetchDataNodeFromDmiRegistryForCmHandle(final String cmHandle) {
        final String xpathForDmiRegistryToFetchCmHandle = "/dmi-registry/cm-handles[@id='" + cmHandle + "']";
        final var dataNode = cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR,
            xpathForDmiRegistryToFetchCmHandle,
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        return dataNode;
    }

    private String prepareOperationBody(final GenericRequestBody.OperationEnum operation,
        final Collection<DataNode> additionalPropertyList) {
        final var requestBody = new GenericRequestBody();
        final Map<String, String> additionalPropertyMap = getAdditionalPropertiesMap(additionalPropertyList);
        requestBody.setOperation(GenericRequestBody.OperationEnum.READ);
        requestBody.setCmHandleProperties(additionalPropertyMap);
        try {
            final var requestJson = objectMapper.writeValueAsString(requestBody);
            return requestJson;
        } catch (final JsonProcessingException je) {
            log.error("Parsing error occurred while converting Object to JSON.");
            throw new NcmpException("Parsing error occurred while converting given object to JSON.",
                je.getMessage());
        }
    }

    private Map<String, String> getAdditionalPropertiesMap(final Collection<DataNode> additionalPropertyList) {
        if (additionalPropertyList == null || additionalPropertyList.size() == 0) {
            return null;
        }
        final Map<String, String> additionalPropertyMap = new LinkedHashMap<>();
        for (final var node : additionalPropertyList) {
            additionalPropertyMap.put(String.valueOf(node.getLeaves().get("name")),
                String.valueOf(node.getLeaves().get("value")));
        }
        return additionalPropertyMap;
    }

    private Object handleResponse(final ResponseEntity<Object> responseEntity) {
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        } else {
            throw new NcmpException("Not able to get resource data.",
                "DMI status code: " + responseEntity.getStatusCodeValue()
                    + ", DMI response body: " + responseEntity.getBody());
        }
    }


    private PersistenceCmHandle toPersistenceCmHandle(final DmiPluginRegistration dmiPluginRegistration,
        final CmHandle cmHandle) {
        final PersistenceCmHandle persistenceCmHandle = new PersistenceCmHandle();
        persistenceCmHandle.setDmiServiceName(dmiPluginRegistration.getDmiPlugin());
        persistenceCmHandle.setId(cmHandle.getCmHandleID());
        if (cmHandle.getCmHandleProperties() == null) {
            persistenceCmHandle.setAdditionalProperties(Collections.EMPTY_MAP);
        } else {
            persistenceCmHandle.setAdditionalProperties(cmHandle.getCmHandleProperties());
        }
        return persistenceCmHandle;
    }

    protected void modelSync(final String cmHandle) {
        final var response = callDmi(cmHandle, "modules");

        final List<ModuleReference> modulesFromDmiForCmHandle = getModuleReferences(response);

        final var knownModulesInCps = cpsModuleService.getAllYangResourcesModuleReferences();

        final List<ModuleReference> missingModules = new ArrayList<>();
        for (final ModuleReference moduleFromDmiForCmHandle : modulesFromDmiForCmHandle) {
            if (!knownModulesInCps.contains(moduleFromDmiForCmHandle)) {
                missingModules.add(moduleFromDmiForCmHandle);
            }
        }

        final var resources = callDmi(cmHandle, "moduleResources");
        final JsonArray convertResources = new Gson().fromJson(resources.getBody(), JsonArray.class);
        final Map<String, String> newYangResourcesModuleNameToContentMap = new HashMap<>();

        for (int i = 0; i < convertResources.size(); i++) {
            final YangResource yangResource = toYangResource(convertResources, i);

            for (final ModuleReference moduleReference : missingModules) {
                if (!moduleReference.getName().equals(yangResource.getName())) {
                    newYangResourcesModuleNameToContentMap.put(yangResource.getName(), yangResource.getYangSource());
                }
            }
        }

        cpsModuleService.createSchemaSetFromModules(NF_PROXY_DATASPACE_NAME, cmHandle,
            newYangResourcesModuleNameToContentMap, modulesFromDmiForCmHandle);
    }

    private YangResource toYangResource(final JsonArray convertResources, final int i) {
        final JsonObject object = (JsonObject) convertResources.get(i);
        final var name = object.get("name").getAsString();
        final var revision = object.get("revision").getAsString();
        final var yangSource = object.get("yang-source").getAsString();

        final YangResource yangResource = new YangResource();
        yangResource.setName(name);
        yangResource.setRevision(revision);
        yangResource.setYangSource(yangSource);
        return yangResource;
    }

    private List<ModuleReference> getModuleReferences(final ResponseEntity<String> response) {
        final List<ModuleReference> modulesFromDmiForCmHandle = new ArrayList<>();
        final JsonObject convertedObject = new Gson().fromJson(response.getBody(), JsonObject.class);
        final JsonObject schemas = convertedObject.getAsJsonObject("schemas");
        final JsonArray modules = schemas.getAsJsonArray("schema");
        for (int i = 0; i < modules.size(); i++) {
            final JsonObject object = (JsonObject) modules.get(i);
            final var name = object.get("identifier").getAsString();
            final var namespace = object.get("namespace").getAsString();
            final var revision = object.get("version").getAsString();

            final var moduleRef = new ModuleReference();
            moduleRef.setName(name);
            moduleRef.setNamespace(namespace);
            moduleRef.setRevision(revision);

            modulesFromDmiForCmHandle.add(moduleRef);
        }
        return modulesFromDmiForCmHandle;
    }

    private ResponseEntity<String> callDmi(final String cmHandle, final String endpoint) {
        final var url = buildDmiUrl(cmHandle, endpoint);
        final var httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(dmiProperties.getAuthUsername(), dmiProperties.getAuthPassword());
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        final var httpEntity = new HttpEntity<>(httpHeaders);
        return restTemplate.postForEntity(url, httpEntity, String.class);
    }

    private  String buildDmiUrl(final String cmHandle, final String endpoint) {
        return UriComponentsBuilder
            .fromHttpUrl("http://" + dmiProperties.getDmiPluginBasePath())
            .path("/v1/ch/" + cmHandle + "/" + endpoint)
            .toUriString();
    }
}
