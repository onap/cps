/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2021 Bell Canada.
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

package org.onap.cps.spi.impl;

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleStateException;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.ConcurrencyException;
import org.onap.cps.spi.exceptions.CpsPathException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CpsDataPersistenceServiceImpl implements CpsDataPersistenceService {

    private DataspaceRepository dataspaceRepository;

    private AnchorRepository anchorRepository;

    private FragmentRepository fragmentRepository;

    /**
     * Constructor.
     *
     * @param dataspaceRepository dataspaceRepository
     * @param anchorRepository    anchorRepository
     * @param fragmentRepository  fragmentRepository
     */
    public CpsDataPersistenceServiceImpl(final DataspaceRepository dataspaceRepository,
        final AnchorRepository anchorRepository, final FragmentRepository fragmentRepository) {
        this.dataspaceRepository = dataspaceRepository;
        this.anchorRepository = anchorRepository;
        this.fragmentRepository = fragmentRepository;
    }

    private static final Gson GSON = new GsonBuilder().create();
    private static final String REG_EX_FOR_OPTIONAL_LIST_INDEX = "(\\[@\\S+?]){0,1})";
    private static final String REG_EX_FOR_LIST_NODE_KEY = "(\\@(.*?))+( and)*\\]";

    @Override
    public void addChildDataNode(final String dataspaceName, final String anchorName, final String parentXpath,
        final DataNode dataNode) {
        final FragmentEntity parentFragment = getFragmentByXpath(dataspaceName, anchorName, parentXpath);
        final var fragmentEntity =
            toFragmentEntity(parentFragment.getDataspace(), parentFragment.getAnchor(), dataNode);
        parentFragment.getChildFragments().add(fragmentEntity);
        try {
            fragmentRepository.save(parentFragment);
        } catch (final DataIntegrityViolationException exception) {
            throw AlreadyDefinedException.forDataNode(dataNode.getXpath(), anchorName, exception);
        }
    }

    @Override
    public void addListDataNodes(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final Collection<DataNode> dataNodes) {
        final FragmentEntity parentFragment = getFragmentByXpath(dataspaceName, anchorName, parentNodeXpath);
        final List<FragmentEntity> newFragmentEntities =
            dataNodes.stream().map(
                dataNode -> toFragmentEntity(parentFragment.getDataspace(), parentFragment.getAnchor(), dataNode)
            ).collect(Collectors.toUnmodifiableList());
        parentFragment.getChildFragments().addAll(newFragmentEntities);
        try {
            fragmentRepository.save(parentFragment);
        } catch (final DataIntegrityViolationException exception) {
            final List<String> conflictXpaths = dataNodes.stream()
                .map(DataNode::getXpath)
                .collect(Collectors.toList());
            throw AlreadyDefinedException.forDataNodes(conflictXpaths, anchorName, exception);
        }
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
            return fragmentRepository.findFirstRootByDataspaceAndAnchor(dataspaceEntity, anchorEntity);
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
        final CpsPathQuery cpsPathQuery;
        try {
            cpsPathQuery = CpsPathQuery.createFrom(cpsPath);
        } catch (final IllegalStateException e) {
            throw new CpsPathException(e.getMessage());
        }
        List<FragmentEntity> fragmentEntities =
            fragmentRepository.findByAnchorAndCpsPath(anchorEntity.getId(), cpsPathQuery);
        if (cpsPathQuery.hasAncestorAxis()) {
            final Set<String> ancestorXpaths = processAncestorXpath(fragmentEntities, cpsPathQuery);
            fragmentEntities = ancestorXpaths.isEmpty()
                ? Collections.emptyList() : fragmentRepository.findAllByAnchorAndXpathIn(anchorEntity, ancestorXpaths);
        }
        return fragmentEntities.stream()
            .map(fragmentEntity -> toDataNode(fragmentEntity, fetchDescendantsOption))
            .collect(Collectors.toUnmodifiableList());
    }

    private static Set<String> processAncestorXpath(final List<FragmentEntity> fragmentEntities,
        final CpsPathQuery cpsPathQuery) {
        final Set<String> ancestorXpath = new HashSet<>();
        final var pattern =
            Pattern.compile("(\\S*\\/" + Pattern.quote(cpsPathQuery.getAncestorSchemaNodeIdentifier())
                + REG_EX_FOR_OPTIONAL_LIST_INDEX + "\\/\\S*");
        for (final FragmentEntity fragmentEntity : fragmentEntities) {
            final var matcher = pattern.matcher(fragmentEntity.getXpath());
            if (matcher.matches()) {
                ancestorXpath.add(matcher.group(1));
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
    public void replaceDataNodeTree(final String dataspaceName, final String anchorName,
        final DataNode dataNode) {
        final var fragmentEntity = getFragmentByXpath(dataspaceName, anchorName, dataNode.getXpath());
        replaceDataNodeTree(fragmentEntity, dataNode);
        try {
            fragmentRepository.save(fragmentEntity);
        } catch (final StaleStateException staleStateException) {
            throw new ConcurrencyException("Concurrent Transactions",
                String.format("dataspace :'%s', Anchor : '%s' and xpath: '%s' is updated by another transaction.",
                    dataspaceName, anchorName, dataNode.getXpath()),
                staleStateException);
        }
    }

    private void replaceDataNodeTree(final FragmentEntity existingFragmentEntity, final DataNode submittedDataNode) {

        existingFragmentEntity.setAttributes(GSON.toJson(submittedDataNode.getLeaves()));

        final Map<String, FragmentEntity> existingChildrenByXpath = existingFragmentEntity.getChildFragments()
            .stream().collect(Collectors.toMap(FragmentEntity::getXpath, childFragmentEntity -> childFragmentEntity));

        final var updatedChildFragments = new HashSet<FragmentEntity>();

        for (final DataNode submittedChildDataNode : submittedDataNode.getChildDataNodes()) {
            final FragmentEntity childFragment;
            if (existingChildrenByXpath.containsKey(submittedChildDataNode.getXpath())) {
                childFragment = existingChildrenByXpath.get(submittedChildDataNode.getXpath());
                replaceDataNodeTree(childFragment, submittedChildDataNode);
            } else {
                childFragment = convertToFragmentWithAllDescendants(
                    existingFragmentEntity.getDataspace(), existingFragmentEntity.getAnchor(), submittedChildDataNode);
            }
            updatedChildFragments.add(childFragment);
        }
        existingFragmentEntity.setChildFragments(updatedChildFragments);
    }

    @Override
    @Transactional
    public void replaceListDataNodes(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final Collection<DataNode> dataNodes) {
        final var parentEntity = getFragmentByXpath(dataspaceName, anchorName, parentNodeXpath);
        var firstChildNodeXpath = dataNodes.iterator().next().getXpath();
        firstChildNodeXpath =  firstChildNodeXpath.replaceAll(REG_EX_FOR_LIST_NODE_KEY, "");
        removeListNodeDescendants(parentEntity, firstChildNodeXpath);
        final Set<FragmentEntity> childFragmentEntities = dataNodes.stream().map(
            dataNode -> convertToFragmentWithAllDescendants(
                parentEntity.getDataspace(), parentEntity.getAnchor(), dataNode)
        ).collect(Collectors.toUnmodifiableSet());
        parentEntity.getChildFragments().addAll(childFragmentEntities);
        fragmentRepository.save(parentEntity);
    }

    @Override
    @Transactional
    public void deleteListDataNodes(final String dataspaceName, final String anchorName, final String listNodeXpath) {
        final var parentNodeXpath = listNodeXpath.substring(0, listNodeXpath.lastIndexOf('/'));
        final var parentEntity = getFragmentByXpath(dataspaceName, anchorName, parentNodeXpath);
        final String descendantNode = listNodeXpath.substring(listNodeXpath.lastIndexOf('/'), listNodeXpath.length());
        final Matcher descendantNodeHasListNodeKey = Pattern.compile(REG_EX_FOR_LIST_NODE_KEY).matcher(descendantNode);
        final Predicate<FragmentEntity> xpathIsValidForAChildFragment =
            (fragment) -> fragment.getXpath().equals(listNodeXpath);
        if (!descendantNodeHasListNodeKey.find()) {
            throw new DataValidationException("Invalid xpath.", "Xpath does not direct to a List-Node.");
        } else if (parentEntity.getChildFragments().stream().anyMatch(xpathIsValidForAChildFragment)) {
            removeListNodeDescendants(parentEntity, listNodeXpath);
        } else {
            throw new DataNodeNotFoundException(parentEntity.getDataspace().getName(),
                parentEntity.getAnchor().getName(), listNodeXpath);
        }
    }

    private void removeListNodeDescendants(final FragmentEntity parentFragmentEntity, final String listNodeXpath) {
        if (parentFragmentEntity.getChildFragments()
            .removeIf(fragment -> fragment.getXpath().startsWith(listNodeXpath))) {
            fragmentRepository.save(parentFragmentEntity);
        }
    }

    private static boolean isRootXpath(final String xpath) {
        return "/".equals(xpath) || "".equals(xpath);
    }
}
