/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.dmi;

import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA;
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATAJOBS_READ;
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATAJOBS_WRITE;
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.MODEL;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DmiServiceNameResolver {

    /**
     * Resolve a dmi service name.
     *
     * @param requiredService   indicates what type of service is required
     * @param yangModelCmHandle cm handle
     * @return dmi service name
     */
    public static String resolveDmiServiceName(final RequiredDmiService requiredService,
                                               final YangModelCmHandle yangModelCmHandle) {
        return resolveDmiServiceName(requiredService,
                yangModelCmHandle.getDmiServiceName(),
                yangModelCmHandle.getDmiDataServiceName(),
                yangModelCmHandle.getDmiModelServiceName(),
                yangModelCmHandle.getDmiDatajobsReadServiceName(),
                yangModelCmHandle.getDmiDatajobsWriteServiceName());
    }

    /**
     * Resolve a dmi service name.
     *
     * @param requiredService     indicates what type of service is required
     * @param ncmpServiceCmHandle cm handle
     * @return dmi service name
     */
    public static String resolveDmiServiceName(final RequiredDmiService requiredService,
                                               final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return resolveDmiServiceName(requiredService,
                ncmpServiceCmHandle.getDmiServiceName(),
                ncmpServiceCmHandle.getDmiDataServiceName(),
                ncmpServiceCmHandle.getDmiModelServiceName(),
                ncmpServiceCmHandle.getDmiDatajobsReadServiceName(),
                ncmpServiceCmHandle.getDmiDatajobsWriteServiceName());
    }

    /**
     * Resolve a dmi service name.
     *
     * @param requiredService       indicates what type of service is required
     * @param dmiPluginRegistration dmi plugin registration
     * @return dmi service name
     */
    public static String resolveDmiServiceName(final RequiredDmiService requiredService,
                                               final DmiPluginRegistration dmiPluginRegistration) {
        return resolveDmiServiceName(requiredService,
                dmiPluginRegistration.getDmiPlugin(),
                dmiPluginRegistration.getDmiDataPlugin(),
                dmiPluginRegistration.getDmiModelPlugin(),
                dmiPluginRegistration.getDmiDatajobsReadPlugin(),
                dmiPluginRegistration.getDmiDatajobsWritePlugin());
    }

    private static String resolveDmiServiceName(final RequiredDmiService requiredService,
                                                final String dmiServiceName,
                                                final String dmiDataServiceName,
                                                final String dmiModelServiceName,
                                                final String dmiDatajobsReadServiceName,
                                                final String dmiDatajobsWriteServiceName) {
        // Try a specific service first, fall back to the combined service
        if (DATA.equals(requiredService) && StringUtils.isNotBlank(dmiDataServiceName)) {
            return dmiDataServiceName;
        }
        if (MODEL.equals(requiredService) && StringUtils.isNotBlank(dmiModelServiceName)) {
            return dmiModelServiceName;
        }
        if (DATAJOBS_READ.equals(requiredService) && StringUtils.isNotBlank(dmiDatajobsReadServiceName)) {
            return dmiDatajobsReadServiceName;
        }
        if (DATAJOBS_WRITE.equals(requiredService) && StringUtils.isNotBlank(dmiDatajobsWriteServiceName)) {
            return dmiDatajobsWriteServiceName;
        }
        // Fallback to combined service
        return dmiServiceName;
    }

}
