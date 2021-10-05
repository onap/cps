/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications (C) 2021 Nordix Foundation
 *  Modification Copyright (C) 2021 highstreet technologies GmbH
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


import com.fasterxml.jackson.databind.ObjectMapper;
import javax.validation.Valid;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyInventoryApi;
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.ncmp-inventory-base-path}")
public class NetworkCmProxyInventoryController implements NetworkCmProxyInventoryApi {

    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor Injection for Dependencies.
     * @param networkCmProxyDataService Data Service Interface
     * @param objectMapper Object Mapper
     */
    public NetworkCmProxyInventoryController(final NetworkCmProxyDataService networkCmProxyDataService,
        final ObjectMapper objectMapper) {
        this.networkCmProxyDataService = networkCmProxyDataService;
        this.objectMapper = objectMapper;
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
        return objectMapper.convertValue(restDmiPluginRegistration, DmiPluginRegistration.class);
    }

}
