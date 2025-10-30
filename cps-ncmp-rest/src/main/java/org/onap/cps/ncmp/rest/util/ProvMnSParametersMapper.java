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
import org.onap.cps.ncmp.rest.provmns.translation.PolicyExecutorOperationDetails;
import org.onap.cps.ncmp.rest.provmns.translation.PolicyExecutorOperationEntry;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvMnSParametersMapper {

    private static final String PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE =
        "Registered DMI does not support the ProvMnS interface.";

    private final ProvMnSErrorResponseBuilder provMnSErrorResponseBuilder;
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
     * and yangModelCmHandle is in a ready state, if so return error response.
     *
     * @param yangModelCmHandle given yangModelCmHandle.
     */
    public ResponseEntity<Object> checkDataProducerIdentifierAndReadyState(final YangModelCmHandle yangModelCmHandle,
                                                                           final String type) {
        if (yangModelCmHandle.getDataProducerIdentifier() == null
            || yangModelCmHandle.getDataProducerIdentifier().isEmpty()) {
            return switch (type) {
                case "GET" -> provMnSErrorResponseBuilder.buildErrorResponseGet(HttpStatus.UNPROCESSABLE_ENTITY,
                    PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE);
                case "PATCH" -> provMnSErrorResponseBuilder.buildErrorResponsePatch(HttpStatus.UNPROCESSABLE_ENTITY,
                    PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE);
                default -> provMnSErrorResponseBuilder.buildErrorResponseDefault(HttpStatus.UNPROCESSABLE_ENTITY,
                    PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE);
            };
        }
        if (yangModelCmHandle.getCompositeState().getCmHandleState() != CmHandleState.READY) {
            return switch (type) {
                case "GET" -> provMnSErrorResponseBuilder.buildErrorResponseGet(HttpStatus.NOT_ACCEPTABLE,
                    provMnSErrorResponseBuilder.buildNotReadyStateMessage(yangModelCmHandle));
                case "PATCH" -> provMnSErrorResponseBuilder.buildErrorResponsePatch(HttpStatus.NOT_ACCEPTABLE,
                    provMnSErrorResponseBuilder.buildNotReadyStateMessage(yangModelCmHandle));
                default -> provMnSErrorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_ACCEPTABLE,
                    provMnSErrorResponseBuilder.buildNotReadyStateMessage(yangModelCmHandle));
            };
        }
        return null;
    }

    /**
     * Creates a policyExecutorOperationDetails object and converts it to json.
     *
     * @param operation   Type of operation delete, create etc.
     * @param path        request parameters including uri-ldn-first-part, className and id
     * @param resource    provided request resource
     * @return JSON string
     */
    public String policyExecutorOperationToJson(final String operation, final ProvMnsRequestParameters path,
                                                final Resource resource) {
        final PolicyExecutorOperationDetails policyExecutorOperationDetails =
            new PolicyExecutorOperationDetails();
        final Map<String, List<PolicyExecutorOperationEntry>> changeRequest = new HashMap<>();
        final PolicyExecutorOperationEntry policyExecutorOperationEntry = new PolicyExecutorOperationEntry();

        policyExecutorOperationDetails.setOperation(operation);
        policyExecutorOperationDetails.setTargetIdentifier(path.getAlternateId());

        final String resourceJson = jsonObjectMapper.asJsonString(resource);

        try {
            final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
            final Map<String, Object> fullValue = objectMapper.readValue(resourceJson, typeRef);

            policyExecutorOperationEntry.setId(path.getId());
            policyExecutorOperationEntry.setAttributes(fullValue.get("attributes"));
        } catch (final JsonProcessingException exception) {
            log.debug("JSON processing error: {}", exception);
        }

        changeRequest.put(path.getClassName(), List.of(policyExecutorOperationEntry));
        policyExecutorOperationDetails.setChangeRequest(changeRequest);
        return jsonObjectMapper.asJsonString(policyExecutorOperationDetails);
    }
}
