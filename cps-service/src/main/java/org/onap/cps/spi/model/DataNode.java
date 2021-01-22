/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation. All rights reserved.
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.model;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataNode {

    private String dataspace;
    private String schemaSetName;
    private String anchorName;
    private ModuleReference moduleReference;
    private String xpath;
    private Map<String, Object> leaves = new HashMap<>();
    private Collection<String> xpathsChildren;
    private Collection<DataNode> childDataNodes;
    private Optional<Set<String>> optionalLeafListNames = Optional.empty();

    public DataNode(final String xpath) {
        this.xpath = xpath;
    }

    /**
     * Define the ChildDataNodes including all children and grandchildren.
     *
     * @param newChildDataNode the latest child data node
     */
    public void addChildDataNode(final DataNode newChildDataNode) {
        if (childDataNodes == null) {
            childDataNodes = ImmutableSet.of(newChildDataNode);
        } else {
            final Set<DataNode> allChildDataNodes = new ImmutableSet.Builder<DataNode>()
                .addAll(childDataNodes)
                .add(newChildDataNode)
                .build();
            childDataNodes = allChildDataNodes;
        }
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
