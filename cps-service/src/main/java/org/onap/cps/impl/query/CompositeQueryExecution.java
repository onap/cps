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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.onap.cps.api.model.CompositeQuery;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.springframework.util.CollectionUtils;

/**
 * Carries the state of a single composite query execution: the dataspace and anchor being queried, the
 * in-memory xpath index of the candidate trees and a cache of condition query results. A new instance is
 * created per query by {@link CompositeQueryProcessor}, so instances are never shared between threads and
 * the state does not need to be passed from method to method.
 */
class CompositeQueryExecution {

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CompositeQueryEvaluator compositeQueryEvaluator;
    private final String dataspaceName;
    private final String anchorName;
    private final Map<String, DataNode> xpathToDataNode = new HashMap<>();
    private final Map<String, Set<String>> xpathsPerConditionCpsPath = new HashMap<>();

    CompositeQueryExecution(final CpsDataPersistenceService cpsDataPersistenceService,
                            final CompositeQueryEvaluator compositeQueryEvaluator,
                            final String dataspaceName,
                            final String anchorName) {
        this.cpsDataPersistenceService = cpsDataPersistenceService;
        this.compositeQueryEvaluator = compositeQueryEvaluator;
        this.dataspaceName = dataspaceName;
        this.anchorName = anchorName;
    }

    /**
     * Executes the three-phase composite query algorithm.
     * Phase 1 fetches the full candidate trees by running the root cpsPath query against the database and
     * indexes every node by xpath. Phase 2 evaluates the (nested) conditions using AND or OR logic to compute
     * the set of selected xpaths within the candidate trees. Phase 3 builds new result trees retaining only
     * selected nodes and their structural ancestors.
     *
     * @param compositeQuery         the validated and normalized composite query
     * @param fetchDescendantsOption option controlling how many levels of descendants to include
     * @return                       collection of DataNodes matching the composite query
     */
    Collection<DataNode> execute(final CompositeQuery compositeQuery,
                                 final FetchDescendantsOption fetchDescendantsOption) {
        final List<DataNode> candidateTrees = cpsDataPersistenceService.queryDataNodes(dataspaceName, anchorName,
            compositeQuery.getCpsPath(), fetchDescendantsOption, 0);
        if (candidateTrees.isEmpty()) {
            return Collections.emptyList();
        }
        for (final DataNode candidateTree : candidateTrees) {
            populateXpathToDataNode(candidateTree);
        }
        final Set<String> candidateXpaths = Set.copyOf(xpathToDataNode.keySet());
        final Set<String> selectedXpaths = computeMatchedXpaths(compositeQuery, candidateXpaths);
        return buildFilteredTrees(candidateTrees, selectedXpaths, fetchDescendantsOption);
    }

    /**
     * Determines which xpaths from the candidate set survive the query conditions.
     * When no conditions are specified, every candidate xpath passes through unchanged because there is
     * nothing to filter against. Otherwise the evaluator combines the per-condition results using AND or OR
     * logic. This method is also called recursively (via evaluateCondition) with a narrowed scope when
     * conditions carry their own nested conditions.
     *
     * @param compositeQuery  the composite query (condition) whose conditions are evaluated
     * @param candidateXpaths the xpaths in scope for this evaluation
     * @return                the xpaths from the candidate set that satisfy the combined conditions
     */
    private Set<String> computeMatchedXpaths(final CompositeQuery compositeQuery,
                                             final Set<String> candidateXpaths) {
        final Collection<CompositeQuery> compositeQueryConditions = compositeQuery.getConditions();
        if (compositeQueryConditions.isEmpty()) {
            return new HashSet<>(candidateXpaths);
        }
        final CompositeQueryOperator operator = getNormalizedOperator(compositeQuery.getOperator());
        return compositeQueryEvaluator.evaluate(compositeQueryConditions, operator, candidateXpaths,
            this::evaluateCondition);
    }

