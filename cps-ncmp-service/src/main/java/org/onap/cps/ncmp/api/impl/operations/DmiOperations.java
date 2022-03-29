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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.operations;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DmiOperations {

    @Getter
    public enum DataStoreEnum {
        PASSTHROUGH_OPERATIONAL("ncmp-datastore:passthrough-operational"),
        PASSTHROUGH_RUNNING("ncmp-datastore:passthrough-running");
        private final String value;

        DataStoreEnum(final String value) {
            this.value = value;
        }
    }

    protected final YangModelCmHandleRetriever yangModelCmHandleRetriever;
    protected final JsonObjectMapper jsonObjectMapper;
    protected final NcmpConfiguration.DmiProperties dmiProperties;
    protected final DmiRestClient dmiRestClient;
    protected final DmiServiceUrlBuilder dmiServiceUrlBuilder;

    String getDmiResourceUrl(final String dmiServiceName, final String cmHandle, final String resourceName) {
        return dmiServiceUrlBuilder.getCmHandleUrl()
                .pathSegment("{resourceName}")
                .buildAndExpand(dmiServiceName, dmiProperties.getDmiBasePath(), cmHandle, resourceName).toUriString();
    }


}
