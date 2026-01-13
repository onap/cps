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
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA;
import static org.onap.cps.ncmp.impl.provmns.ParameterHelper.NO_OP;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import io.netty.handler.timeout.TimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.data.policyexecutor.ClassInstance;
import org.onap.cps.ncmp.impl.data.policyexecutor.OperationDetails;
import org.onap.cps.ncmp.impl.data.policyexecutor.OperationDetailsFactory;
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.ParameterHelper;
import org.onap.cps.ncmp.impl.provmns.ParametersBuilder;
import org.onap.cps.ncmp.impl.provmns.RequestParameters;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.PatchItem;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.provmns-base-path}")
@RequiredArgsConstructor
@Slf4j
public class ProvMnSController implements ProvMnS {

    private static final String NO_AUTHORIZATION = null;
    private static final String PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE =
        "Registered DMI does not support the ProvMnS interface.";

    private final AlternateIdMatcher alternateIdMatcher;
    private final DmiRestClient dmiRestClient;
    private final InventoryPersistence inventoryPersistence;
    private final ParametersBuilder parametersBuilder;
    private final PolicyExecutor policyExecutor;
    private final JsonObjectMapper jsonObjectMapper;
    private final OperationDetailsFactory operationDetailsFactory;

    @Value("${app.ncmp.provmns.max-patch-operations:10}")
    private Integer maxNumberOfPatchOperations;

    @Override
    public ResponseEntity<Object> getMoi(final HttpServletRequest httpServletRequest,
                                         final Scope scope,
                                         final String filter,
                                         final List<String> attributes,
                                         final List<String> fields,
                                         final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        final RequestParameters requestParameters = ParameterHelper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = getAndValidateYangModelCmHandle(requestParameters);
            final UrlTemplateParameters urlTemplateParameters = parametersBuilder.createUrlTemplateParametersForRead(
                yangModelCmHandle, requestParameters.fdn(), scope, filter, attributes, fields, dataNodeSelector);
            return dmiRestClient.synchronousGetOperation(DATA, urlTemplateParameters);
        } catch (final Exception exception) {
            throw toProvMnSException(httpServletRequest.getMethod(), exception, NO_OP);
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
            final YangModelCmHandle yangModelCmHandle = getAndValidateYangModelCmHandle(requestParameters);
            checkPermissionForEachPatchItem(requestParameters.fdn(), patchItems, yangModelCmHandle);
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestParameters.fdn());
            return dmiRestClient.synchronousPatchOperation(DATA, patchItems, urlTemplateParameters,
                httpServletRequest.getContentType());
        } catch (final Exception exception) {
            throw toProvMnSException(httpServletRequest.getMethod(), exception, NO_OP);
        }
    }

    @Override
    public ResponseEntity<Object> putMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final RequestParameters requestParameters = ParameterHelper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = getAndValidateYangModelCmHandle(requestParameters);
            final OperationDetails operationDetails =
                operationDetailsFactory.buildOperationDetails(CREATE, requestParameters, resource);
            checkPermission(yangModelCmHandle, operationDetails);
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestParameters.fdn());
            return dmiRestClient.synchronousPutOperation(DATA, resource, urlTemplateParameters);
        } catch (final Exception exception) {
            throw toProvMnSException(httpServletRequest.getMethod(), exception, NO_OP);
        }
    }

    @Override
    public ResponseEntity<Object> deleteMoi(final HttpServletRequest httpServletRequest) {
        final RequestParameters requestParameters = ParameterHelper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = getAndValidateYangModelCmHandle(requestParameters);
            final OperationDetails operationDetails =
                operationDetailsFactory.buildOperationDetailsForDelete(requestParameters.fdn());
            checkPermission(yangModelCmHandle, operationDetails);
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestParameters.fdn());
            return dmiRestClient.synchronousDeleteOperation(DATA, urlTemplateParameters);
        } catch (final Exception exception) {
            throw toProvMnSException(httpServletRequest.getMethod(), exception, NO_OP);
        }
    }

    private YangModelCmHandle getAndValidateYangModelCmHandle(final RequestParameters requestParameters)
                                                              throws ProvMnSException {
        final String fdn = requestParameters.fdn();
        try {
            final String cmHandleId = alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(fdn, "/");
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
            if (!StringUtils.hasText(yangModelCmHandle.getDataProducerIdentifier())) {
                throw new ProvMnSException(requestParameters.httpMethodName(), UNPROCESSABLE_ENTITY,
                                           PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE, NO_OP);
            }
            if (yangModelCmHandle.getCompositeState().getCmHandleState() != CmHandleState.READY) {
                final String title = yangModelCmHandle.getId() + " is not in READY state. Current state: "
                    + yangModelCmHandle.getCompositeState().getCmHandleState().name();
                throw new ProvMnSException(requestParameters.httpMethodName(), NOT_ACCEPTABLE, title, NO_OP);
            }
            return yangModelCmHandle;
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            throw new ProvMnSException(requestParameters.httpMethodName(), NOT_FOUND, fdn + " not found", NO_OP);
        }
    }

    private void checkPermission(final YangModelCmHandle yangModelCmHandle,
                                 final OperationDetails operationDetails) {
        final Map<String, List<ClassInstance>> changeRequestAsMap = new HashMap<>(1);
        changeRequestAsMap.put(operationDetails.className(), operationDetails.ClassInstances());
        final String changeRequestAsJson = jsonObjectMapper.asJsonString(changeRequestAsMap);
        final int index = yangModelCmHandle.getAlternateId().length() + 1;
        final String resourceIdentifier = operationDetails.parentFdn().substring(index);
        policyExecutor.checkPermission(yangModelCmHandle, operationDetails.operationType(),
            NO_AUTHORIZATION, resourceIdentifier, changeRequestAsJson);
    }

    private void checkPermissionForEachPatchItem(final String baseFdn,
                                                 final List<PatchItem> patchItems,
                                                 final YangModelCmHandle yangModelCmHandle) {
        int patchItemCounter = 0;
        for (final PatchItem patchItem : patchItems) {
            final String extendedPath = baseFdn + patchItem.getPath();
            final RequestParameters requestParameters = ParameterHelper.createRequestParametersForPatch(extendedPath);
            final OperationDetails operationDetails =
                operationDetailsFactory.buildOperationDetails(requestParameters, patchItem);
            try {
                checkPermission(yangModelCmHandle, operationDetails);
                patchItemCounter++;
            } catch (final Exception exception) {
                final String httpMethodName = "PATCH";
                throw toProvMnSException(httpMethodName, exception, "/" + patchItemCounter);
            }
        }
    }

    private ProvMnSException toProvMnSException(final String httpMethodName, final Exception exception,
                                                final String badOp) {
        if (exception instanceof ProvMnSException) {
            return (ProvMnSException) exception;
        }
        final ProvMnSException provMnSException = new ProvMnSException();
        provMnSException.setHttpMethodName(httpMethodName);
        provMnSException.setTitle(exception.getMessage());
        provMnSException.setBadOp(badOp);
        final HttpStatus httpStatus;
        if (exception instanceof PolicyExecutorException) {
            httpStatus = CONFLICT;
        } else if (exception instanceof DataValidationException) {
            httpStatus = BAD_REQUEST;
        } else if (exception.getCause() instanceof TimeoutException) {
            httpStatus = GATEWAY_TIMEOUT;
        } else {
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        provMnSException.setHttpStatus(httpStatus);
        log.warn("ProvMns Exception: {}", provMnSException.getTitle());
        return provMnSException;
    }

}
