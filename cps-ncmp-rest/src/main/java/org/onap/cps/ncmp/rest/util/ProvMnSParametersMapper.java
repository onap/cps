/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
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

package org.onap.cps.ncmp.rest.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.rest.provmns.exception.ProvMnSNotCompatible;
import org.onap.cps.ncmp.rest.provmns.exception.ProvMnSNotReady;
import org.onap.cps.ncmp.rest.provmns.translation.ConfigurationManagementOperation;
import org.onap.cps.ncmp.rest.provmns.translation.OperationEntry;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvMnSParametersMapper {

    private final JsonObjectMapper jsonObjectMapper;
    private final ObjectMapper objectMapper;

    /**
     * Creates a UrlTemplateParameters object containing the relevant fields for a get.
     *
     * @param scope               Provided className parameter.
     * @param filter              Filter string.
     * @param attributes          Attributes List.
     * @param fields              Fields list
     * @param dataNodeSelector    dataNodeSelector parameter
     * @param yangModelCmHandle   yangModelCmHandle object for resolved alternate ID
     * @return UrlTemplateParameters object.
     */
    public UrlTemplateParameters getUrlTemplateParameters(final Scope scope, final String filter,
                                                          final List<String> attributes, final List<String> fields,
                                                      final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector,
                                                      final YangModelCmHandle yangModelCmHandle) {
        return RestServiceUrlTemplateBuilder.newInstance()
            .queryParameter("scopeType", scope.getScopeType() != null
                ? scope.getScopeType().getValue() : null)
            .queryParameter("scopeLevel", scope.getScopeLevel() != null
                ? scope.getScopeLevel().toString() : null)
            .queryParameter("filter", filter)
            .queryParameter("attributes", attributes != null ? attributes.toString() : null)
            .queryParameter("fields", fields != null ? fields.toString() : null)
            .queryParameter("dataNodeSelector", dataNodeSelector.getDataNodeSelector() != null
                ? dataNodeSelector.getDataNodeSelector() : null)
            .createUrlTemplateParameters(yangModelCmHandle.getDmiServiceName(), "ProvMnS");
    }

    /**
     * Creates a UrlTemplateParameters object containing the relevant fields for a put.
     *
     * @param resource            Provided resource parameter.
     * @param yangModelCmHandle   yangModelCmHandle object for resolved alternate ID
     * @return UrlTemplateParameters object.
     */
    public UrlTemplateParameters putUrlTemplateParameters(final Resource resource,
                                                          final YangModelCmHandle yangModelCmHandle) {

        return RestServiceUrlTemplateBuilder.newInstance()
            .queryParameter("resource", resource.toString())
            .createUrlTemplateParameters(yangModelCmHandle.getDmiServiceName(), "ProvMnS");
    }

    /**
     * Check if dataProducerIdentifier is empty or null
     * and yangModelCmHandle is in a ready state, if so throw exception.
     *
     * @param yangModelCmHandle given yangModelCmHandle.
     */
    public void checkDataProducerIdentifierAndReadyState(final YangModelCmHandle yangModelCmHandle, final String type) {
        if (yangModelCmHandle.getDataProducerIdentifier() == null
            || yangModelCmHandle.getDataProducerIdentifier().isEmpty()) {
            throw new ProvMnSNotCompatible(yangModelCmHandle.getId(), type);
        }
        if (yangModelCmHandle.getCompositeState().getCmHandleState() != CmHandleState.READY) {
            throw new ProvMnSNotReady(yangModelCmHandle.getId(),
                yangModelCmHandle.getCompositeState().getCmHandleState(), type);
        }
    }

    /**
     * Red.
     *
     * @param operation   Type of operation delete, create etc.
     * @param path        request parameters including uri-ldn-first-part, className and id
     * @param resource    provided request resource
     * @return JSON string
     */
    public String configurationManagementOperationToJson(final String operation, final ProvMnsRequestParameters path,
                                                         final Resource resource) {
        final ConfigurationManagementOperation configurationManagementOperation =
            new ConfigurationManagementOperation();
        final Map<String, List<OperationEntry>> changeRequest = new HashMap<>();
        final OperationEntry operationEntry = new OperationEntry();

        configurationManagementOperation.setOperation(operation);
        configurationManagementOperation.setTargetIdentifier(path.getAlternateId());

        final String resourceJson = jsonObjectMapper.asJsonString(resource);

        try {
            final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
            final Map<String, Object> fullValue = objectMapper.readValue(resourceJson, typeRef);

            operationEntry.setId(path.getId());
            operationEntry.setAttributes(fullValue.get("attributes"));
        } catch (final JsonProcessingException exception) {
            log.debug("JSON processing error: {}", exception);
        }

        changeRequest.put(path.getClassName(), List.of(operationEntry));
        configurationManagementOperation.setChangeRequest(changeRequest);
        return jsonObjectMapper.asJsonString(configurationManagementOperation);
    }
}
