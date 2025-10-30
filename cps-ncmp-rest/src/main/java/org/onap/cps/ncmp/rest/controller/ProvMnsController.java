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
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.rest.provmns.exception.ProvMnSAlternateIdNotFound;
import org.onap.cps.ncmp.rest.provmns.exception.ProvMnSCoordinationManagementDenied;
import org.onap.cps.ncmp.rest.util.ProvMnSParametersMapper;
import org.onap.cps.ncmp.rest.util.ProvMnsRequestParameters;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.provmns-base-path}")
@RequiredArgsConstructor
public class ProvMnsController implements ProvMnS {

    private final AlternateIdMatcher alternateIdMatcher;
    private final DmiRestClient dmiRestClient;
    private final InventoryPersistence inventoryPersistence;
    private final ProvMnSParametersMapper provMnsParametersMapper;
    private final PolicyExecutor policyExecutor;

    @Override
    public ResponseEntity<Resource> getMoi(final HttpServletRequest httpServletRequest, final Scope scope,
                                                   final String filter, final List<String> attributes,
                                                   final List<String> fields,
                                                   final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        final ProvMnsRequestParameters provMnsRequestParameters =
            ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    provMnsRequestParameters.getAlternateId(), "/"));
            provMnsParametersMapper.checkDataProducerIdentifierAndReadyState(yangModelCmHandle, "GET");
            final UrlTemplateParameters urlTemplateParameters = provMnsParametersMapper.getUrlTemplateParameters(scope,
                filter, attributes,
                fields, dataNodeSelector,
                yangModelCmHandle);
            return dmiRestClient.synchronousGetOperation(
                RequiredDmiService.DATA, urlTemplateParameters, OperationType.READ);
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            throw new ProvMnSAlternateIdNotFound(provMnsRequestParameters.getAlternateId(), "GET");
        }
    }

    @Override
    public ResponseEntity<Resource> patchMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Resource> putMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final ProvMnsRequestParameters provMnsRequestParameters =
            ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    provMnsRequestParameters.getAlternateId(), "/"));
            provMnsParametersMapper.checkDataProducerIdentifierAndReadyState(yangModelCmHandle, "DEFAULT");
            try {
                policyExecutor.checkPermission(yangModelCmHandle,
                    OperationType.CREATE,
                    null,
                    provMnsRequestParameters.getAlternateId(),
                    provMnsParametersMapper.configurationManagementOperationToJson(
                        "create", provMnsRequestParameters, resource)
                );
            } catch (final RuntimeException exception) {
                throw new ProvMnSCoordinationManagementDenied(exception.getMessage(), "DEFAULT");
            }
            final UrlTemplateParameters urlTemplateParameters =
                provMnsParametersMapper.putUrlTemplateParameters(resource,
                    yangModelCmHandle);
            return dmiRestClient.synchronousPutOperation(
                RequiredDmiService.DATA, urlTemplateParameters, OperationType.CREATE);
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            throw new ProvMnSAlternateIdNotFound(provMnsRequestParameters.getAlternateId(), "DEFAULT");
        }
    }

    @Override
    public ResponseEntity<Void> deleteMoi(final HttpServletRequest httpServletRequest) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
