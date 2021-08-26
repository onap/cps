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
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
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
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
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

    private static final OffsetDateTime NO_TIMESTAMP = null;

    private static  final String NCMP_DMI_SERVICE_NAME = "dmi-service-name";

    private CpsDataService cpsDataService;

    private ObjectMapper objectMapper;

    private CpsQueryService cpsQueryService;

    private DmiOperations dmiOperations;

    /**
     * Constructor Injection for Dependencies.
     * @param dmiOperations DMI operation
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
            cpsDataService.saveData(getDataspaceName(), cmHandle, jsonData, NO_TIMESTAMP);
        } else {
            cpsDataService.saveData(getDataspaceName(), cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
        }
    }

    @Override
    public void addListNodeElements(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.saveListNodeData(getDataspaceName(), cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
    }

    @Override
    public void updateNodeLeaves(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.updateNodeLeaves(getDataspaceName(), cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
    }

    @Override
    public void replaceNodeTree(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.replaceNodeTree(getDataspaceName(), cmHandle, parentNodeXpath, jsonData, NO_TIMESTAMP);
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
                                                                final @NotNull Object requestBody,
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
        final ResponseEntity<Void> responseEntity = dmiOperations
                .createResourceDataPassThroughRunningFromDmi(dmiServiceName,
                        cmHandle,
                        resourceIdentifier,
                        dmiRequestBody);
        handleResponseForPost(responseEntity);
    }

    private DataNode fetchDataNodeFromDmiRegistryForCmHandle(final String cmHandle) {
        final String xpathForDmiRegistryToFetchCmHandle = "/dmi-registry/cm-handles[@id='" + cmHandle + "']";
        final var dataNode = cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
                NCMP_DMI_REGISTRY_ANCHOR,
                xpathForDmiRegistryToFetchCmHandle,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        return dataNode;
    }

    private String prepareOperationBody(final GenericRequestBody requestBodyObject) {
        try {
            return objectMapper.writeValueAsString(requestBodyObject);
        } catch (final JsonProcessingException je) {
            log.error("Parsing error occurred while converting Object to JSON.");
            throw new NcmpException("Parsing error occurred while converting given object to JSON.",
                    je.getMessage());
        }
    }

    private Map<String, String> getCmHandlePropertiesAsMap(final Collection<DataNode> cmHandlePropertiesList) {
        if (cmHandlePropertiesList == null || cmHandlePropertiesList.size() == 0) {
            return null;
        }
        final Map<String, String> cmHandlePropertiesMap = new LinkedHashMap<>();
        for (final var node: cmHandlePropertiesList) {
            cmHandlePropertiesMap.put(String.valueOf(node.getLeaves().get("name")),
                    String.valueOf(node.getLeaves().get("value")));
        }
        return cmHandlePropertiesMap;
    }

    private Object handleResponse(final @NotNull ResponseEntity<Object> responseEntity) {
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        } else {
            throw new NcmpException("Not able to get resource data.",
                    "DMI status code: " + responseEntity.getStatusCodeValue()
                            + ", DMI response body: " + responseEntity.getBody());
        }
    }

    private void handleResponseForPost(final @NotNull ResponseEntity<Void> responseEntity) {
        if (responseEntity.getStatusCode() != HttpStatus.CREATED) {
            throw new NcmpException("Not able to create resource data.",
                    "DMI status code: " + responseEntity.getStatusCodeValue()
                            + ", DMI response body: " + responseEntity.getBody());
        }
    }

    private String getGenericRequestBody(final DataNode cmHandleDataNode) {
        final Collection<DataNode> cmHandlePropertiesList = cmHandleDataNode.getChildDataNodes();
        final Map<String, String> cmHandlePropertiesMap = getCmHandlePropertiesAsMap(cmHandlePropertiesList);
        final var requetBodyObject = GenericRequestBody.builder()
                .operation(GenericRequestBody.OperationEnum.READ)
                .cmHandleProperties(cmHandlePropertiesMap)
                .build();
        return prepareOperationBody(requetBodyObject);
    }

    private PersistenceCmHandle toPersistenceCmHandle(final String dmiPluginService,
                                                      final CmHandle cmHandle) {
        final PersistenceCmHandle persistenceCmHandle = new PersistenceCmHandle();
        persistenceCmHandle.setDmiServiceName(dmiPluginService);
        persistenceCmHandle.setId(cmHandle.getCmHandleID());
        if (cmHandle.getCmHandleProperties() == null) {
            persistenceCmHandle.setAdditionalProperties(Collections.EMPTY_MAP);
        } else {
            persistenceCmHandle.setAdditionalProperties(cmHandle.getCmHandleProperties());
        }
        return persistenceCmHandle;
    }

    private void parseAndCreateCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        try {
            final List<PersistenceCmHandle> createdPersistenceCmHandles =
                    new LinkedList<>();
            for (final CmHandle cmHandle: dmiPluginRegistration.getCreatedCmHandles()) {
                createdPersistenceCmHandles.add(toPersistenceCmHandle(dmiPluginRegistration.getDmiPlugin(), cmHandle));
            }
            final PersistenceCmHandlesList persistenceCmHandlesList = new PersistenceCmHandlesList();
            persistenceCmHandlesList.setCmHandles(createdPersistenceCmHandles);
            final String cmHandleJsonData = objectMapper.writeValueAsString(persistenceCmHandlesList);
            cpsDataService.saveListNodeData(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, "/dmi-registry",
                    cmHandleJsonData, NO_TIMESTAMP);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON for DMI Registry.");
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
                updatedPersistenceCmHandles.add(toPersistenceCmHandle(dmiPluginRegistration.getDmiPlugin(), cmHandle));
            }
            final PersistenceCmHandlesList persistenceCmHandlesList = new PersistenceCmHandlesList();
            persistenceCmHandlesList.setCmHandles(updatedPersistenceCmHandles);
            final String cmHandlesJsonData = objectMapper.writeValueAsString(persistenceCmHandlesList);
            cpsDataService.updateNodeLeavesAndExistingDescendantLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    "/dmi-registry", cmHandlesJsonData, NO_TIMESTAMP);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON DMI Registry.");
            throw new DataValidationException(
                    "Parsing error occurred while processing DMI Plugin Registration" + dmiPluginRegistration, e
                    .getMessage(), e);
        }
    }

    private void parseAndRemoveCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        for (final String cmHandle: dmiPluginRegistration.getRemovedCmHandles()) {
            try {
                cpsDataService.deleteListNodeData(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                        "/dmi-registry/cm-handles[@id='" + cmHandle + "']", NO_TIMESTAMP);
            } catch (final DataNodeNotFoundException e) {
                log.warn("Datanode {} not deleted message {}", cmHandle, e.getMessage());
            }
        }
    }
}
