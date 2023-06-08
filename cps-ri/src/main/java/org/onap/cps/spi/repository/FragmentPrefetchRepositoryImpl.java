/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.repository;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FragmentPrefetchRepositoryImpl implements FragmentPrefetchRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<FragmentEntity> prefetchDescendantsForFragmentEntities(
            final FetchDescendantsOption fetchDescendantsOption,
            final Collection<FragmentEntity> proxiedFragmentEntities) {

        if (FetchDescendantsOption.OMIT_DESCENDANTS.equals(fetchDescendantsOption)) {
            return proxiedFragmentEntities;
        }

        final List<Long> fragmentEntityIds = proxiedFragmentEntities.stream()
                .map(FragmentEntity::getId).collect(Collectors.toList());

        final Map<Long, AnchorEntity> anchorEntityPerId = proxiedFragmentEntities.stream()
                .map(FragmentEntity::getAnchor)
                .collect(Collectors.toMap(AnchorEntity::getId, Function.identity(), (a1, a2) -> a1));

        final int maxDepth = fetchDescendantsOption.equals(FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
                ? Integer.MAX_VALUE
                : fetchDescendantsOption.getDepth();
        return findAllWithDescendantsByIds(fragmentEntityIds, anchorEntityPerId, maxDepth);
    }

    private Collection<FragmentEntity> findAllWithDescendantsByIds(final Collection<Long> ids,
                                                                   final Map<Long, AnchorEntity> anchorEntityPerId,
                                                                   final int maxDepth) {
        final String sql = "WITH RECURSIVE parent_search AS ("
                + "  SELECT id, 0 AS depth "
                + "    FROM fragment "
                + "   WHERE id = ANY (?) "
                + "   UNION "
                + "  SELECT c.id, depth + 1 "
                + "    FROM fragment c INNER JOIN parent_search p ON c.parent_id = p.id"
                + "   WHERE depth <= ?"
                + ") "
                + "SELECT f.id, anchor_id AS anchorId, xpath, f.parent_id AS parentId, "
                + "       CAST(attributes AS TEXT) AS attributes "
                + "FROM fragment f INNER JOIN parent_search p ON f.id = p.id";

        final PreparedStatementSetter preparedStatementSetter = preparedStatement -> {
            final Connection connection = preparedStatement.getConnection();
            final java.sql.Array idArray = connection.createArrayOf("bigint", ids.toArray());
            preparedStatement.setArray(1, idArray);
            preparedStatement.setInt(2, maxDepth);
        };

        final RowMapper<FragmentEntity> fragmentEntityRowMapper = (resultSet, rowNum) -> {
            final FragmentEntity fragmentEntity = new FragmentEntity();
            fragmentEntity.setId(resultSet.getLong("id"));
            fragmentEntity.setXpath(resultSet.getString("xpath"));
            fragmentEntity.setParentId(resultSet.getLong("parentId"));
            fragmentEntity.setAttributes(resultSet.getString("attributes"));
            fragmentEntity.setAnchor(anchorEntityPerId.get(resultSet.getLong("anchorId")));
            fragmentEntity.setChildFragments(new HashSet<>());
            return fragmentEntity;
        };

        final Map<Long, FragmentEntity> fragmentEntityPerId;
        try (final Stream<FragmentEntity> fragmentEntityStream = jdbcTemplate.queryForStream(sql,
                preparedStatementSetter, fragmentEntityRowMapper)) {
            fragmentEntityPerId = fragmentEntityStream.collect(
                    Collectors.toMap(FragmentEntity::getId, Function.identity()));
        }
        return reuniteChildrenWithTheirParents(fragmentEntityPerId);
    }

    private static Collection<FragmentEntity> reuniteChildrenWithTheirParents(
            final Map<Long, FragmentEntity> fragmentEntityPerId) {
        final Collection<FragmentEntity> fragmentEntitiesWithoutParentInResultSet = new HashSet<>();
        for (final FragmentEntity fragmentEntity : fragmentEntityPerId.values()) {
            final FragmentEntity parentFragmentEntity = fragmentEntityPerId.get(fragmentEntity.getParentId());
            if (parentFragmentEntity == null) {
                fragmentEntitiesWithoutParentInResultSet.add(fragmentEntity);
            } else {
                parentFragmentEntity.getChildFragments().add(fragmentEntity);
            }
        }
        return fragmentEntitiesWithoutParentInResultSet;
    }

}
