/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2021 Bell Canada.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsPathException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.spi.query.CpsPathQuery;
import org.onap.cps.spi.query.CpsPathQueryType;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
    private static final String REG_EX_FOR_OPTIONAL_LIST_INDEX = "(\\[@\\S+?]){0,1})";

    @Override
    public void addChildDataNode(final String dataspaceName, final String anchorName, final String parentXpath,
        final DataNode dataNode) {
        final FragmentEntity parentFragment = getFragmentByXpath(dataspaceName, anchorName, parentXpath);
        final var fragmentEntity =
            toFragmentEntity(parentFragment.getDataspace(), parentFragment.getAnchor(), dataNode);
        parentFragment.getChildFragments().add(fragmentEntity);
        fragmentRepository.save(parentFragment);
    }

    @Override
    public void storeDataNode(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final var fragmentEntity = convertToFragmentWithAllDescendants(dataspaceEntity, anchorEntity,
            dataNode);
        try {
            fragmentRepository.save(fragmentEntity);
        } catch (final DataIntegrityViolationException exception) {
            throw AlreadyDefinedException.forDataNode(dataNode.getXpath(), anchorName, exception);
        }
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
        final var parentFragment = toFragmentEntity(dataspaceEntity, anchorEntity, dataNodeToBeConverted);
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
        final var fragmentEntity = getFragmentByXpath(dataspaceName, anchorName, xpath);
        return toDataNode(fragmentEntity, fetchDescendantsOption);
    }

    private FragmentEntity getFragmentByXpath(final String dataspaceName, final String anchorName,
        final String xpath) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        if (isRootXpath(xpath)) {
            return fragmentRepository.getFirstByDataspaceAndAnchor(dataspaceEntity, anchorEntity);
        } else {
            return fragmentRepository.getByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity,
                xpath);
        }
    }

    @Override
    public List<DataNode> queryDataNodes(final String dataspaceName, final String anchorName, final String cpsPath,
        final FetchDescendantsOption fetchDescendantsOption) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final var cpsPathQuery = CpsPathQuery.createFrom(cpsPath);
        List<FragmentEntity> fragmentEntities;
        if (CpsPathQueryType.XPATH_LEAF_VALUE.equals(cpsPathQuery.getCpsPathQueryType())) {
            fragmentEntities = fragmentRepository
                .getByAnchorAndXpathAndLeafAttributes(anchorEntity.getId(), cpsPathQuery.getXpathPrefix(),
                    cpsPathQuery.getLeafName(), cpsPathQuery.getLeafValue());
        } else if (CpsPathQueryType.XPATH_HAS_DESCENDANT_WITH_LEAF_VALUES.equals(cpsPathQuery.getCpsPathQueryType())) {
            final String leafDataAsJson = GSON.toJson(cpsPathQuery.getLeavesData());
            fragmentEntities = fragmentRepository
                .getByAnchorAndDescendentNameAndLeafValues(anchorEntity.getId(), cpsPathQuery.getDescendantName(),
                    leafDataAsJson);
        } else {
            fragmentEntities = fragmentRepository
                .getByAnchorAndXpathEndsInDescendantName(anchorEntity.getId(), cpsPathQuery.getDescendantName());
        }
        if (cpsPathQuery.hasAncestorAxis()) {
            final Set<String> ancestorXpaths = processAncestorXpath(fragmentEntities, cpsPathQuery);
            fragmentEntities = fragmentRepository.findAllByAnchorAndXpathIn(anchorEntity, ancestorXpaths);
        }
        return fragmentEntities.stream()
            .map(fragmentEntity -> toDataNode(fragmentEntity, fetchDescendantsOption))
            .collect(Collectors.toUnmodifiableList());
    }

    private static Set<String> processAncestorXpath(final List<FragmentEntity> fragmentEntities,
        final CpsPathQuery cpsPathQuery) {
        final Set<String> ancestorXpath = new HashSet<>();
        final var pattern =
            Pattern.compile("(\\S*\\/" + cpsPathQuery.getAncestorSchemaNodeIdentifier() + REG_EX_FOR_OPTIONAL_LIST_INDEX
                + "\\/\\S*");
        for (final FragmentEntity fragmentEntity : fragmentEntities) {
            final var matcher = pattern.matcher(fragmentEntity.getXpath());
            if (matcher.matches()) {
                ancestorXpath.add(matcher.group(1));
            } else {
                throw new CpsPathException("Invalid cps path.",
                    String.format("Cannot interpret or parse attributes in cps path '%s'.", cpsPathQuery));
            }
        }
        return ancestorXpath;
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

    @Override
    public void updateDataLeaves(final String dataspaceName, final String anchorName, final String xpath,
        final Map<String, Object> leaves) {
        final var fragmentEntity = getFragmentByXpath(dataspaceName, anchorName, xpath);
        fragmentEntity.setAttributes(GSON.toJson(leaves));
        fragmentRepository.save(fragmentEntity);
    }

    @Override
    public void replaceDataNodeTree(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final var fragmentEntity = getFragmentByXpath(dataspaceName, anchorName, dataNode.getXpath());
        removeExistingDescendants(fragmentEntity);

        fragmentEntity.setAttributes(GSON.toJson(dataNode.getLeaves()));
        final Set<FragmentEntity> childFragmentEntities = dataNode.getChildDataNodes().stream().map(
            childDataNode -> convertToFragmentWithAllDescendants(
                fragmentEntity.getDataspace(), fragmentEntity.getAnchor(), childDataNode)
        ).collect(Collectors.toUnmodifiableSet());
        fragmentEntity.setChildFragments(childFragmentEntities);

        fragmentRepository.save(fragmentEntity);
    }

    private void removeExistingDescendants(final FragmentEntity fragmentEntity) {
        fragmentEntity.setChildFragments(Collections.emptySet());
        fragmentRepository.save(fragmentEntity);
    }

    private boolean isRootXpath(final String xpath) {
        return "/".equals(xpath) || "".equals(xpath);
    }
}
