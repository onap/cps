/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2025 Nordix Foundation
 *  Modifications Copyright (C) 2022-2025 TechMahindra Ltd.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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
import jakarta.validation.ValidationException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsFacade;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.rest.api.CpsDataApi;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.XmlFileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class DataRestController implements CpsDataApi {

    private static final String ROOT_XPATH = "/";
    private static final DateTimeFormatter ISO_TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final CpsFacade cpsFacade;
    private final CpsDataService cpsDataService;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    public ResponseEntity<String> createNode(final String apiVersion,
                                             final String dataspaceName, final String anchorName,
                                             final String nodeData, final String parentNodeXpath,
                                             final Boolean dryRunEnabled, final String observedTimestamp,
                                             final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        if (Boolean.TRUE.equals(dryRunEnabled)) {
            cpsDataService.validateData(dataspaceName, anchorName, parentNodeXpath, nodeData, contentType);
            return ResponseEntity.ok().build();
        } else {
            if (isRootXpath(parentNodeXpath)) {
                cpsDataService.saveData(dataspaceName, anchorName, nodeData,
                        toOffsetDateTime(observedTimestamp), contentType);
            } else {
                cpsDataService.saveData(dataspaceName, anchorName, parentNodeXpath,
                        nodeData, toOffsetDateTime(observedTimestamp), contentType);
            }
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteDataNode(final String apiVersion, final String dataspaceName,
                                               final String anchorName, final String xpath,
                                               final String observedTimestamp) {
        cpsDataService.deleteDataNode(dataspaceName, anchorName, xpath, toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<String> addListElements(final String apiVersion, final String dataspaceName,
                                                  final String anchorName, final String parentNodeXpath,
                                                  final String nodeData, final Boolean dryRunEnabled,
                                                  final String observedTimestamp, final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        if (Boolean.TRUE.equals(dryRunEnabled)) {
            cpsDataService.validateData(dataspaceName, anchorName, parentNodeXpath, nodeData, contentType);
            return ResponseEntity.ok().build();
        } else {
            cpsDataService.saveListElements(dataspaceName, anchorName, parentNodeXpath,
                    nodeData, toOffsetDateTime(observedTimestamp), contentType);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.get.v1", description = "Time taken to get data node")
    public ResponseEntity<Object> getNodeByDataspaceAndAnchor(final String dataspaceName,
                                                              final String anchorName,
                                                              final String xpath,
                                                              final Boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(includeDescendants);
        final Map<String, Object> dataNodeAsMap =
            cpsFacade.getFirstDataNodeByAnchor(dataspaceName, anchorName, xpath, fetchDescendantsOption);
        return new ResponseEntity<>(dataNodeAsMap, HttpStatus.OK);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.get.v2", description = "Time taken to get data node")
    public ResponseEntity<Object> getNodeByDataspaceAndAnchorV2(final String dataspaceName, final String anchorName,
                                                                final String xpath,
                                                                final String fetchDescendantsOptionAsString,
                                                                final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        final FetchDescendantsOption fetchDescendantsOption =
                FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final List<Map<String, Object>> dataNodesAsMaps =
            cpsFacade.getDataNodesByAnchor(dataspaceName, anchorName, xpath, fetchDescendantsOption);
        return buildResponseEntity(dataNodesAsMaps, contentType);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.get.v3", description = "Time taken to get data node")
    public ResponseEntity<Object> getNodeByDataspaceAndAnchorV3(final String dataspaceName, final String anchorName,
                                                                final String xpath,
                                                                final String fetchDescendantsOptionAsString,
                                                                final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final Map<String, Object> dataNodesAsMap =
            cpsFacade.getDataNodesByAnchorV3(dataspaceName, anchorName, xpath, fetchDescendantsOption);
        return buildResponseEntity(dataNodesAsMap, contentType);
    }

    @Override
    public ResponseEntity<Object> updateNodeLeaves(final String apiVersion, final String dataspaceName,
                                                   final String anchorName, final String nodeData,
                                                   final String parentNodeXpath, final Boolean dryRunEnabled,
                                                   final String observedTimestamp, final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        if (Boolean.TRUE.equals(dryRunEnabled)) {
            cpsDataService.validateData(dataspaceName, anchorName, parentNodeXpath, nodeData, contentType);
            return ResponseEntity.ok().build();
        } else {
            cpsDataService.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath,
                    nodeData, toOffsetDateTime(observedTimestamp), contentType);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Override
    public ResponseEntity<Object> replaceNode(final String apiVersion, final String dataspaceName,
                                              final String anchorName, final String nodeData,
                                              final String parentNodeXpath, final Boolean dryRunEnabled,
                                              final String observedTimestamp, final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        if (Boolean.TRUE.equals(dryRunEnabled)) {
            cpsDataService.validateData(dataspaceName, anchorName, parentNodeXpath, nodeData, contentType);
            return ResponseEntity.ok().build();
        }
        final boolean hasNewDataNodes  = cpsDataService.updateDataNodeAndDescendants(dataspaceName, anchorName,
                parentNodeXpath, nodeData, toOffsetDateTime(observedTimestamp), contentType);
        final HttpStatus httpStatus = hasNewDataNodes  ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).build();
    }

    @Override
    public ResponseEntity<Object> replaceListContent(final String apiVersion, final String dataspaceName,
                                                     final String anchorName, final String parentNodeXpath,
                                                     final String nodeData, final Boolean dryRunEnabled,
                                                     final String observedTimestamp, final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        if (Boolean.TRUE.equals(dryRunEnabled)) {
            cpsDataService.validateData(dataspaceName, anchorName, parentNodeXpath, nodeData,
                    ContentType.JSON);
            return ResponseEntity.ok().build();
        } else {
            cpsDataService.replaceListContent(dataspaceName, anchorName, parentNodeXpath,
                    nodeData, toOffsetDateTime(observedTimestamp), contentType);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Override
    public ResponseEntity<Void> deleteListOrListElement(final String dataspaceName, final String anchorName,
                                                        final String listElementXpath, final String observedTimestamp) {
        cpsDataService
            .deleteListOrListElement(dataspaceName, anchorName, listElementXpath, toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<Object> buildResponseEntity(final Object dataMaps, final ContentType contentType) {
        final String responseData;
        if (ContentType.XML.equals(contentType)) {
            responseData = XmlFileUtils.convertDataMapsToXml(dataMaps);
        } else {
            responseData = jsonObjectMapper.asJsonString(dataMaps);
        }
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }

    private static boolean isRootXpath(final String xpath) {
        return ROOT_XPATH.equals(xpath);
    }

    private static OffsetDateTime toOffsetDateTime(final String dateTimestamp) {
        try {
            return StringUtils.isEmpty(dateTimestamp)
                ? null : OffsetDateTime.parse(dateTimestamp, ISO_TIMESTAMP_FORMATTER);
        } catch (final Exception exception) {
            throw new ValidationException("observed-timestamp must be in ISO format");
        }
    }
}
