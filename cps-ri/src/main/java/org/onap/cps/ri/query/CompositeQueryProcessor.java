/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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

package org.onap.cps.ri.query;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.model.CompositeQuery;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.impl.DataNodeBuilder;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.FragmentEntity;
import org.onap.cps.ri.repository.FragmentRepository;
import org.onap.cps.ri.repository.FragmentRepositoryCpsPathQuery;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompositeQueryProcessor {

    @Qualifier("fragmentRepositoryCpsPathQueryImpl")
    private final FragmentRepositoryCpsPathQuery fragmentRepositoryCpsPathQuery;
    private final FragmentRepository fragmentRepository;
    private final JsonObjectMapper jsonObjectMapper;


    /**
     * Processes a composite query by executing the base cpsPath query, applying nested conditions
     * using the configured operator logic, and converting matching fragments to DataNodes.
     *
     * @param anchor                 the anchor entity to query against
     * @param compositeQuery         the composite query containing cpsPath, operator and conditions
     * @param fetchDescendantsOption option controlling how many levels of descendants to include
     * @return                       collection of DataNodes matching the composite query
     */
    public Collection<DataNode> processCompositeQuery(final AnchorEntity anchor,
                                                      final CompositeQuery compositeQuery,
                                                      final FetchDescendantsOption fetchDescendantsOption) {
        final CpsPathQuery baseCpsPath = CpsPathUtil.getCpsPathQuery(compositeQuery.getCpsPath());
        final List<FragmentEntity> baseFragments = fragmentRepositoryCpsPathQuery
            .findByAnchorAndCpsPath(anchor, baseCpsPath, 0);

        if (baseFragments.isEmpty()) {
            return Collections.emptyList();
        }

        final Collection<FragmentEntity> baseFragmentsWithDescendants =
            fragmentRepository.prefetchDescendantsOfFragmentEntities(fetchDescendantsOption, baseFragments);

        final Map<Long, FragmentEntity> flatFragmentEntities = flattenFragmentHierarchy(baseFragmentsWithDescendants);

        final Set<Long> matchingIds = applyConditionsOnFlatMap(compositeQuery, flatFragmentEntities, anchor);

        final Set<Long> ancestorIds = findAncestorIds(matchingIds, flatFragmentEntities);

        return convertToDataNodes(matchingIds, ancestorIds, baseFragmentsWithDescendants, fetchDescendantsOption);
    }

    private Set<Long> findAncestorIds(final Set<Long> matchingIds,
                                      final Map<Long, FragmentEntity> idsToFragmentEntitiesMap) {
        final Set<Long> ancestorIds = new HashSet<>();
        for (final Long id : matchingIds) {
            final Set<Long> visited = new HashSet<>();
            FragmentEntity fragmentEntity = idsToFragmentEntitiesMap.get(id);
            while (fragmentEntity != null && fragmentEntity.getParentId() != null
                   && visited.add(fragmentEntity.getId())) {
                if (!matchingIds.contains(fragmentEntity.getParentId())) {
                    ancestorIds.add(fragmentEntity.getParentId());
                }
                fragmentEntity = idsToFragmentEntitiesMap.get(fragmentEntity.getParentId());
            }
        }
        return ancestorIds;
    }

    private Map<Long, FragmentEntity> flattenFragmentHierarchy(final Collection<FragmentEntity> fragmentEntities) {
        final Map<Long, FragmentEntity> fragmentIdToFragmentEntities = new HashMap<>();
        fragmentEntities.forEach(fragmentEntity -> addFragmentAndDescendantsToMap(fragmentEntity,
            fragmentIdToFragmentEntities));
        return fragmentIdToFragmentEntities;
    }

    private void addFragmentAndDescendantsToMap(final FragmentEntity fragmentEntity,
                                                final Map<Long, FragmentEntity> fragmentIdToFragmentEntities) {
        fragmentIdToFragmentEntities.put(fragmentEntity.getId(), fragmentEntity);
        if (fragmentEntity.getChildFragments() != null) {
            fragmentEntity.getChildFragments().forEach(child -> addFragmentAndDescendantsToMap(child,
                fragmentIdToFragmentEntities));
        }
    }

    private Set<Long> applyConditionsOnFlatMap(final CompositeQuery compositeQuery,
                                               final Map<Long, FragmentEntity> flattenFragmentEntities,
                                               final AnchorEntity anchor) {
        if (compositeQuery.getConditions() == null || compositeQuery.getConditions().isEmpty()) {
            return new HashSet<>(flattenFragmentEntities.keySet());
        }

        final CompositeQueryOperators operator = CompositeQueryOperators.fromString(compositeQuery.getOperator());

        return operator.evaluateOperator(
            compositeQuery.getConditions(),
            flattenFragmentEntities,
            anchor,
            this::evaluateCondition
        );
    }

    private Set<Long> evaluateCondition(final CompositeQuery compositeQuery,
                               final Map<Long, FragmentEntity> flattenFragmentEntities,
                               final AnchorEntity anchor) {
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(compositeQuery.getCpsPath());

        final List<FragmentEntity> fragmentEntities = fragmentRepositoryCpsPathQuery
            .findByAnchorAndCpsPath(anchor, cpsPathQuery, 0);

        final Set<Long> fragmentIds = fragmentEntities.stream()
            .map(FragmentEntity::getId)
            .filter(flattenFragmentEntities::containsKey)
            .collect(Collectors.toSet());

        if (compositeQuery.getConditions() != null && !compositeQuery.getConditions().isEmpty()) {
            final Map<Long, FragmentEntity> scopedFragments =
                getScopedFragmentsFromFlatMap(fragmentIds, flattenFragmentEntities);

            final Set<Long> nestedMatches = applyConditionsOnFlatMap(compositeQuery, scopedFragments, anchor);

            if (nestedMatches.isEmpty()) {
                final CompositeQueryOperators nestedOperator =
                    CompositeQueryOperators.fromString(compositeQuery.getOperator());
                return nestedOperator.requiresAllConditions() ? Collections.emptySet() : fragmentIds;
            }

            final Set<Long> result = new HashSet<>(fragmentIds);
            result.addAll(nestedMatches);
            return result;
        }
        return fragmentIds;
    }

    private Collection<DataNode> convertToDataNodes(final Set<Long> matchingIds,
                                           final Set<Long> ancestorIds,
                                           final Collection<FragmentEntity> rootFragments,
                                           final FetchDescendantsOption fetchDescendantsOption) {
        if (matchingIds.isEmpty()) {
            return Collections.emptyList();
        }

        final Set<Long> allRelevantIds = combineIds(matchingIds, ancestorIds);

        return rootFragments.stream()
            .filter(root -> allRelevantIds.contains(root.getId()) || hasMatchingDescendant(root, allRelevantIds))
            .map(root -> toDataNodeWithDescendants(root, fetchDescendantsOption, matchingIds, allRelevantIds))
            .toList();
    }

    private boolean hasMatchingDescendant(final FragmentEntity fragment, final Set<Long> relevantIds) {
        final var children = fragment.getChildFragments();
        if (children == null || children.isEmpty()) {
            return false;
        }
        return children.stream()
            .anyMatch(child -> relevantIds.contains(child.getId())
                || hasMatchingDescendant(child, relevantIds));
    }

    private DataNode toDataNodeWithDescendants(final FragmentEntity fragmentEntity,
                                               final FetchDescendantsOption fetchDescendantsOption,
                                               final Set<Long> matchingIds,
                                               final Set<Long> allRelevantIds) {
        final List<DataNode> childDataNodes = getFilteredChildDataNodes(
            fragmentEntity, fetchDescendantsOption, matchingIds, allRelevantIds);

        return buildDataNode(fragmentEntity, childDataNodes);
    }

    private List<DataNode> getFilteredChildDataNodes(final FragmentEntity fragmentEntity,
                                                     final FetchDescendantsOption fetchDescendantsOption,
                                                     final Set<Long> matchingIds,
                                                     final Set<Long> allRelevantIds) {
        if (!fetchDescendantsOption.hasNext() || fragmentEntity.getChildFragments() == null) {
            return Collections.emptyList();
        }

        if (matchingIds.contains(fragmentEntity.getId())) {
            final boolean hasMatchedDescendants = fragmentEntity.getChildFragments().stream()
                .anyMatch(child -> matchingIds.contains(child.getId())
                    || hasMatchingDescendant(child, matchingIds));

            if (!hasMatchedDescendants) {
                return fragmentEntity.getChildFragments().stream()
                    .map(child -> toDataNodeWithAllDescendants(child, fetchDescendantsOption.next()))
                    .toList();
            }
        }

        return fragmentEntity.getChildFragments().stream()
            .filter(child -> allRelevantIds.contains(child.getId())
                || hasMatchingDescendant(child, allRelevantIds))
            .map(child -> toDataNodeWithDescendants(child, fetchDescendantsOption.next(), matchingIds, allRelevantIds))
            .toList();
    }

    private DataNode toDataNodeWithAllDescendants(final FragmentEntity fragmentEntity,
                                                  final FetchDescendantsOption fetchDescendantsOption) {
        final List<DataNode> childDataNodes = getAllChildDataNodes(fragmentEntity, fetchDescendantsOption);
        return buildDataNode(fragmentEntity, childDataNodes);
    }

    private List<DataNode> getAllChildDataNodes(final FragmentEntity fragmentEntity,
                                                final FetchDescendantsOption fetchDescendantsOption) {
        if (!fetchDescendantsOption.hasNext() || fragmentEntity.getChildFragments() == null) {
            return Collections.emptyList();
        }
        return fragmentEntity.getChildFragments().stream()
            .map(child -> toDataNodeWithAllDescendants(child, fetchDescendantsOption.next()))
            .toList();
    }

    private Set<Long> combineIds(final Set<Long> matchingIds, final Set<Long> ancestorIds) {
        final Set<Long> combined = new HashSet<>(matchingIds);
        combined.addAll(ancestorIds);
        return combined;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Serializable> parseLeaves(final FragmentEntity fragmentEntity) {
        return fragmentEntity.getAttributes() != null
            ? jsonObjectMapper.convertJsonString(fragmentEntity.getAttributes(), Map.class)
            : Collections.emptyMap();
    }

    private DataNode buildDataNode(final FragmentEntity fragmentEntity, final List<DataNode> childDataNodes) {
        return new DataNodeBuilder()
            .withXpath(fragmentEntity.getXpath())
            .withLeaves(parseLeaves(fragmentEntity))
            .withDataspace(fragmentEntity.getAnchor().getDataspace().getName())
            .withAnchor(fragmentEntity.getAnchor().getName())
            .withChildDataNodes(childDataNodes)
            .build();
    }

    private Map<Long, FragmentEntity> getScopedFragmentsFromFlatMap(final Set<Long> parentIds,
                                                            final Map<Long, FragmentEntity> flattenFragmentEntities) {
        final Set<Long> descendantIds = new HashSet<>();
        for (final Long parentId : parentIds) {
            final FragmentEntity parentFragment = flattenFragmentEntities.get(parentId);
            if (parentFragment != null) {
                collectDescendantsFromFragment(parentFragment, descendantIds);
            }
        }
        return flattenFragmentEntities.entrySet().stream()
            .filter(entry -> descendantIds.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void collectDescendantsFromFragment(final FragmentEntity fragmentEntity, final Set<Long> childFragmentIds) {
        if (fragmentEntity.getChildFragments() == null) {
            return;
        }
        for (final FragmentEntity childFragment : fragmentEntity.getChildFragments()) {
            if (childFragmentIds.add(childFragment.getId())) {
                collectDescendantsFromFragment(childFragment, childFragmentIds);
            }
        }
    }
}