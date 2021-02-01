/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CpsDataPersistenceServiceImpl implements CpsDataPersistenceService {

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private AnchorRepository anchorRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public void addChildDataNode(final String dataspaceName, final String anchorName, final String parentXpath,
        final DataNode dataNode) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final FragmentEntity parentFragment =
            fragmentRepository.getByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, parentXpath);
        final FragmentEntity childFragment = toFragmentEntity(dataspaceEntity, anchorEntity, dataNode);
        parentFragment.getChildFragments().add(childFragment);
        fragmentRepository.save(parentFragment);
    }

    @Override
    public void storeDataNode(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final FragmentEntity fragmentEntity = convertToFragmentWithAllDescendants(dataspaceEntity, anchorEntity,
            dataNode);
        fragmentRepository.save(fragmentEntity);
    }

    /**
     * Convert DataNode object into Fragment and places the result in the fragments placeholder. Performs same action
     * for all DataNode children recursively.
     *
     * @param dataspaceEntity       dataspace
     * @param anchorEntity          anchorEntity
     * @param dataNodeToBeConverted dataNode
     * @return a Fragment built from current DataNode
     */
    private static FragmentEntity convertToFragmentWithAllDescendants(final DataspaceEntity dataspaceEntity,
        final AnchorEntity anchorEntity, final DataNode dataNodeToBeConverted) {
        final FragmentEntity parentFragment = toFragmentEntity(dataspaceEntity, anchorEntity, dataNodeToBeConverted);
        final Builder<FragmentEntity> childFragmentsImmutableSetBuilder = ImmutableSet.builder();
        for (final DataNode childDataNode : dataNodeToBeConverted.getChildDataNodes()) {
            final FragmentEntity childFragment =
                convertToFragmentWithAllDescendants(parentFragment.getDataspace(), parentFragment.getAnchor(),
                    childDataNode);
            childFragmentsImmutableSetBuilder.add(childFragment);
        }
        parentFragment.setChildFragments(childFragmentsImmutableSetBuilder.build());
        return parentFragment;
    }

    private static FragmentEntity toFragmentEntity(final DataspaceEntity dataspaceEntity,
        final AnchorEntity anchorEntity, final DataNode dataNode) {
        return FragmentEntity.builder()
            .dataspace(dataspaceEntity)
            .anchor(anchorEntity)
            .xpath(dataNode.getXpath())
            .attributes(GSON.toJson(dataNode.getLeaves()))
            .build();
    }

    @Override
    public DataNode getDataNode(final String dataspaceName, final String anchorName, final String xpath,
        final FetchDescendantsOption fetchDescendantsOption) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final FragmentEntity fragmentEntity =
            fragmentRepository.getByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, xpath);
        return toDataNode(fragmentEntity, fetchDescendantsOption);
    }

    private static DataNode toDataNode(final FragmentEntity fragmentEntity,
        final FetchDescendantsOption fetchDescendantsOption) {
        final Map<String, Object> leaves = GSON.fromJson(fragmentEntity.getAttributes(), Map.class);
        final List<DataNode> childDataNodes = getChildDataNodes(fragmentEntity, fetchDescendantsOption);
        return new DataNodeBuilder()
            .withXpath(fragmentEntity.getXpath())
            .withLeaves(leaves)
            .withChildDataNodes(childDataNodes).build();
    }

    private static List<DataNode> getChildDataNodes(final FragmentEntity fragmentEntity,
        final FetchDescendantsOption fetchDescendantsOption) {
        if (fetchDescendantsOption == INCLUDE_ALL_DESCENDANTS) {
            return fragmentEntity.getChildFragments().stream()
                .map(childFragmentEntity -> toDataNode(childFragmentEntity, fetchDescendantsOption))
                .collect(Collectors.toUnmodifiableList());
        }
        return Collections.emptyList();
    }
}
