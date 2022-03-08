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

package org.onap.cps.ncmp.api.impl.yangmodels;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.exceptions.DataValidationException;

@Slf4j
@Getter
public class YangModelCmHandlesList {

    @JsonProperty("cm-handles")
    private final List<YangModelCmHandle> yangModelCmHandles = new ArrayList<>();

    private static final Pattern REG_EX_VALIDATION_PATTERN_FOR_NETWORK_FUNCTIONS = Pattern.compile("^[^,-]+$");

    /**
     * Create a YangModelCmHandleList given all service names and a collection of cmHandles.
     * @param dmiServiceName the dmi service name
     * @param dmiDataServiceName the dmi data service name
     * @param dmiModelServiceName the dmi model service name
     * @param ncmpServiceCmHandles cm handles rest model
     * @return instance of YangModelCmHandleList
     */
    public static YangModelCmHandlesList toYangModelCmHandlesList(final String dmiServiceName,
                                                                  final String dmiDataServiceName,
                                                                  final String dmiModelServiceName,
                                                                  final Collection<NcmpServiceCmHandle>
                                                                      ncmpServiceCmHandles) {
        final YangModelCmHandlesList yangModelCmHandlesList = new YangModelCmHandlesList();
        for (final NcmpServiceCmHandle ncmpServiceCmHandle : ncmpServiceCmHandles) {
            try {
                final YangModelCmHandle yangModelCmHandle =
                    YangModelCmHandle.toYangModelCmHandle(
                        dmiServiceName,
                        dmiDataServiceName,
                        dmiModelServiceName,
                        ncmpServiceCmHandle);
                final Matcher matcher
                    = REG_EX_VALIDATION_PATTERN_FOR_NETWORK_FUNCTIONS.matcher(yangModelCmHandle.getId());
                if (matcher.matches()) {
                    yangModelCmHandlesList.add(yangModelCmHandle);
                } else {
                    throw new DataValidationException("Invalid Data", "Cm Handle "
                        + "'" + yangModelCmHandle.getId() + "'" + " cannot have dashes or commas in the name");
                }
            } catch (final DataValidationException e) {
                log.warn("Cm Handle not added. {}", e.getDetails());
            }
        }
        return yangModelCmHandlesList;
    }

    /**
     * Add a yangModelCmHandle.
     *
     * @param yangModelCmHandle the yangModelCmHandle to add
     */
    public void add(final YangModelCmHandle yangModelCmHandle) {
        yangModelCmHandles.add(yangModelCmHandle);
    }
}
