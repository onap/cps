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
import org.onap.cps.ri.repository.DeltaProjectionDto;
import org.onap.cps.ri.repository.FragmentRepository;
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
        final List<DeltaProjectionDto> allDeltaFragments =
            fetchDeltaFragments(dataspaceName, sourceAnchorName, targetAnchorName, xpath, fetchDescendantsOption);
        return buildDeltaReports(allDeltaFragments);
    }

    @Override
    public List<DeltaReport> getGroupedDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                                  final String sourceAnchorName,
                                                                  final String targetAnchorName,
                                                                  final String xpath,
                                                                  final FetchDescendantsOption fetchDescendantsOption) {
        final List<DeltaProjectionDto> allDeltaFragments =
            fetchDeltaFragments(dataspaceName, sourceAnchorName, targetAnchorName, xpath, fetchDescendantsOption);
        return buildGroupedDeltaReports(allDeltaFragments);
    }

    private List<DeltaProjectionDto> fetchDeltaFragments(final String dataspaceName,
                                                         final String sourceAnchorName,
                                                         final String targetAnchorName,
                                                         final String xpath,
                                                         final FetchDescendantsOption fetchDescendantsOption) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final long sourceAnchorId = anchorRepository.getByDataspaceAndName(dataspaceEntity, sourceAnchorName).getId();
        final long targetAnchorId = anchorRepository.getByDataspaceAndName(dataspaceEntity, targetAnchorName).getId();
        return findDeltaFragmentsByDepth(sourceAnchorId, targetAnchorId, xpath, fetchDescendantsOption);
    }

    private List<DeltaProjectionDto> findDeltaFragmentsByDepth(final long sourceAnchorId, final long targetAnchorId,
                                                               final String xpath,
                                                               final FetchDescendantsOption fetchDescendantsOption) {
        final int depth = fetchDescendantsOption.getDepth();
        if (depth == 0) {
            return fragmentRepository.findAllDeltaFragmentsExactXpath(sourceAnchorId, targetAnchorId, xpath);
        }
        if (depth == 1) {
            return fragmentRepository.findAllDeltaFragmentsWithDirectChildren(
                sourceAnchorId, targetAnchorId, xpath);
        }
        return fragmentRepository.findAllDeltaFragments(sourceAnchorId, targetAnchorId, xpath);
    }

    private List<DeltaReport> buildDeltaReports(final List<DeltaProjectionDto> deltaFragments) {
        final List<DeltaReport> deltaReports = new ArrayList<>(deltaFragments.size());
        for (final DeltaProjectionDto fragment : deltaFragments) {
            final String fragmentXpath = fragment.getXpath();
            final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(fragmentXpath);
            final boolean sourceExists = fragment.getSourceId() != null;
            final boolean targetExists = fragment.getTargetId() != null;

            if (sourceExists && !targetExists) {
                deltaReports.add(buildCreateOrRemoveDeltaReport(fragmentXpath, cpsPathQuery,
                    fragment.getSourceAttributes(), DeltaReport.REMOVE_ACTION));
            } else if (!sourceExists && targetExists) {
                deltaReports.add(buildCreateOrRemoveDeltaReport(fragmentXpath, cpsPathQuery,
                    fragment.getTargetAttributes(), DeltaReport.CREATE_ACTION));
            } else {
                buildUpdatedDeltaReport(fragmentXpath, cpsPathQuery, fragment.getSourceAttributes(),
                    fragment.getTargetAttributes(), deltaReports);
            }
        }
        return Collections.unmodifiableList(deltaReports);
    }

    private DeltaReport buildCreateOrRemoveDeltaReport(final String xpath, final CpsPathQuery cpsPathQuery,
                                                       final String attributesJson, final String action) {
        final Map<String, Serializable> data =
            wrapUnderNodeName(xpath, cpsPathQuery, parseAttributes(attributesJson));
        final DeltaReportBuilder builder = new DeltaReportBuilder().withXpath(xpath);
        if (DeltaReport.REMOVE_ACTION.equals(action)) {
            builder.actionRemove().withSourceData(data);
        } else {
            builder.actionCreate().withTargetData(data);
        }
        return builder.build();
    }

    private void buildUpdatedDeltaReport(final String xpath, final CpsPathQuery cpsPathQuery,
                                         final String sourceAttributesJson,
                                         final String targetAttributesJson,
                                         final List<DeltaReport> deltaReports) {
        final Map<String, Serializable> sourceAttributes = parseAttributes(sourceAttributesJson);
        final Map<String, Serializable> targetAttributes = parseAttributes(targetAttributesJson);
        final Map<String, Serializable> updatedSourceLeaves = new HashMap<>();
        final Map<String, Serializable> updatedTargetLeaves = new HashMap<>();
        computeLeafLevelDiff(sourceAttributes, targetAttributes, updatedSourceLeaves, updatedTargetLeaves);
        if (!updatedSourceLeaves.isEmpty() || !updatedTargetLeaves.isEmpty()) {
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

    private List<DeltaReport> buildGroupedDeltaReports(final List<DeltaProjectionDto> deltaFragments) {
        final List<DeltaReport> groupedDeltaReports = new ArrayList<>();
        final Map<String, List<DeltaProjectionDto>> removedByParent = new LinkedHashMap<>();
        final Map<String, List<DeltaProjectionDto>> addedByParent = new LinkedHashMap<>();
        final Map<String, CpsPathQuery> xpathToCpsPathQuery = new HashMap<>();

        for (final DeltaProjectionDto fragment : deltaFragments) {
            final boolean sourceExists = fragment.getSourceId() != null;
            final boolean targetExists = fragment.getTargetId() != null;
            final String xpath = fragment.getXpath();
            final CpsPathQuery cpsPathQuery = getCachedCpsPathQuery(xpath, xpathToCpsPathQuery);

            if (sourceExists && !targetExists) {
                final String parentXpath = getParentXpathFromQuery(xpath, cpsPathQuery);
                removedByParent.computeIfAbsent(parentXpath, key -> new ArrayList<>()).add(fragment);
            } else if (!sourceExists && targetExists) {
                final String parentXpath = getParentXpathFromQuery(xpath, cpsPathQuery);
                addedByParent.computeIfAbsent(parentXpath, key -> new ArrayList<>()).add(fragment);
            } else {
                buildUpdatedDeltaReport(xpath, cpsPathQuery, fragment.getSourceAttributes(),
                    fragment.getTargetAttributes(), groupedDeltaReports);
            }
        }

        groupedDeltaReports.addAll(
            buildGroupedCreateOrRemoveDeltaReports(removedByParent, DeltaReport.REMOVE_ACTION, xpathToCpsPathQuery));
        groupedDeltaReports.addAll(
            buildGroupedCreateOrRemoveDeltaReports(addedByParent, DeltaReport.CREATE_ACTION, xpathToCpsPathQuery));

        return Collections.unmodifiableList(groupedDeltaReports);
    }

    private List<DeltaReport> buildGroupedCreateOrRemoveDeltaReports(
            final Map<String, List<DeltaProjectionDto>> fragmentsByParent,
            final String action,
            final Map<String, CpsPathQuery> xpathToCpsPathQuery) {
        final List<DeltaReport> deltaReports = new ArrayList<>();
        for (final Map.Entry<String, List<DeltaProjectionDto>> entry : fragmentsByParent.entrySet()) {
            final String parentXpath = entry.getKey();
            final Map<String, Serializable> mergedData =
                mergeAttributesUnderNodeName(entry.getValue(), action, xpathToCpsPathQuery);
            final DeltaReportBuilder deltaReportBuilder = new DeltaReportBuilder().withXpath(parentXpath);
            if (DeltaReport.REMOVE_ACTION.equals(action)) {
                deltaReportBuilder.actionRemove().withSourceData(mergedData);
            } else {
                deltaReportBuilder.actionCreate().withTargetData(mergedData);
            }
            deltaReports.add(deltaReportBuilder.build());
        }
        return deltaReports;
    }

    private Map<String, Serializable> mergeAttributesUnderNodeName(final List<DeltaProjectionDto> fragments,
                                                                   final String action,
                                                                   final Map<String, CpsPathQuery> xpathToCpsPath) {
        final Map<String, List<Map<String, Serializable>>> nodeNameToAttributesList = new LinkedHashMap<>();
        for (final DeltaProjectionDto fragment : fragments) {
            final String xpath = fragment.getXpath();
            final String attributesJson = DeltaReport.REMOVE_ACTION.equals(action)
                ? fragment.getSourceAttributes() : fragment.getTargetAttributes();
            final Map<String, Serializable> attributes = parseAttributes(attributesJson);
            final CpsPathQuery cpsPathQuery = getCachedCpsPathQuery(xpath, xpathToCpsPath);
            final String nodeName = getNodeIdentifier(xpath, cpsPathQuery);
            if (cpsPathQuery.isPathToListElement()) {
                addKeyLeavesToUpdatedData(cpsPathQuery, attributes);
            }
            nodeNameToAttributesList.computeIfAbsent(nodeName, key -> new ArrayList<>()).add(attributes);
        }
        final Map<String, Serializable> mergedData = new LinkedHashMap<>();
        for (final Map.Entry<String, List<Map<String, Serializable>>> entry : nodeNameToAttributesList.entrySet()) {
            mergedData.put(entry.getKey(), (Serializable) entry.getValue());
        }
        return mergedData;
    }

    private static CpsPathQuery getCachedCpsPathQuery(final String xpath,
                                                      final Map<String, CpsPathQuery> cpsPathQuerycache) {
        return cpsPathQuerycache.computeIfAbsent(xpath, CpsPathUtil::getCpsPathQuery);
    }

    private static String getParentXpathFromQuery(final String xpath, final CpsPathQuery cpsPathQuery) {
        final String parentXpath = cpsPathQuery.getNormalizedParentPath();
        return parentXpath.isEmpty() ? xpath : parentXpath;
    }
}
