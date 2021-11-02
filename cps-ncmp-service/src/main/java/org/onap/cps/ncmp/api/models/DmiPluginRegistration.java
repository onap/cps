/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;

/**
 * Dmi Registry request object.
 */
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class DmiPluginRegistration {

    private String dmiPlugin;
    private String dmiDataPlugin;
    private String dmiModelPlugin;
    private List<CmHandle> createdCmHandles;
    private List<CmHandle> updatedCmHandles;
    private List<String> removedCmHandles;
    public static final String PLEASE_SUPPLY_CORRECT_PLUGIN_INFORMATION = "Please supply correct plugin information.";

    public void validateDmiPluginRegistration() throws NcmpException {
        final String combinedServiceName = dmiPlugin;
        final String dataServiceName = dmiDataPlugin;
        final String modelsServiceName = dmiModelPlugin;


        if (combinedServiceName.isEmpty() && dataServiceName.isEmpty() && modelsServiceName.isEmpty()) {
            throw new NcmpException("No DMI plugin service names supplied.",
                PLEASE_SUPPLY_CORRECT_PLUGIN_INFORMATION);
        }

        if (!combinedServiceName.isEmpty() && (!dataServiceName.isEmpty() || !modelsServiceName.isEmpty())) {
            throw new NcmpException("Too many plugin details supplied.",
                PLEASE_SUPPLY_CORRECT_PLUGIN_INFORMATION);
        }
    }
}