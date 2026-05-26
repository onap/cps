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
        final Set<String> candidateXpaths = resolveMatchingXpaths(compositeQuery, candidateTree,
            dataspaceName, anchorName);
        return pruneAndFlattenTree(candidateTree, candidateXpaths, fetchDescendantsOption);
    }

    private List<DataNode> fetchDataNodeTree(final String dataspaceName, final String anchorName,
                                             final String cpsPath,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.queryDataNodes(dataspaceName, anchorName,
            cpsPath, fetchDescendantsOption, 0);
    }

    private Set<String> resolveMatchingXpaths(final CompositeQuery compositeQuery,
                                              final Collection<DataNode> candidateTree,
                                              final String dataspaceName,
                                              final String anchorName) {
        final Set<String> candidateXpaths = new HashSet<>();
        final Map<String, DataNode> xpathToDataNode = new HashMap<>();
        candidateTree.forEach(dataNode -> populateXpathToDataNode(dataNode, candidateXpaths, xpathToDataNode));
        return applyOperator(compositeQuery, candidateXpaths, xpathToDataNode, dataspaceName, anchorName);
    }

    private Set<String> applyOperator(final CompositeQuery compositeQuery,
                                      final Set<String> candidateXpaths,
                                      final Map<String, DataNode> xpathToDataNode,
                                      final String dataspaceName,
                                      final String anchorName) {
        final Collection<CompositeQuery> subConditions = compositeQuery.getConditions();
        if (subConditions == null || subConditions.isEmpty()) {
            return new HashSet<>(candidateXpaths);
        }
        final CompositeQueryOperator normalizedOperator = getNormalizedOperator(compositeQuery.getOperator());
        return compositeQueryEvaluator.evaluate(
            subConditions,
            normalizedOperator,
            candidateXpaths,
            dataspaceName,
            anchorName,
            (condition, conditionScopeXpaths, conditionDataspaceName, conditionAnchorName) ->
                evaluateConditions(condition, conditionScopeXpaths, xpathToDataNode,
                    conditionDataspaceName, conditionAnchorName));
    }

    private Set<String> evaluateConditions(final CompositeQuery compositeQuery,
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
        if (subConditions == null || subConditions.isEmpty()) {
            return matchedXpaths;
        }
        final Set<String> nestedScope = new HashSet<>();
        matchedXpaths.forEach(xpath -> nestedScope.addAll(collectDescendantXpaths(xpath, xpathToDataNode)));
        final Set<String> nestedMatches = applyOperator(compositeQuery, nestedScope, xpathToDataNode,
            dataspaceName, anchorName);
        if (nestedMatches.isEmpty()) {
            final CompositeQueryOperator nestedOperator = getNormalizedOperator(compositeQuery.getOperator());
            return nestedOperator.requiresAllConditions() ? Collections.emptySet() : matchedXpaths;
        }
        final Set<String> result = new HashSet<>(matchedXpaths);
        result.addAll(nestedMatches);
        return result;
    }

    private void populateXpathToDataNode(final DataNode dataNode,
                                         final Set<String> candidateXpaths,
                                         final Map<String, DataNode> xpathToDataNode) {
        final String dataNodeXpath = dataNode.getXpath();
        candidateXpaths.add(dataNodeXpath);
        xpathToDataNode.put(dataNodeXpath, dataNode);
        final Collection<DataNode> childDataNodes = dataNode.getChildDataNodes();
        if (childDataNodes != null) {
            childDataNodes.forEach(childDataNode ->
                populateXpathToDataNode(childDataNode, candidateXpaths, xpathToDataNode));
        }
    }

    private Set<String> collectDescendantXpaths(final String parentXpath,
                                                final Map<String, DataNode> xpathToDataNode) {
        final Set<String> descendantsXpaths = new HashSet<>();
        final DataNode parentDataNode = xpathToDataNode.get(parentXpath);
        if (parentDataNode.getChildDataNodes() != null) {
            parentDataNode.getChildDataNodes().forEach(childDataNode -> {
                final String childXpath = childDataNode.getXpath();
                if (descendantsXpaths.add(childXpath)) {
                    descendantsXpaths.addAll(collectDescendantXpaths(childXpath, xpathToDataNode));
                }
            });
        }
        return descendantsXpaths;
    }

    private Collection<DataNode> pruneAndFlattenTree(final Collection<DataNode> candidateTree,
                                                     final Set<String> matchedXpaths,
                                                     final FetchDescendantsOption fetchOption) {
        return candidateTree.stream()
            .map(rootNode -> walkNode(rootNode, matchedXpaths, fetchOption))
            .filter(Objects::nonNull)
            .toList();
    }

    private DataNode walkNode(final DataNode node,
                              final Set<String> matchedXpaths,
                              final FetchDescendantsOption fetchOption) {
        if (!fetchOption.hasNext() || node.getChildDataNodes() == null || node.getChildDataNodes().isEmpty()) {
            return matchedXpaths.contains(node.getXpath())
                ? buildFilteredNode(node, Collections.emptyList())
                : null;
        }
        final List<DataNode> childDataNodes = node.getChildDataNodes().stream()
            .map(child -> walkNode(child, matchedXpaths, fetchOption.next()))
            .filter(Objects::nonNull)
            .toList();
        if (matchedXpaths.contains(node.getXpath()) && childDataNodes.isEmpty()) {
            return buildFilteredNode(node, node.getChildDataNodes().stream()
                .map(child -> walkAllDescendants(child, fetchOption.next()))
                .toList());
        }
        if (matchedXpaths.contains(node.getXpath()) || !childDataNodes.isEmpty()) {
            return buildFilteredNode(node, childDataNodes);
        }
        return null;
    }

    private DataNode walkAllDescendants(final DataNode node, final FetchDescendantsOption fetchOption) {
        final List<DataNode> childDataNodes = fetchOption.hasNext()
            && node.getChildDataNodes() != null && !node.getChildDataNodes().isEmpty()
            ? node.getChildDataNodes().stream()
                .map(child -> walkAllDescendants(child, fetchOption.next()))
                .toList()
            : Collections.emptyList();
        return buildFilteredNode(node, childDataNodes);
    }

    private DataNode buildFilteredNode(final DataNode source, final List<DataNode> filteredChildren) {
        final DataNode result = new DataNode();
        result.setXpath(source.getXpath());
        result.setLeaves(source.getLeaves());
        result.setDataspace(source.getDataspace());
        result.setAnchorName(source.getAnchorName());
        result.setChildDataNodes(filteredChildren);
        return result;
    }
}
