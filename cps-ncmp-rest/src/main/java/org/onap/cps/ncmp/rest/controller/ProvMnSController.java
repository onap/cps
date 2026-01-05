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

import io.netty.handler.timeout.TimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.data.policyexecutor.CreateOperationDetails;
import org.onap.cps.ncmp.impl.data.policyexecutor.DeleteOperationDetails;
import org.onap.cps.ncmp.impl.data.policyexecutor.OperationDetails;
import org.onap.cps.ncmp.impl.data.policyexecutor.OperationDetailsFactory;
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.ParameterMapper;
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

    private static final Pattern STANDARD_ATTRIBUTE_PATTERN = Pattern.compile("[^#]/attributes");
    private static final String NO_AUTHORIZATION = null;
    private static final String PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE =
        "Registered DMI does not support the ProvMnS interface.";

    private final AlternateIdMatcher alternateIdMatcher;
    private final DmiRestClient dmiRestClient;
    private final InventoryPersistence inventoryPersistence;
    private final ParametersBuilder parametersBuilder;
    private final ParameterMapper parameterMapper;
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
        final RequestParameters requestParameters = parameterMapper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = getAndValidateYangModelCmHandle(requestParameters);
            final String targetFdn = requestParameters.toTargetFdn();
            final UrlTemplateParameters urlTemplateParameters = parametersBuilder.createUrlTemplateParametersForRead(
                yangModelCmHandle, targetFdn, scope, filter, attributes, fields, dataNodeSelector);
            return dmiRestClient.synchronousGetOperation(DATA, urlTemplateParameters);
        } catch (final Throwable throwable) {
            throw toProvMnSException(httpServletRequest.getMethod(), throwable);
        }
    }

    @Override
    public ResponseEntity<Object> patchMoi(final HttpServletRequest httpServletRequest,
                                           final List<PatchItem> patchItems) {
        if (patchItems.size() > maxNumberOfPatchOperations) {
            final String title = patchItems.size() + " operations in request, this exceeds the maximum of "
                + maxNumberOfPatchOperations;
            throw new ProvMnSException(httpServletRequest.getMethod(), HttpStatus.PAYLOAD_TOO_LARGE, title);
        }
        final RequestParameters requestParameters = parameterMapper.extractRequestParameters(httpServletRequest);
        for (final PatchItem patchItem : patchItems) {
            validateAttributeReference(httpServletRequest.getMethod(), httpServletRequest.getContentType(), patchItem);
        }
        try {
            final YangModelCmHandle yangModelCmHandle = getAndValidateYangModelCmHandle(requestParameters);
            checkPermissionForEachPatchItem(requestParameters, patchItems, yangModelCmHandle);
            final String targetFdn = requestParameters.toTargetFdn();
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, targetFdn);
            return dmiRestClient.synchronousPatchOperation(DATA, patchItems, urlTemplateParameters,
                httpServletRequest.getContentType());
        } catch (final Throwable throwable) {
            throw toProvMnSException(httpServletRequest.getMethod(), throwable);
        }
    }

    @Override
    public ResponseEntity<Object> putMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final RequestParameters requestParameters = parameterMapper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = getAndValidateYangModelCmHandle(requestParameters);
            final CreateOperationDetails createOperationDetails =
                operationDetailsFactory.buildCreateOperationDetails(CREATE, requestParameters, resource);
            checkPermission(yangModelCmHandle, CREATE, requestParameters.toTargetFdn(), createOperationDetails);
            final String targetFdn = requestParameters.toTargetFdn();
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, targetFdn);
            return dmiRestClient.synchronousPutOperation(DATA, resource, urlTemplateParameters);
        } catch (final Throwable throwable) {
            throw toProvMnSException(httpServletRequest.getMethod(), throwable);
        }
    }

    @Override
    public ResponseEntity<Object> deleteMoi(final HttpServletRequest httpServletRequest) {
        final RequestParameters requestParameters = parameterMapper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = getAndValidateYangModelCmHandle(requestParameters);
            final DeleteOperationDetails deleteOperationDetails =
                operationDetailsFactory.buildDeleteOperationDetails(requestParameters.toTargetFdn());
            checkPermission(yangModelCmHandle, DELETE, requestParameters.toTargetFdn(), deleteOperationDetails);
            final String targetFdn = requestParameters.toTargetFdn();
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, targetFdn);
            return dmiRestClient.synchronousDeleteOperation(DATA, urlTemplateParameters);
        } catch (final Throwable throwable) {
            throw toProvMnSException(httpServletRequest.getMethod(), throwable);
        }
    }

    private YangModelCmHandle getAndValidateYangModelCmHandle(final RequestParameters requestParameters)
                                                              throws ProvMnSException {
        final String alternateId = requestParameters.toTargetFdn();
        try {
            final String cmHandleId = alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(alternateId, "/");
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
            if (!StringUtils.hasText(yangModelCmHandle.getDataProducerIdentifier())) {
                throw new ProvMnSException(requestParameters.getHttpMethodName(), HttpStatus.UNPROCESSABLE_ENTITY,
                                           PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE);
            }
            if (yangModelCmHandle.getCompositeState().getCmHandleState() != CmHandleState.READY) {
                final String title = yangModelCmHandle.getId() + " is not in READY state. Current state: "
                    + yangModelCmHandle.getCompositeState().getCmHandleState().name();
                throw new ProvMnSException(requestParameters.getHttpMethodName(), HttpStatus.NOT_ACCEPTABLE, title);
            }
            return yangModelCmHandle;
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String title = alternateId + " not found";
            throw new ProvMnSException(requestParameters.getHttpMethodName(), HttpStatus.NOT_FOUND, title);
        }
    }

    private void checkPermission(final YangModelCmHandle yangModelCmHandle,
                                 final OperationType operationType,
                                 final String alternateId,
                                 final OperationDetails operationDetails) {
        final String operationDetailsAsJson = jsonObjectMapper.asJsonString(operationDetails);
        policyExecutor.checkPermission(yangModelCmHandle, operationType, NO_AUTHORIZATION, alternateId,
            operationDetailsAsJson);
    }

    private void checkPermissionForEachPatchItem(final RequestParameters requestParameters,
                                                 final List<PatchItem> patchItems,
                                                 final YangModelCmHandle yangModelCmHandle) {
        for (final PatchItem patchItem : patchItems) {
            final OperationDetails operationDetails =
                operationDetailsFactory.buildOperationDetails(requestParameters, patchItem);
            final OperationType operationType = OperationType.fromOperationName(operationDetails.operation());
            try {
                checkPermission(yangModelCmHandle, operationType, requestParameters.toTargetFdn(), operationDetails);
            } catch (final Throwable throwable) {
                throw toProvMnSException("PATCH", throwable, operationType.name());
            }
        }
    }

    private ProvMnSException toProvMnSException(final String httpMethodName, final Throwable throwable) {
        throw toProvMnSException(httpMethodName, throwable, null);
    }

    private ProvMnSException toProvMnSException(final String httpMethodName, final Throwable throwable,
                                                final String badOp) {
        if (throwable instanceof ProvMnSException) {
            return (ProvMnSException) throwable;
        }
        final ProvMnSException provMnSException = new ProvMnSException();
        provMnSException.setHttpMethodName(httpMethodName);
        provMnSException.setTitle(throwable.getMessage());
        provMnSException.setBadOp(badOp);
        final HttpStatus httpStatus;
        if (throwable instanceof PolicyExecutorException) {
            httpStatus = HttpStatus.CONFLICT;
        } else if (throwable instanceof DataValidationException) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (throwable.getCause() instanceof TimeoutException) {
            httpStatus = HttpStatus.GATEWAY_TIMEOUT;
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        provMnSException.setHttpStatus(httpStatus);
        log.warn("ProvMns Exception: {}", provMnSException.getTitle());
        return provMnSException;
    }

    private void validateAttributeReference(final String httpMethodName,
                                            final String contentType,
                                            final PatchItem patchItem) {
        final String path = patchItem.getPath();
        boolean attributesReferenceIncorrect = false;
        if (path.contains("#/attributes") && "application/json-patch+json".equals(contentType))  {
            attributesReferenceIncorrect = true;
        } else {
            final Matcher matcher = STANDARD_ATTRIBUTE_PATTERN.matcher(path);
            if ("application/3gpp-json-patch+json".equals(contentType) && matcher.find()) {
                attributesReferenceIncorrect = true;
            }
        }
        if (attributesReferenceIncorrect) {
            throw new ProvMnSException(httpMethodName, HttpStatus.BAD_REQUEST,
                                        "Invalid path for content-type " + contentType);
        }
    }

}