    /**
     * Evaluates a single composite query condition against the candidate xpaths of its parent query.
     * The condition's cps path is queried across the whole anchor (results are cached per cps path) and
     * intersected with the parent scope so that only nodes present in the candidate trees are selected.
     * When the condition carries nested conditions, those are evaluated recursively against a scope narrowed
     * to the descendants of the nodes selected by this condition, because nesting means "within those selected
     * nodes, further filter by these conditions". The switch on the operator is exhaustive: a new operator
     * will not compile until its empty-nested-result behaviour is defined here.
     *
     * @param compositeQueryCondition    the condition to evaluate, possibly carrying nested conditions
     * @param parentQueryCandidateXpaths the xpaths in scope for this condition
     * @return the selected xpaths: the nodes matching this condition's cps path within the parent scope,
     *         plus any nested matches. When nested evaluation selects nothing, an AND operator invalidates
     *         the selection entirely (empty set) while an OR operator preserves it.
     */
    private Set<String> evaluateCondition(final CompositeQuery compositeQueryCondition,
                                          final Set<String> parentQueryCandidateXpaths) {
        final Set<String> xpathsMatchingCondition =
            getXpathsMatchingConditionAcrossAnchor(compositeQueryCondition.getCpsPath());
        final Set<String> selectedXpaths = new HashSet<>();
        for (final String xpath : xpathsMatchingCondition) {
            if (parentQueryCandidateXpaths.contains(xpath)) {
                selectedXpaths.add(xpath);
            }
        }
        if (compositeQueryCondition.getConditions().isEmpty()) {
            return selectedXpaths;
        }
        final Set<String> nestedScopeXpaths = new HashSet<>();
        for (final String selectedXpath : selectedXpaths) {
            nestedScopeXpaths.addAll(collectDescendantXpaths(selectedXpath));
        }
        final Set<String> nestedMatches = computeMatchedXpaths(compositeQueryCondition, nestedScopeXpaths);
        if (nestedMatches.isEmpty()) {
            return switch (getNormalizedOperator(compositeQueryCondition.getOperator())) {
                case AND -> Collections.emptySet();
                case OR -> selectedXpaths;
            };
        }
        selectedXpaths.addAll(nestedMatches);
        return selectedXpaths;
    }

    /**
     * Queries the xpaths of all data nodes matching the given cps path anywhere in the anchor, using
     * OMIT_DESCENDANTS because only the xpaths of matching nodes are needed, not their subtree content.
     * Results are cached for the duration of this execution so that the same cps path appearing in multiple
     * conditions is only queried once. The raw (unscoped) result is cached because the same cps path may be
     * evaluated against different scopes at different nesting levels.
     *
     * @param conditionCpsPath the cps path of one composite query condition
     * @return                 the xpaths of all data nodes in the anchor matching the cps path
     */
    private Set<String> getXpathsMatchingConditionAcrossAnchor(final String conditionCpsPath) {
        if (xpathsPerConditionCpsPath.containsKey(conditionCpsPath)) {
            return xpathsPerConditionCpsPath.get(conditionCpsPath);
        }
        final List<DataNode> dataNodesForOneCompositeQueryCondition = cpsDataPersistenceService.queryDataNodes(
            dataspaceName, anchorName, conditionCpsPath, OMIT_DESCENDANTS, 0);
        final Set<String> xpathsMatchingCondition = new HashSet<>(dataNodesForOneCompositeQueryCondition.size());
        for (final DataNode dataNode : dataNodesForOneCompositeQueryCondition) {
            xpathsMatchingCondition.add(dataNode.getXpath());
        }
        xpathsPerConditionCpsPath.put(conditionCpsPath, xpathsMatchingCondition);
        return xpathsMatchingCondition;
    }

    /**
     * Recursively walks every node in a candidate tree and registers it in the xpath-to-DataNode index for
     * O(1) lookup during condition evaluation.
     * The raw null check on childDataNodes is necessary because DataNode instances produced by Jackson
     * deserialization via the no-arg constructor bypass the @Builder.Default initializer, leaving the field
     * as null rather than an empty list.
     */
    private void populateXpathToDataNode(final DataNode dataNode) {
        xpathToDataNode.put(dataNode.getXpath(), dataNode);
        final Collection<DataNode> childDataNodes = dataNode.getChildDataNodes();
        if (childDataNodes != null) {
            for (final DataNode childDataNode : childDataNodes) {
                populateXpathToDataNode(childDataNode);
            }
        }
    }

    /**
     * Collects the xpaths of all descendants of a given node by traversing the in-memory index, avoiding
     * additional database queries. Returns an empty set when the xpath is not in the index or the node has
     * no children. Deduplication relies on Set.add() returning false when a node was already collected, which
     * prevents revisiting nodes in case of shared references.
     */
    private Set<String> collectDescendantXpaths(final String parentXpath) {
        final DataNode parentDataNode = xpathToDataNode.get(parentXpath);
        if (parentDataNode == null || CollectionUtils.isEmpty(parentDataNode.getChildDataNodes())) {
            return Collections.emptySet();
        }
        final Set<String> descendantXpaths = new HashSet<>();
        for (final DataNode childDataNode : parentDataNode.getChildDataNodes()) {
            final String childXpath = childDataNode.getXpath();
            if (descendantXpaths.add(childXpath)) {
                descendantXpaths.addAll(collectDescendantXpaths(childXpath));
            }
        }
        return descendantXpaths;
    }

