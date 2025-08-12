/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG
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

import static org.onap.cps.rest.utils.MultipartFileUtil.extractYangResourcesMap;
import static org.onap.cps.utils.ContentType.XML;

import io.micrometer.core.annotation.Timed;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.rest.api.CpsDeltaApi;
import org.onap.cps.rest.utils.MultipartFileUtil;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.deltareport.DeltaReportUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class DeltaRestController implements CpsDeltaApi {

    private final CpsDeltaService cpsDeltaService;
    private final JsonObjectMapper jsonObjectMapper;
    private final DeltaReportUtils deltaReportUtils;

    @Timed(value = "cps.delta.controller.get.delta",
        description = "Time taken to get delta between anchors")
    @Override
    public ResponseEntity<Object> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                                final String sourceAnchorName,
                                                                final String targetAnchorName,
                                                                final String xpath,
                                                                final String descendants,
                                                                final Boolean groupDataNodes,
                                                                final String contentTypeInHeader) {
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(descendants);
        final List<DeltaReport> deltaReports =
            cpsDeltaService.getDeltaByDataspaceAndAnchors(dataspaceName, sourceAnchorName,
                targetAnchorName, xpath, fetchDescendantsOption, groupDataNodes);
        return buildDeltaResponseEntity(deltaReports, ContentType.fromString(contentTypeInHeader));

    }

    @Timed(value = "cps.delta.controller.get.delta",
        description = "Time taken to get delta between anchors")
    @Override
    public ResponseEntity<Object> getDeltaByDataspaceAnchorAndPayload(final String dataspaceName,
                                                                      final String sourceAnchorName,
                                                                      final MultipartFile targetDataAsJsonFile,
                                                                      final String xpath,
                                                                      final Boolean groupDataNodes,
                                                                      final MultipartFile yangResourceFile) {
        final FetchDescendantsOption fetchDescendantsOption = FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
        final String targetData = MultipartFileUtil.extractJsonContent(targetDataAsJsonFile, jsonObjectMapper);
        final Map<String, String> yangResourceMap;
        if (yangResourceFile == null) {
            yangResourceMap = Collections.emptyMap();
        } else {
            yangResourceMap = extractYangResourcesMap(yangResourceFile);
        }
        final Collection<DeltaReport> deltaReports = Collections.unmodifiableList(
            cpsDeltaService.getDeltaByDataspaceAnchorAndPayload(dataspaceName, sourceAnchorName,
                xpath, yangResourceMap, targetData, fetchDescendantsOption, groupDataNodes));
        return new ResponseEntity<>(jsonObjectMapper.asJsonString(deltaReports), HttpStatus.OK);
    }

    public ResponseEntity<String> applyChangesInDeltaReport(final String dataspaceName,
                                                            final String anchorName,
                                                            final String deltaReport) {
        cpsDeltaService.applyChangesInDeltaReport(dataspaceName, anchorName, deltaReport);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private ResponseEntity<Object> buildDeltaResponseEntity(final List<DeltaReport> deltaReports,
                                                            final ContentType contentType) {
        final String responseData =
                        XML.equals(contentType)
                        ? deltaReportUtils.buildXml(deltaReports)
                        : jsonObjectMapper.asJsonString(deltaReports);
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }
}
