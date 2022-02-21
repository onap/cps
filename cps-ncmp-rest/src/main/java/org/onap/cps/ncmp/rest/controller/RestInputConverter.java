/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C)  2022 Nordix Foundation
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.rest.model.RestCmHandle;
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestInputConverter {

    /**
     * Converts Rest Input To NCMP Service CM Handle.
     * @param restDmiPluginRegistration rest dmi plugin registration input
     * @return NCMP Service CM Handle
     */
    public DmiPluginRegistration toDmiPluginRegistration(
        final RestDmiPluginRegistration restDmiPluginRegistration) {
        final DmiPluginRegistration dmiPluginRegistration = new DmiPluginRegistration();
        dmiPluginRegistration.setDmiPlugin(restDmiPluginRegistration.getDmiPlugin());
        dmiPluginRegistration.setDmiModelPlugin(restDmiPluginRegistration.getDmiModelPlugin());
        dmiPluginRegistration.setDmiDataPlugin(restDmiPluginRegistration.getDmiDataPlugin());
        dmiPluginRegistration.setCreatedCmHandles(toNcmpServiceCmHandles(
            restDmiPluginRegistration.getCreatedCmHandles()));
        dmiPluginRegistration.setUpdatedCmHandles(toNcmpServiceCmHandles(
            restDmiPluginRegistration.getUpdatedCmHandles()));
        dmiPluginRegistration.setRemovedCmHandles(restDmiPluginRegistration.getRemovedCmHandles());
        return dmiPluginRegistration;
    }

    private List<CmHandle> toNcmpServiceCmHandles(final List<RestCmHandle> restCmHandles) {
        if (restCmHandles != null) {
            return restCmHandles.stream()
                .map(RestInputConverter::toNcmpServiceCmHandle)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static CmHandle toNcmpServiceCmHandle(final RestCmHandle restCmHandle) {
        final CmHandle cmHandle = new CmHandle();
        cmHandle.setCmHandleID(restCmHandle.getCmHandle());
        cmHandle.setDmiProperties(restCmHandle.getCmHandleProperties());
        cmHandle.setPublicProperties(restCmHandle.getPublicCmHandleProperties());
        return cmHandle;
    }

}
