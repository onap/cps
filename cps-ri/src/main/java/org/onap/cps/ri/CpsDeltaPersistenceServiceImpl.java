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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

        final List<DeltaReport> deltaReports = new ArrayList<>();
        deltaReports.addAll(buildRemovedDeltaReports(sourceAnchorId, targetAnchorId, xpath, fetchDescendantsOption));
        deltaReports.addAll(buildUpdatedDeltaReports(sourceAnchorId, targetAnchorId, xpath, fetchDescendantsOption));
        deltaReports.addAll(buildAddedDeltaReports(sourceAnchorId, targetAnchorId, xpath, fetchDescendantsOption));
        return Collections.unmodifiableList(deltaReports);
    }

    private List<DeltaReport> buildRemovedDeltaReports(final long sourceAnchorId, final long targetAnchorId,
                                                       final String xpath,
                                                       final FetchDescendantsOption fetchDescendantsOption) {
        final List<DeltaProjection> removedFragments =
            findRemovedFragmentsByDepth(sourceAnchorId, targetAnchorId, xpath, fetchDescendantsOption);
        final List<DeltaReport> removedDeltaReports = new ArrayList<>(removedFragments.size());
        for (final DeltaProjection removedFragment : removedFragments) {
            final Map<String, Serializable> sourceData =
                wrapUnderNodeName(removedFragment.getXpath(), parseAttributes(removedFragment.getSourceAttributes()));
            removedDeltaReports.add(new DeltaReportBuilder()
                .actionRemove()
                .withXpath(removedFragment.getXpath())
                .withSourceData(sourceData)
                .build());
        }
        return removedDeltaReports;
    }

    private List<DeltaReport> buildAddedDeltaReports(final long sourceAnchorId, final long targetAnchorId,
                                                     final String xpath,
                                                     final FetchDescendantsOption fetchDescendantsOption) {
        final List<DeltaProjection> addedFragments =
            findAddedFragmentsByDepth(sourceAnchorId, targetAnchorId, xpath, fetchDescendantsOption);
        final List<DeltaReport> addedDeltaReports = new ArrayList<>(addedFragments.size());
        for (final DeltaProjection addedFragment : addedFragments) {
            final Map<String, Serializable> targetData =
                wrapUnderNodeName(addedFragment.getXpath(), parseAttributes(addedFragment.getTargetAttributes()));
            addedDeltaReports.add(new DeltaReportBuilder()
                .actionCreate()
                .withXpath(addedFragment.getXpath())
                .withTargetData(targetData)
                .build());
        }
        return addedDeltaReports;
    }

    private List<DeltaReport> buildUpdatedDeltaReports(final long sourceAnchorId, final long targetAnchorId,
                                                       final String xpath,
                                                       final FetchDescendantsOption fetchDescendantsOption) {
        final List<DeltaProjection> updatedFragments =
            findUpdatedFragmentsByDepth(sourceAnchorId, targetAnchorId, xpath, fetchDescendantsOption);
        final List<DeltaReport> updatedDeltaReports = new ArrayList<>(updatedFragments.size());
        for (final DeltaProjection updatedFragment : updatedFragments) {
            final Map<String, Serializable> sourceAttributes = parseAttributes(updatedFragment.getSourceAttributes());
            final Map<String, Serializable> targetAttributes = parseAttributes(updatedFragment.getTargetAttributes());
            final Map<String, Serializable> updatedSourceLeaves = new HashMap<>();
            final Map<String, Serializable> updatedTargetLeaves = new HashMap<>();
            computeLeafLevelDiff(sourceAttributes, targetAttributes, updatedSourceLeaves, updatedTargetLeaves);
            if (!updatedSourceLeaves.isEmpty() || !updatedTargetLeaves.isEmpty()) {
                final String fragmentXpath = updatedFragment.getXpath();
                addKeyLeavesToUpdatedData(fragmentXpath, updatedSourceLeaves);
                addKeyLeavesToUpdatedData(fragmentXpath, updatedTargetLeaves);
                updatedDeltaReports.add(new DeltaReportBuilder()
                    .actionReplace()
                    .withXpath(fragmentXpath)
                    .withSourceData(wrapUnderNodeName(fragmentXpath, updatedSourceLeaves))
                    .withTargetData(wrapUnderNodeName(fragmentXpath, updatedTargetLeaves))
                    .build());
            }
        }
        return updatedDeltaReports;
    }

    private List<DeltaProjection> findRemovedFragmentsByDepth(final long sourceAnchorId, final long targetAnchorId,
                                                              final String xpath,
                                                              final FetchDescendantsOption fetchDescendantsOption) {
        final int depth = fetchDescendantsOption.getDepth();
        if (depth == 0) {
            return fragmentRepository.findRemovedFragmentsExactXpath(sourceAnchorId, targetAnchorId, xpath);
        } else if (depth == 1) {
            final String escapedXpath = EscapeUtils.escapeForSqlLike(xpath);
            return fragmentRepository.findRemovedFragmentsWithDirectChildren(sourceAnchorId, targetAnchorId,
                xpath, escapedXpath);
        }
        return fragmentRepository.findDeltaRemovedFragments(sourceAnchorId, targetAnchorId, xpath);
    }

    private List<DeltaProjection> findAddedFragmentsByDepth(final long sourceAnchorId, final long targetAnchorId,
                                                            final String xpath,
                                                            final FetchDescendantsOption fetchDescendantsOption) {
        final int depth = fetchDescendantsOption.getDepth();
        if (depth == 0) {
            return fragmentRepository.findAddedFragmentsExactXpath(sourceAnchorId, targetAnchorId, xpath);
        } else if (depth == 1) {
            final String escapedXpath = EscapeUtils.escapeForSqlLike(xpath);
            return fragmentRepository.findAddedFragmentsWithDirectChildren(sourceAnchorId, targetAnchorId,
                xpath, escapedXpath);
        }
        return fragmentRepository.findDeltaAddedFragments(sourceAnchorId, targetAnchorId, xpath);
    }

    private List<DeltaProjection> findUpdatedFragmentsByDepth(final long sourceAnchorId, final long targetAnchorId,
                                                              final String xpath,
                                                              final FetchDescendantsOption fetchDescendantsOption) {
        final int depth = fetchDescendantsOption.getDepth();
        if (depth == 0) {
            return fragmentRepository.findUpdatedFragmentsExactXpath(sourceAnchorId, targetAnchorId, xpath);
        } else if (depth == 1) {
            final String escapedXpath = EscapeUtils.escapeForSqlLike(xpath);
            return fragmentRepository.findUpdatedFragmentsWithDirectChildren(sourceAnchorId, targetAnchorId,
                xpath, escapedXpath);
        }
        return fragmentRepository.findDeltaUpdatedFragments(sourceAnchorId, targetAnchorId, xpath);
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

    private static void addKeyLeavesToUpdatedData(final String xpath,
                                                  final Map<String, Serializable> updatedLeaves) {
        if (!updatedLeaves.isEmpty() && CpsPathUtil.isPathToListElement(xpath)) {
            final List<CpsPathQuery.LeafCondition> leafConditions =
                CpsPathUtil.getCpsPathQuery(xpath).getLeafConditions();
            for (final CpsPathQuery.LeafCondition leafCondition : leafConditions) {
                updatedLeaves.put(leafCondition.name(), (Serializable) leafCondition.value());
            }
        }
    }

    private static Map<String, Serializable> wrapUnderNodeName(final String xpath,
                                                               final Map<String, Serializable> attributes) {
        if (attributes.isEmpty()) {
            return attributes;
        }
        final String nodeName = getNodeIdentifier(xpath);
        final Map<String, Serializable> wrappedData = new LinkedHashMap<>();
        if (isListElement(xpath)) {
            wrappedData.put(nodeName, (Serializable) Collections.singletonList(attributes));
        } else {
            wrappedData.put(nodeName, (Serializable) attributes);
        }
        return wrappedData;
    }

    private static boolean isListElement(final String xpath) {
        return xpath.endsWith("]");
    }

    private static String getNodeIdentifier(final String xpath) {
        String effectiveXpath = xpath;
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(xpath);
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
