/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG
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

package org.onap.cps.ri;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.impl.DeltaReportBuilder;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.repository.AnchorRepository;
import org.onap.cps.ri.repository.DataspaceRepository;
import org.onap.cps.ri.repository.DeltaProjection;
import org.onap.cps.ri.repository.FragmentRepository;
import org.onap.cps.ri.utils.EscapeUtils;
import org.onap.cps.spi.CpsDeltaPersistenceService;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CpsDeltaPersistenceServiceImpl implements CpsDeltaPersistenceService {

    private final DataspaceRepository dataspaceRepository;
    private final AnchorRepository anchorRepository;
    private final FragmentRepository fragmentRepository;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    public List<DeltaReport> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                           final String sourceAnchorName,
                                                           final String targetAnchorName,
                                                           final String xpath,
                                                           final FetchDescendantsOption fetchDescendantsOption) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final long sourceAnchorId = anchorRepository.getByDataspaceAndName(dataspaceEntity, sourceAnchorName).getId();
        final long targetAnchorId = anchorRepository.getByDataspaceAndName(dataspaceEntity, targetAnchorName).getId();

        final List<DeltaProjection> allDeltaFragments =
            findDeltaFragmentsByDepth(sourceAnchorId, targetAnchorId, xpath, fetchDescendantsOption);

        return buildDeltaReports(allDeltaFragments);
    }

    private List<DeltaProjection> findDeltaFragmentsByDepth(final long sourceAnchorId, final long targetAnchorId,
                                                            final String xpath,
                                                            final FetchDescendantsOption fetchDescendantsOption) {
        final int depth = fetchDescendantsOption.getDepth();
        if (depth == 0) {
            return fragmentRepository.findCompositeDeltaFragmentsExactXpath(sourceAnchorId, targetAnchorId, xpath);
        } else if (depth == 1) {
            final String escapedXpath = EscapeUtils.escapeForSqlLike(xpath);
            return fragmentRepository.findCompositeDeltaFragmentsWithDirectChildren(
                sourceAnchorId, targetAnchorId, xpath, escapedXpath);
        }
        return fragmentRepository.findAllDeltaFragments(sourceAnchorId, targetAnchorId, xpath);
    }

    private List<DeltaReport> buildDeltaReports(final List<DeltaProjection> deltaFragments) {
        final List<DeltaReport> deltaReports = new ArrayList<>(deltaFragments.size());
        for (final DeltaProjection fragment : deltaFragments) {
            final String fragmentXpath = fragment.getXpath();
            final boolean sourceExists = fragment.getSourceId() != null;
            final boolean targetExists = fragment.getTargetId() != null;

            if (sourceExists && !targetExists) {
                deltaReports.add(buildRemovedDeltaReport(fragmentXpath, fragment.getSourceAttributes()));
            } else if (!sourceExists && targetExists) {
                deltaReports.add(buildAddedDeltaReport(fragmentXpath, fragment.getTargetAttributes()));
            } else {
                buildUpdatedDeltaReport(fragmentXpath, fragment.getSourceAttributes(),
                    fragment.getTargetAttributes(), deltaReports);
            }
        }
        return Collections.unmodifiableList(deltaReports);
    }

    private DeltaReport buildRemovedDeltaReport(final String xpath, final String sourceAttributesJson) {
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(xpath);
        final Map<String, Serializable> sourceData =
            wrapUnderNodeName(xpath, cpsPathQuery, parseAttributes(sourceAttributesJson));
        return new DeltaReportBuilder()
            .actionRemove()
            .withXpath(xpath)
            .withSourceData(sourceData)
            .build();
    }

    private DeltaReport buildAddedDeltaReport(final String xpath, final String targetAttributesJson) {
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(xpath);
        final Map<String, Serializable> targetData =
            wrapUnderNodeName(xpath, cpsPathQuery, parseAttributes(targetAttributesJson));
        return new DeltaReportBuilder()
            .actionCreate()
            .withXpath(xpath)
            .withTargetData(targetData)
            .build();
    }

    private void buildUpdatedDeltaReport(final String xpath, final String sourceAttributesJson,
                                         final String targetAttributesJson,
                                         final List<DeltaReport> deltaReports) {
        final Map<String, Serializable> sourceAttributes = parseAttributes(sourceAttributesJson);
        final Map<String, Serializable> targetAttributes = parseAttributes(targetAttributesJson);
        final Map<String, Serializable> updatedSourceLeaves = new HashMap<>();
        final Map<String, Serializable> updatedTargetLeaves = new HashMap<>();
        computeLeafLevelDiff(sourceAttributes, targetAttributes, updatedSourceLeaves, updatedTargetLeaves);
        if (!updatedSourceLeaves.isEmpty() || !updatedTargetLeaves.isEmpty()) {
            final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(xpath);
            addKeyLeavesToUpdatedData(cpsPathQuery, updatedSourceLeaves);
            addKeyLeavesToUpdatedData(cpsPathQuery, updatedTargetLeaves);
            deltaReports.add(new DeltaReportBuilder()
                .actionReplace()
                .withXpath(xpath)
                .withSourceData(wrapUnderNodeName(xpath, cpsPathQuery, updatedSourceLeaves))
                .withTargetData(wrapUnderNodeName(xpath, cpsPathQuery, updatedTargetLeaves))
                .build());
        }
    }

    private void computeLeafLevelDiff(final Map<String, Serializable> sourceAttributes,
                                      final Map<String, Serializable> targetAttributes,
                                      final Map<String, Serializable> updatedSourceData,
                                      final Map<String, Serializable> updatedTargetData) {
        for (final Map.Entry<String, Serializable> entry : sourceAttributes.entrySet()) {
            final String key = entry.getKey();
            final Serializable sourceValue = entry.getValue();
            final Serializable targetValue = targetAttributes.get(key);
            if (targetValue == null) {
                updatedSourceData.put(key, sourceValue);
            } else if (!Objects.equals(sourceValue, targetValue)) {
                updatedSourceData.put(key, sourceValue);
                updatedTargetData.put(key, targetValue);
            }
        }
        for (final Map.Entry<String, Serializable> entry : targetAttributes.entrySet()) {
            if (!sourceAttributes.containsKey(entry.getKey())) {
                updatedTargetData.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void addKeyLeavesToUpdatedData(final CpsPathQuery cpsPathQuery,
                                                  final Map<String, Serializable> updatedLeaves) {
        if (!updatedLeaves.isEmpty() && cpsPathQuery.isPathToListElement()) {
            final List<CpsPathQuery.LeafCondition> leafConditions = cpsPathQuery.getLeafConditions();
            for (final CpsPathQuery.LeafCondition leafCondition : leafConditions) {
                updatedLeaves.put(leafCondition.name(), (Serializable) leafCondition.value());
            }
        }
    }

    private static Map<String, Serializable> wrapUnderNodeName(final String xpath,
                                                               final CpsPathQuery cpsPathQuery,
                                                               final Map<String, Serializable> attributes) {
        if (attributes.isEmpty()) {
            return attributes;
        }
        final String nodeName = getNodeIdentifier(xpath, cpsPathQuery);
        final Map<String, Serializable> wrappedData = new LinkedHashMap<>();
        if (cpsPathQuery.isPathToListElement()) {
            wrappedData.put(nodeName, (Serializable) Collections.singletonList(attributes));
        } else {
            wrappedData.put(nodeName, (Serializable) attributes);
        }
        return wrappedData;
    }

    private static String getNodeIdentifier(final String xpath, final CpsPathQuery cpsPathQuery) {
        String effectiveXpath = xpath;
        if (cpsPathQuery.isPathToListElement()) {
            effectiveXpath = cpsPathQuery.getXpathPrefix();
        }
        final int fromIndex = effectiveXpath.lastIndexOf('/') + 1;
        return effectiveXpath.substring(fromIndex);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Serializable> parseAttributes(final String attributesJson) {
        if (attributesJson == null || attributesJson.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, Serializable> map =
            jsonObjectMapper.convertJsonString(attributesJson, Map.class);
        return map != null ? map : Collections.emptyMap();
    }
}
