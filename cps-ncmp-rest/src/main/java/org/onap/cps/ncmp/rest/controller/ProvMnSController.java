/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe
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

import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE;
import static org.onap.cps.ncmp.api.data.models.OperationType.DELETE;
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA;
import static org.onap.cps.ncmp.impl.provmns.ParameterHelper.NO_OP;
import static org.onap.cps.ncmp.rest.util.ProvMnSExceptionMapper.toProvMnSException;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.onap.cps.ncmp.impl.data.policyexecutor.ClassInstance;
import org.onap.cps.ncmp.impl.data.policyexecutor.OperationDetails;
import org.onap.cps.ncmp.impl.data.policyexecutor.OperationDetailsFactory;
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.ParameterHelper;
import org.onap.cps.ncmp.impl.provmns.ParametersBuilder;
import org.onap.cps.ncmp.impl.provmns.RequestParameters;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.provmns.model.PatchItem;
import org.onap.cps.ncmp.provmns.model.Resource;
import org.onap.cps.ncmp.provmns.model.Scope;
import org.onap.cps.ncmp.rest.util.ProvMnSCmHandleRetriever;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.provmns-base-path}")
@RequiredArgsConstructor
@Slf4j
public class ProvMnSController implements ProvMnS {

    private final DmiRestClient dmiRestClient;
    private final ParametersBuilder parametersBuilder;
    private final PolicyExecutor policyExecutor;
    private final JsonObjectMapper jsonObjectMapper;
    private final OperationDetailsFactory operationDetailsFactory;
    private final ProvMnSCmHandleRetriever provMnSCmHandleRetriever;

    @Value("${app.ncmp.provmns.max-patch-operations:10}")
    private Integer maxNumberOfPatchOperations;

    @Value("${ncmp.policy-executor.enabled:false}")
    private boolean policyExecutorEnabled;

