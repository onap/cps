/*
 *  ============LICENSE_START=======================================================
 *  Modifications Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl;

import static org.onap.cps.ncmp.api.impl.utils.RestQueryParametersValidator.validateCmHandleQueryParameters;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.ParameterizedCmHandleQueryService;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.api.impl.utils.InventoryQueryConditions;
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyInventoryFacade {

    private final CmHandleRegistrationService cmHandleRegistrationService;
    private final CmHandleQueryService cmHandleQueryService;
    private final ParameterizedCmHandleQueryService parameterizedCmHandleQueryService;

    /**
     * Registration of Created, Removed, Updated or Upgraded CM Handles.
     *
     * @param dmiPluginRegistration Dmi Plugin Registration details
     * @return dmiPluginRegistrationResponse
     */

    public DmiPluginRegistrationResponse updateDmiRegistrationAndSyncModule(
        final DmiPluginRegistration dmiPluginRegistration) {
        return cmHandleRegistrationService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);
    }

    /**
     * Get all cm handle IDs by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @return collection of cm handle IDs
     */
    public Collection<String> getAllCmHandleIdsByDmiPluginIdentifier(final String dmiPluginIdentifier) {
        return cmHandleQueryService.getCmHandleIdsByDmiPluginIdentifier(dmiPluginIdentifier);
    }

    /**
     * Get all cm handle IDs by various properties.
     *
     * @param cmHandleQueryServiceParameters cm handle query parameters
     * @return collection of cm handle IDs
     */
    public Collection<String> executeCmHandleIdSearchForInventory(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, InventoryQueryConditions.ALL_CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandleIdsForInventory(cmHandleQueryServiceParameters);
    }

}
