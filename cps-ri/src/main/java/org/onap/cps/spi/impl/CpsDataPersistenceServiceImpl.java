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

package org.onap.cps.spi.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Fragment;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CpsDataPersistenceServiceImpl implements CpsDataPersistenceService {

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    private static Gson GSON = new GsonBuilder().create();

    @Override
    public void storeDataNode(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final List<Fragment> fragments = new LinkedList<>();
        populateFragments(fragments, dataspace, dataNode.getAnchorName(), dataNode);
        fragmentRepository.saveAll(fragments);
    }

    /**
     * Convert DataNode object into Fragment and places result in the fragments placeholder. Performs same action for
     * all DataNode children recursively.
     *
     * @param dataspace  dataspace
     * @param anchorName anchor name
     * @param dataNode   dataNode
     * @param fragments  a list of Fragments
     * @return a Fragment built from current DataNode
     */
    private static Fragment populateFragments(final List<Fragment> fragments, final Dataspace dataspace,
        final String anchorName,
        final DataNode dataNode) {

        final Fragment fragment = Fragment.builder()
            .dataspace(dataspace)
            .anchorName(anchorName)
            .xpath(dataNode.getXpath())
            .attributes(GSON.toJson(dataNode.getLeaves()))
            .build();
        fragments.add(fragment);

        populateChildFragments(fragments, fragment, dataNode.getChildDataNodes());

        return fragment;
    }

    /**
     * Populate child fragments.
     *
     * @param fragments a list of fragments
     * @param parentFragment the parent Fragment
     * @param childDataNodes the child DataNode
     */
    private static void populateChildFragments(final List<Fragment> fragments,
        final Fragment parentFragment, final Collection<DataNode> childDataNodes) {
        for (final DataNode childDataNode : childDataNodes) {
            final Fragment childFragment =
                populateFragments(fragments, parentFragment.getDataspace(), parentFragment.getAnchorName(),
                    childDataNode);
            childFragment.setParentFragment(parentFragment);
            fragments.add(childFragment);
        }
    }
}
