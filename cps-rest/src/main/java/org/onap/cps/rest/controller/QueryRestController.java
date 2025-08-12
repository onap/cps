/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada.
 *  Modifications Copyright (C) 2022-2024 Deutsche Telekom AG
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

import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsFacade;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.api.parameters.PaginationOption;
import org.onap.cps.rest.api.CpsQueryApi;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.XmlUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class QueryRestController implements CpsQueryApi {

    private final CpsFacade cpsFacade;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    @Timed(value = "cps.data.controller.datanode.query.v1", description = "Time taken to query data nodes")
    public ResponseEntity<Object> getNodesByDataspaceAndAnchorAndCpsPath(final String dataspaceName,
                                                                         final String anchorName,
                                                                         final String cpsPath,
                                                                         final Boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(includeDescendants);
        final List<Map<String, Object>> dataNodesAsMaps
            = cpsFacade.executeAnchorQuery(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
        return buildResponseEntity(dataNodesAsMaps, ContentType.JSON);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.query.v2", description = "Time taken to query data nodes")
    public ResponseEntity<Object> getNodesByDataspaceAndAnchorAndCpsPathV2(final String dataspaceName,
                                                                           final String anchorName,
                                                                           final String cpsPath,
                                                                           final String fetchDescendantsOptionAsString,
                                                                           final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final List<Map<String, Object>> dataNodesAsMaps
            = cpsFacade.executeAnchorQuery(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
        return buildResponseEntity(dataNodesAsMaps, contentType);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.query.across.anchors",
            description = "Time taken to query data nodes across anchors")
    public ResponseEntity<Object> getNodesByDataspaceAndCpsPath(final String dataspaceName,
                                                                final String cpsPath,
                                                                final String fetchDescendantsOptionAsString,
                                                                final Integer pageIndex,
                                                                final Integer pageSize) {
        final FetchDescendantsOption fetchDescendantsOption =
                FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final PaginationOption paginationOption = (pageIndex == null || pageSize == null)
                ? PaginationOption.NO_PAGINATION : new PaginationOption(pageIndex, pageSize);
        final List<Map<String, Object>> dataNodesAsMaps
            = cpsFacade.executeDataspaceQuery(dataspaceName, cpsPath, fetchDescendantsOption, paginationOption);

        final int totalPages = cpsFacade.countAnchorsInDataspaceQuery(dataspaceName, cpsPath, paginationOption);
        return ResponseEntity.ok().header("total-pages", String.valueOf(totalPages))
                .body(jsonObjectMapper.asJsonString(dataNodesAsMaps));
    }

    private ResponseEntity<Object> buildResponseEntity(final List<Map<String, Object>> dataNodesAsMaps,
                                               final ContentType contentType) {
        final String responseData;
        if (ContentType.XML.equals(contentType)) {
            responseData = XmlUtils.convertDataMapsToXml(dataNodesAsMaps);
        } else {
            responseData = jsonObjectMapper.asJsonString(dataNodesAsMaps);
        }
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }
}