    /** Builds filtered trees from the candidate roots, retaining only roots that contain at least one selected
     * node. */
    private Collection<DataNode> buildFilteredTrees(final Collection<DataNode> candidateTrees,
                                                    final Set<String> selectedXpaths,
                                                    final FetchDescendantsOption fetchDescendantsOption) {
        final List<DataNode> resultTrees = new ArrayList<>(candidateTrees.size());
        for (final DataNode rootNode : candidateTrees) {
            final DataNode filteredTree = buildFilteredTree(rootNode, selectedXpaths, fetchDescendantsOption);
            if (filteredTree != null) {
                resultTrees.add(filteredTree);
            }
        }
        return resultTrees;
    }

    /**
     * Recursive core of the result tree building.
     * If the current node is in selectedXpaths, its full subtree is rebuilt up to the depth limit via
     * buildCompleteTree with no further match-filtering applied.
     * If the current node is not selected but descent is still possible, its children are recursively
     * evaluated via buildFilteredSubTrees and the node is returned as a structural path node holding only
     * those children that had at least one selected node beneath them.
     * If the node is neither selected nor able to descend, it is discarded and null is returned to signal
     * that nothing survived this subtree.
     */
    private DataNode buildFilteredTree(final DataNode dataNode,
                                       final Set<String> selectedXpaths,
                                       final FetchDescendantsOption fetchDescendantsOption) {
        if (selectedXpaths.contains(dataNode.getXpath())) {
            return buildCompleteTree(dataNode, fetchDescendantsOption);
        }
        if (isLeafOrDepthReached(dataNode, fetchDescendantsOption)) {
            return null;
        }
        final List<DataNode> filteredSubTrees = buildFilteredSubTrees(dataNode, selectedXpaths, fetchDescendantsOption);
        if (filteredSubTrees.isEmpty()) {
            return null;
        }
        return copyDataNodeWithChildren(dataNode, filteredSubTrees);
    }

    /** Drives recursive descent into child nodes for unselected path nodes by passing each child to
     * buildFilteredTree, then filters out null returns so only children with at least one selected node
     * beneath them are included. */
    private List<DataNode> buildFilteredSubTrees(final DataNode dataNode,
                                                 final Set<String> selectedXpaths,
                                                 final FetchDescendantsOption fetchDescendantsOption) {
        final List<DataNode> resultTrees = new ArrayList<>();
        for (final DataNode childDataNode : dataNode.getChildDataNodes()) {
            final DataNode filteredSubTree
                = buildFilteredTree(childDataNode, selectedXpaths, fetchDescendantsOption.next());
            if (filteredSubTree != null) {
                resultTrees.add(filteredSubTree);
            }
        }
        return resultTrees;
    }

    /** Reconstructs the full subtree of a selected node carrying every descendant up to the depth limit without
     * applying any match-filtering, because a selected node implies all its descendants should be visible. When
     * the depth limit or a leaf is reached, the node is returned with an empty children list. */
    private DataNode buildCompleteTree(final DataNode dataNode,
                                       final FetchDescendantsOption fetchDescendantsOption) {
        if (isLeafOrDepthReached(dataNode, fetchDescendantsOption)) {
            return copyDataNodeWithChildren(dataNode, Collections.emptyList());
        }
        final List<DataNode> subTrees = new ArrayList<>(dataNode.getChildDataNodes().size());
        for (final DataNode childDataNode : dataNode.getChildDataNodes()) {
            subTrees.add(buildCompleteTree(childDataNode, fetchDescendantsOption.next()));
        }
        return copyDataNodeWithChildren(dataNode, subTrees);
    }

    /** Returns true when recursion must stop — either because the depth limit has been exhausted or because
     * the node has no children to descend into. */
    private boolean isLeafOrDepthReached(final DataNode dataNode, final FetchDescendantsOption fetchDescendantsOption) {
        return !fetchDescendantsOption.hasNext() || CollectionUtils.isEmpty(dataNode.getChildDataNodes());
    }

    /** Creates a copy of the source node with the specified children, preserving xpath, leaves, dataspace and
     * anchorName. A copy is necessary because the original nodes are shared references used during tree
     * traversal. */
    private DataNode copyDataNodeWithChildren(final DataNode source, final List<DataNode> childDataNodes) {
        final DataNode copy = new DataNode();
        copy.setXpath(source.getXpath());
        copy.setLeaves(source.getLeaves());
        copy.setDataspace(source.getDataspace());
        copy.setAnchorName(source.getAnchorName());
        copy.setChildDataNodes(childDataNodes);
        return copy;
    }
}
