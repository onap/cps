/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.onap.cps.ncmp.api.impl.exception.DmiRequestException;
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

    private List<NcmpServiceCmHandle> createdCmHandles = Collections.emptyList();

    private List<NcmpServiceCmHandle> updatedCmHandles = Collections.emptyList();

    private List<String> removedCmHandles = Collections.emptyList();

    /**
     * Validates plugin service names.
     * @throws NcmpException if validation fails.
     */
    public void validateDmiPluginRegistration() throws NcmpException {
        final String combinedServiceName = dmiPlugin;
        final String dataServiceName = dmiDataPlugin;
        final String modelsServiceName = dmiModelPlugin;

        String errorMessage = null;

        if (isNullEmptyOrBlank(combinedServiceName)) {
            if ((isNullEmptyOrBlank(dataServiceName) && isNullEmptyOrBlank(modelsServiceName))) {
                errorMessage = "No DMI plugin service names";
            } else {
                if (isNullEmptyOrBlank(dataServiceName) || isNullEmptyOrBlank(modelsServiceName)) {
                    errorMessage = "Cannot register just a Data or Model plugin service name";
                }
            }
        } else {
            if (!isNullEmptyOrBlank(dataServiceName) || !isNullEmptyOrBlank(modelsServiceName)) {
                errorMessage = "Cannot register combined plugin service name and other service names";
            }
        }

        if (errorMessage != null) {
            throw new DmiRequestException(errorMessage, "Please supply correct plugin information.");
        }
    }

    private static boolean isNullEmptyOrBlank(final String serviceName) {
        return Strings.isNullOrEmpty(serviceName) || serviceName.isBlank();
    }

}
