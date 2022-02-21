/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyInventoryApi;
import org.onap.cps.ncmp.rest.model.RestCmHandle;
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.ncmp-inventory-base-path}")
public class NetworkCmProxyInventoryController implements NetworkCmProxyInventoryApi {

    private final NetworkCmProxyDataService networkCmProxyDataService;

    /**
     * Constructor Injection for Dependencies.
     * @param networkCmProxyDataService Data Service Interface
     */
    public NetworkCmProxyInventoryController(final NetworkCmProxyDataService networkCmProxyDataService) {
        this.networkCmProxyDataService = networkCmProxyDataService;
    }

    /**
     * Update DMI Plugin Registration (used for first registration also).
     * @param restDmiPluginRegistration the registration data
     */
    @Override
    public ResponseEntity<Void> updateDmiPluginRegistration(
        final @Valid RestDmiPluginRegistration restDmiPluginRegistration) {
        final DmiPluginRegistration dmiPluginRegistration =
            convertRestObjectToJavaApiObject(restDmiPluginRegistration);
        networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    private DmiPluginRegistration convertRestObjectToJavaApiObject(
        final RestDmiPluginRegistration restDmiPluginRegistration) {
        final DmiPluginRegistration dmiPluginRegistration = new DmiPluginRegistration();
        dmiPluginRegistration.setDmiPlugin(Optional.ofNullable(
            restDmiPluginRegistration.getDmiPlugin()).orElse(""));
        dmiPluginRegistration.setDmiModelPlugin(Optional.ofNullable(
            restDmiPluginRegistration.getDmiModelPlugin()).orElse(""));
        dmiPluginRegistration.setDmiDataPlugin(Optional.ofNullable(
            restDmiPluginRegistration.getDmiDataPlugin()).orElse(""));
        setCreatedCmHandles(restDmiPluginRegistration, dmiPluginRegistration);
        setUpdatedCmHandles(restDmiPluginRegistration, dmiPluginRegistration);
        setRemovedCmHandles(restDmiPluginRegistration, dmiPluginRegistration);
        return dmiPluginRegistration;
    }

    private void setCreatedCmHandles(final RestDmiPluginRegistration restDmiPluginRegistration,
                                     final DmiPluginRegistration dmiPluginRegistration) {
        if (restDmiPluginRegistration.getCreatedCmHandles() != null) {
            final List<RestCmHandle> restCmHandles = restDmiPluginRegistration.getCreatedCmHandles();
            final CmHandle cmHandle = new CmHandle();
            final List<CmHandle> cmHandleList = new ArrayList<>();
            for (final RestCmHandle restCmHandle: restCmHandles) {
                cmHandle.setCmHandleID(restCmHandle.getCmHandle());
                cmHandle.setDmiProperties(restCmHandle.getCmHandleProperties());
                cmHandle.setPublicProperties(restCmHandle.getPublicCmHandleProperties());
                cmHandleList.add(cmHandle);
            }
            dmiPluginRegistration.setCreatedCmHandles(cmHandleList);
        }
    }

    private void setUpdatedCmHandles(final RestDmiPluginRegistration restDmiPluginRegistration,
                                     final DmiPluginRegistration dmiPluginRegistration) {
        if (restDmiPluginRegistration.getUpdatedCmHandles() != null) {
            final List<RestCmHandle> restCmHandles = restDmiPluginRegistration.getUpdatedCmHandles();
            final CmHandle cmHandle = new CmHandle();
            final List<CmHandle> cmHandleList = new ArrayList<>();
            for (final RestCmHandle restCmHandle: restCmHandles) {
                cmHandle.setCmHandleID(restCmHandle.getCmHandle());
                cmHandle.setDmiProperties(restCmHandle.getCmHandleProperties());
                cmHandle.setPublicProperties(restCmHandle.getPublicCmHandleProperties());
                cmHandleList.add(cmHandle);
            }
            dmiPluginRegistration.setUpdatedCmHandles(cmHandleList);
        }
    }

    private void setRemovedCmHandles(final RestDmiPluginRegistration restDmiPluginRegistration,
                                    final DmiPluginRegistration dmiPluginRegistration) {
        if (restDmiPluginRegistration.getRemovedCmHandles() != null) {
            final List<String> restCmHandles = restDmiPluginRegistration.getRemovedCmHandles();
            dmiPluginRegistration.setRemovedCmHandles(restCmHandles);
        } else {
            dmiPluginRegistration.setRemovedCmHandles(Collections.emptyList());
        }
    }

}
