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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.operation.DmiOperations;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.ncmp.api.models.PersistenceCmHandlesList;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private static final String NF_PROXY_DATASPACE_NAME = "NFP-Operational";

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";

    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private static final String NCMP_DATA_TOP_PATH = "/dmi-registry";

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
        try {
            final List<PersistenceCmHandle> persistenceCmHandles =
                new ArrayList<>();
            for (final CmHandle cmHandle: dmiPluginRegistration.getCreatedCmHandles()) {
                final PersistenceCmHandle persistenceCmHandle = new PersistenceCmHandle();
                persistenceCmHandle.setDmiServiceName(dmiPluginRegistration.getDmiPlugin());
                persistenceCmHandle.setId(cmHandle.getCmHandle());
                persistenceCmHandle.setAdditionalProperties(cmHandle.getCmHandleProperties());
                persistenceCmHandles.add(persistenceCmHandle);
            }
            final PersistenceCmHandlesList persistenceCmHandlesList = new PersistenceCmHandlesList();
            persistenceCmHandlesList.setCmHandles(persistenceCmHandles);
            final String cmHandleJsonData = objectMapper.writeValueAsString(persistenceCmHandlesList);
            cpsDataService.saveListNodeData(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DATA_TOP_PATH,
                cmHandleJsonData);
        } catch (final JsonProcessingException e) {
            throw new DataValidationException(
                "Parsing error occurred while processing DMI Plugin Registration" + dmiPluginRegistration, e
                .getMessage(), e);
        }
    }

    @Override
    public Object getResourceDataOperationalFoCmHandle(final String cmHandle,
                                                       final String resourceIdentifier,
                                                       final String acceptParam,
                                                       final String fieldsQueryParam,
                                                       final Integer depthQueryParam) {

        final String xpathForDmiRegistryToFetchCmHandle = "dmi-registry/cm-handles@[id=" + cmHandle + "]";
        final var dataNode = cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
                NCMP_DMI_REGISTRY_ANCHOR,
                xpathForDmiRegistryToFetchCmHandle,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        final String dmiServiceName = dataNode.getLeaves().get("dmi-service-name");
        final ResponseEntity<Object> response = dmiOperations.getResouceDataFromNode(dmiServiceName,
                cmHandle,
                resourceIdentifier,
                fieldsQueryParam,
                depthQueryParam,
                acceptParam,
                "");

        return handleResponse(response);
    }

    private Object handleResponse(final ResponseEntity<Object> responseEntity) {
        return responseEntity.getBody();
    }


}
