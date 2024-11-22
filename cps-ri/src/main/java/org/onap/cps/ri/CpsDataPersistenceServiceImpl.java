/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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

package org.onap.cps.ri;

import static org.onap.cps.spi.PaginationOption.NO_PAGINATION;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import io.micrometer.core.annotation.Timed;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleStateException;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.FragmentEntity;
import org.onap.cps.ri.repository.AnchorRepository;
import org.onap.cps.ri.repository.DataspaceRepository;
import org.onap.cps.ri.repository.FragmentRepository;
import org.onap.cps.ri.utils.SessionManager;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.PaginationOption;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.ConcurrencyException;
import org.onap.cps.spi.exceptions.CpsAdminException;
import org.onap.cps.spi.exceptions.CpsPathException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundExceptionBatch;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
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
            } catch (final DataIntegrityViolationException dataIntegrityViolationException) {
                failedXpaths.add(dataNode.getXpath());
            }
        }
        if (!failedXpaths.isEmpty()) {
            throw AlreadyDefinedException.forDataNodes(failedXpaths, anchorEntity.getName());
        }
    }

    @Override
    public void addListElements(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                final Collection<DataNode> newListElements) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        addChildrenDataNodes(anchorEntity, parentNodeXpath, newListElements);
    }

    @Override
    public void addChildDataNodes(final String dataspaceName, final String anchorName,
                                  final String parentNodeXpath, final Collection<DataNode> dataNodes) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        addChildrenDataNodes(anchorEntity, parentNodeXpath, dataNodes);
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

        logMissingXPaths(xpaths, existingFragmentEntities);

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
            } catch (final StaleStateException staleStateException) {
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
        copyAttributesFromNewDataNode(existingFragmentEntity, newDataNode);

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
    @Timed(value = "cps.data.persistence.service.datanode.query",
            description = "Time taken to query data nodes")
    public List<DataNode> queryDataNodes(final String dataspaceName, final String anchorName, final String cpsPath,
                                         final FetchDescendantsOption fetchDescendantsOption) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        final CpsPathQuery cpsPathQuery = getCpsPathQuery(cpsPath);

        Collection<FragmentEntity> fragmentEntities;
        fragmentEntities = fragmentRepository.findByAnchorAndCpsPath(anchorEntity, cpsPathQuery);
        if (cpsPathQuery.hasAncestorAxis()) {
            final Collection<String> ancestorXpaths = processAncestorXpath(fragmentEntities, cpsPathQuery);
            fragmentEntities = fragmentRepository.findByAnchorAndXpathIn(anchorEntity, ancestorXpaths);
        }
        return createDataNodesFromFragmentEntities(fetchDescendantsOption, fragmentEntities);
    }

    @Override
    @Timed(value = "cps.data.persistence.service.datanode.query.anchors",
            description = "Time taken to query data nodes across all anchors or list of anchors")
    public List<DataNode> queryDataNodesAcrossAnchors(final String dataspaceName, final String cpsPath,
                                                      final FetchDescendantsOption fetchDescendantsOption,
                                                      final PaginationOption paginationOption) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final CpsPathQuery cpsPathQuery = getCpsPathQuery(cpsPath);

        final List<Long> anchorIds;
        if (paginationOption == NO_PAGINATION) {
            anchorIds = Collections.emptyList();
        } else {
            anchorIds = getAnchorIdsForPagination(dataspaceEntity, cpsPathQuery, paginationOption);
            if (anchorIds.isEmpty()) {
                return Collections.emptyList();
            }
        }
        Collection<FragmentEntity> fragmentEntities =
            fragmentRepository.findByDataspaceAndCpsPath(dataspaceEntity, cpsPathQuery, anchorIds);

        if (cpsPathQuery.hasAncestorAxis()) {
            final Collection<String> ancestorXpaths = processAncestorXpath(fragmentEntities, cpsPathQuery);
            if (anchorIds.isEmpty()) {
                fragmentEntities = fragmentRepository.findByDataspaceAndXpathIn(dataspaceEntity, ancestorXpaths);
            } else {
                fragmentEntities = fragmentRepository.findByAnchorIdsAndXpathIn(anchorIds, ancestorXpaths);
            }
        }
        return createDataNodesFromFragmentEntities(fetchDescendantsOption, fragmentEntities);
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

    @Override
    public Integer countAnchorsForDataspaceAndCpsPath(final String dataspaceName, final String cpsPath) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final CpsPathQuery cpsPathQuery = getCpsPathQuery(cpsPath);
        final List<Long> anchorIdList = getAnchorIdsForPagination(dataspaceEntity, cpsPathQuery, NO_PAGINATION);
        return anchorIdList.size();
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

        final Collection<String> deleteChecklist = getNormalizedXpaths(xpathsToDelete);
        final Collection<String> xpathsToExistingContainers =
                fragmentRepository.findAllXpathByAnchorAndXpathIn(anchorEntity, deleteChecklist);
        if (onlySupportListDeletion) {
            final Collection<String> xpathsToExistingListElements = xpathsToExistingContainers.stream()
                    .filter(CpsPathUtil::isPathToListElement).toList();
            deleteChecklist.removeAll(xpathsToExistingListElements);
        } else {
            deleteChecklist.removeAll(xpathsToExistingContainers);
        }

        final Collection<String> xpathsToExistingLists = deleteChecklist.stream()
                .filter(xpath -> fragmentRepository.existsByAnchorAndXpathStartsWith(anchorEntity, xpath + "["))
                .toList();
        deleteChecklist.removeAll(xpathsToExistingLists);

        if (!deleteChecklist.isEmpty()) {
            throw new DataNodeNotFoundExceptionBatch(dataspaceName, anchorName, deleteChecklist);
        }

        if (!xpathsToExistingContainers.isEmpty()) {
            fragmentRepository.deleteByAnchorIdAndXpaths(anchorEntity.getId(), xpathsToExistingContainers);
        }
        for (final String listXpath : xpathsToExistingLists) {
            fragmentRepository.deleteListByAnchorIdAndXpath(anchorEntity.getId(), listXpath);
        }
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
        final Collection<FragmentEntity> fragmentEntities = getFragmentEntities(anchorEntity, xpaths);
        return createDataNodesFromFragmentEntities(fetchDescendantsOption, fragmentEntities);
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
        } catch (final DataIntegrityViolationException dataIntegrityViolationException) {
            log.warn("Exception occurred : {} , While saving : {} children, retrying using individual save operations",
                    dataIntegrityViolationException, fragmentEntities.size());
            retrySavingEachChildIndividually(anchorEntity, parentNodeXpath, newChildren);
        }
    }

    private void addNewChildDataNode(final AnchorEntity anchorEntity, final String parentNodeXpath,
                                     final DataNode newChild) {
        final FragmentEntity parentFragmentEntity = getFragmentEntity(anchorEntity, parentNodeXpath);
        final FragmentEntity newChildAsFragmentEntity = convertToFragmentWithAllDescendants(anchorEntity, newChild);
        newChildAsFragmentEntity.setParentId(parentFragmentEntity.getId());
        try {
            fragmentRepository.save(newChildAsFragmentEntity);
        } catch (final DataIntegrityViolationException dataIntegrityViolationException) {
            throw AlreadyDefinedException.forDataNodes(Collections.singletonList(newChild.getXpath()),
                    anchorEntity.getName());
        }
    }

    private void retrySavingEachChildIndividually(final AnchorEntity anchorEntity, final String parentNodeXpath,
                                                  final Collection<DataNode> newChildren) {
        final Collection<String> failedXpaths = new HashSet<>();
        for (final DataNode newChild : newChildren) {
            try {
                addNewChildDataNode(anchorEntity, parentNodeXpath, newChild);
            } catch (final AlreadyDefinedException alreadyDefinedException) {
                failedXpaths.add(newChild.getXpath());
            }
        }
        if (!failedXpaths.isEmpty()) {
            throw AlreadyDefinedException.forDataNodes(failedXpaths, anchorEntity.getName());
        }
    }

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

    private List<DataNode> createDataNodesFromFragmentEntities(final FetchDescendantsOption fetchDescendantsOption,
                                                               final Collection<FragmentEntity> fragmentEntities) {
        final Collection<FragmentEntity> fragmentEntitiesWithDescendants =
                fragmentRepository.prefetchDescendantsOfFragmentEntities(fetchDescendantsOption, fragmentEntities);
        final List<DataNode> dataNodes = new ArrayList<>(fragmentEntitiesWithDescendants.size());
        for (final FragmentEntity fragmentEntity : fragmentEntitiesWithDescendants) {
            dataNodes.add(toDataNode(fragmentEntity, fetchDescendantsOption));
        }
        return Collections.unmodifiableList(dataNodes);
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
                .toList();
        }
        return Collections.emptyList();
    }

    private AnchorEntity getAnchorEntity(final String dataspaceName, final String anchorName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        return anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
    }

    private List<Long> getAnchorIdsForPagination(final DataspaceEntity dataspaceEntity, final CpsPathQuery cpsPathQuery,
                                                 final PaginationOption paginationOption) {
        return fragmentRepository.findAnchorIdsForPagination(dataspaceEntity, cpsPathQuery, paginationOption);
    }

    private FragmentEntity getFragmentEntity(final AnchorEntity anchorEntity, final String xpath) {
        final FragmentEntity fragmentEntity =
                fragmentRepository.findByAnchorIdAndXpath(anchorEntity.getId(), getNormalizedXpath(xpath));
        if (fragmentEntity == null) {
            throw new DataNodeNotFoundException(anchorEntity.getDataspace().getName(), anchorEntity.getName(), xpath);
        }
        return fragmentEntity;
    }

    private Collection<FragmentEntity> getFragmentEntities(final AnchorEntity anchorEntity,
                                                           final Collection<String> xpaths) {
        final Collection<String> normalizedXpaths = getNormalizedXpaths(xpaths);

        final boolean haveRootXpath = normalizedXpaths.removeIf(CpsDataPersistenceServiceImpl::isRootXpath);

        final List<FragmentEntity> fragmentEntities = fragmentRepository.findByAnchorAndXpathIn(anchorEntity,
                normalizedXpaths);

        for (final FragmentEntity fragmentEntity : fragmentEntities) {
            normalizedXpaths.remove(fragmentEntity.getXpath());
        }

        for (final String xpath : normalizedXpaths) {
            if (!CpsPathUtil.isPathToListElement(xpath)) {
                fragmentEntities.addAll(fragmentRepository.findListByAnchorAndXpath(anchorEntity, xpath));
            }
        }

        if (haveRootXpath) {
            fragmentEntities.addAll(fragmentRepository.findRootsByAnchorId(anchorEntity.getId()));
        }

        return fragmentEntities;
    }

    private FragmentEntity getFragmentForReplacement(final FragmentEntity parentEntity,
                                                     final DataNode newListElement,
                                                     final FragmentEntity existingListElementEntity) {
        if (existingListElementEntity == null) {
            return convertToFragmentWithAllDescendants(parentEntity.getAnchor(), newListElement);
        }
        if (newListElement.getChildDataNodes().isEmpty()) {
            copyAttributesFromNewDataNode(existingListElementEntity, newListElement);
            existingListElementEntity.getChildFragments().clear();
        } else {
            updateFragmentEntityAndDescendantsWithDataNode(existingListElementEntity, newListElement);
        }
        return existingListElementEntity;
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

    private void copyAttributesFromNewDataNode(final FragmentEntity existingFragmentEntity,
                                               final DataNode newDataNode) {
        final String oldOrderedLeavesAsJson = getOrderedLeavesAsJson(existingFragmentEntity.getAttributes());
        final String newOrderedLeavesAsJson = getOrderedLeavesAsJson(newDataNode.getLeaves());
        if (!oldOrderedLeavesAsJson.equals(newOrderedLeavesAsJson)) {
            existingFragmentEntity.setAttributes(jsonObjectMapper.asJsonString(newDataNode.getLeaves()));
        }
    }

    private String getOrderedLeavesAsJson(final Map<String, Serializable> currentLeaves) {
        final Map<String, Serializable> sortedLeaves = new TreeMap<>(String::compareTo);
        sortedLeaves.putAll(currentLeaves);
        return jsonObjectMapper.asJsonString(sortedLeaves);
    }

    private String getOrderedLeavesAsJson(final String currentLeavesAsString) {
        if (currentLeavesAsString == null) {
            return "{}";
        }
        final Map<String, Serializable> sortedLeaves = jsonObjectMapper.convertJsonString(currentLeavesAsString,
            TreeMap.class);
        return jsonObjectMapper.asJsonString(sortedLeaves);
    }

    private static String getNormalizedXpath(final String xpathSource) {
        if (isRootXpath(xpathSource)) {
            return xpathSource;
        }
        try {
            return CpsPathUtil.getNormalizedXpath(xpathSource);
        } catch (final PathParsingException pathParsingException) {
            throw new CpsPathException(pathParsingException.getMessage());
        }
    }

    private static Collection<String> getNormalizedXpaths(final Collection<String> xpaths) {
        final Collection<String> normalizedXpaths = new HashSet<>(xpaths.size());
        for (final String xpath : xpaths) {
            try {
                normalizedXpaths.add(getNormalizedXpath(xpath));
            } catch (final CpsPathException cpsPathException) {
                log.warn("Error parsing xpath \"{}\": {}", xpath, cpsPathException.getMessage());
            }
        }
        return normalizedXpaths;
    }

    private static String getListElementXpathPrefix(final Collection<DataNode> newListElements) {
        if (newListElements.isEmpty()) {
            throw new CpsAdminException("Invalid list replacement",
                    "Cannot replace list elements with empty collection");
        }
        final String firstChildNodeXpath = newListElements.iterator().next().getXpath();
        return firstChildNodeXpath.substring(0, firstChildNodeXpath.lastIndexOf('[') + 1);
    }

    private static Map<String, FragmentEntity> extractListElementFragmentEntitiesByXPath(
            final Set<FragmentEntity> childEntities, final String listElementXpathPrefix) {
        return childEntities.stream()
                .filter(fragmentEntity -> fragmentEntity.getXpath().startsWith(listElementXpathPrefix))
                .collect(Collectors.toMap(FragmentEntity::getXpath, fragmentEntity -> fragmentEntity));
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

    private static boolean isRootXpath(final String xpath) {
        return "/".equals(xpath) || "".equals(xpath);
    }

    private static boolean isNewDataNode(final DataNode replacementDataNode,
                                         final Map<String, FragmentEntity> existingListElementsByXpath) {
        return !existingListElementsByXpath.containsKey(replacementDataNode.getXpath());
    }

    private static CpsPathQuery getCpsPathQuery(final String cpsPath) {
        try {
            return CpsPathUtil.getCpsPathQuery(cpsPath);
        } catch (final PathParsingException e) {
            throw new CpsPathException(e.getMessage());
        }
    }

    private static void logMissingXPaths(final Collection<String> xpaths,
                                         final Collection<FragmentEntity> existingFragmentEntities) {
        final Set<String> existingXPaths =
                existingFragmentEntities.stream().map(FragmentEntity::getXpath).collect(Collectors.toSet());
        final Set<String> missingXPaths =
                xpaths.stream().filter(xpath -> !existingXPaths.contains(xpath)).collect(Collectors.toSet());
        if (!missingXPaths.isEmpty()) {
            log.warn("Cannot update data nodes: Target XPaths {} not found in DB.", missingXPaths);
        }
    }

}
