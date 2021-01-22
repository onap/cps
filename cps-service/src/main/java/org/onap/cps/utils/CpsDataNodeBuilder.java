/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.onap.cps.spi.model.DataNode;

@Getter
public class CpsDataNodeBuilder {

    private Optional<Set<String>> optionalLeafListNames = Optional.empty();
    private Map<String, Object> leaves = new HashMap<>();

    /**
     * Create a parent Data Node.
     *
     * @param xpath  the xpath of root fragment
     */
    public static DataNode createDataNode(final String xpath) {
        return new DataNode(xpath);
    }

    /**
     * Create a Child dataNode where the parentDataNode is the parent.
     *
     * @param parentDataNode the parent data Node
     * @param childXPath The child xpath (relative to the parent)
     * @return the child data node
     */
    public static  DataNode createChildNode(final DataNode parentDataNode, final String childXPath) {
        final DataNode childDataNode = new DataNode(parentDataNode.getXpath() + childXPath);
        addChildDataNode(parentDataNode, childDataNode);
        return childDataNode;
    }

    /**
     * Define the ChildDataNodes by adding then to the collection of child nodes for the parent.
     *
     * @param currentDataNode the node where the child needs to be added
     * @param newChildDataNode the latest child data node
     */
    public static Collection<DataNode> addChildDataNode(final DataNode currentDataNode,
        final DataNode newChildDataNode) {
        if (currentDataNode.getChildDataNodes() == null) {
            currentDataNode.setChildDataNodes(ImmutableSet.of(newChildDataNode));
        } else {
            final Set<DataNode> allChildDataNodes = new ImmutableSet.Builder<DataNode>()
                .addAll(currentDataNode.getChildDataNodes())
                .add(newChildDataNode)
                .build();
            currentDataNode.setChildDataNodes(allChildDataNodes);
        }
        return currentDataNode.getChildDataNodes();
    }

    /**
     * Define a leaf list by providing its name.
     * The list is not instantiated until the first value is provided
     *
     * @param name the name of the leaf list
     */
    public void addLeafListName(final String name) {
        if (optionalLeafListNames.isEmpty()) {
            optionalLeafListNames = Optional.of(new HashSet<>());
        }
        optionalLeafListNames.get().add(name);
    }

    /**
     * Add a leaf or leaf list value.
     * For Leaf lists it is essential to first define the attribute is a leaf list by using addLeafListName method
     *
     * @param name  the name of the leaf (or leaf list)
     * @param value the value of the leaf (or element of leaf list)
     */
    public void addLeafValue(final String name, final Object value) {
        if (optionalLeafListNames.isPresent() && optionalLeafListNames.get().contains(name)) {
            addLeafListValue(name, value);
        } else {
            leaves.put(name, value);
        }
    }

    /**
     * Map each leaf with its corresponding values.
     * For Leaf lists it is essential to first define the attribute is a leaf list by using addLeafListName method
     * and defining their values using addLeafValue method
     *
     * @param name  the name of the leaf (or leaf list)
     * @param value the value of the leaf (or element of leaf list)
     */
    private void addLeafListValue(final String name, final Object value) {
        if (leaves.containsKey(name)) {
            final ImmutableList<Object> oldList = (ImmutableList<Object>) leaves.get(name);
            final List<Object> newList = new ArrayList<>(oldList);
            newList.add(value);
            leaves.put(name, ImmutableList.copyOf(newList));
        } else {
            leaves.put(name, ImmutableList.of(value));
        }
    }

}
