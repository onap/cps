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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.onap.cps.ncmp.api.impl.operation.DmiOperations;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.GenericRequestBody;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.ncmp.api.models.PersistenceCmHandlesList;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;
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

    private CpsDataService cpsDataService;

    private ObjectMapper objectMapper;

    private CpsQueryService cpsQueryService;

    private DmiOperations dmiOperations;

    /**
     * Constructor Injection for Dependencies.
     * @param dmiOperations dmi operation
     * @param cpsDataService Data Service Interface
     * @param cpsQueryService Query Service Interface
     * @param objectMapper Object Mapper
     */
    public NetworkCmProxyDataServiceImpl(final DmiOperations dmiOperations, final CpsDataService cpsDataService,
        final CpsQueryService cpsQueryService, final ObjectMapper objectMapper) {
        this.dmiOperations = dmiOperations;
        this.cpsDataService = cpsDataService;
        this.cpsQueryService = cpsQueryService;
        this.objectMapper = objectMapper;
    }

    private String getDataspaceName() {
        return NF_PROXY_DATASPACE_NAME;
    }

    @Override
    public DataNode getDataNode(final String cmHandle, final String xpath,
        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNode(getDataspaceName(), cmHandle, xpath, fetchDescendantsOption);
    }

    @Override
    public Collection<DataNode> queryDataNodes(final String cmHandle, final String cpsPath,
                                               final FetchDescendantsOption fetchDescendantsOption) {
        return cpsQueryService.queryDataNodes(getDataspaceName(), cmHandle, cpsPath, fetchDescendantsOption);
    }

    @Override
    public void createDataNode(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        if (!StringUtils.hasText(parentNodeXpath) || "/".equals(parentNodeXpath)) {
            cpsDataService.saveData(getDataspaceName(), cmHandle, jsonData);
        } else {
            cpsDataService.saveData(getDataspaceName(), cmHandle, parentNodeXpath, jsonData);
        }
    }

    @Override
    public void addListNodeElements(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.saveListNodeData(getDataspaceName(), cmHandle, parentNodeXpath, jsonData);
    }

    @Override
    public void updateNodeLeaves(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.updateNodeLeaves(getDataspaceName(), cmHandle, parentNodeXpath, jsonData);
    }

    @Override
    public void replaceNodeTree(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.replaceNodeTree(getDataspaceName(), cmHandle, parentNodeXpath, jsonData);
    }

    @Override
    public void updateDmiPluginRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        if (dmiPluginRegistration.getCreatedCmHandles() != null) {
            parseAndCreateCmHandlesInDmiRegistration(dmiPluginRegistration);
        }
        if (dmiPluginRegistration.getUpdatedCmHandles() != null) {
            parseAndUpdateCmHandlesInDmiRegistration(dmiPluginRegistration);
        }
        if (dmiPluginRegistration.getRemovedCmHandles() != null) {
            parseAndRemoveCmHandlesInDmiRegistration(dmiPluginRegistration);
        }
    }

    private void parseAndCreateCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        try {
            final List<PersistenceCmHandle> createdPersistenceCmHandles =
                new LinkedList<>();
            for (final CmHandle cmHandle: dmiPluginRegistration.getCreatedCmHandles()) {
                createdPersistenceCmHandles.add(toPersistenceCmHandle(dmiPluginRegistration, cmHandle));
            }
            final PersistenceCmHandlesList persistenceCmHandlesList = new PersistenceCmHandlesList();
            persistenceCmHandlesList.setCmHandles(createdPersistenceCmHandles);
            final String cmHandleJsonData = objectMapper.writeValueAsString(persistenceCmHandlesList);
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
            final List<PersistenceCmHandle> updatedPersistenceCmHandles =
                new LinkedList<>();
            for (final CmHandle cmHandle: dmiPluginRegistration.getUpdatedCmHandles()) {
                updatedPersistenceCmHandles.add(toPersistenceCmHandle(dmiPluginRegistration, cmHandle));
            }
            final PersistenceCmHandlesList persistenceCmHandlesList = new PersistenceCmHandlesList();
            persistenceCmHandlesList.setCmHandles(updatedPersistenceCmHandles);
            final String cmHandlesJsonData = objectMapper.writeValueAsString(persistenceCmHandlesList);
            cpsDataService.updateNodeLeavesAndExistingDescendantLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                "/dmi-registry", cmHandlesJsonData);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON Dmi Registry.");
            throw new DataValidationException(
                "Parsing error occurred while processing DMI Plugin Registration" + dmiPluginRegistration, e
                .getMessage(), e);
        }
    }

    private void parseAndRemoveCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        try {
            for (final String cmHandle: dmiPluginRegistration.getRemovedCmHandles()) {
                cpsDataService.deleteListNodeData(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    "/dmi-registry/cm-handles[@id='" + cmHandle + "']");
            }
        } catch (final Exception e) {
            log.error("{} Test", e.getMessage());
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
        for (final var node: additionalPropertyList) {
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
        persistenceCmHandle.setAdditionalProperties(cmHandle.getCmHandleProperties());
        return persistenceCmHandle;
    }

}
