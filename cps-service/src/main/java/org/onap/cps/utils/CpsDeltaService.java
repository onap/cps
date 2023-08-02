/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CpsDeltaService {
    private static final String ADD_ACTION = "add";
    private static final String REMOVE_ACTION = "remove";
    private static final String UPDATE_ACTION = "update";

    /**
     * Calculates delta/difference between DataNodes and returns the result as a Collection of Maps. Each map contains
     * information such as action, xpath, source-payload and target-payload. It performs the same operation recursively
     * for every child data node
     *
     * @param referenceDataNodes data nodes used as reference/source for generating delta
     * @param comparandDataNodes data nodes used as comparand value, and are compared against reference data nodes
     * @return Collection of Maps as the delta report between each data node
     */
    public static List<Map<String, Object>> getDeltaBetweenDataNodes(final Collection<DataNode> referenceDataNodes,
                                                                     final Collection<DataNode> comparandDataNodes) {

        final List<Map<String, Object>> deltaReport = new ArrayList<>();

        //map of xpaths to data node for reference nodes and comparand nodes
        final Map<String, DataNode> xpathToReferenceDataNodes = getXpathToDataNode(referenceDataNodes);
        final Map<String, DataNode> xpathToComparandDataNodes = getXpathToDataNode(comparandDataNodes);

        deltaReport.addAll(getRemovedAndUpdatedNodes(referenceDataNodes, xpathToComparandDataNodes));

        deltaReport.addAll(getAddedNodes(xpathToReferenceDataNodes, xpathToComparandDataNodes));

        return Collections.unmodifiableList(deltaReport);
    }

    /**
     * Method to find the data nodes that are deleted or data nodes which have been updated.
     * Using one method to find the deleted and updated nodes makes the approach simpler as we can first find the data
     * nodes that are deleted and then remaining data nodes are compared to find the leaves that might have been
     * updated. By using one method we can fulfill 2 scenarios using one iterative loop rather than having 2 individual
     * loops for each scenario.
     * In order to do so we iterate over the collection of reference data nodes and find the corresponding comparand
     * data node that may/may not be present in the Map of Comparand data node which uses the xpaths as key in key-value
     * pair
     *
     * @param referenceDataNodes            data nodes used as point of reference for comparision
     * @param xpathToComparandDataNodes     data nodes being compared to reference data nodes
     * @return                              deleted or updated data nodes
     */
    private static List<Map<String, Object>>  getRemovedAndUpdatedNodes(final Collection<DataNode> referenceDataNodes,
                                                                final Map<String, DataNode> xpathToComparandDataNodes) {

        final List<Map<String, Object>> removedAndUpdatedNodes = new ArrayList<>();
        for (final DataNode referenceDataNode : referenceDataNodes) {
            final String xpath = referenceDataNode.getXpath();
            final DataNode comparandDataNode = xpathToComparandDataNodes.get(xpath);
            /*
             *  if comparand node corresponding to xpath of an existing reference node is null would mean the data node
             *  was deleted
             */
            if (comparandDataNode == null) {
                removedAndUpdatedNodes.addAll(getDeletedNodes(xpath, referenceDataNode));
            }
            /*
             * else if a comparand data node exists corresponding to the xpath of existing reference data node then
             * that would require us to check if the leaves and child data nodes exist were modified or not, hence the
             * update scenario
             */
            else {
                removedAndUpdatedNodes.addAll(getUpdatedNodes(xpath, referenceDataNode, comparandDataNode));
            }
        }
        return removedAndUpdatedNodes;
    }

    /**
     * Method to find deleted nodes. Used as a helper method for getRemovedAndUpdatedNodes(). Once we have the reference
     * data node that was deleted, we need to check for 2^2 = 4 conditions for each reference data node.
     *
     * @param xpath                 parent node xpath
     * @param referenceDataNode     Reference data node, no comparand data node needed as in delete scenario
     *                              comparand will be null
     * @return                      deleted node data in delta report format
     */
    private static List<Map<String, Object>> getDeletedNodes(final String xpath, final DataNode referenceDataNode) {

        final List<Map<String, Object>> removedNodes = new ArrayList<>();

        final Map<String, Serializable> referenceDataNodeLeaves = referenceDataNode.getLeaves();
        final Collection<DataNode> referenceDataNodesChildren = referenceDataNode.getChildDataNodes();
        /*
         * the 4 scenarios to be checked when reference node is deleted. Reference node can have
         * 1. leaves and child nodes,
         * 2. only leaves,
         * 3. only child nodes,
         * 4. empty data node
         */
        if (!referenceDataNodeLeaves.isEmpty() && !referenceDataNodesChildren.isEmpty()) {
            removedNodes.add(getDeltaReportEntities(REMOVE_ACTION, xpath, referenceDataNodeLeaves,
                    Collections.emptyMap()));
            removedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren,
                    Collections.emptyList()));
        } else if (!referenceDataNodeLeaves.isEmpty()) {
            removedNodes.add(getDeltaReportEntities(REMOVE_ACTION, xpath,
                    referenceDataNodeLeaves, Collections.emptyMap()));
        } else if (!referenceDataNode.getChildDataNodes().isEmpty()) {
            removedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren,
                    Collections.emptyList()));
        } else {
            removedNodes.add(getDeltaReportEntities(REMOVE_ACTION, xpath, Collections.emptyMap(),
                    Collections.emptyMap()));
        }
        return removedNodes;
    }

    /**
     * Method to find the updated data nodes. This is a helper method for getRemovedAndUpdatedNodes(). This method
     * compares reference data nodes with comparand data nodes. Each data node can have leaves and child data nodes.
     * So for each of reference data node and comparand data node we need to compare leaves and child data nodes, which
     * gives us a total of 2^4 = 16 scenarios to be checked for. Each of these scenarios are covered in their respective
     * helper methods.
     *
     * @param xpath                 parent node xpath
     * @param referenceDataNode     reference data node
     * @param comparandDataNode     comparand data node
     * @return                      updated node data in delta report format
     */
    private static List<Map<String, Object>> getUpdatedNodes(final String xpath, final DataNode referenceDataNode,
                                                      final DataNode comparandDataNode) {

        final List<Map<String, Object>> updatedNodes = new ArrayList<>();

        final Map<String, Serializable> referenceDataNodeLeaves = referenceDataNode.getLeaves();
        final Collection<DataNode> referenceDataNodesChildren = referenceDataNode.getChildDataNodes();

        /*
         * Here we start the comparison of reference data nodes to corresponding comparand data node. To do so we find
         * data nodes that share the same xpath. Starting with the 4 scenarios of reference data node.
         * THe scenarios being
         * 1. reference node has leaves and child data nodes
         * 2. reference node has only leaves
         * 3. reference node has only child nodes
         * 4. reference node is empty
         */
        if (!referenceDataNodeLeaves.isEmpty() && !referenceDataNodesChildren.isEmpty()) {
            // covers scenarios for comparand data node when reference node has leaves and child data nodes
            getUpdatedNodesWhenReferenceDataNodeHasLeavesAndChildDataNodes(xpath, referenceDataNodeLeaves,
                    referenceDataNodesChildren, comparandDataNode, updatedNodes);
        }
        // covers scenarios for comparand data node when reference node has only leaves
        else if (!referenceDataNodeLeaves.isEmpty()) {
            getUpdatedNodesWhenReferenceDataNodeHasLeaves(xpath, referenceDataNodeLeaves,
                    comparandDataNode, updatedNodes);
        }
        // covers scenarios for comparand data node when reference node has only child nodes
        else if (!referenceDataNodesChildren.isEmpty()) {
            getUpdatedNodesWhenReferenceDataNodeHasChildDataNodes(xpath, referenceDataNodesChildren, comparandDataNode,
                    updatedNodes);
        }
        // covers scenarios for comparand data node when reference node is empty
        else {
            getUpdatedNodesWhenReferenceDataNodeIsEmpty(xpath, comparandDataNode, updatedNodes);
        }
        return updatedNodes;
    }

    /**
     * This method finds the added data nodes separately, once the deleted and updated nodes are identified. The logic
     * is that once the deleted and updated nodes are identified, the reference nodes are removed from map of comparand
     * nodes and to do so we use the maps created previously
     *
     * @param xpathToReferenceDataNodes     Map of xpath to reference nodes
     * @param xpathToComparandDataNodes     Map of xpaths to comparand nodes
     * @return                              Added nodes in delta report format
     */
    private static List<Map<String, Object>> getAddedNodes(final Map<String, DataNode> xpathToReferenceDataNodes,
                                                    final Map<String, DataNode> xpathToComparandDataNodes) {

        final List<Map<String, Object>> addedNodes = new ArrayList<>();
        final Map<String, DataNode> xpathToAddedNodes = new LinkedHashMap<>(xpathToComparandDataNodes);
        //removing all the reference nodes from the comparand nodes to get added nodes using the xpaths as key
        xpathToAddedNodes.keySet().removeAll(xpathToReferenceDataNodes.keySet());
        //Iterating over the added nodes
        for (final Map.Entry<String, DataNode> entry: xpathToAddedNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode dataNode = entry.getValue();
            /*
             * 4 possible scenarios with the added nodes are covered here
             * 1. added nodes have leaves and child nodes
             * 2. added nodes have only leaves
             * 3. added nodes have child nodes only
             * 4. added node is empty
             */
            if (!dataNode.getLeaves().isEmpty() && !dataNode.getChildDataNodes().isEmpty()) {
                addedNodes.add(getDeltaReportEntities(ADD_ACTION, xpath, Collections.emptyMap(), dataNode.getLeaves()));
                addedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), dataNode.getChildDataNodes()));
            } else if (!dataNode.getLeaves().isEmpty()) {
                addedNodes.add(getDeltaReportEntities(ADD_ACTION, xpath, Collections.emptyMap(), dataNode.getLeaves()));
            } else if (!dataNode.getChildDataNodes().isEmpty()) {
                addedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), dataNode.getChildDataNodes()));
            } else {
                addedNodes.add(
                        getDeltaReportEntities(ADD_ACTION, xpath, Collections.emptyMap(), Collections.emptyMap()));
            }
        }
        return addedNodes;
    }

    /**
     * This is a helper method for getUpdatedNodes() and is used to cover the scenarios for comparand node when
     * reference node has leaves and child data nodes.
     *
     * @param xpath                         xpath
     * @param referenceDataNodeLeaves       leaves of reference data node
     * @param referenceDataNodesChildren    child data nodes of reference node
     * @param comparandDataNode             comparand data node
     * @param updatedNodes                  updated nodes
     */
    private static void getUpdatedNodesWhenReferenceDataNodeHasLeavesAndChildDataNodes(final String xpath,
                                                                final Map<String, Serializable> referenceDataNodeLeaves,
                                                                final Collection<DataNode> referenceDataNodesChildren,
                                                                final DataNode comparandDataNode,
                                                                final List<Map<String, Object>> updatedNodes) {

        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();
        final Collection<DataNode> comparandDataNodesChildren = comparandDataNode.getChildDataNodes();

        /*
         * Here we cover the 4 scenarios of comparand data node
         * 1. reference node has leaves and child data nodes, comparand has leaves and child nodes
         * 2. reference node has leaves and child data nodes, comparand has only leaves
         * 3. reference node has leaves and child data nodes, comparand has only child nodes
         * 4. reference node has leaves and child data nodes, comparand is empty
         */
        //comparand has leaves and child nodes
        if (!comparandDataNodeLeaves.isEmpty() && !comparandDataNodesChildren.isEmpty()) {
            //calling a method to get the updated leaves, which contain both source and target leaves
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            //adding source and target leaves to delta report and getting them in the desired format
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
            //recursive call to getDeltaBetweenDataNodes() to find delta between child data nodes of
            // reference and comparand node
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, comparandDataNodesChildren));
        }
        //comparand has only leaves
        else if (!comparandDataNodeLeaves.isEmpty()) {
            //calling a method to get the updated leaves, which contain both source and target leaves
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            //adding source and target leaves to delta report and getting them in the desired format
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
            //recursive call to getDeltaBetweenDataNodes() to find delta between child data nodes of
            // reference and empty comparand node
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, Collections.emptyList()));
        }
        //comparand has only child nodes
        else if (!comparandDataNodesChildren.isEmpty()) {
            //adding leaves of reference node to delta report as existing data was updated to null value
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
            //recursive call to getDeltaBetweenDataNodes() to find delta between child data nodes of
            // reference and comparand node
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, comparandDataNodesChildren));
        }
        //comparand is empty
        else {
            //adding leaves of reference node to delta report as existing data was updated to null value
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
            //recursive call to getDeltaBetweenDataNodes() to find delta between child data nodes of
            // reference and empty comparand node
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, Collections.emptyList()));
        }
    }

    /**
     * This is a helper method for getUpdatedNodes() and is used to cover the scenarios for comparand node when
     * reference node has only leaves
     *
     * @param xpath                     xpath
     * @param referenceDataNodeLeaves   leaves of reference nodes
     * @param comparandDataNode         comparand adata node
     * @param updatedNodes              updated nodes
     */
    private static void getUpdatedNodesWhenReferenceDataNodeHasLeaves(final String xpath,
                                                            final Map<String, Serializable> referenceDataNodeLeaves,
                                                            final DataNode comparandDataNode,
                                                            final List<Map<String, Object>> updatedNodes) {

        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();
        final Collection<DataNode> comparandDataNodesChildren = comparandDataNode.getChildDataNodes();

        /*
         * Here we cover the 4 scenarios of comparand data node
         * 1. reference node has only leaves, comparand has leaves and child nodes
         * 2. reference node has only leaves, comparand has only leaves
         * 3. reference node has only leaves, comparand has only child nodes
         * 4. reference node has only leaves, comparand is empty
         *
         * Not adding setailed description of each scenario as it becomes self explanatory from previous method.
         * Also I have kept the different conditions separate as each of them handles the code after the if-else check
         * bit differently. We can technically combine the if-else statements but that compromises the code readability
         * and makes the code less maintainable. Currently the code is kept simple for easier readability and less
         * complexity
         */
        if (!comparandDataNodeLeaves.isEmpty() && !comparandDataNodesChildren.isEmpty()) {
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
            updatedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), comparandDataNodesChildren));
        } else if (!comparandDataNodeLeaves.isEmpty()) {
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
        } else if (!comparandDataNodesChildren.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
            updatedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), comparandDataNodesChildren));
        } else {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
        }
    }

    /**
     * This is a helper method for getUpdatedNodes() and is used to cover the scenarios for comparand node when
     * reference node has only child nodes
     *
     * @param xpath                         xpath
     * @param referenceDataNodesChildren    child data nodes under a reference node
     * @param comparandDataNode             comparand data node
     * @param updatedNodes                  updated nodes
     */
    private static void getUpdatedNodesWhenReferenceDataNodeHasChildDataNodes(final String xpath,
                                                                final Collection<DataNode> referenceDataNodesChildren,
                                                                final DataNode comparandDataNode,
                                                                final List<Map<String, Object>> updatedNodes) {

        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();
        final Collection<DataNode> comparandDataNodesChildren = comparandDataNode.getChildDataNodes();

        /*
         * Here we cover the 4 scenarios of comparand data node
         * 1. reference node has only child nodes, comparand has leaves and child nodes
         * 2. reference node has only child nodes, comparand has only leaves
         * 3. reference node has only child nodes, comparand has only child nodes
         * 4. reference node has only child nodes, comparand is empty
         */
        if (!comparandDataNodeLeaves.isEmpty() && !comparandDataNodesChildren.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, comparandDataNodesChildren));
        } else if (!comparandDataNode.getLeaves().isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, Collections.emptyList()));
        } else if (!comparandDataNodesChildren.isEmpty()) {
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, comparandDataNodesChildren));
        } else {
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, Collections.emptyList()));
        }
    }

    /**
     * This is a helper method for getUpdatedNodes() and is used to cover the scenarios for comparand node when
     * reference node is empty
     *
     * @param xpath                 xpath
     * @param comparandDataNode     comparand data node
     * @param updatedNodes          updated nodes
     */
    private static void getUpdatedNodesWhenReferenceDataNodeIsEmpty(final String xpath,
                                                                    final DataNode comparandDataNode,
                                                                    final List<Map<String, Object>> updatedNodes) {

        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();
        final Collection<DataNode> comparandDataNodesChildren = comparandDataNode.getChildDataNodes();

        /*
         * Here we cover the 3 scenarios of comparand data node
         * 1. reference node is empty, comparand has leaves and child nodes
         * 2. reference node is empty, comparand has only leaves
         * 3. reference node is empty, comparand has only child nodes
         * We dont need to cover the scenario where both data nodes are empty as both will be same and have no data
         * 4. reference node is empty, comparand is empty
         */
        if (!comparandDataNodeLeaves.isEmpty() && !comparandDataNodesChildren.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
            updatedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), comparandDataNodesChildren));
        } else if (!comparandDataNodeLeaves.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
        } else if (!comparandDataNodesChildren.isEmpty()) {
            updatedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), comparandDataNodesChildren));
        }
    }

    /**
     * This method compares leaves when they are present both in reference and comparand nodes, and if leaves are
     * present in both it compares their values and if there is a change in value it addes them to Maps of source and
     * target leaves which is then used to add the source and target data in deltas report.
     * IF either of reference and comparand leaves are empty then, either source data or target data is added to the
     * corresponding maps respectively. ALl if them are then added to a Linked hash map before being returned to
     * calling method
     *
     * @param leavesOfReferenceDataNode     reference node leaves
     * @param leavesOfComparandDataNode     comaprand node leaves
     * @return                              Map of source to updated leaves
     */
    private static Map<Map<String, Serializable>, Map<String, Serializable>> getSourceLeavesAndUpdatedLeaves(
            final Map<String, Serializable> leavesOfReferenceDataNode,
            final Map<String, Serializable> leavesOfComparandDataNode) {

        //map containing final source and target leaves
        final Map<Map<String, Serializable>, Map<String, Serializable>> sourceToUpdatedLeaves = new LinkedHashMap<>();
        //unique leaves of comparand node, in case a leaf was updated from null value to a quantative value
        final Map<String, Serializable> uniqueLeavesOfComparandDataNode =
                new LinkedHashMap<>(leavesOfComparandDataNode);
        uniqueLeavesOfComparandDataNode.keySet().removeAll(leavesOfReferenceDataNode.keySet());
        //source leaves
        final Map<String, Serializable> sourceLeaves = new LinkedHashMap<>();
        //target or updated leaves
        final Map<String, Serializable> updatedLeaves = new LinkedHashMap<>();

        //first loop iterates over reference node leaves to find updated values
        for (final Map.Entry<String, Serializable> entry: leavesOfReferenceDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable referenceLeaf = entry.getValue();
            final Serializable comparandLeaf = leavesOfComparandDataNode.get(key);
            addReferenceAndComparandLeafToSourceAndUpdatedLeavesMap(sourceLeaves, updatedLeaves, key,
                    referenceLeaf, comparandLeaf);

        }

        //second loop iterates over comparand node leaves to find updated leaves which are unique to comparand node
        for (final Map.Entry<String, Serializable> entry: uniqueLeavesOfComparandDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable comparandLeaf = entry.getValue();
            final Serializable referenceLeaf = leavesOfReferenceDataNode.get(key);
            addReferenceAndComparandLeafToSourceAndUpdatedLeavesMap(sourceLeaves, updatedLeaves, key, referenceLeaf,
                    comparandLeaf);
        }
        if (!sourceLeaves.isEmpty() || !updatedLeaves.isEmpty()) {
            sourceToUpdatedLeaves.put(sourceLeaves, updatedLeaves);
        }
        return sourceToUpdatedLeaves;
    }

    /**
     * This is a helper method for getSourceLeavesAndUpdatedLeaves()to add source and target leaves to respective maps
     *
     * @param sourceLeaves      map to hold source leaves to be added to delta report
     * @param updatedLeaves     map to hold updated leaves to be added to delta report
     * @param key               key of particular leaf
     * @param referenceLeaf     leaf of reference node to be added as source data
     * @param comparandLeaf     leaf of comparand node to be added as target data
     */
    private static void addReferenceAndComparandLeafToSourceAndUpdatedLeavesMap(
            final Map<String, Serializable> sourceLeaves, final Map<String, Serializable> updatedLeaves,
            final String key, final Serializable referenceLeaf, final Serializable comparandLeaf) {
        //if both reference and comparand leaves are pressent, add them to respective source and updated leaves map
        //the maps are then added as source and target data to delta report
        if (referenceLeaf != null && comparandLeaf != null) {
            if (!Objects.equals(referenceLeaf, comparandLeaf)) {
                sourceLeaves.put(key, referenceLeaf);
                updatedLeaves.put(key, comparandLeaf);
            }
        }
        /*
         * if only reference leaves are present then add them to source leaves map. Since the value was updated from
         * some quantative value to null so the target value is not required in delta report hence no need of updated
         * leaves map
         */
        else if (referenceLeaf != null) {
            sourceLeaves.put(key, referenceLeaf);
        }
        /*
         * if only comparand leaves are present then add them to updated leaves map. Since the value was updated from
         * some null to a quantative value so the source value is not required in delta report hence no need of source
         * leaves map
         */
        else if (comparandLeaf != null) {
            updatedLeaves.put(key, comparandLeaf);
        }
    }

    /**
     * Helper method for getUpdatedNodesWhenReferenceDataNodeHasLeavesAndChildDataNodes() and
     * getUpdatedNodesWhenReferenceDataNodeHasLeaves() it is used to add leaves that have been updated to delta report
     *
     * @param xpath                         xpath
     * @param updatedNodes                  updated nodes list
     * @param sourceLeavesToUpdatedLeaves   source to updated leaves map
     */
    private static void addUpdatedLeavesToDeltaReport(final String xpath, final List<Map<String, Object>> updatedNodes,
                        final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves) {
        if (!sourceLeavesToUpdatedLeaves.isEmpty()) {
            for (final Map.Entry<Map<String, Serializable>, Map<String, Serializable>> entry:
                    sourceLeavesToUpdatedLeaves.entrySet()) {
                updatedNodes
                        .add(getDeltaReportEntities(UPDATE_ACTION, xpath, entry.getKey(), entry.getValue()));
            }
        }
    }

    /**
     * Method to generate the delta report.
     * @param action            can be add, remove or update
     * @param xpath             xpath of a data node which was added, deleted or updated
     * @param sourcePayload     source/original data
     * @param targetPayload     data after modification/update
     * @return                  delta report
     */
    private static Map<String, Object> getDeltaReportEntities(final String action, final String xpath,
                                                              final Map<String, Serializable> sourcePayload,
                                                              final Map<String, Serializable> targetPayload) {
        final Map<String, Object> actionEntity = new LinkedHashMap<>();
        actionEntity.put("action", action);
        actionEntity.put("xpath", xpath);
        if (Objects.equals(action, ADD_ACTION)) {
            actionEntity.put("target-payload", targetPayload);
        } else if (Objects.equals(action, REMOVE_ACTION)) {
            actionEntity.put("source-payload", sourcePayload);
        } else {
            if (!sourcePayload.values().stream().allMatch(Objects::isNull)) {
                actionEntity.put("source-payload", sourcePayload);
            }
            if (!targetPayload.values().stream().allMatch(Objects::isNull)) {
                actionEntity.put("target-payload", targetPayload);
            }
        }
        return actionEntity;
    }

    /***
     * Helper method to get map of xpath to data nodes from a collection of data nodes.
     * @param dataNodes     Collection of data nodes
     * @return              map of xpath to data mode
     */
    private static Map<String, DataNode> getXpathToDataNode(final Collection<DataNode> dataNodes) {
        final Map<String, DataNode> xpathToDataNode = new LinkedHashMap<>();
        for (final DataNode dataNode : dataNodes) {
            xpathToDataNode.put(dataNode.getXpath(), dataNode);
        }
        return xpathToDataNode;
    }
}
