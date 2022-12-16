/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
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
import org.onap.cps.spi.entities.FragmentEntityArranger;
import org.onap.cps.spi.entities.FragmentExtract;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.AlreadyDefinedExceptionBatch;
import org.onap.cps.spi.exceptions.ConcurrencyException;
import org.onap.cps.spi.exceptions.CpsAdminException;
import org.onap.cps.spi.exceptions.CpsPathException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
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

    private static final String REG_EX_FOR_OPTIONAL_LIST_INDEX = "(\\[@[\\s\\S]+?]){0,1})";
    private static final Pattern REG_EX_PATTERN_FOR_LIST_ELEMENT_KEY_PREDICATE =
            Pattern.compile("\\[(\\@([^\\/]{0,9999}))\\]$");
    private static final String TOP_LEVEL_MODULE_PREFIX_PROPERTY_NAME = "topLevelModulePrefix";

    @Override
    public void addChildDataNode(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                 final DataNode newChildDataNode) {
        addNewChildDataNode(dataspaceName, anchorName, parentNodeXpath, newChildDataNode);
    }

    @Override
    public void addListElements(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                final Collection<DataNode> newListElements) {
        addChildrenDataNodes(dataspaceName, anchorName, parentNodeXpath, newListElements);
    }

    @Override
    public void addMultipleLists(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                 final Collection<Collection<DataNode>> newLists) {
        final Collection<String> failedXpaths = new HashSet<>();
        newLists.forEach(newList -> {
            try {
                addChildrenDataNodes(dataspaceName, anchorName, parentNodeXpath, newList);
            } catch (final AlreadyDefinedExceptionBatch e) {
                failedXpaths.addAll(e.getAlreadyDefinedXpaths());
            }
        });

        if (!failedXpaths.isEmpty()) {
            throw new AlreadyDefinedExceptionBatch(failedXpaths);
        }

    }

    private void addNewChildDataNode(final String dataspaceName, final String anchorName,
                                     final String parentNodeXpath, final DataNode newChild) {
        final FragmentEntity parentFragmentEntity =
            getFragmentWithoutDescendantsByXpath(dataspaceName, anchorName, parentNodeXpath);
        final FragmentEntity newChildAsFragmentEntity =
                convertToFragmentWithAllDescendants(parentFragmentEntity.getDataspace(),
                        parentFragmentEntity.getAnchor(), newChild);
        newChildAsFragmentEntity.setParentId(parentFragmentEntity.getId());
        try {
            fragmentRepository.save(newChildAsFragmentEntity);
        } catch (final DataIntegrityViolationException e) {
            throw AlreadyDefinedException.forDataNode(newChild.getXpath(), anchorName, e);
        }

    }

    private void addChildrenDataNodes(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                      final Collection<DataNode> newChildren) {
        final FragmentEntity parentFragmentEntity =
            getFragmentWithoutDescendantsByXpath(dataspaceName, anchorName, parentNodeXpath);
        final List<FragmentEntity> fragmentEntities = new ArrayList<>(newChildren.size());
        try {
            newChildren.forEach(newChildAsDataNode -> {
                final FragmentEntity newChildAsFragmentEntity =
                        convertToFragmentWithAllDescendants(parentFragmentEntity.getDataspace(),
                                parentFragmentEntity.getAnchor(), newChildAsDataNode);
                newChildAsFragmentEntity.setParentId(parentFragmentEntity.getId());
                fragmentEntities.add(newChildAsFragmentEntity);
            });
            fragmentRepository.saveAll(fragmentEntities);
        } catch (final DataIntegrityViolationException e) {
            log.warn("Exception occurred : {} , While saving : {} children, retrying using individual save operations",
                    e, fragmentEntities.size());
            retrySavingEachChildIndividually(dataspaceName, anchorName, parentNodeXpath, newChildren);
        }
    }

    private void retrySavingEachChildIndividually(final String dataspaceName, final String anchorName,
                                                  final String parentNodeXpath,
                                                  final Collection<DataNode> newChildren) {
        final Collection<String> failedXpaths = new HashSet<>();
        for (final DataNode newChild : newChildren) {
            try {
                addNewChildDataNode(dataspaceName, anchorName, parentNodeXpath, newChild);
            } catch (final AlreadyDefinedException e) {
                failedXpaths.add(newChild.getXpath());
            }
        }
        if (!failedXpaths.isEmpty()) {
            throw new AlreadyDefinedExceptionBatch(failedXpaths);
        }
    }

    @Override
    public void storeDataNode(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final FragmentEntity fragmentEntity = convertToFragmentWithAllDescendants(dataspaceEntity, anchorEntity,
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
    private FragmentEntity convertToFragmentWithAllDescendants(final DataspaceEntity dataspaceEntity,
                                                               final AnchorEntity anchorEntity,
                                                               final DataNode dataNodeToBeConverted) {
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

    private FragmentEntity toFragmentEntity(final DataspaceEntity dataspaceEntity,
                                            final AnchorEntity anchorEntity, final DataNode dataNode) {
        return FragmentEntity.builder()
                .dataspace(dataspaceEntity)
                .anchor(anchorEntity)
                .xpath(dataNode.getXpath())
                .attributes(jsonObjectMapper.asJsonString(dataNode.getLeaves()))
                .build();
    }

    @Override
    public DataNode getDataNode(final String dataspaceName, final String anchorName, final String xpath,
                                final FetchDescendantsOption fetchDescendantsOption) {
        final FragmentEntity fragmentEntity = getFragmentByXpath(dataspaceName, anchorName, xpath,
            fetchDescendantsOption);
        return toDataNode(fragmentEntity, fetchDescendantsOption);
    }

    private FragmentEntity getFragmentWithoutDescendantsByXpath(final String dataspaceName,
                                                                final String anchorName,
                                                                final String xpath) {
        return getFragmentByXpath(dataspaceName, anchorName, xpath, FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    private FragmentEntity getFragmentByXpath(final String dataspaceName, final String anchorName,
                                              final String xpath, final FetchDescendantsOption fetchDescendantsOption) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        if (isRootXpath(xpath)) {
            final List<FragmentExtract> fragmentExtracts = fragmentRepository.getTopLevelFragments(dataspaceEntity,
                    anchorEntity);
            return FragmentEntityArranger.toFragmentEntityTree(anchorEntity,
                    fragmentExtracts);
        } else {
            final String normalizedXpath = getNormalizedXpath(xpath);
            final FragmentEntity fragmentEntity;
            if (FetchDescendantsOption.OMIT_DESCENDANTS.equals(fetchDescendantsOption)) {
                fragmentEntity =
                    fragmentRepository.getByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, normalizedXpath);
            } else {
                fragmentEntity = buildFragmentEntityFromFragmentExtracts(anchorEntity, normalizedXpath);
            }
            if (fragmentEntity == null) {
                throw new DataNodeNotFoundException(dataspaceEntity.getName(), anchorEntity.getName(), xpath);
            }
            return fragmentEntity;
        }
    }

    private FragmentEntity buildFragmentEntityFromFragmentExtracts(final AnchorEntity anchorEntity,
                                                                   final String normalizedXpath) {
        final FragmentEntity fragmentEntity;
        final List<FragmentExtract> fragmentExtracts =
                fragmentRepository.findByAnchorIdAndParentXpath(anchorEntity.getId(), normalizedXpath);
        log.debug("Fetched {} fragment entities by anchor {} and cps path {}.",
                fragmentExtracts.size(), anchorEntity.getName(), normalizedXpath);
        fragmentEntity = FragmentEntityArranger.toFragmentEntityTree(anchorEntity, fragmentExtracts);
        return fragmentEntity;
    }

    @Override
    public List<DataNode> queryDataNodes(final String dataspaceName, final String anchorName, final String cpsPath,
                                         final FetchDescendantsOption fetchDescendantsOption) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final CpsPathQuery cpsPathQuery;
        try {
            cpsPathQuery = CpsPathUtil.getCpsPathQuery(cpsPath);
        } catch (final PathParsingException e) {
            throw new CpsPathException(e.getMessage());
        }
        List<FragmentEntity> fragmentEntities =
                fragmentRepository.findByAnchorAndCpsPath(anchorEntity.getId(), cpsPathQuery);
        if (cpsPathQuery.hasAncestorAxis()) {
            final Set<String> ancestorXpaths = processAncestorXpath(fragmentEntities, cpsPathQuery);
            fragmentEntities = ancestorXpaths.isEmpty() ? Collections.emptyList()
                    : fragmentRepository.findAllByAnchorAndXpathIn(anchorEntity, ancestorXpaths);
        }
        return createDataNodesFromFragmentEntities(fetchDescendantsOption, anchorEntity,
                fragmentEntities);
    }

    private List<DataNode> createDataNodesFromFragmentEntities(final FetchDescendantsOption fetchDescendantsOption,
                                                               final AnchorEntity anchorEntity,
                                                               final List<FragmentEntity> fragmentEntities) {
        final List<DataNode> dataNodes = new ArrayList<>(fragmentEntities.size());
        for (final FragmentEntity proxiedFragmentEntity : fragmentEntities) {
            final DataNode dataNode;
            if (FetchDescendantsOption.OMIT_DESCENDANTS.equals(fetchDescendantsOption)) {
                dataNode = toDataNode(proxiedFragmentEntity, fetchDescendantsOption);
            } else {
                final String normalizedXpath = getNormalizedXpath(proxiedFragmentEntity.getXpath());
                final FragmentEntity unproxiedFragmentEntity = buildFragmentEntityFromFragmentExtracts(anchorEntity,
                        normalizedXpath);
                dataNode = toDataNode(unproxiedFragmentEntity, fetchDescendantsOption);
            }
            dataNodes.add(dataNode);
        }
        return Collections.unmodifiableList(dataNodes);
    }

    private static String getNormalizedXpath(final String xpathSource) {
        final String normalizedXpath;
        try {
            normalizedXpath = CpsPathUtil.getNormalizedXpath(xpathSource);
        } catch (final PathParsingException e) {
            throw new CpsPathException(e.getMessage());
        }
        return normalizedXpath;
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

    private static Set<String> processAncestorXpath(final List<FragmentEntity> fragmentEntities,
                                                    final CpsPathQuery cpsPathQuery) {
        final Set<String> ancestorXpath = new HashSet<>();
        final Pattern pattern =
                Pattern.compile("([\\s\\S]*\\/" + Pattern.quote(cpsPathQuery.getAncestorSchemaNodeIdentifier())
                        + REG_EX_FOR_OPTIONAL_LIST_INDEX + "\\/[\\s\\S]*");
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
    public void updateDataLeaves(final String dataspaceName, final String anchorName, final String xpath,
                                 final Map<String, Serializable> leaves) {
        final FragmentEntity fragmentEntity = getFragmentWithoutDescendantsByXpath(dataspaceName, anchorName, xpath);
        fragmentEntity.setAttributes(jsonObjectMapper.asJsonString(leaves));
        fragmentRepository.save(fragmentEntity);
    }

    @Override
    public void updateDataNodeAndDescendants(final String dataspaceName, final String anchorName,
                                             final DataNode dataNode) {
        final FragmentEntity fragmentEntity =
            getFragmentWithoutDescendantsByXpath(dataspaceName, anchorName, dataNode.getXpath());
        updateFragmentEntityAndDescendantsWithDataNode(fragmentEntity, dataNode);
        try {
            fragmentRepository.save(fragmentEntity);
        } catch (final StaleStateException staleStateException) {
            throw new ConcurrencyException("Concurrent Transactions",
                    String.format("dataspace :'%s', Anchor : '%s' and xpath: '%s' is updated by another transaction.",
                            dataspaceName, anchorName, dataNode.getXpath()));
        }
    }

    @Override
    public void updateDataNodesAndDescendants(final String dataspaceName,
                                              final String anchorName,
                                              final List<DataNode> dataNodes) {

        final Map<DataNode, FragmentEntity> dataNodeFragmentEntityMap = dataNodes.stream()
                .collect(Collectors.toMap(
                        dataNode -> dataNode,
                        dataNode ->
                            getFragmentWithoutDescendantsByXpath(dataspaceName, anchorName, dataNode.getXpath())));
        dataNodeFragmentEntityMap.forEach(
                (dataNode, fragmentEntity) -> updateFragmentEntityAndDescendantsWithDataNode(fragmentEntity, dataNode));
        try {
            fragmentRepository.saveAll(dataNodeFragmentEntityMap.values());
        } catch (final StaleStateException staleStateException) {
            retryUpdateDataNodesIndividually(dataspaceName, anchorName, dataNodeFragmentEntityMap.values());
        }
    }

    private void retryUpdateDataNodesIndividually(final String dataspaceName, final String anchorName,
                                                  final Collection<FragmentEntity> fragmentEntities) {
        final Collection<String> failedXpaths = new HashSet<>();

        fragmentEntities.forEach(dataNodeFragment -> {
            try {
                fragmentRepository.save(dataNodeFragment);
            } catch (final StaleStateException e) {
                failedXpaths.add(dataNodeFragment.getXpath());
            }
        });

        if (!failedXpaths.isEmpty()) {
            final String failedXpathsConcatenated = String.join(",", failedXpaths);
            throw new ConcurrencyException("Concurrent Transactions", String.format(
                    "DataNodes : %s in Dataspace :'%s' with Anchor : '%s'  are updated by another transaction.",
                    failedXpathsConcatenated, dataspaceName, anchorName));
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
                childFragment = convertToFragmentWithAllDescendants(
                        existingFragmentEntity.getDataspace(), existingFragmentEntity.getAnchor(), newDataNodeChild);
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
        final FragmentEntity parentEntity =
            getFragmentWithoutDescendantsByXpath(dataspaceName, anchorName, parentNodeXpath);
        final String listElementXpathPrefix = getListElementXpathPrefix(newListElements);
        final Map<String, FragmentEntity> existingListElementFragmentEntitiesByXPath =
                extractListElementFragmentEntitiesByXPath(parentEntity.getChildFragments(), listElementXpathPrefix);
        deleteListElements(parentEntity.getChildFragments(), existingListElementFragmentEntitiesByXPath);
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
                .ifPresent(
                        anchorEntity -> fragmentRepository.deleteByAnchorIn(Set.of(anchorEntity)));
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
               String parentNodeXpath = null;
               String newXpath = null;
               String lastXpathElement = null;
               FragmentEntity parentFragmentEntity = null;
               boolean targetDeleted;
               if (isRootXpath(targetXpath)) {
                   deleteDataNodes(dataspaceName, anchorName);
                       targetDeleted = true;
                     } else {
                            if (isRootContainerNodeXpath(targetXpath)) {
                                parentNodeXpath = targetXpath;
                                lastXpathElement = targetXpath.substring(targetXpath.lastIndexOf('/'));
                                } else {
				if(targetXpath.lastIndexOf('/')>=0 && targetXpath.substring(targetXpath.lastIndexOf('/')).contains("]") && !targetXpath.substring(targetXpath.lastIndexOf('/')).contains("[")) {
                                              if(targetXpath.lastIndexOf('@')>=0 && targetXpath.substring(targetXpath.lastIndexOf('@')).contains("/")) {
                                                   newXpath = targetXpath.replace(targetXpath.substring(targetXpath.lastIndexOf('@')), "");
                                                   parentNodeXpath = newXpath.substring(0, newXpath.lastIndexOf('/'));
                                                   lastXpathElement = newXpath.substring(newXpath.lastIndexOf('/'))+ targetXpath.substring(targetXpath.lastIndexOf('@'));
                                                 }
                                             }else {
                                                   parentNodeXpath = targetXpath.substring(0, targetXpath.lastIndexOf('/'));
                                                   lastXpathElement = targetXpath.substring(targetXpath.lastIndexOf('/'));
                                                   }
                                        }
                                        parentFragmentEntity = getFragmentWithoutDescendantsByXpath(dataspaceName, anchorName, parentNodeXpath);
                                        final boolean isListElement = REG_EX_PATTERN_FOR_LIST_ELEMENT_KEY_PREDICATE.matcher(lastXpathElement).find();
                                        if (isListElement) {
                                             targetDeleted = deleteDataNode(parentFragmentEntity, targetXpath);
                                        } else {
                                targetDeleted = deleteAllListElements(parentFragmentEntity, targetXpath);
                                final boolean tryToDeleteDataNode = !targetDeleted && !onlySupportListNodeDeletion;
                                if (tryToDeleteDataNode) {
                                      targetDeleted = deleteDataNode(parentFragmentEntity, targetXpath);
                                  }
                                }
                       }
                       if (!targetDeleted) {
                      final String additionalInformation = onlySupportListNodeDeletion ? "The target is probably not a List.": "";
                               throw new DataNodeNotFoundException(parentFragmentEntity.getDataspace().getName(),
                               parentFragmentEntity.getAnchor().getName(), targetXpath, additionalInformation);
           }
        }

    private boolean deleteDataNode(final FragmentEntity parentFragmentEntity, final String targetXpath) {
        final String normalizedTargetXpath = CpsPathUtil.getNormalizedXpath(targetXpath);
        if (parentFragmentEntity.getXpath().equals(normalizedTargetXpath)) {
            fragmentRepository.delete(parentFragmentEntity);
            return true;
        }
        if (parentFragmentEntity.getChildFragments()
                .removeIf(fragment -> fragment.getXpath().equals(normalizedTargetXpath))) {
            fragmentRepository.save(parentFragmentEntity);
            return true;
        }
        return false;
    }

    private boolean deleteAllListElements(final FragmentEntity parentFragmentEntity, final String listXpath) {
        final String normalizedListXpath = CpsPathUtil.getNormalizedXpath(listXpath);
        final String deleteTargetXpathPrefix = normalizedListXpath + "[";
        if (parentFragmentEntity.getChildFragments()
                .removeIf(fragment -> fragment.getXpath().startsWith(deleteTargetXpathPrefix))) {
            fragmentRepository.save(parentFragmentEntity);
            return true;
        }
        return false;
    }

    private static void deleteListElements(
            final Collection<FragmentEntity> fragmentEntities,
            final Map<String, FragmentEntity> existingListElementFragmentEntitiesByXPath) {
        fragmentEntities.removeAll(existingListElementFragmentEntitiesByXPath.values());
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
            return convertToFragmentWithAllDescendants(
                    parentEntity.getDataspace(), parentEntity.getAnchor(), newListElement);
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

    private static boolean isRootContainerNodeXpath(final String xpath) {
        return 0 == xpath.lastIndexOf('/');
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
}
