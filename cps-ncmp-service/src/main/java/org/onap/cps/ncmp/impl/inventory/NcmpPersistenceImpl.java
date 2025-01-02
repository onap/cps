/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory;

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

import io.micrometer.core.annotation.Timed;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class NcmpPersistenceImpl implements NcmpPersistence {

    protected final JsonObjectMapper jsonObjectMapper;
    protected final CpsAnchorService cpsAnchorService;
    protected final CpsDataService cpsDataService;

    @Override
    public void deleteListOrListElement(final String listElementXpath) {
        cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, listElementXpath,
                NO_TIMESTAMP);
    }

    @Override
    @Timed(value = "cps.ncmp.inventory.persistence.datanode.get",
            description = "Time taken to get a data node (from ncmp dmi registry)")
    public Collection<DataNode> getDataNode(final String xpath) {
        return getDataNode(xpath, INCLUDE_ALL_DESCENDANTS);
    }

    @Override
    @Timed(value = "cps.ncmp.inventory.persistence.datanode.get",
            description = "Time taken to get a data node (from ncmp dmi registry)")
    public Collection<DataNode> getDataNode(final String xpath, final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, xpath,
                fetchDescendantsOption);
    }

    @Override
    public Collection<DataNode> getDataNodes(final Collection<String> xpaths) {
        return getDataNodes(xpaths, INCLUDE_ALL_DESCENDANTS);
    }

    @Override
    public Collection<DataNode> getDataNodes(final Collection<String> xpaths,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNodesForMultipleXpaths(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, xpaths,
                fetchDescendantsOption);
    }

    @Override
    public void replaceListContent(final String parentNodeXpath, final Collection<DataNode> dataNodes) {
        cpsDataService.replaceListContent(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, parentNodeXpath, dataNodes,
                NO_TIMESTAMP);
    }

    @Override
    public void deleteDataNode(final String dataNodeXpath) {
        cpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, dataNodeXpath, NO_TIMESTAMP);
    }

    @Override
    public void deleteDataNodes(final Collection<String> dataNodeXpaths) {
        cpsDataService.deleteDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, dataNodeXpaths, NO_TIMESTAMP);
    }

    @Override
    public void deleteAnchors(final Collection<String> anchorIds) {
        cpsAnchorService.deleteAnchors(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, anchorIds);
    }

}
