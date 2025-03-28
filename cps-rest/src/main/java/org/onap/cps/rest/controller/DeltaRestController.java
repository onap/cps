/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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

import io.micrometer.core.annotation.Timed;
import jakarta.validation.ValidationException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.rest.api.CpsDeltaApi;
import org.onap.cps.rest.utils.MultipartFileUtil;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class DeltaRestController implements CpsDeltaApi {

    private static final String ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateTimeFormatter ISO_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIMESTAMP_FORMAT);

    private final CpsDeltaService cpsDeltaService;
    private final JsonObjectMapper jsonObjectMapper;

    @Timed(value = "cps.delta.controller.get.delta",
        description = "Time taken to get delta between anchors")
    @Override
    public ResponseEntity<Object> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                                final String sourceAnchorName,
                                                                final String targetAnchorName,
                                                                final String xpath,
                                                                final String descendants,
                                                                final Boolean groupDataNodes) {
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(descendants);
        final List<DeltaReport> deltaBetweenAnchors =
            cpsDeltaService.getDeltaByDataspaceAndAnchors(dataspaceName, sourceAnchorName,
                targetAnchorName, xpath, fetchDescendantsOption, groupDataNodes);
        return new ResponseEntity<>(jsonObjectMapper.asJsonString(deltaBetweenAnchors), HttpStatus.OK);
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

    @Override
    public ResponseEntity<String> applyDelta(final String dataspaceName,
                                             final String anchorName,
                                             final String deltaReport,
                                             final String observedTimestamp) {

        cpsDeltaService.applyDelta(dataspaceName, anchorName, deltaReport, toOffsetDateTime(observedTimestamp));
        return ResponseEntity.status(HttpStatus.CREATED).build();
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
