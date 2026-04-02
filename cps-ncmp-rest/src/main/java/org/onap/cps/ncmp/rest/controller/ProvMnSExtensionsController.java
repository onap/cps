/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe
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

import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA;
import static org.onap.cps.ncmp.impl.provmns.ParameterHelper.NO_OP;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.ActionRequestParameters;
import org.onap.cps.ncmp.impl.provmns.ParameterHelper;
import org.onap.cps.ncmp.impl.provmns.ParametersBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.ncmp.rest.model.ActionRequest;
import org.onap.cps.ncmp.rest.util.ProvMnSHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.provmns-extensions-base-path}")
@RequiredArgsConstructor
@Slf4j
public class ProvMnSExtensionsController implements ProvMnSExtensions {

    private final DmiRestClient dmiRestClient;
    private final ParametersBuilder parametersBuilder;
    private final ProvMnSHelper provMnSHelper;

    @Override
    public ResponseEntity<Object> executeAction(final HttpServletRequest httpServletRequest,
                                                final ActionRequest actionRequest) {
        final ActionRequestParameters actionRequestParameters =
            ParameterHelper.extractActionRequestParameters(httpServletRequest);
        try {
            final YangModelCmHandle yangModelCmHandle = provMnSHelper.getAndValidateYangModelCmHandle(
                actionRequestParameters.fdn(), actionRequestParameters.httpMethodName());
            final UrlTemplateParameters urlTemplateParameters =
                parametersBuilder.createUrlTemplateParametersForAction(yangModelCmHandle,
                    actionRequestParameters.fdn(), actionRequestParameters.action());
            return dmiRestClient.synchronousPostOperationWithPassthroughReturn(DATA, actionRequest,
                urlTemplateParameters, actionRequestParameters.authorization());
        } catch (final Exception exception) {
            throw provMnSHelper.toProvMnSException(httpServletRequest.getMethod(), exception, NO_OP);
        }
    }
}
