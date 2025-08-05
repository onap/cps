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
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.provmns.model.Resource;
import org.onap.cps.ncmp.provmns.model.Scope;
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

    @Override
    public ResponseEntity<Resource> putMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final ProvMnsRequestParameters provMnsRequestParameters =
            ProvMnsRequestParameters.toProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Resource> getMoi(final HttpServletRequest httpServletRequest, final Scope scope,
                                                   final String filter, final List<String> attributes,
                                                   final List<String> fields,
                                                   final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.toProvMnsRequestParameters(httpServletRequest);
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(
            alternateIdMatcher.getCmHandleId(requestParameters.getAlternateId()));
        provMnsParametersMapper.checkDataProducerIdentifier(yangModelCmHandle);
        final UrlTemplateParameters urlTemplateParameters = provMnsParametersMapper.getUrlTemplateParameters(scope,
                                                                                     filter, attributes,
                                                                                     fields, dataNodeSelector,
                                                                                     yangModelCmHandle);
        return dmiRestClient.synchronousGetOperationWithJsonData(
            RequiredDmiService.DATA, urlTemplateParameters, OperationType.READ);
    }

    @Override
    public ResponseEntity<Resource> patchMoi(final HttpServletRequest httpServletRequest, final Resource resource) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.toProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> deleteMoi(final HttpServletRequest httpServletRequest) {
        final ProvMnsRequestParameters requestParameters =
            ProvMnsRequestParameters.toProvMnsRequestParameters(httpServletRequest);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
