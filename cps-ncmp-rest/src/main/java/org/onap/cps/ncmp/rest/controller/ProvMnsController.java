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

package org.onap.cps.ncmp.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.provmns.ErrorResponseBuilder;
import org.onap.cps.ncmp.impl.provmns.ParametersBuilder;
import org.onap.cps.ncmp.impl.provmns.RequestParameters;
import org.onap.cps.ncmp.impl.provmns.RequestValidator;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.provmns-base-path}")
@RequiredArgsConstructor
public class ProvMnsController implements ProvMnS {

    private static final String NO_AUTHORIZATION = null;
    private final AlternateIdMatcher alternateIdMatcher;
    private final DmiRestClient dmiRestClient;
    private final InventoryPersistence inventoryPersistence;
    private final ParametersBuilder parametersBuilder;
    private final RequestValidator requestValidator;
    private final ErrorResponseBuilder errorResponseBuilder;
    private final PolicyExecutor policyExecutor;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    public ResponseEntity<Object> getMoi(final HttpServletRequest httpServletRequest, final Scope scope,
                                                   final String filter, final List<String> attributes,
                                                   final List<String> fields,
                                                   final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        final RequestParameters requestParameters =
            RequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    requestParameters.getAlternateId(), "/"));
            final ResponseEntity<Object> errorResponse =
                requestValidator.checkValidElseReturnNull(yangModelCmHandle, "GET");
            if (errorResponse != null) {
                return errorResponse;
            }
            final UrlTemplateParameters urlTemplateParameters = parametersBuilder.createUrlTemplateParametersForGet(
                scope, filter, attributes,
                fields, dataNodeSelector,
                yangModelCmHandle);
            return dmiRestClient.synchronousGetOperation(
                RequiredDmiService.DATA, urlTemplateParameters, OperationType.READ);
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String reason = buildNotFoundMessage(requestParameters.getAlternateId());
            return errorResponseBuilder.buildErrorResponseGet(HttpStatus.NOT_FOUND, reason);
        }
    }

    @Override
    public ResponseEntity<Object> patchMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final RequestParameters requestParameters =
            RequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    requestParameters.getAlternateId(), "/"));
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String reason = buildNotFoundMessage(requestParameters.getAlternateId());
            return errorResponseBuilder.buildErrorResponsePatch(HttpStatus.NOT_FOUND, reason);
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Object> putMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final RequestParameters requestParameters =
            RequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    requestParameters.getAlternateId(), "/"));
            final ResponseEntity<Object> errorResponse =
                requestValidator.checkValidElseReturnNull(yangModelCmHandle, "PUT");
            if (errorResponse != null) {
                return errorResponse;
            }
            try {
                policyExecutor.checkPermission(yangModelCmHandle,
                    OperationType.CREATE,
                    null,
                    requestParameters.getAlternateId(),
                    jsonObjectMapper.asJsonString(policyExecutor.createOperationDetailsFromRequest(
                        OperationType.CREATE, requestParameters, resource))
                );
            } catch (final RuntimeException exception) {
                return errorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_ACCEPTABLE,
                                                                            exception.getMessage());
            }
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForPut(resource,
                    yangModelCmHandle);
            return dmiRestClient.synchronousPutOperation(
                RequiredDmiService.DATA, urlTemplateParameters, OperationType.CREATE);
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String reason = buildNotFoundMessage(requestParameters.getAlternateId());
            return errorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_FOUND, reason);
        }
    }

    @Override
    public ResponseEntity<Object> deleteMoi(final HttpServletRequest httpServletRequest) {
        final RequestParameters requestParameters =
            RequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        try {
            final String cmHandleId = alternateIdMatcher.getCmHandleId(requestParameters.getAlternateId());
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);

            final ResponseEntity<Object> errorResponse =
                requestValidator.checkValidElseReturnNull(yangModelCmHandle, "DELETE");
            if (errorResponse != null) {
                return errorResponse;
            }
            try {
                policyExecutor.checkPermission(yangModelCmHandle,
                    OperationType.DELETE,
                    NO_AUTHORIZATION,
                    requestParameters.getAlternateId(),
                    jsonObjectMapper.asJsonString(policyExecutor.deleteOperationDetailsFromRequest(
                        OperationType.DELETE, requestParameters.getAlternateId()))
                );
            } catch (final RuntimeException exception) {
                return errorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_ACCEPTABLE,
                    exception.getMessage());
            }
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForDelete(yangModelCmHandle);
            return dmiRestClient.synchronousDeleteOperation(RequiredDmiService.DATA, urlTemplateParameters);
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String reason = buildNotFoundMessage(requestParameters.getAlternateId());
            return errorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_FOUND, reason);
        }
    }

    private String buildNotFoundMessage(final String alternateId) {
        return alternateId + " not found";
    }

}
