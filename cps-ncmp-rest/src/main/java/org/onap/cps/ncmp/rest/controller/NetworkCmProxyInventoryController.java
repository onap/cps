/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2022 Nordix Foundation
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

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.Status;
import org.onap.cps.ncmp.api.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyInventoryApi;
import org.onap.cps.ncmp.rest.model.CmHandlerRegistrationErrorResponse;
import org.onap.cps.ncmp.rest.model.DmiPluginRegistrationErrorResponse;
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.ncmp-inventory-base-path}")
@RequiredArgsConstructor
public class NetworkCmProxyInventoryController implements NetworkCmProxyInventoryApi {

    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final NcmpRestMapper ncmpRestMapper;

    /**
     * Update DMI Plugin Registration (used for first registration also).
     *
     * @param restDmiPluginRegistration the registration data
     */
    @Override
    public ResponseEntity updateDmiPluginRegistration(
        final @Valid RestDmiPluginRegistration restDmiPluginRegistration) {
        final DmiPluginRegistrationResponse dmiPluginRegistrationResponse =
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(
                ncmpRestMapper.toDmiPluginRegistration(restDmiPluginRegistration));
        final DmiPluginRegistrationErrorResponse failedRegistrationErrorResponse =
            getFailureRegistrationResponse(dmiPluginRegistrationResponse);
        return allRegistrationsSuccessful(failedRegistrationErrorResponse)
            ? new ResponseEntity<>(HttpStatus.OK)
            : new ResponseEntity<>(failedRegistrationErrorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean allRegistrationsSuccessful(
        final DmiPluginRegistrationErrorResponse dmiPluginRegistrationErrorResponse) {
        return dmiPluginRegistrationErrorResponse.getFailedCreatedCmHandles().isEmpty()
            && dmiPluginRegistrationErrorResponse.getFailedUpdatedCmHandles().isEmpty()
            && dmiPluginRegistrationErrorResponse.getFailedRemovedCmHandles().isEmpty();

    }

    private DmiPluginRegistrationErrorResponse getFailureRegistrationResponse(
        final DmiPluginRegistrationResponse dmiPluginRegistrationResponse) {
        final DmiPluginRegistrationErrorResponse dmiPluginRegistrationErrorResponse =
            new DmiPluginRegistrationErrorResponse();
        dmiPluginRegistrationErrorResponse.setFailedCreatedCmHandles(
            getFailedResponses(dmiPluginRegistrationResponse.getCreatedCmHandles()));
        dmiPluginRegistrationErrorResponse.setFailedUpdatedCmHandles(
            getFailedResponses(dmiPluginRegistrationResponse.getUpdatedCmHandles()));
        dmiPluginRegistrationErrorResponse.setFailedRemovedCmHandles(
            getFailedResponses(dmiPluginRegistrationResponse.getRemovedCmHandles()));

        return dmiPluginRegistrationErrorResponse;
    }

    private List<CmHandlerRegistrationErrorResponse> getFailedResponses(
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponseList) {
        return cmHandleRegistrationResponseList.stream()
            .filter(cmHandleRegistrationResponse -> cmHandleRegistrationResponse.getStatus() == Status.FAILURE)
            .map(this::toCmHandleRegistrationErrorResponse)
            .collect(Collectors.toList());
    }

    private CmHandlerRegistrationErrorResponse toCmHandleRegistrationErrorResponse(
        final CmHandleRegistrationResponse registrationResponse) {
        return new CmHandlerRegistrationErrorResponse()
            .cmHandle(registrationResponse.getCmHandle())
            .errorCode(registrationResponse.getRegistrationError().errorCode)
            .errorText(registrationResponse.getErrorText());
    }

}


