/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
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
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.findByDataspaceAndName(dataspaceEntity, anchorName)
            .orElseThrow();
        final List<FragmentEntity> allFragmentsToStoreInDatabase = new ArrayList<>();
        final FragmentEntity fragmentEntity = convertToFragmentWithAllDescendants(dataspaceEntity, anchorEntity,
            dataNode);
        populateFragmentWithDescendantsFragments(fragmentEntity, allFragmentsToStoreInDatabase);
        fragmentRepository.saveAll(allFragmentsToStoreInDatabase);
    }

    /**
     * Convert DataNode object into Fragment and places the result in the fragments placeholder. Performs same action
     * for all DataNode children recursively.
     *
     * @param dataspaceEntity dataspace
     * @param anchorEntity    anchorEntity
     * @param dataNode        dataNode
     * @return a Fragment built from current DataNode
     */
    private static FragmentEntity convertToFragmentWithAllDescendants(final DataspaceEntity dataspaceEntity,
        final AnchorEntity anchorEntity,
        final DataNode dataNode) {

        final FragmentEntity parentFragment = FragmentEntity.builder()
            .dataspace(dataspaceEntity)
            .anchor(anchorEntity)
            .xpath(dataNode.getXpath())
            .attributes(GSON.toJson(dataNode.getLeaves()))
            .build();

        final Set<FragmentEntity> childFragments = new HashSet<>(dataNode.getChildDataNodes().size());
        for (final DataNode childDataNode : dataNode.getChildDataNodes()) {
            final FragmentEntity childFragment =
                convertToFragmentWithAllDescendants(parentFragment.getDataspace(), parentFragment.getAnchor(),
                    childDataNode);
            childFragments.add(childFragment);
        }
        parentFragment.setChildFragments(Collections.unmodifiableSet(childFragments));
        return parentFragment;
    }

    /**
     * Populate fragments with descendant fragments.
     *
     * @param parentFragment                parent Fragment
     * @param allFragmentsToStoreInDatabase a list of Fragments
     */
    public static void populateFragmentWithDescendantsFragments(final FragmentEntity parentFragment,
        final List<FragmentEntity> allFragmentsToStoreInDatabase) {
        allFragmentsToStoreInDatabase.add(parentFragment);
        for (final FragmentEntity childFragment : parentFragment.getChildFragments()) {
            populateFragmentWithDescendantsFragments(childFragment, allFragmentsToStoreInDatabase);
        }
    }
}
