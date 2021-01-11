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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Fragment;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CpsDataPersistenceServiceImpl implements CpsDataPersistenceService {

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private AnchorRepository anchorRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    private static Gson GSON = new GsonBuilder().create();

    @Override
    public void storeDataNode(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.findByDataspaceAndName(dataspace, anchorName).orElseThrow();
        final List<Fragment> allFragmentsToStoreInDatabase = new LinkedList<>();
        final Fragment fragment = convertToFragmentWithAllDescendants(dataspace, anchorEntity, dataNode);
        populateFragmentWithDescendantsFragments(fragment, allFragmentsToStoreInDatabase);
        fragmentRepository.saveAll(allFragmentsToStoreInDatabase);
    }

    /**
     * Convert DataNode object into Fragment and places the result in the fragments placeholder. Performs same action
     * for all DataNode children recursively.
     *
     * @param dataspace    dataspace
     * @param anchorEntity anchorEntity
     * @param dataNode     dataNode
     * @return a Fragment built from current DataNode
     */
    private static Fragment convertToFragmentWithAllDescendants(final Dataspace dataspace,
        final AnchorEntity anchorEntity,
        final DataNode dataNode) {

        final Fragment parentFragment = Fragment.builder()
            .dataspace(dataspace)
            .anchor(anchorEntity)
            .xpath(dataNode.getXpath())
            .attributes(GSON.toJson(dataNode.getLeaves()))
            .build();

        final Set<Fragment> childFragments = new HashSet<>(dataNode.getChildDataNodes().size());
        for (final DataNode childDataNode : dataNode.getChildDataNodes()) {
            final Fragment childFragment =
                convertToFragmentWithAllDescendants(parentFragment.getDataspace(), parentFragment.getAnchor(),
                    childDataNode);
            childFragment.setParentFragment(parentFragment);
            childFragments.add(childFragment);
        }
        parentFragment.setChildFragments(Collections.unmodifiableSet(childFragments));
        return parentFragment;
    }

    /**
     * Populate framents with descendant fragments.
     *
     * @param parentFragment parent Fragment
     * @param allFragmentsToStoreInDatabase a list of Fragments
     */
    public static void populateFragmentWithDescendantsFragments(final Fragment parentFragment,
        final List<Fragment> allFragmentsToStoreInDatabase) {
        allFragmentsToStoreInDatabase.add(parentFragment);
        for (final Fragment childFragment : parentFragment.getChildFragments()) {
            populateFragmentWithDescendantsFragments(childFragment, allFragmentsToStoreInDatabase);
        }
    }
}
