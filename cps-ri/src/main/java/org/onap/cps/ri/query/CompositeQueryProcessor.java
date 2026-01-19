package org.onap.cps.ri.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.CompositeQuery;
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


    public Collection<DataNode> processCompositeQuery(final AnchorEntity anchor,
                                                      final CompositeQuery searchQuery,
                                                      final FetchDescendantsOption fetchDescendantsOption) {
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(searchQuery.getCpsPath());
        final List<FragmentEntity> baseFragments = fragmentRepositoryCpsPathQuery
            .findByAnchorAndCpsPath(anchor, cpsPathQuery, 0);

        if (baseFragments.isEmpty()) {
            return Collections.emptyList();
        }

        final Collection<FragmentEntity> rootFragmentsWithDescendants =
            fragmentRepository.prefetchDescendantsOfFragmentEntities(fetchDescendantsOption, baseFragments);

        final Map<Long, FragmentEntity> flatFragmentEntities = flattenFragmentHierarchy(rootFragmentsWithDescendants);

        final Set<Long> matchingIds = applyConditionsOnFlatMap(searchQuery, flatFragmentEntities, anchor);

        final Set<Long> ancestorIds = findAncestorIds(matchingIds, flatFragmentEntities);

        return convertToDataNodes(matchingIds, ancestorIds, rootFragmentsWithDescendants, fetchDescendantsOption);
    }

    private Set<Long> findAncestorIds(final Set<Long> matchingIds,
                                      final Map<Long, FragmentEntity> idsToFragmentEntitiesMap) {
        final Set<Long> ancestorIds = new HashSet<>();
        for (final Long id : matchingIds) {
            FragmentEntity fragmentEntity = idsToFragmentEntitiesMap.get(id);
            while (fragmentEntity != null && fragmentEntity.getParentId() != null) {
                if (!matchingIds.contains(fragmentEntity.getParentId())) {
                    ancestorIds.add(fragmentEntity.getParentId());
                }
                fragmentEntity = idsToFragmentEntitiesMap.get(fragmentEntity.getParentId());
            }
        }
        return ancestorIds;
    }

    private Map<Long, FragmentEntity> flattenFragmentHierarchy(final Collection<FragmentEntity> fragmentEntities) {
        final Map<Long, FragmentEntity> flatMap = new HashMap<>();
        fragmentEntities.forEach(fragmentEntity -> addFragmentAndDescendantsToMap(fragmentEntity, flatMap));
        return flatMap;
    }

    private void addFragmentAndDescendantsToMap(final FragmentEntity fragment,
                                                final Map<Long, FragmentEntity> flatMap) {
        flatMap.put(fragment.getId(), fragment);
        if (fragment.getChildFragments() != null) {
            fragment.getChildFragments().forEach(child -> addFragmentAndDescendantsToMap(child, flatMap));
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

        final Set<Long> allRelevantIds = new HashSet<>(matchingIds);
        allRelevantIds.addAll(ancestorIds);

        final List<FragmentEntity> relevantRootFragments = rootFragments.stream()
            .filter(root ->
                allRelevantIds.contains(root.getId()) || hasMatchingDescendant(root, allRelevantIds)).toList();


        final List<DataNode> result = new ArrayList<>();
        for (final FragmentEntity rootFragment : relevantRootFragments) {
            result.add(toDataNodeWithDescendants(rootFragment, fetchDescendantsOption, matchingIds, ancestorIds));
        }
        return Collections.unmodifiableList(result);
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


    @SuppressWarnings("unchecked")
    private DataNode toDataNodeWithDescendants(final FragmentEntity fragmentEntity,
                                               final FetchDescendantsOption fetchDescendantsOption,
                                               final Set<Long> matchingIds,
                                               final Set<Long> ancestorIds) {
        final Set<Long> allRelevantIds = new HashSet<>(matchingIds);
        allRelevantIds.addAll(ancestorIds);
        final List<DataNode> childDataNodes = getFilteredChildDataNodes(
            fragmentEntity, fetchDescendantsOption, matchingIds, ancestorIds, allRelevantIds);

        final Map<String, Serializable> leaves = new HashMap<>();
        if (fragmentEntity.getAttributes() != null) {
            leaves.putAll(jsonObjectMapper.convertJsonString(fragmentEntity.getAttributes(), Map.class));
        }
        return new DataNodeBuilder()
            .withXpath(fragmentEntity.getXpath())
            .withLeaves(leaves)
            .withDataspace(fragmentEntity.getAnchor().getDataspace().getName())
            .withAnchor(fragmentEntity.getAnchor().getName())
            .withChildDataNodes(childDataNodes)
            .build();
    }

    private List<DataNode> getFilteredChildDataNodes(final FragmentEntity fragmentEntity,
                                                     final FetchDescendantsOption fetchDescendantsOption,
                                                     final Set<Long> matchingIds,
                                                     final Set<Long> ancestorIds,
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
            .map(child -> toDataNodeWithDescendants(child, fetchDescendantsOption.next(), matchingIds, ancestorIds))
            .toList();
    }

    @SuppressWarnings("unchecked")
    private DataNode toDataNodeWithAllDescendants(final FragmentEntity fragmentEntity,
                                                  final FetchDescendantsOption fetchDescendantsOption) {
        final List<DataNode> childDataNodes = getAllChildDataNodes(fragmentEntity, fetchDescendantsOption);

        Map<String, Serializable> leaves = new HashMap<>();
        if (fragmentEntity.getAttributes() != null) {
            leaves = jsonObjectMapper.convertJsonString(fragmentEntity.getAttributes(), Map.class);
        }
        return new DataNodeBuilder()
            .withXpath(fragmentEntity.getXpath())
            .withLeaves(leaves)
            .withDataspace(fragmentEntity.getAnchor().getDataspace().getName())
            .withAnchor(fragmentEntity.getAnchor().getName())
            .withChildDataNodes(childDataNodes)
            .build();
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