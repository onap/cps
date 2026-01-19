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
        final List<DataNode> dataNodeTree = fetchDataNodeTree(dataspaceName, anchorName,
            compositeQuery.getCpsPath(), fetchDescendantsOption);

        if (dataNodeTree.isEmpty()) {
            return Collections.emptyList();
        }

        final Set<String> matchingXpaths = resolveMatchingXpaths(compositeQuery, dataNodeTree,
            dataspaceName, anchorName);
        return walkTree(dataNodeTree, matchingXpaths, fetchDescendantsOption);
    }

    private List<DataNode> fetchDataNodeTree(final String dataspaceName, final String anchorName,
                                             final String cpsPath,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.queryDataNodes(dataspaceName, anchorName,
            cpsPath, fetchDescendantsOption, 0);
    }

    private Set<String> resolveMatchingXpaths(final CompositeQuery compositeQuery,
                                              final Collection<DataNode> dataNodeTree,
                                              final String dataspaceName,
                                              final String anchorName) {
        final Map<String, DataNode> xpathIndex = flattenDataNodeHierarchy(dataNodeTree);
        return applyConditionsOnFlatMap(compositeQuery, xpathIndex, dataspaceName, anchorName);
    }

    private Collection<DataNode> walkTree(final Collection<DataNode> roots,
                                          final Set<String> matchedXpaths,
                                          final FetchDescendantsOption fetchOption) {
        return roots.stream()
            .map(root -> walkNode(root, matchedXpaths, fetchOption))
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
        final List<DataNode> childNodes = node.getChildDataNodes().stream()
            .map(child -> walkNode(child, matchedXpaths, fetchOption.next()))
            .filter(Objects::nonNull)
            .toList();
        if (matchedXpaths.contains(node.getXpath()) && childNodes.isEmpty()) {
            return buildFilteredNode(node, node.getChildDataNodes().stream()
                .map(child -> walkAllDescendants(child, fetchOption.next()))
                .toList());
        }
        if (matchedXpaths.contains(node.getXpath()) || !childNodes.isEmpty()) {
            return buildFilteredNode(node, childNodes);
        }
        return null;
    }

    private DataNode walkAllDescendants(final DataNode node, final FetchDescendantsOption fetchOption) {
        final List<DataNode> childNodes = fetchOption.hasNext()
            && node.getChildDataNodes() != null && !node.getChildDataNodes().isEmpty()
            ? node.getChildDataNodes().stream()
                .map(child -> walkAllDescendants(child, fetchOption.next()))
                .toList()
            : Collections.emptyList();
        return buildFilteredNode(node, childNodes);
    }

    private Map<String, DataNode> flattenDataNodeHierarchy(final Collection<DataNode> dataNodes) {
        final Map<String, DataNode> xpathToDataNode = new HashMap<>();
        dataNodes.forEach(dataNode -> addNodeAndDescendantsToMap(dataNode, xpathToDataNode));
        return xpathToDataNode;
    }

    private void addNodeAndDescendantsToMap(final DataNode dataNode, final Map<String, DataNode> xpathToDataNode) {
        xpathToDataNode.put(dataNode.getXpath(), dataNode);
        if (dataNode.getChildDataNodes() != null) {
            dataNode.getChildDataNodes().forEach(child -> addNodeAndDescendantsToMap(child, xpathToDataNode));
        }
    }

    private Set<String> applyConditionsOnFlatMap(final CompositeQuery compositeQuery,
                                                 final Map<String, DataNode> xpathToDataNodes,
                                                 final String dataspaceName,
                                                 final String anchorName) {
        if (compositeQuery.getConditions() == null || compositeQuery.getConditions().isEmpty()) {
            return new HashSet<>(xpathToDataNodes.keySet());
        }
        final CompositeQueryOperator normalizedOperator = getNormalizedOperator(compositeQuery.getOperator());
        return normalizedOperator.evaluateOperator(
            compositeQuery.getConditions(),
            xpathToDataNodes,
            dataspaceName,
            anchorName,
            this::evaluateCondition
        );
    }

    private Set<String> evaluateCondition(final CompositeQuery compositeQuery,
                                          final Map<String, DataNode> xpathToDataNodes,
                                          final String dataspaceName,
                                          final String anchorName) {
        final List<DataNode> queryResults = cpsDataPersistenceService.queryDataNodes(
            dataspaceName, anchorName, compositeQuery.getCpsPath(),
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0);
        final Set<String> matchedXpaths = queryResults.stream()
            .map(DataNode::getXpath)
            .filter(xpathToDataNodes::containsKey)
            .collect(Collectors.toSet());
        if (compositeQuery.getConditions() != null && !compositeQuery.getConditions().isEmpty()) {
            final Map<String, DataNode> scopedNodes =
                getScopedNodesFromFlatMap(matchedXpaths, xpathToDataNodes);
            final Set<String> nestedMatches = applyConditionsOnFlatMap(compositeQuery, scopedNodes,
                dataspaceName, anchorName);
            if (nestedMatches.isEmpty()) {
                final CompositeQueryOperator nestedOperator = getNormalizedOperator(compositeQuery.getOperator());
                return nestedOperator.requiresAllConditions() ? Collections.emptySet() : matchedXpaths;
            }
            final Set<String> result = new HashSet<>(matchedXpaths);
            result.addAll(nestedMatches);
            return result;
        }
        return matchedXpaths;
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

    private Map<String, DataNode> getScopedNodesFromFlatMap(final Set<String> parentXpaths,
                                                            final Map<String, DataNode> xpathToDataNodes) {
        final Set<String> descendantXpaths = new HashSet<>();
        for (final String parentXpath : parentXpaths) {
            collectDescendantsFromDataNode(xpathToDataNodes.get(parentXpath), descendantXpaths);
        }
        final Map<String, DataNode> scopedNodes = new HashMap<>();
        for (final String descendantXpath : descendantXpaths) {
            scopedNodes.put(descendantXpath, xpathToDataNodes.get(descendantXpath));
        }
        return scopedNodes;
    }

    private void collectDescendantsFromDataNode(final DataNode dataNode, final Set<String> descendantXpaths) {
        if (dataNode.getChildDataNodes() != null) {
            for (final DataNode child : dataNode.getChildDataNodes()) {
                if (descendantXpaths.add(child.getXpath())) {
                    collectDescendantsFromDataNode(child, descendantXpaths);
                }
            }
        }
    }
}

