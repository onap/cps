/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

import java.util.Collection;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private static final String NF_PROXY_DATASPACE_NAME = "NFP-Operational";

    @Autowired
    private CpsDataService cpsDataService;

    @Autowired
    private CpsQueryService cpsQueryService;

    private String getDataspaceName() {
        return NF_PROXY_DATASPACE_NAME;
    }

    @Override
    public DataNode getDataNode(final String cmHandle, final String xpath,
        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNode(getDataspaceName(), cmHandle, xpath, fetchDescendantsOption);
    }

    @Override
    public Collection<DataNode> queryDataNodes(final String cmHandle, final String cpsPath,
        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsQueryService.queryDataNodes(getDataspaceName(), cmHandle, cpsPath, fetchDescendantsOption);
    }

    @Override
    public void createDataNode(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        if (!StringUtils.hasText(parentNodeXpath) || "/".equals(parentNodeXpath)) {
            cpsDataService.saveData(getDataspaceName(), cmHandle, jsonData);
        } else {
            cpsDataService.saveData(getDataspaceName(), cmHandle, parentNodeXpath, jsonData);
        }
    }

    @Override
    public void addListNodeElements(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.saveListNodeData(getDataspaceName(), cmHandle, parentNodeXpath, jsonData);
    }

    @Override
    public void updateNodeLeaves(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.updateNodeLeaves(getDataspaceName(), cmHandle, parentNodeXpath, jsonData);
    }

    @Override
    public void replaceNodeTree(final String cmHandle, final String parentNodeXpath, final String jsonData) {
        cpsDataService.replaceNodeTree(getDataspaceName(), cmHandle, parentNodeXpath, jsonData);
    }
}
