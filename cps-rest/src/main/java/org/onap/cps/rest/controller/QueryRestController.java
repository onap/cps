/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada.
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

package org.onap.cps.rest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.rest.api.CpsQueryApi;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class QueryRestController implements CpsQueryApi {

    private final CpsQueryService cpsQueryService;
    private final JsonObjectMapper jsonObjectMapper;
    private final PrefixResolver prefixResolver;

    @Override
    public ResponseEntity<Object> getNodesByDataspaceAndAnchorAndCpsPath(final String dataspaceName,
        final String anchorName, final String cpsPath, final Boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption = Boolean.TRUE.equals(includeDescendants)
            ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS : FetchDescendantsOption.OMIT_DESCENDANTS;
        final Collection<DataNode> dataNodes =
            cpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
        final List<Map<String, Object>> dataMaps = new ArrayList<>(dataNodes.size());
        String prefix = null;
        for (final DataNode dataNode : dataNodes) {
            if (prefix == null) {
                prefix = prefixResolver.getPrefix(dataspaceName, anchorName, dataNode.getXpath());
            }
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataMaps.add(dataMap);
        }

        return new ResponseEntity<>(jsonObjectMapper.asJsonString(dataMaps), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> getNodesByDataspaceAndAnchorAndCpsPathV2(final String dataspaceName,
        final String anchorName, final String cpsPath, final String descendants) {
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantOption(descendants);
        final Collection<DataNode> dataNodes =
            cpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
        final List<Map<String, Object>> dataMaps = new ArrayList<>(dataNodes.size());
        String prefix = null;
        for (final DataNode dataNode : dataNodes) {
            if (prefix == null) {
                prefix = prefixResolver.getPrefix(dataspaceName, anchorName, dataNode.getXpath());
            }
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataMaps.add(dataMap);
        }
        return new ResponseEntity<>(jsonObjectMapper.asJsonString(dataMaps), HttpStatus.OK);

    }

}
