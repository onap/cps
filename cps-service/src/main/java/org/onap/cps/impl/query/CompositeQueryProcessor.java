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
     * Executes a three-phase composite query against the given dataspace and anchor.
     * Phase 1 fetches the full candidate tree by running the root cpsPath query against the database.
     * Phase 2 evaluates nested conditions using AND or OR logic to compute the set of matched xpaths
     * within the candidate tree.
     * Phase 3 prunes the candidate tree so that only matched nodes and their structural ancestors are retained.
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

    /** Delegates to the persistence layer with pageNumber=0 t
     * o request a full non-paginated fetch of the matching subtree. */
    private List<DataNode> fetchDataNodeTree(final String dataspaceName, final String anchorName,
                                             final String cpsPath,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.queryDataNodes(dataspaceName, anchorName,
            cpsPath, fetchDescendantsOption, 0);
    }

    /**
     * Determines which xpaths from the candidate set survive the query conditions.
     * When no sub-conditions are specified, every candidate xpath passes through unchanged because there is nothing to
     * filter against. When sub-conditions are present, the operator string is resolved to its enum form and the
     * evaluator applies AND or OR logic across all sub-conditions.
     */
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

    /** Constructs the ConditionEvaluator lambda that bridges the evaluator framework to this processor's
     * evaluateCondition logic, closing over xpathToDataNode so that sub-condition calls can resolve descendants from
     * the Phase 1 in-memory map without issuing additional DB queries. */
    private CompositeQueryOperator.ConditionEvaluator buildConditionEvaluator(
            final Map<String, DataNode> xpathToDataNode) {
        return (condition, scopedXpaths, conditionDataspaceName, conditionAnchorName) ->
            evaluateCondition(condition, scopedXpaths, xpathToDataNode,
                conditionDataspaceName, conditionAnchorName);
    }

    /**
     * Evaluates a single composite query condition against the current candidate scope.
     * The DB fetch uses OMIT_DESCENDANTS because only the xpaths of matching nodes are needed,
     * not their subtree content.
     * The result is intersected with candidateXpaths to restrict it to nodes that exist in the Phase 1 candidate tree.
     * If the condition carries nested sub-conditions, the matched xpaths are forwarded to resolveNestedMatches for
     * further evaluation.
     */
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

    /**
     * Handles the case where a condition carries its own nested sub-conditions.
     * The scope for nested evaluation is narrowed to only the descendants of the nodes that matched the parent
     * condition, because nesting implies "within those matched nodes, further filter by these sub-conditions."
     * If nested evaluation yields no matches, the behaviour diverges by operator:
     * AND invalidates the parent match because not all conditions were satisfied,
     * while OR preserves the parent match because partial satisfaction is sufficient.
     */
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

    /**
     * Recursively walks every node in the candidate tree and registers it in the xpath-to-DataNode map for O(1)
     * lookup during Phase 2.
     * The raw null check on childDataNodes is necessary because DataNode instances produced by Jackson deserialization
     * via the no-arg constructor bypass the @Builder.Default initializer, leaving the field as null rather than an
     * empty list.
     */
    private void populateXpathToDataNode(final DataNode dataNode,
                                         final Map<String, DataNode> xpathToDataNode) {
        xpathToDataNode.put(dataNode.getXpath(), dataNode);
        final Collection<DataNode> childDataNodes = dataNode.getChildDataNodes();
        if (childDataNodes != null) {
            childDataNodes.forEach(childDataNode -> populateXpathToDataNode(childDataNode, xpathToDataNode));
        }
    }

    /**
     * Collects the xpaths of all descendants of a given node by traversing the in-memory map, avoiding additional DB
     * queries.
     * Returns an empty set immediately when the parent node has no children to avoid unnecessary traversal.
     * Deduplication relies on Set.add() returning false when a node was already collected, which prevents revisiting
     * nodes in case of shared references.
     */
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

    /** Entry point for Phase 3, which streams the top-level candidate roots through walkNode and removes any root for
     * which walkNode returned null, meaning no matched node existed anywhere in that root's subtree. */
    private Collection<DataNode> pruneTree(final Collection<DataNode> candidateTree,
                                           final Set<String> matchedXpaths,
                                           final FetchDescendantsOption fetchDescendantsOption) {
        return candidateTree.stream()
            .map(rootNode -> walkNode(rootNode, matchedXpaths, fetchDescendantsOption))
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Recursive core of Phase 3.
     * If the current node is in matchedXpaths, its full subtree is rebuilt up to the depth limit via
     * rebuildDataNodeTree with no further match-filtering applied.
     * If the current node is not matched but descent is still possible, its children are recursively evaluated via
     * collectMatchingChildNodes and the node is returned as a structural path node holding only those children that
     * had at least one match beneath them.
     * If the node is neither matched nor able to descend, it is discarded and null is returned to signal that nothing
     * survived this subtree.
     */
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

    /** Drives recursive descent into child nodes for unmatched path nodes by passing each child to walkNode,
     * then filters out null returns so only children with at least one surviving match beneath them are included. */
    private List<DataNode> collectMatchingChildNodes(final DataNode dataNode,
                                                     final Set<String> matchedXpaths,
                                                     final FetchDescendantsOption fetchDescendantsOption) {
        return dataNode.getChildDataNodes().stream()
            .map(childDataNode -> walkNode(childDataNode, matchedXpaths, fetchDescendantsOption.next()))
            .filter(Objects::nonNull)
            .toList();
    }

    /** Reconstructs the full subtree of a matched node carrying every descendant up to the depth limit without
     * applying any match-filtering, because a matched node implies all its descendants should be visible. When the
     * depth limit or a leaf is reached, the node is returned with an empty children list. */
    private DataNode rebuildDataNodeTree(final DataNode dataNode,
                                         final FetchDescendantsOption fetchDescendantsOption) {
        if (!canDescendDataTree(dataNode, fetchDescendantsOption)) {
            return buildFilteredNode(dataNode, Collections.emptyList());
        }
        return buildFilteredNode(dataNode, rebuildChildDataNodes(dataNode, fetchDescendantsOption));
    }

    /** Rebuilds each child's subtree recursively by forwarding the decremented fetchDescendantsOption so the depth
     * limit is correctly tracked through each level of recursion. */
    private List<DataNode> rebuildChildDataNodes(final DataNode dataNode,
                                                 final FetchDescendantsOption fetchDescendantsOption) {
        return dataNode.getChildDataNodes().stream()
            .map(childDataNode -> rebuildDataNodeTree(childDataNode, fetchDescendantsOption.next()))
            .toList();
    }

    /** Guards both walkNode and rebuildDataNodeTree before any attempt to recurse into children;
     * fetchDescendantsOption.hasNext() confirms the depth limit has not been exhausted, and CollectionUtils.isEmpty
     * confirms the node actually has children to descend into. Both conditions must hold. */
    private boolean canDescendDataTree(final DataNode dataNode, final FetchDescendantsOption fetchDescendantsOption) {
        return fetchDescendantsOption.hasNext() && !CollectionUtils.isEmpty(dataNode.getChildDataNodes());
    }

    /** Creates a shallow copy of the source DataNode with a replacement children list, preserving the node's own data
     * (xpath, leaves, dataspace, anchorName) while substituting the children; the original candidate tree nodes must
     * never be mutated because they are shared references across the recursive stream pipeline. */
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