    @Override
    public ResponseEntity<Object> getMoi(final HttpServletRequest httpServletRequest,
                                         final Scope scope,
                                         final String filter,
                                         final List<String> attributes,
                                         final List<String> fields,
                                         final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        final RequestParameters requestParameters = ParameterHelper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = provMnSCmHandleRetriever.getAndValidateYangModelCmHandle(
                requestParameters.fdn(), requestParameters.httpMethodName());
            final UrlTemplateParameters urlTemplateParameters = parametersBuilder.createUrlTemplateParametersForRead(
                yangModelCmHandle, requestParameters.fdn(), scope, filter, attributes, fields, dataNodeSelector);
            return dmiRestClient.synchronousGetOperation(DATA,
                urlTemplateParameters, requestParameters.authorization());
        } catch (final Exception exception) {
            throw toProvMnSException(exception, httpServletRequest.getMethod(), NO_OP);
        }
    }

    @Override
    public ResponseEntity<Object> patchMoi(final HttpServletRequest httpServletRequest,
                                           final List<PatchItem> patchItems) {
        if (patchItems.size() > maxNumberOfPatchOperations) {
            final String title = patchItems.size() + " operations in request, this exceeds the maximum of "
                + maxNumberOfPatchOperations;
            throw new ProvMnSException(httpServletRequest.getMethod(), PAYLOAD_TOO_LARGE, title, NO_OP);
        }
        final RequestParameters requestParameters = ParameterHelper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = provMnSCmHandleRetriever.getAndValidateYangModelCmHandle(
                requestParameters.fdn(), requestParameters.httpMethodName());
            if (policyExecutorEnabled) {
                checkPermissionForEachPatchItem(requestParameters.fdn(), patchItems,
                    yangModelCmHandle, requestParameters.authorization());
            }
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestParameters.fdn());
            return dmiRestClient.synchronousPatchOperation(DATA, patchItems, urlTemplateParameters,
                httpServletRequest.getContentType(), requestParameters.authorization());
        } catch (final Exception exception) {
            throw toProvMnSException(exception, httpServletRequest.getMethod(), NO_OP);
        }
    }

    @Override
    public ResponseEntity<Object> putMoi(final HttpServletRequest httpServletRequest, final String requestBody) {
        final RequestParameters requestParameters = ParameterHelper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = provMnSCmHandleRetriever.getAndValidateYangModelCmHandle(
                requestParameters.fdn(), requestParameters.httpMethodName());
            if (policyExecutorEnabled) {
                final Resource resource = jsonObjectMapper.convertJsonString(requestBody, Resource.class);
                final OperationDetails operationDetails =
                    operationDetailsFactory.buildOperationDetails(CREATE, requestParameters, resource);
                checkPermission(yangModelCmHandle, operationDetails, requestParameters);
            }
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestParameters.fdn());
            return dmiRestClient.synchronousPutOperation(DATA, requestBody,
                urlTemplateParameters, requestParameters.authorization());
        } catch (final Exception exception) {
            throw toProvMnSException(exception, httpServletRequest.getMethod(), NO_OP);
        }
    }

    @Override
    public ResponseEntity<Object> deleteMoi(final HttpServletRequest httpServletRequest) {
        final RequestParameters requestParameters = ParameterHelper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = provMnSCmHandleRetriever.getAndValidateYangModelCmHandle(
                    requestParameters.fdn(), requestParameters.httpMethodName());
            if (policyExecutorEnabled) {
                final OperationDetails operationDetails =
                    operationDetailsFactory.buildOperationDetailsForDelete(requestParameters.fdn());
                checkPermission(yangModelCmHandle, operationDetails, requestParameters);
            }
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestParameters.fdn());
            return dmiRestClient.synchronousDeleteOperation(DATA,
                urlTemplateParameters, requestParameters.authorization());
        } catch (final Exception exception) {
            throw toProvMnSException(exception, httpServletRequest.getMethod(), NO_OP);
        }
    }

    private void checkPermission(final YangModelCmHandle yangModelCmHandle,
                                 final OperationDetails operationDetails,
                                 final RequestParameters requestParameters) {
        final Map<String, List<ClassInstance>> changeRequestAsMap = new HashMap<>(1);
        changeRequestAsMap.put(operationDetails.className(), operationDetails.ClassInstances());
        final String changeRequestAsJson = jsonObjectMapper.asJsonString(changeRequestAsMap);
        if (targetIsRootMo(yangModelCmHandle.getAlternateId(), operationDetails)) {
            throw new DataValidationException("Data manipulation operations are not supported on "
                + requestParameters.fdn(), "");
        }
        final int index = yangModelCmHandle.getAlternateId().length();
        final String resourceIdentifier = operationDetails.parentFdn().substring(index);
        policyExecutor.checkPermission(yangModelCmHandle, operationDetails.operationType(),
            requestParameters.authorization(), resourceIdentifier, changeRequestAsJson);
    }

    private static boolean targetIsRootMo(final String alternateId, final OperationDetails operationDetails) {
        if (DELETE.equals(operationDetails.operationType())) {
            return operationDetails.parentFdn().length() <= alternateId.length();
        }
        return operationDetails.parentFdn().length() < alternateId.length();
    }

    private void checkPermissionForEachPatchItem(final String baseFdn,
                                                 final List<PatchItem> patchItems,
                                                 final YangModelCmHandle yangModelCmHandle,
                                                 final String authorization) {
        int patchItemCounter = 0;
        for (final PatchItem patchItem : patchItems) {
            final String extendedPath = baseFdn + patchItem.getPath();
            final RequestParameters requestParameters =
                ParameterHelper.createRequestParametersForPatch(extendedPath, authorization);
            final OperationDetails operationDetails =
                operationDetailsFactory.buildOperationDetails(requestParameters, patchItem);
            try {
                checkPermission(yangModelCmHandle, operationDetails, requestParameters);
                patchItemCounter++;
            } catch (final Exception exception) {
                final String httpMethodName = "PATCH";
                throw toProvMnSException(exception, httpMethodName, "/" + patchItemCounter);
            }
        }
    }
}
