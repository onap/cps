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

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.provmns.ActionRequestParameters;
import org.onap.cps.ncmp.impl.provmns.ParameterHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.provmns-extensions-base-path}")
@RequiredArgsConstructor
@Slf4j
public class ProvMnSExtensionsController implements ProvMnSExtensions {

    @Override
    public ResponseEntity<Object> executeAction(final HttpServletRequest httpServletRequest,
                                                         final Object body) {
        final ActionRequestParameters actionRequestParameters =
            ParameterHelper.extractActionRequestParameters(httpServletRequest);
        log.info("Provmns Extension Action called for FDN: {}, Action: {}",
            actionRequestParameters.fdn(), actionRequestParameters.action());
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
