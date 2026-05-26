/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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

package org.onap.cps.impl.query;

import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS;
import static org.onap.cps.impl.query.CompositeQueryOperator.getNormalizedOperator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.model.CompositeQuery;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class CompositeQueryProcessor {

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CompositeQueryEvaluator compositeQueryEvaluator;

    /**
     * Processes a composite query by executing the base cpsPath query, applying nested conditions
     * using the configured operator logic, and returning matching DataNodes.
     *
     * @param dataspaceName          the dataspace name
     * @param anchorName             the anchor name
     * @param compositeQuery         the composite query containing cpsPath, operator and conditions
     * @param fetchDescendantsOption option controlling how many levels of descendants to include
     * @return                       collection of DataNodes matching the composite query
     */
    public Collection<DataNode> processCompositeQuery(final String dataspaceName,
                                                      final String anchorName,
                                                      final CompositeQuery compositeQuery,
                                                      final FetchDescendantsOption fetchDescendantsOption) {
        final List<DataNode> candidateTree = fetchDataNodeTree(dataspaceName, anchorName,
            compositeQuery.getCpsPath(), fetchDescendantsOption);
        if (candidateTree.isEmpty()) {
            return Collections.emptyList();
        }
        final Map<String, DataNode> xpathToDataNode = new HashMap<>();
        candidateTree.forEach(dataNode -> populateXpathToDataNode(dataNode, xpathToDataNode));
        final Set<String> candidateXpaths = xpathToDataNode.keySet();
        final Set<String> matchedXpaths =
                computeMatchedXpaths(compositeQuery, candidateXpaths, xpathToDataNode, dataspaceName, anchorName);
        return pruneTree(candidateTree, matchedXpaths, fetchDescendantsOption);
    }

    private List<DataNode> fetchDataNodeTree(final String dataspaceName, final String anchorName,
                                             final String cpsPath,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.queryDataNodes(dataspaceName, anchorName,
            cpsPath, fetchDescendantsOption, 0);
    }

    private Set<String> computeMatchedXpaths(final CompositeQuery compositeQuery,
                                             final Set<String> candidateXpaths,
                                             final Map<String, DataNode> xpathToDataNode,
                                             final String dataspaceName,
                                             final String anchorName) {
        final Collection<CompositeQuery> subConditions = compositeQuery.getConditions();
        if (CollectionUtils.isEmpty(subConditions)) {
            return new HashSet<>(candidateXpaths);
        }
        final CompositeQueryOperator normalizedOperator = getNormalizedOperator(compositeQuery.getOperator());
        return compositeQueryEvaluator.evaluate(subConditions, normalizedOperator, candidateXpaths,
            dataspaceName, anchorName, buildConditionEvaluator(xpathToDataNode));
    }

    private CompositeQueryOperator.ConditionEvaluator buildConditionEvaluator(
            final Map<String, DataNode> xpathToDataNode) {
        return (condition, scopedXpaths, conditionDataspaceName, conditionAnchorName) ->
            evaluateCondition(condition, scopedXpaths, xpathToDataNode,
                conditionDataspaceName, conditionAnchorName);
    }

    private Set<String> evaluateCondition(final CompositeQuery compositeQuery,
                                          final Set<String> candidateXpaths,
                                          final Map<String, DataNode> xpathToDataNode,
                                          final String dataspaceName,
                                          final String anchorName) {
        final List<DataNode> queryResults = fetchDataNodeTree(dataspaceName, anchorName, compositeQuery.getCpsPath(),
            OMIT_DESCENDANTS);
        final Set<String> matchedXpaths = queryResults.stream()
            .map(DataNode::getXpath)
            .filter(candidateXpaths::contains)
            .collect(Collectors.toSet());
        final Collection<CompositeQuery> subConditions = compositeQuery.getConditions();
        if (CollectionUtils.isEmpty(subConditions)) {
            return matchedXpaths;
        }
        return resolveNestedMatches(matchedXpaths, compositeQuery, xpathToDataNode, dataspaceName, anchorName);
    }

    private Set<String> resolveNestedMatches(final Set<String> matchedXpaths,
                                             final CompositeQuery compositeQuery,
                                             final Map<String, DataNode> xpathToDataNode,
                                             final String dataspaceName,
                                             final String anchorName) {
        final Set<String> nestedScope = new HashSet<>();
        matchedXpaths.forEach(xpath -> nestedScope.addAll(collectDescendantXpaths(xpath, xpathToDataNode)));
        final Set<String> nestedMatches = computeMatchedXpaths(compositeQuery, nestedScope, xpathToDataNode,
            dataspaceName, anchorName);
        if (nestedMatches.isEmpty()) {
            final CompositeQueryOperator nestedOperator = getNormalizedOperator(compositeQuery.getOperator());
            return nestedOperator.requiresAllConditions() ? Collections.emptySet() : matchedXpaths;
        }
        final Set<String> allMatchedXpaths = new HashSet<>(matchedXpaths);
        allMatchedXpaths.addAll(nestedMatches);
        return allMatchedXpaths;
    }

    private void populateXpathToDataNode(final DataNode dataNode,
                                         final Map<String, DataNode> xpathToDataNode) {
        xpathToDataNode.put(dataNode.getXpath(), dataNode);
        final Collection<DataNode> childDataNodes = dataNode.getChildDataNodes();
        if (childDataNodes != null) {
            childDataNodes.forEach(childDataNode -> populateXpathToDataNode(childDataNode, xpathToDataNode));
        }
    }

    private Set<String> collectDescendantXpaths(final String parentXpath,
                                                final Map<String, DataNode> xpathToDataNode) {
        final DataNode parentDataNode = xpathToDataNode.get(parentXpath);
        if (CollectionUtils.isEmpty(parentDataNode.getChildDataNodes())) {
            return Collections.emptySet();
        }
        final Set<String> descendantXpaths = new HashSet<>();
        parentDataNode.getChildDataNodes().forEach(childDataNode -> {
            final String childXpath = childDataNode.getXpath();
            if (descendantXpaths.add(childXpath)) {
                descendantXpaths.addAll(collectDescendantXpaths(childXpath, xpathToDataNode));
            }
        });
        return descendantXpaths;
    }

    private Collection<DataNode> pruneTree(final Collection<DataNode> candidateTree,
                                           final Set<String> matchedXpaths,
                                           final FetchDescendantsOption fetchDescendantsOption) {
        return candidateTree.stream()
            .map(rootNode -> walkNode(rootNode, matchedXpaths, fetchDescendantsOption))
            .filter(Objects::nonNull)
            .toList();
    }

    private DataNode walkNode(final DataNode dataNode,
                              final Set<String> matchedXpaths,
                              final FetchDescendantsOption fetchDescendantsOption) {
        final boolean isMatchedXpath = matchedXpaths.contains(dataNode.getXpath());
        final boolean canDescend = canDescendDataTree(dataNode, fetchDescendantsOption);
        if (isMatchedXpath) {
            return canDescend
                ? rebuildDataNodeTree(dataNode, fetchDescendantsOption)
                : buildFilteredNode(dataNode, Collections.emptyList());
        }
        if (!canDescend) {
            return null;
        }
        final List<DataNode> matchingChildNodes =
                collectMatchingChildNodes(dataNode, matchedXpaths, fetchDescendantsOption);
        return matchingChildNodes.isEmpty() ? null : buildFilteredNode(dataNode, matchingChildNodes);
    }

    private List<DataNode> collectMatchingChildNodes(final DataNode dataNode,
                                                     final Set<String> matchedXpaths,
                                                     final FetchDescendantsOption fetchDescendantsOption) {
        return dataNode.getChildDataNodes().stream()
            .map(childDataNode -> walkNode(childDataNode, matchedXpaths, fetchDescendantsOption.next()))
            .filter(Objects::nonNull)
            .toList();
    }

    private DataNode rebuildDataNodeTree(final DataNode dataNode,
                                         final FetchDescendantsOption fetchDescendantsOption) {
        if (!canDescendDataTree(dataNode, fetchDescendantsOption)) {
            return buildFilteredNode(dataNode, Collections.emptyList());
        }
        return buildFilteredNode(dataNode, rebuildChildDataNodes(dataNode, fetchDescendantsOption));
    }

    private List<DataNode> rebuildChildDataNodes(final DataNode dataNode,
                                                 final FetchDescendantsOption fetchDescendantsOption) {
        return dataNode.getChildDataNodes().stream()
            .map(childDataNode -> rebuildDataNodeTree(childDataNode, fetchDescendantsOption.next()))
            .toList();
    }

    private boolean canDescendDataTree(final DataNode dataNode, final FetchDescendantsOption fetchDescendantsOption) {
        return fetchDescendantsOption.hasNext() && !CollectionUtils.isEmpty(dataNode.getChildDataNodes());
    }

    private DataNode buildFilteredNode(final DataNode source, final List<DataNode> childDataNodes) {
        final DataNode filteredNode = new DataNode();
        filteredNode.setXpath(source.getXpath());
        filteredNode.setLeaves(source.getLeaves());
        filteredNode.setDataspace(source.getDataspace());
        filteredNode.setAnchorName(source.getAnchorName());
        filteredNode.setChildDataNodes(childDataNodes);
        return filteredNode;
    }
}