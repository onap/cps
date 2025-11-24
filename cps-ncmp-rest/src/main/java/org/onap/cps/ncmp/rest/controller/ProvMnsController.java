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
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.provmns.ParameterMapper;
import org.onap.cps.ncmp.impl.provmns.ParametersBuilder;
import org.onap.cps.ncmp.impl.provmns.RequestPathParameters;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.PatchItem;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.rest.provmns.ErrorResponseBuilder;
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
    private static final String PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE =
        "Registered DMI does not support the ProvMnS interface.";
    private static final String CM_HANDLE_NOT_READY = "NOT READY";

    private final AlternateIdMatcher alternateIdMatcher;
    private final DmiRestClient dmiRestClient;
    private final InventoryPersistence inventoryPersistence;
    private final ParametersBuilder parametersBuilder;
    private final ParameterMapper parameterMapper;
    private final ErrorResponseBuilder errorResponseBuilder;
    private final PolicyExecutor policyExecutor;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    public ResponseEntity<Object> getMoi(final HttpServletRequest httpServletRequest,
                                         final Scope scope,
                                         final String filter,
                                         final List<String> attributes,
                                         final List<String> fields,
                                         final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        final RequestPathParameters requestPathParameters =
            parameterMapper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    requestPathParameters.toAlternateId(), "/"));
            checkTarget(yangModelCmHandle);
            final UrlTemplateParameters urlTemplateParameters = parametersBuilder.createUrlTemplateParametersForRead(
                scope, filter, attributes, fields, dataNodeSelector, yangModelCmHandle, requestPathParameters);
            return dmiRestClient.synchronousGetOperation(
                RequiredDmiService.DATA, urlTemplateParameters);
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String reason = buildNotFoundMessage(requestPathParameters.toAlternateId());
            return errorResponseBuilder.buildErrorResponseGet(HttpStatus.NOT_FOUND, reason);
        } catch (final ProvMnSException exception) {
            return errorResponseBuilder.buildErrorResponseGet(
                getHttpStatusForProvMnSException(exception), exception.getDetails());
        }
    }

    @Override
    public ResponseEntity<Object> patchMoi(final HttpServletRequest httpServletRequest,
                                           final List<PatchItem> patchItems) {
        final RequestPathParameters requestPathParameters =
            parameterMapper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    requestPathParameters.toAlternateId(), "/"));
            checkTarget(yangModelCmHandle);
            policyExecutor.checkPermission(yangModelCmHandle,
                OperationType.CREATE,
                NO_AUTHORIZATION,
                requestPathParameters.toAlternateId(),
                jsonObjectMapper.asJsonString(
                    policyExecutor.buildPatchOperationDetails(requestPathParameters, patchItems))
            );
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestPathParameters);
            return dmiRestClient.synchronousPatchOperation(RequiredDmiService.DATA, patchItems,
                urlTemplateParameters, httpServletRequest.getContentType());
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String reason = buildNotFoundMessage(requestPathParameters.toAlternateId());
            return errorResponseBuilder.buildErrorResponsePatch(HttpStatus.NOT_FOUND, reason);
        } catch (final ProvMnSException exception) {
            return errorResponseBuilder.buildErrorResponsePatch(
                getHttpStatusForProvMnSException(exception), exception.getDetails());
        } catch (final RuntimeException exception) {
            return errorResponseBuilder.buildErrorResponsePatch(HttpStatus.NOT_ACCEPTABLE, exception.getMessage());
        }
    }

    @Override
    public ResponseEntity<Object> putMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final RequestPathParameters requestPathParameters =
            parameterMapper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    requestPathParameters.toAlternateId(), "/"));
            checkTarget(yangModelCmHandle);
            policyExecutor.checkPermission(yangModelCmHandle,
                OperationType.CREATE,
                NO_AUTHORIZATION,
                requestPathParameters.toAlternateId(),
                jsonObjectMapper.asJsonString(
                    policyExecutor.buildCreateOperationDetails(OperationType.CREATE, requestPathParameters, resource))
            );
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestPathParameters);
            return dmiRestClient.synchronousPutOperation(RequiredDmiService.DATA, resource, urlTemplateParameters);
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String reason = buildNotFoundMessage(requestPathParameters.toAlternateId());
            return errorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_FOUND, reason);
        } catch (final ProvMnSException exception) {
            return errorResponseBuilder.buildErrorResponseDefault(
                getHttpStatusForProvMnSException(exception), exception.getDetails());
        } catch (final RuntimeException exception) {
            return errorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_ACCEPTABLE, exception.getMessage());
        }
    }

    @Override
    public ResponseEntity<Object> deleteMoi(final HttpServletRequest httpServletRequest) {
        final RequestPathParameters requestPathParameters =
            parameterMapper.extractRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    requestPathParameters.toAlternateId(), "/"));
            checkTarget(yangModelCmHandle);
            policyExecutor.checkPermission(yangModelCmHandle,
                OperationType.DELETE,
                NO_AUTHORIZATION,
                requestPathParameters.toAlternateId(),
                jsonObjectMapper.asJsonString(
                    policyExecutor.buildDeleteOperationDetails(requestPathParameters.toAlternateId()))
            );
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForWrite(yangModelCmHandle, requestPathParameters);
            return dmiRestClient.synchronousDeleteOperation(RequiredDmiService.DATA, urlTemplateParameters);
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            final String reason = buildNotFoundMessage(requestPathParameters.toAlternateId());
            return errorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_FOUND, reason);
        } catch (final ProvMnSException exception) {
            return errorResponseBuilder.buildErrorResponseDefault(
                getHttpStatusForProvMnSException(exception), exception.getDetails());
        } catch (final RuntimeException exception) {
            return errorResponseBuilder.buildErrorResponseDefault(HttpStatus.NOT_ACCEPTABLE,
                exception.getMessage());
        }
    }

    private void checkTarget(final YangModelCmHandle yangModelCmHandle) {
        if (yangModelCmHandle.getDataProducerIdentifier() == null
            || yangModelCmHandle.getDataProducerIdentifier().isBlank()) {
            throw new ProvMnSException("NO DATA PRODUCER ID", PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE);
        } else if (yangModelCmHandle.getCompositeState().getCmHandleState() != CmHandleState.READY) {
            throw new ProvMnSException(CM_HANDLE_NOT_READY, buildNotReadyStateMessage(yangModelCmHandle));
        }
    }

    private String buildNotReadyStateMessage(final YangModelCmHandle yangModelCmHandle) {
        return yangModelCmHandle.getId() + " is not in ready state. Current state:"
            + yangModelCmHandle.getCompositeState().getCmHandleState().name();
    }

    private String buildNotFoundMessage(final String alternateId) {
        return alternateId + " not found";
    }

    private HttpStatus getHttpStatusForProvMnSException(final ProvMnSException exception) {
        return CM_HANDLE_NOT_READY.equals(exception.getMessage())
            ? HttpStatus.NOT_ACCEPTABLE : HttpStatus.UNPROCESSABLE_ENTITY;
    }

}
