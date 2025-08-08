/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ri.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.parameters.PaginationOption;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.query.parser.QuerySelectWhere;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.FragmentEntity;
import org.onap.cps.utils.JsonObjectMapper;

@RequiredArgsConstructor
@Slf4j
public class FragmentRepositoryCpsPathQueryImpl implements FragmentRepositoryCpsPathQuery {

    private final FragmentQueryBuilder fragmentQueryBuilder;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    @Transactional
    public List<FragmentEntity> findByAnchorAndCpsPath(final AnchorEntity anchorEntity,
                                                       final CpsPathQuery cpsPathQuery,
                                                       final int queryResultLimit) {
        final Query query = fragmentQueryBuilder.getQueryForAnchorAndCpsPath(anchorEntity, cpsPathQuery,
                queryResultLimit);
        final List<FragmentEntity> fragmentEntities = query.getResultList();
        log.debug("Fetched {} fragment entities by anchor and cps path.", fragmentEntities.size());
        if (queryResultLimit > 0) {
            log.debug("Result limited to {} entries", queryResultLimit);
        }
        return fragmentEntities;
    }

    @Override
    @Transactional
    public <T> Set<T> findAttributeValuesByAnchorAndCpsPath(final AnchorEntity anchorEntity,
                                                            final CpsPathQuery cpsPathQuery,
                                                            final String attributeName,
                                                            final int queryResultLimit,
                                                            final Class<T> targetClass) {
        final Query query = fragmentQueryBuilder.getQueryForAnchorAndCpsPath(anchorEntity, cpsPathQuery,
                queryResultLimit);
        final List<String> jsonResultList = query.getResultList();
        return jsonResultList.stream()
                .map(jsonValue -> jsonObjectMapper.convertJsonString(jsonValue, targetClass))
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public List<FragmentEntity> findByDataspaceAndCpsPath(final DataspaceEntity dataspaceEntity,
                                                          final CpsPathQuery cpsPathQuery, final List<Long> anchorIds) {
        final Query query = fragmentQueryBuilder.getQueryForDataspaceAndCpsPath(
                dataspaceEntity, cpsPathQuery, anchorIds);
        final List<FragmentEntity> fragmentEntities = query.getResultList();
        log.debug("Fetched {} fragment entities by cps path across all anchors.", fragmentEntities.size());
        return fragmentEntities;
    }

    @Override
    @Transactional
    public List<Long> findAnchorIdsForPagination(final DataspaceEntity dataspaceEntity, final CpsPathQuery cpsPathQuery,
                                                 final PaginationOption paginationOption) {
        final Query query = fragmentQueryBuilder.getQueryForAnchorIdsForPagination(
                dataspaceEntity, cpsPathQuery, paginationOption);
        return query.getResultList();
    }

    @Override
    public List<Map<String, Object>> findCustomNodes(final Long id, final String xpath,
                                                     final List<String> selectFields, final String whereConditions,
                                                     final QuerySelectWhere querySelectWhere) {
        final Query query =  fragmentQueryBuilder.getCustomNodesQuery(id, xpath,
                selectFields, whereConditions, querySelectWhere);

        final List<Object> results = query.getResultList();

        final List<Map<String, Object>> response = new ArrayList<>();
        for (final Object result : results) {
            final ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> jsonb = null;
            try {
                jsonb = objectMapper.readValue(result.toString(), Map.class);
            } catch (final JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            final Map<String, Object> filtered = new HashMap<>();
            for (final String field : querySelectWhere.getSelectFields()) {
                if (jsonb.containsKey(field)) {
                    filtered.put(field, jsonb.get(field));
                }
            }
            response.add(filtered);
        }

        return response;

    }

}
