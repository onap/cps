/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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

package org.onap.cps.api.impl;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Class to store a Yang Fragment (container or list element).
 */
public class Fragment {

    @Getter
    private final String xpath;

    @Getter
    private final Map<String, Object> attributes = new HashMap<>();

    @Getter
    private final Module module;

    @Getter
    private final Fragment parentFragment;

    @Getter
    private final Set<Fragment> childFragments = new HashSet<>(0);

    private final QName[] qnames;

    private Optional<Set<String>> optionalLeafListNames = Optional.empty();

    /**
     * Create a root Fragment.
     *
     * @param module the Yang module that encompasses this fragment
     * @param qnames the list of qualified names that points the schema node for this fragment
     * @param xpath  the xpath of root fragment
     */
    public static Fragment createRootFragment(final Module module, final QName[] qnames, final String xpath) {
        return new Fragment(null, module, qnames, xpath);
    }

    /**
     * Create a Child Fragment under a given Parent Fragment.
     *
     * @param parentFragment the parent (can be null for 'root' objects)
     * @param module         the Yang module that encompasses this fragment
     * @param qnames         array of qualified names that points the schema node for this fragment
     * @param xpath          the xpath for this fragment
     */
    private Fragment(final Fragment parentFragment, final Module module, final QName[] qnames, String xpath) {
        this.parentFragment = parentFragment;
        this.module = module;
        this.qnames = qnames;
        this.xpath = xpath;
    }

    /**
     * Create a Child Fragment where the current Fragment is the parent.
     *
     * @param childQname The Qualified name for the child (relative to the parent)
     * @param childXPath The child xpath (relative to the parrent)
     * @return the child fragment
     */
    public Fragment createChildFragment(final QName childQname, final String childXPath) {
        final QName[] qnamesForChild = Arrays.copyOf(qnames, qnames.length + 1);
        qnamesForChild[qnamesForChild.length - 1] = childQname;
        final Fragment childFragment = new Fragment(this, module, qnamesForChild, getXpath() + childXPath);
        childFragments.add(childFragment);
        return childFragment;
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
            attributes.put(name, value);
        }
    }

    private void addLeafListValue(final String name, final Object value) {
        if (attributes.containsKey(name)) {
            final ImmutableList<Object> oldList = (ImmutableList<Object>) attributes.get(name);
            final List<Object> newList = new ArrayList<>(oldList);
            newList.add(value);
            attributes.put(name, ImmutableList.copyOf(newList));
        } else {
            attributes.put(name, ImmutableList.of(value));
        }
    }

    /**
     * Get the SchemaNodeIdentifier for this fragment.
     *
     * @return the SchemaNodeIdentifier
     */
    public String getSchemaNodeIdentifier() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final QName qname : qnames) {
            stringBuilder.append(qname.getLocalName());
            stringBuilder.append('/');
        }
        return stringBuilder.toString();
    }

    /**
     * Get the Optional SchemaNode (model) for this data fragment.
     *
     * @return the Optional SchemaNode
     */
    public Optional<DataSchemaNode> getSchemaNode() {
        return module.findDataTreeChild(qnames);
    }
}
