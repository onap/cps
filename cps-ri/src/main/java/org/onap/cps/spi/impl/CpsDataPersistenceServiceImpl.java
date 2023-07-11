/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2022-2023 TechMahindra Ltd.
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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import io.micrometer.core.annotation.Timed;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleStateException;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.ConcurrencyException;
import org.onap.cps.spi.exceptions.CpsAdminException;
import org.onap.cps.spi.exceptions.CpsPathException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundExceptionBatch;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.onap.cps.spi.utils.SessionManager;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsDataPersistenceServiceImpl implements CpsDataPersistenceService {

    private final DataspaceRepository dataspaceRepository;
    private final AnchorRepository anchorRepository;
    private final FragmentRepository fragmentRepository;
    private final JsonObjectMapper jsonObjectMapper;
    private final SessionManager sessionManager;

    private static final String REG_EX_FOR_OPTIONAL_LIST_INDEX = "(\\[@.+?])?)";
    private static final String QUERY_ACROSS_ANCHORS = null;
    private static final AnchorEntity ALL_ANCHORS = null;

    @Override
    public void addChildDataNodes(final String dataspaceName, final String anchorName,
                                  final String parentNodeXpath, final Collection<DataNode> dataNodes) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        addChildrenDataNodes(anchorEntity, parentNodeXpath, dataNodes);
    }

    @Override
    public void addListElements(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                final Collection<DataNode> newListElements) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        addChildrenDataNodes(anchorEntity, parentNodeXpath, newListElements);
    }

    @Override
    public void addMultipleLists(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                 final Collection<Collection<DataNode>> newLists) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        final Collection<String> failedXpaths = new HashSet<>();
        for (final Collection<DataNode> newList : newLists) {
            try {
                addChildrenDataNodes(anchorEntity, parentNodeXpath, newList);
            } catch (final AlreadyDefinedException alreadyDefinedException) {
                failedXpaths.addAll(alreadyDefinedException.getAlreadyDefinedObjectNames());
            }
        }
        if (!failedXpaths.isEmpty()) {
            throw AlreadyDefinedException.forDataNodes(failedXpaths, anchorEntity.getName());
        }
    }

    private void addNewChildDataNode(final AnchorEntity anchorEntity, final String parentNodeXpath,
                                     final DataNode newChild) {
        final FragmentEntity parentFragmentEntity = getFragmentEntity(anchorEntity, parentNodeXpath);
        final FragmentEntity newChildAsFragmentEntity = convertToFragmentWithAllDescendants(anchorEntity, newChild);
        newChildAsFragmentEntity.setParentId(parentFragmentEntity.getId());
        try {
            fragmentRepository.save(newChildAsFragmentEntity);
        } catch (final DataIntegrityViolationException e) {
            throw AlreadyDefinedException.forDataNodes(Collections.singletonList(newChild.getXpath()),
                    anchorEntity.getName());
        }
    }

    private void addChildrenDataNodes(final AnchorEntity anchorEntity, final String parentNodeXpath,
                                      final Collection<DataNode> newChildren) {
        final FragmentEntity parentFragmentEntity = getFragmentEntity(anchorEntity, parentNodeXpath);
        final List<FragmentEntity> fragmentEntities = new ArrayList<>(newChildren.size());
        try {
            for (final DataNode newChildAsDataNode : newChildren) {
                final FragmentEntity newChildAsFragmentEntity =
                    convertToFragmentWithAllDescendants(anchorEntity, newChildAsDataNode);
                newChildAsFragmentEntity.setParentId(parentFragmentEntity.getId());
                fragmentEntities.add(newChildAsFragmentEntity);
            }
            fragmentRepository.saveAll(fragmentEntities);
        } catch (final DataIntegrityViolationException e) {
            log.warn("Exception occurred : {} , While saving : {} children, retrying using individual save operations",
                    e, fragmentEntities.size());
            retrySavingEachChildIndividually(anchorEntity, parentNodeXpath, newChildren);
        }
    }

    private void retrySavingEachChildIndividually(final AnchorEntity anchorEntity, final String parentNodeXpath,
                                                  final Collection<DataNode> newChildren) {
        final Collection<String> failedXpaths = new HashSet<>();
        for (final DataNode newChild : newChildren) {
            try {
                addNewChildDataNode(anchorEntity, parentNodeXpath, newChild);
            } catch (final AlreadyDefinedException e) {
                failedXpaths.add(newChild.getXpath());
            }
        }
        if (!failedXpaths.isEmpty()) {
            throw AlreadyDefinedException.forDataNodes(failedXpaths, anchorEntity.getName());
        }
    }

    @Override
    public void storeDataNodes(final String dataspaceName, final String anchorName,
                               final Collection<DataNode> dataNodes) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        final List<FragmentEntity> fragmentEntities = new ArrayList<>(dataNodes.size());
        try {
            for (final DataNode dataNode: dataNodes) {
                final FragmentEntity fragmentEntity = convertToFragmentWithAllDescendants(anchorEntity, dataNode);
                fragmentEntities.add(fragmentEntity);
            }
            fragmentRepository.saveAll(fragmentEntities);
        } catch (final DataIntegrityViolationException exception) {
            log.warn("Exception occurred : {} , While saving : {} data nodes, Retrying saving data nodes individually",
                    exception, dataNodes.size());
            storeDataNodesIndividually(anchorEntity, dataNodes);
        }
    }

    private void storeDataNodesIndividually(final AnchorEntity anchorEntity, final Collection<DataNode> dataNodes) {
        final Collection<String> failedXpaths = new HashSet<>();
        for (final DataNode dataNode: dataNodes) {
            try {
                final FragmentEntity fragmentEntity = convertToFragmentWithAllDescendants(anchorEntity, dataNode);
                fragmentRepository.save(fragmentEntity);
            } catch (final DataIntegrityViolationException e) {
                failedXpaths.add(dataNode.getXpath());
            }
        }
        if (!failedXpaths.isEmpty()) {
            throw AlreadyDefinedException.forDataNodes(failedXpaths, anchorEntity.getName());
        }
    }

    /**
     * Convert DataNode object into Fragment and places the result in the fragments placeholder. Performs same action
     * for all DataNode children recursively.
     *
     * @param anchorEntity          anchorEntity
     * @param dataNodeToBeConverted dataNode
     * @return a Fragment built from current DataNode
     */
    private FragmentEntity convertToFragmentWithAllDescendants(final AnchorEntity anchorEntity,
                                                               final DataNode dataNodeToBeConverted) {
        final FragmentEntity parentFragment = toFragmentEntity(anchorEntity, dataNodeToBeConverted);
        final Builder<FragmentEntity> childFragmentsImmutableSetBuilder = ImmutableSet.builder();
        for (final DataNode childDataNode : dataNodeToBeConverted.getChildDataNodes()) {
            final FragmentEntity childFragment = convertToFragmentWithAllDescendants(anchorEntity, childDataNode);
            childFragmentsImmutableSetBuilder.add(childFragment);
        }
        parentFragment.setChildFragments(childFragmentsImmutableSetBuilder.build());
        return parentFragment;
    }

    private FragmentEntity toFragmentEntity(final AnchorEntity anchorEntity, final DataNode dataNode) {
        return FragmentEntity.builder()
                .anchor(anchorEntity)
                .xpath(dataNode.getXpath())
                .attributes(jsonObjectMapper.asJsonString(dataNode.getLeaves()))
                .build();
    }

    @Override
    @Timed(value = "cps.data.persistence.service.datanode.get",
            description = "Time taken to get a data node")
    public Collection<DataNode> getDataNodes(final String dataspaceName, final String anchorName,
                                             final String xpath,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        final String targetXpath = getNormalizedXpath(xpath);
        final Collection<DataNode> dataNodes = getDataNodesForMultipleXpaths(dataspaceName, anchorName,
                Collections.singletonList(targetXpath), fetchDescendantsOption);
        if (dataNodes.isEmpty()) {
            throw new DataNodeNotFoundException(dataspaceName, anchorName, xpath);
        }
        return dataNodes;
    }

    @Override
    @Timed(value = "cps.data.persistence.service.datanode.batch.get",
            description = "Time taken to get data nodes")
    public Collection<DataNode> getDataNodesForMultipleXpaths(final String dataspaceName, final String anchorName,
                                                              final Collection<String> xpaths,
                                                              final FetchDescendantsOption fetchDescendantsOption) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        Collection<FragmentEntity> fragmentEntities = getFragmentEntities(anchorEntity, xpaths);
        fragmentEntities = fragmentRepository.prefetchDescendantsOfFragmentEntities(fetchDescendantsOption,
                fragmentEntities);
        return createDataNodesFromFragmentEntities(fetchDescendantsOption, fragmentEntities);
    }

    private Collection<FragmentEntity> getFragmentEntities(final AnchorEntity anchorEntity,
                                                           final Collection<String> xpaths) {
        final Collection<String> nonRootXpaths = new HashSet<>(xpaths);
        final boolean haveRootXpath = nonRootXpaths.removeIf(CpsDataPersistenceServiceImpl::isRootXpath);

        final Collection<String> normalizedXpaths = new HashSet<>(nonRootXpaths.size());
        for (final String xpath : nonRootXpaths) {
            try {
                normalizedXpaths.add(CpsPathUtil.getNormalizedXpath(xpath));
            } catch (final PathParsingException e) {
                log.warn("Error parsing xpath \"{}\": {}", xpath, e.getMessage());
            }
        }
        if (haveRootXpath) {
            normalizedXpaths.addAll(fragmentRepository.findAllXpathByAnchorAndParentIdIsNull(anchorEntity));
        }

        return fragmentRepository.findByAnchorAndXpathIn(anchorEntity, normalizedXpaths);
    }

    private FragmentEntity getFragmentEntity(final AnchorEntity anchorEntity, final String xpath) {
        final FragmentEntity fragmentEntity;
        if (isRootXpath(xpath)) {
            fragmentEntity = fragmentRepository.findOneByAnchorId(anchorEntity.getId()).orElse(null);
        } else {
            fragmentEntity = fragmentRepository.getByAnchorAndXpath(anchorEntity, getNormalizedXpath(xpath));
        }
        if (fragmentEntity == null) {
            throw new DataNodeNotFoundException(anchorEntity.getDataspace().getName(), anchorEntity.getName(), xpath);
        }
        return fragmentEntity;
    }

    @Override
    @Timed(value = "cps.data.persistence.service.datanode.query",
            description = "Time taken to query data nodes")
    public List<DataNode> queryDataNodes(final String dataspaceName, final String anchorName, final String cpsPath,
                                         final FetchDescendantsOption fetchDescendantsOption) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = Strings.isNullOrEmpty(anchorName) ? ALL_ANCHORS
            : anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final CpsPathQuery cpsPathQuery;
        try {
            cpsPathQuery = CpsPathUtil.getCpsPathQuery(cpsPath);
        } catch (final PathParsingException e) {
            throw new CpsPathException(e.getMessage());
        }

        Collection<FragmentEntity> fragmentEntities;
        if (anchorEntity == ALL_ANCHORS) {
            fragmentEntities = fragmentRepository.findByDataspaceAndCpsPath(dataspaceEntity, cpsPathQuery);
        } else {
            fragmentEntities = fragmentRepository.findByAnchorAndCpsPath(anchorEntity, cpsPathQuery);
        }
        if (cpsPathQuery.hasAncestorAxis()) {
            final Collection<String> ancestorXpaths = processAncestorXpath(fragmentEntities, cpsPathQuery);
            if (anchorEntity == ALL_ANCHORS) {
                fragmentEntities = fragmentRepository.findByDataspaceAndXpathIn(dataspaceEntity, ancestorXpaths);
            } else {
                fragmentEntities = fragmentRepository.findByAnchorAndXpathIn(anchorEntity, ancestorXpaths);
            }
        }
        fragmentEntities = fragmentRepository.prefetchDescendantsOfFragmentEntities(fetchDescendantsOption,
                fragmentEntities);
        return createDataNodesFromFragmentEntities(fetchDescendantsOption, fragmentEntities);
    }

    @Override
    public List<DataNode> queryDataNodesAcrossAnchors(final String dataspaceName, final String cpsPath,
                                                      final FetchDescendantsOption fetchDescendantsOption) {
        return queryDataNodes(dataspaceName, QUERY_ACROSS_ANCHORS, cpsPath, fetchDescendantsOption);
    }

    private List<DataNode> createDataNodesFromFragmentEntities(final FetchDescendantsOption fetchDescendantsOption,
                                                               final Collection<FragmentEntity> fragmentEntities) {
        final List<DataNode> dataNodes = new ArrayList<>(fragmentEntities.size());
        for (final FragmentEntity fragmentEntity : fragmentEntities) {
            dataNodes.add(toDataNode(fragmentEntity, fetchDescendantsOption));
        }
        return Collections.unmodifiableList(dataNodes);
    }

    private static String getNormalizedXpath(final String xpathSource) {
        if (isRootXpath(xpathSource)) {
            return xpathSource;
        }
        try {
            return CpsPathUtil.getNormalizedXpath(xpathSource);
        } catch (final PathParsingException e) {
            throw new CpsPathException(e.getMessage());
        }
    }

    @Override
    public String startSession() {
        return sessionManager.startSession();
    }

    @Override
    public void closeSession(final String sessionId) {
        sessionManager.closeSession(sessionId, SessionManager.WITH_COMMIT);
    }

    @Override
    public void lockAnchor(final String sessionId, final String dataspaceName,
                           final String anchorName, final Long timeoutInMilliseconds) {
        sessionManager.lockAnchor(sessionId, dataspaceName, anchorName, timeoutInMilliseconds);
    }

    private static Set<String> processAncestorXpath(final Collection<FragmentEntity> fragmentEntities,
                                                    final CpsPathQuery cpsPathQuery) {
        final Set<String> ancestorXpath = new HashSet<>();
        final Pattern pattern =
                Pattern.compile("(.*/" + Pattern.quote(cpsPathQuery.getAncestorSchemaNodeIdentifier())
                        + REG_EX_FOR_OPTIONAL_LIST_INDEX + "/.*");
        for (final FragmentEntity fragmentEntity : fragmentEntities) {
            final Matcher matcher = pattern.matcher(fragmentEntity.getXpath());
            if (matcher.matches()) {
                ancestorXpath.add(matcher.group(1));
            }
        }
        return ancestorXpath;
    }

    private DataNode toDataNode(final FragmentEntity fragmentEntity,
                                final FetchDescendantsOption fetchDescendantsOption) {
        final List<DataNode> childDataNodes = getChildDataNodes(fragmentEntity, fetchDescendantsOption);
        Map<String, Serializable> leaves = new HashMap<>();
        if (fragmentEntity.getAttributes() != null) {
            leaves = jsonObjectMapper.convertJsonString(fragmentEntity.getAttributes(), Map.class);
        }
        return new DataNodeBuilder()
                .withXpath(fragmentEntity.getXpath())
                .withLeaves(leaves)
                .withDataspace(fragmentEntity.getAnchor().getDataspace().getName())
                .withAnchor(fragmentEntity.getAnchor().getName())
                .withChildDataNodes(childDataNodes).build();
    }

    private List<DataNode> getChildDataNodes(final FragmentEntity fragmentEntity,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        if (fetchDescendantsOption.hasNext()) {
            return fragmentEntity.getChildFragments().stream()
                    .map(childFragmentEntity -> toDataNode(childFragmentEntity, fetchDescendantsOption.next()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void batchUpdateDataLeaves(final String dataspaceName, final String anchorName,
                                        final Map<String, Map<String, Serializable>> updatedLeavesPerXPath) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);

        final Collection<String> xpathsOfUpdatedLeaves = updatedLeavesPerXPath.keySet();
        final Collection<FragmentEntity> fragmentEntities = getFragmentEntities(anchorEntity, xpathsOfUpdatedLeaves);

        for (final FragmentEntity fragmentEntity : fragmentEntities) {
            final Map<String, Serializable> updatedLeaves = updatedLeavesPerXPath.get(fragmentEntity.getXpath());
            final String mergedLeaves = mergeLeaves(updatedLeaves, fragmentEntity.getAttributes());
            fragmentEntity.setAttributes(mergedLeaves);
        }

        try {
            fragmentRepository.saveAll(fragmentEntities);
        } catch (final StaleStateException staleStateException) {
            retryUpdateDataNodesIndividually(anchorEntity, fragmentEntities);
        }
    }

    @Override
    public void updateDataNodesAndDescendants(final String dataspaceName, final String anchorName,
                                              final Collection<DataNode> updatedDataNodes) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);

        final Map<String, DataNode> xpathToUpdatedDataNode = updatedDataNodes.stream()
            .collect(Collectors.toMap(DataNode::getXpath, dataNode -> dataNode));

        final Collection<String> xpaths = xpathToUpdatedDataNode.keySet();
        Collection<FragmentEntity> existingFragmentEntities = getFragmentEntities(anchorEntity, xpaths);
        existingFragmentEntities = fragmentRepository.prefetchDescendantsOfFragmentEntities(
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, existingFragmentEntities);

        for (final FragmentEntity existingFragmentEntity : existingFragmentEntities) {
            final DataNode updatedDataNode = xpathToUpdatedDataNode.get(existingFragmentEntity.getXpath());
            updateFragmentEntityAndDescendantsWithDataNode(existingFragmentEntity, updatedDataNode);
        }

        try {
            fragmentRepository.saveAll(existingFragmentEntities);
        } catch (final StaleStateException staleStateException) {
            retryUpdateDataNodesIndividually(anchorEntity, existingFragmentEntities);
        }
    }

    private void retryUpdateDataNodesIndividually(final AnchorEntity anchorEntity,
                                                  final Collection<FragmentEntity> fragmentEntities) {
        final Collection<String> failedXpaths = new HashSet<>();
        for (final FragmentEntity dataNodeFragment : fragmentEntities) {
            try {
                fragmentRepository.save(dataNodeFragment);
            } catch (final StaleStateException e) {
                failedXpaths.add(dataNodeFragment.getXpath());
            }
        }
        if (!failedXpaths.isEmpty()) {
            final String failedXpathsConcatenated = String.join(",", failedXpaths);
            throw new ConcurrencyException("Concurrent Transactions", String.format(
                    "DataNodes : %s in Dataspace :'%s' with Anchor : '%s'  are updated by another transaction.",
                    failedXpathsConcatenated, anchorEntity.getDataspace().getName(), anchorEntity.getName()));
        }
    }

    private void updateFragmentEntityAndDescendantsWithDataNode(final FragmentEntity existingFragmentEntity,
                                                                final DataNode newDataNode) {
        existingFragmentEntity.setAttributes(jsonObjectMapper.asJsonString(newDataNode.getLeaves()));

        final Map<String, FragmentEntity> existingChildrenByXpath = existingFragmentEntity.getChildFragments().stream()
                .collect(Collectors.toMap(FragmentEntity::getXpath, childFragmentEntity -> childFragmentEntity));

        final Collection<FragmentEntity> updatedChildFragments = new HashSet<>();
        for (final DataNode newDataNodeChild : newDataNode.getChildDataNodes()) {
            final FragmentEntity childFragment;
            if (isNewDataNode(newDataNodeChild, existingChildrenByXpath)) {
                childFragment = convertToFragmentWithAllDescendants(existingFragmentEntity.getAnchor(),
                    newDataNodeChild);
            } else {
                childFragment = existingChildrenByXpath.get(newDataNodeChild.getXpath());
                updateFragmentEntityAndDescendantsWithDataNode(childFragment, newDataNodeChild);
            }
            updatedChildFragments.add(childFragment);
        }

        existingFragmentEntity.getChildFragments().clear();
        existingFragmentEntity.getChildFragments().addAll(updatedChildFragments);
    }

    @Override
    @Transactional
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                   final Collection<DataNode> newListElements) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        final FragmentEntity parentEntity = getFragmentEntity(anchorEntity, parentNodeXpath);
        final String listElementXpathPrefix = getListElementXpathPrefix(newListElements);
        final Map<String, FragmentEntity> existingListElementFragmentEntitiesByXPath =
                extractListElementFragmentEntitiesByXPath(parentEntity.getChildFragments(), listElementXpathPrefix);
        parentEntity.getChildFragments().removeAll(existingListElementFragmentEntitiesByXPath.values());
        final Set<FragmentEntity> updatedChildFragmentEntities = new HashSet<>();
        for (final DataNode newListElement : newListElements) {
            final FragmentEntity existingListElementEntity =
                    existingListElementFragmentEntitiesByXPath.get(newListElement.getXpath());
            final FragmentEntity entityToBeAdded = getFragmentForReplacement(parentEntity, newListElement,
                    existingListElementEntity);
            updatedChildFragmentEntities.add(entityToBeAdded);
        }
        parentEntity.getChildFragments().addAll(updatedChildFragmentEntities);
        fragmentRepository.save(parentEntity);
    }

    @Override
    @Transactional
    public void deleteDataNodes(final String dataspaceName, final String anchorName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        anchorRepository.findByDataspaceAndName(dataspaceEntity, anchorName)
            .ifPresent(anchorEntity -> fragmentRepository.deleteByAnchorIn(Collections.singletonList(anchorEntity)));
    }

    @Override
    @Transactional
    public void deleteDataNodes(final String dataspaceName, final Collection<String> anchorNames) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final Collection<AnchorEntity> anchorEntities =
            anchorRepository.findAllByDataspaceAndNameIn(dataspaceEntity, anchorNames);
        fragmentRepository.deleteByAnchorIn(anchorEntities);
    }

    @Override
    @Transactional
    public void deleteDataNodes(final String dataspaceName, final String anchorName,
                                final Collection<String> xpathsToDelete) {
        deleteDataNodes(dataspaceName, anchorName, xpathsToDelete, false);
    }

    private void deleteDataNodes(final String dataspaceName, final String anchorName,
                                 final Collection<String> xpathsToDelete, final boolean onlySupportListDeletion) {
        final boolean haveRootXpath = xpathsToDelete.stream().anyMatch(CpsDataPersistenceServiceImpl::isRootXpath);
        if (haveRootXpath) {
            deleteDataNodes(dataspaceName, anchorName);
            return;
        }

        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);

        final Collection<String> deleteChecklist = new HashSet<>(xpathsToDelete.size());
        for (final String xpath : xpathsToDelete) {
            try {
                deleteChecklist.add(CpsPathUtil.getNormalizedXpath(xpath));
            } catch (final PathParsingException e) {
                log.warn("Error parsing xpath \"{}\": {}", xpath, e.getMessage());
            }
        }

        final Collection<String> xpathsToExistingContainers =
            fragmentRepository.findAllXpathByAnchorAndXpathIn(anchorEntity, deleteChecklist);
        if (onlySupportListDeletion) {
            final Collection<String> xpathsToExistingListElements = xpathsToExistingContainers.stream()
                .filter(CpsPathUtil::isPathToListElement).collect(Collectors.toList());
            deleteChecklist.removeAll(xpathsToExistingListElements);
        } else {
            deleteChecklist.removeAll(xpathsToExistingContainers);
        }

        final Collection<String> xpathsToExistingLists = deleteChecklist.stream()
            .filter(xpath -> fragmentRepository.existsByAnchorAndXpathStartsWith(anchorEntity, xpath + "["))
            .collect(Collectors.toList());
        deleteChecklist.removeAll(xpathsToExistingLists);

        if (!deleteChecklist.isEmpty()) {
            throw new DataNodeNotFoundExceptionBatch(dataspaceName, anchorName, deleteChecklist);
        }

        fragmentRepository.deleteByAnchorIdAndXpaths(anchorEntity.getId(), xpathsToExistingContainers);
        fragmentRepository.deleteListsByAnchorIdAndXpaths(anchorEntity.getId(), xpathsToExistingLists);
    }

    @Override
    @Transactional
    public void deleteListDataNode(final String dataspaceName, final String anchorName,
                                   final String targetXpath) {
        deleteDataNode(dataspaceName, anchorName, targetXpath, true);
    }

    @Override
    @Transactional
    public void deleteDataNode(final String dataspaceName, final String anchorName, final String targetXpath) {
        deleteDataNode(dataspaceName, anchorName, targetXpath, false);
    }

    private void deleteDataNode(final String dataspaceName, final String anchorName, final String targetXpath,
                                final boolean onlySupportListNodeDeletion) {
        final String normalizedXpath = getNormalizedXpath(targetXpath);
        try {
            deleteDataNodes(dataspaceName, anchorName, Collections.singletonList(normalizedXpath),
                onlySupportListNodeDeletion);
        } catch (final DataNodeNotFoundExceptionBatch dataNodeNotFoundExceptionBatch) {
            throw new DataNodeNotFoundException(dataspaceName, anchorName, targetXpath);
        }
    }

    private static String getListElementXpathPrefix(final Collection<DataNode> newListElements) {
        if (newListElements.isEmpty()) {
            throw new CpsAdminException("Invalid list replacement",
                    "Cannot replace list elements with empty collection");
        }
        final String firstChildNodeXpath = newListElements.iterator().next().getXpath();
        return firstChildNodeXpath.substring(0, firstChildNodeXpath.lastIndexOf('[') + 1);
    }

    private FragmentEntity getFragmentForReplacement(final FragmentEntity parentEntity,
                                                     final DataNode newListElement,
                                                     final FragmentEntity existingListElementEntity) {
        if (existingListElementEntity == null) {
            return convertToFragmentWithAllDescendants(parentEntity.getAnchor(), newListElement);
        }
        if (newListElement.getChildDataNodes().isEmpty()) {
            copyAttributesFromNewListElement(existingListElementEntity, newListElement);
            existingListElementEntity.getChildFragments().clear();
        } else {
            updateFragmentEntityAndDescendantsWithDataNode(existingListElementEntity, newListElement);
        }
        return existingListElementEntity;
    }

    private static boolean isNewDataNode(final DataNode replacementDataNode,
                                         final Map<String, FragmentEntity> existingListElementsByXpath) {
        return !existingListElementsByXpath.containsKey(replacementDataNode.getXpath());
    }

    private void copyAttributesFromNewListElement(final FragmentEntity existingListElementEntity,
                                                  final DataNode newListElement) {
        final FragmentEntity replacementFragmentEntity =
                FragmentEntity.builder().attributes(jsonObjectMapper.asJsonString(
                        newListElement.getLeaves())).build();
        existingListElementEntity.setAttributes(replacementFragmentEntity.getAttributes());
    }

    private static Map<String, FragmentEntity> extractListElementFragmentEntitiesByXPath(
            final Set<FragmentEntity> childEntities, final String listElementXpathPrefix) {
        return childEntities.stream()
                .filter(fragmentEntity -> fragmentEntity.getXpath().startsWith(listElementXpathPrefix))
                .collect(Collectors.toMap(FragmentEntity::getXpath, fragmentEntity -> fragmentEntity));
    }

    private static boolean isRootXpath(final String xpath) {
        return "/".equals(xpath) || "".equals(xpath);
    }

    private String mergeLeaves(final Map<String, Serializable> updateLeaves, final String currentLeavesAsString) {
        Map<String, Serializable> currentLeavesAsMap = new HashMap<>();
        if (currentLeavesAsString != null) {
            currentLeavesAsMap = jsonObjectMapper.convertJsonString(currentLeavesAsString, Map.class);
            currentLeavesAsMap.putAll(updateLeaves);
        }

        if (currentLeavesAsMap.isEmpty()) {
            return "";
        }
        return jsonObjectMapper.asJsonString(currentLeavesAsMap);
    }

    private AnchorEntity getAnchorEntity(final String dataspaceName, final String anchorName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        return anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
    }
}
