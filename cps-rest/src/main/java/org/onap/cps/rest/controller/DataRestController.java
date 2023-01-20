/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import javax.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.rest.api.CpsDataApi;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class DataRestController implements CpsDataApi {

    private static final String ROOT_XPATH = "/";
    private static final String ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateTimeFormatter ISO_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIMESTAMP_FORMAT);

    private final CpsDataService cpsDataService;
    private final JsonObjectMapper jsonObjectMapper;
    private final PrefixResolver prefixResolver;

    @Override
    public ResponseEntity<String> createNode(@RequestHeader(value = "Content-Type") final String contentTypeHeader,
                                             final String apiVersion,
                                             final String dataspaceName, final String anchorName,
                                             final String nodeData, final String parentNodeXpath,
                                             final String observedTimestamp) {
        final ContentType contentType = contentTypeHeader.contains(MediaType.APPLICATION_XML_VALUE) ? ContentType.XML
                : ContentType.JSON;
        if (isRootXpath(parentNodeXpath)) {
            cpsDataService.saveData(dataspaceName, anchorName, nodeData,
                    toOffsetDateTime(observedTimestamp), contentType);
        } else {
            cpsDataService.saveData(dataspaceName, anchorName, parentNodeXpath,
                    nodeData, toOffsetDateTime(observedTimestamp), contentType);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteDataNode(final String apiVersion,
        final String dataspaceName, final String anchorName,
        final String xpath, final String observedTimestamp) {
        cpsDataService.deleteDataNode(dataspaceName, anchorName, xpath,
            toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<String> addListElements(final String parentNodeXpath, final String apiVersion,
        final String dataspaceName, final String anchorName, final Object jsonData, final String observedTimestamp) {
        cpsDataService.saveListElements(dataspaceName, anchorName, parentNodeXpath,
                jsonObjectMapper.asJsonString(jsonData), toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Object> getNodeByDataspaceAndAnchor(final String dataspaceName,
        final String anchorName, final String xpath, final Boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption = Boolean.TRUE.equals(includeDescendants)
            ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS : FetchDescendantsOption.OMIT_DESCENDANTS;
        final DataNode dataNode = cpsDataService.getDataNode(dataspaceName, anchorName, xpath,
            fetchDescendantsOption);
        final String prefix = prefixResolver.getPrefix(dataspaceName, anchorName, xpath);
        return new ResponseEntity<>(DataMapUtils.toDataMapWithIdentifier(dataNode, prefix), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> getNodeByDataspaceAndAnchorV2(final String dataspaceName, final String anchorName,
        final String xpath, final String fetchDescendantsOptionAsString) {
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final DataNode dataNode = cpsDataService.getDataNode(dataspaceName, anchorName, xpath,
            fetchDescendantsOption);
        final String prefix = prefixResolver.getPrefix(dataspaceName, anchorName, xpath);
        return new ResponseEntity<>(DataMapUtils.toDataMapWithIdentifier(dataNode, prefix), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> updateNodeLeaves(final String apiVersion, final String dataspaceName,
        final String anchorName, final Object jsonData, final String parentNodeXpath, final String observedTimestamp) {
        cpsDataService.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath,
                jsonObjectMapper.asJsonString(jsonData), toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> replaceNode(final String apiVersion,
        final String dataspaceName, final String anchorName,
        final Object jsonData, final String parentNodeXpath, final String observedTimestamp) {
        cpsDataService
                .updateDataNodeAndDescendants(dataspaceName, anchorName, parentNodeXpath,
                        jsonObjectMapper.asJsonString(jsonData), toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> replaceListContent(final String parentNodeXpath,
        final String apiVersion, final String dataspaceName, final String anchorName, final Object jsonData,
        final String observedTimestamp) {
        cpsDataService.replaceListContent(dataspaceName, anchorName, parentNodeXpath,
                jsonObjectMapper.asJsonString(jsonData), toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteListOrListElement(final String dataspaceName, final String anchorName,
        final String listElementXpath, final String observedTimestamp) {
        cpsDataService
            .deleteListOrListElement(dataspaceName, anchorName, listElementXpath, toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private static boolean isRootXpath(final String xpath) {
        return ROOT_XPATH.equals(xpath);
    }

    private static OffsetDateTime toOffsetDateTime(final String datetTimestamp) {
        try {
            return StringUtils.isEmpty(datetTimestamp)
                ? null : OffsetDateTime.parse(datetTimestamp, ISO_TIMESTAMP_FORMATTER);
        } catch (final Exception exception) {
            throw new ValidationException(
                String.format("observed-timestamp must be in '%s' format", ISO_TIMESTAMP_FORMAT));
        }
    }

}
