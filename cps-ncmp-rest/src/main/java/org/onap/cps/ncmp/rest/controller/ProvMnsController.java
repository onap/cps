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
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.rest.provmns.model.ConfigurationManagementDeleteInput;
import org.onap.cps.ncmp.rest.util.ProvMnSParametersMapper;
import org.onap.cps.ncmp.rest.util.ProvMnsRequestParameters;
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
    private final PolicyExecutor policyExecutor;
    private final ProvMnSParametersMapper provMnsParametersMapper;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    public ResponseEntity<Resource> getMoi(final HttpServletRequest httpServletRequest, final Scope scope,
                                                   final String filter, final List<String> attributes,
                                                   final List<String> fields,
                                                   final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
            alternateIdMatcher.getCmHandleId(requestParameters.getAlternateId()));
        provMnsParametersMapper.checkDataProducerIdentifier(yangModelCmHandle);
        final UrlTemplateParameters urlTemplateParameters = provMnsParametersMapper.getUrlTemplateParameters(scope,
                                                                                     filter, attributes,
                                                                                     fields, dataNodeSelector,
                                                                                     yangModelCmHandle);
        return dmiRestClient.synchronousGetOperation(
            RequiredDmiService.DATA, urlTemplateParameters, OperationType.READ);
    }

    @Override
    public ResponseEntity<Resource> patchMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        //TODO: implement if a different user sotry
        //    final ProvMnsRequestParameters requestParameters =
        //    ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Resource> putMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final ProvMnsRequestParameters provMnsRequestParameters =
            ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Object> deleteMoi(final HttpServletRequest httpServletRequest) {
        final ProvMnsRequestParameters provMnsRequestParameters =
            ProvMnsRequestParameters.extractProvMnsRequestParameters(httpServletRequest);

        final String cmHandleId = alternateIdMatcher.getCmHandleId(provMnsRequestParameters.getFullUriLdn());
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);

        //TODO: implement if a different user story
        //if (!yangModelCmHandle.getDataProducerIdentifier().isEmpty()
        //      && CmHandleState.READY == yangModelCmHandle.getCompositeState().getCmHandleState()) {

        final ConfigurationManagementDeleteInput configurationManagementDeleteInput =
                new ConfigurationManagementDeleteInput(OperationType.DELETE.name(),
                        provMnsRequestParameters.getFullUriLdn());

        policyExecutor.checkPermission(yangModelCmHandle,
                OperationType.DELETE,
                NO_AUTHORIZATION,
                provMnsRequestParameters.getFullUriLdn(),
                jsonObjectMapper.asJsonString(configurationManagementDeleteInput));

        final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
                .fixedPathSegment(configurationManagementDeleteInput.targetIdentifier())
                .createUrlTemplateParameters(yangModelCmHandle.getDmiServiceName(),
                        "/ProvMnS");

        return dmiRestClient.synchronousDeleteOperation(RequiredDmiService.DATA, urlTemplateParameters);
    }
}
