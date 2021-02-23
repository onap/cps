/*
 * ============LICENSE_START=======================================================
 *  ONAP CPS
 *  ================================================================================
 *  Copyright (C) 2021 highstreet technologies GmbH Intellectual Property.
 *  All rights reserved.
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

package org.onap.cps.nfproxy.api.impl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.nfproxy.api.NfProxyDataService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NfProxyDataServiceImpl implements NfProxyDataService {

    static final String NF_PROXY_DATASPACE_NAME = "NFP-Operational";

    @Autowired
    private CpsDataService cpsDataService;

    @Override
    public DataNode getDataNode(@NonNull final String cmHandleId, @NonNull final String xpath,
                                @NonNull final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNode(NF_PROXY_DATASPACE_NAME, cmHandleId, xpath,
            fetchDescendantsOption);
    }
}
