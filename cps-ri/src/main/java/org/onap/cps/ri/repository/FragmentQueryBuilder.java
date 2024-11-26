/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ri.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import org.onap.cps.cpspath.parser.CpsPathPrefixType;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.FragmentEntity;
import org.onap.cps.ri.utils.EscapeUtils;
import org.onap.cps.spi.api.PaginationOption;
import org.onap.cps.spi.api.exceptions.CpsPathException;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FragmentQueryBuilder {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Create a sql query to retrieve by anchor(id) and cps path.
     *
     * @param anchorEntity the anchor
     * @param cpsPathQuery the cps path query to be transformed into a sql query
     * @return a executable query object
     */
    public Query getQueryForAnchorAndCpsPath(final AnchorEntity anchorEntity, final CpsPathQuery cpsPathQuery) {
        final StringBuilder sqlStringBuilder = new StringBuilder();
        final Map<String, Object> queryParameters = new HashMap<>();

        sqlStringBuilder.append("SELECT fragment.* FROM fragment");
        addWhereClauseForAnchor(anchorEntity, sqlStringBuilder, queryParameters);
        addNodeSearchConditions(cpsPathQuery, sqlStringBuilder, queryParameters, false);

        return getQuery(sqlStringBuilder.toString(), queryParameters, FragmentEntity.class);
    }

    /**
     * Create a sql query to retrieve by cps path.
     *
     * @param dataspaceEntity the dataspace
     * @param cpsPathQuery the cps path query to be transformed into a sql query
     * @return a executable query object
     */
    public Query getQueryForDataspaceAndCpsPath(final DataspaceEntity dataspaceEntity,
                                                final CpsPathQuery cpsPathQuery,
                                                final List<Long> anchorIdsForPagination) {
        final StringBuilder sqlStringBuilder = new StringBuilder();
        final Map<String, Object> queryParameters = new HashMap<>();

        sqlStringBuilder.append("SELECT fragment.* FROM fragment");
        if (anchorIdsForPagination.isEmpty()) {
            addWhereClauseForDataspace(dataspaceEntity, sqlStringBuilder, queryParameters);
        } else {
            addWhereClauseForAnchorIds(anchorIdsForPagination, sqlStringBuilder, queryParameters);
        }
        addNodeSearchConditions(cpsPathQuery, sqlStringBuilder, queryParameters, true);

        return getQuery(sqlStringBuilder.toString(), queryParameters, FragmentEntity.class);
    }

    /**
     * Get query for dataspace, cps path, page index and page size.
     * @param dataspaceEntity data space entity
     * @param cpsPathQuery cps path query
     * @param paginationOption pagination option
     * @return query for given dataspace, cps path and pagination parameters
     */
    public Query getQueryForAnchorIdsForPagination(final DataspaceEntity dataspaceEntity,
                                                   final CpsPathQuery cpsPathQuery,
                                                   final PaginationOption paginationOption) {
        final StringBuilder sqlStringBuilder = new StringBuilder();
        final Map<String, Object> queryParameters = new HashMap<>();

        sqlStringBuilder.append("SELECT distinct(fragment.anchor_id) FROM fragment");
        addWhereClauseForDataspace(dataspaceEntity, sqlStringBuilder, queryParameters);
        addNodeSearchConditions(cpsPathQuery, sqlStringBuilder, queryParameters, true);
        sqlStringBuilder.append(" ORDER BY fragment.anchor_id");
        addPaginationCondition(sqlStringBuilder, queryParameters, paginationOption);

        return getQuery(sqlStringBuilder.toString(), queryParameters, Long.class);
    }

    private Query getQuery(final String sql, final Map<String, Object> queryParameters, final Class<?> returnType) {
        final Query query = entityManager.createNativeQuery(sql, returnType);
        setQueryParameters(query, queryParameters);
        return query;
    }

    private static void addWhereClauseForAnchor(final AnchorEntity anchorEntity,
                                                final StringBuilder sqlStringBuilder,
                                                final Map<String, Object> queryParameters) {
        sqlStringBuilder.append(" WHERE anchor_id = :anchorId");
        queryParameters.put("anchorId", anchorEntity.getId());
    }

    private static void addWhereClauseForAnchorIds(final List<Long> anchorIdsForPagination,
                                                   final StringBuilder sqlStringBuilder,
                                                   final Map<String, Object> queryParameters) {
        sqlStringBuilder.append(" WHERE anchor_id IN (:anchorIdsForPagination)");
        queryParameters.put("anchorIdsForPagination", anchorIdsForPagination);
    }

    private static void addWhereClauseForDataspace(final DataspaceEntity dataspaceEntity,
                                                   final StringBuilder sqlStringBuilder,
                                                   final Map<String, Object> queryParameters) {
        sqlStringBuilder.append(" JOIN anchor ON anchor.id = fragment.anchor_id WHERE dataspace_id = :dataspaceId");
        queryParameters.put("dataspaceId", dataspaceEntity.getId());
    }

    private static void addNodeSearchConditions(final CpsPathQuery cpsPathQuery,
                                                final StringBuilder sqlStringBuilder,
                                                final Map<String, Object> queryParameters,
                                                final boolean acrossAnchors) {
        addAbsoluteParentXpathSearchCondition(cpsPathQuery, sqlStringBuilder, queryParameters, acrossAnchors);
        addXpathSearchCondition(cpsPathQuery, sqlStringBuilder, queryParameters);
        addLeafConditions(cpsPathQuery, sqlStringBuilder);
        addTextFunctionCondition(cpsPathQuery, sqlStringBuilder, queryParameters);
        addContainsFunctionCondition(cpsPathQuery, sqlStringBuilder, queryParameters);
    }

    private static void addXpathSearchCondition(final CpsPathQuery cpsPathQuery,
                                                final StringBuilder sqlStringBuilder,
                                                final Map<String, Object> queryParameters) {
        sqlStringBuilder.append(" AND (xpath LIKE :escapedXpath OR "
                + "(xpath LIKE :escapedXpath||'[@%]' AND xpath NOT LIKE :escapedXpath||'[@%]/%[@%]'))");
        if (CpsPathPrefixType.ABSOLUTE.equals(cpsPathQuery.getCpsPathPrefixType())) {
            queryParameters.put("escapedXpath", EscapeUtils.escapeForSqlLike(cpsPathQuery.getXpathPrefix()));
        } else {
            queryParameters.put("escapedXpath", "%/" + EscapeUtils.escapeForSqlLike(cpsPathQuery.getDescendantName()));
        }
    }

    private static void addAbsoluteParentXpathSearchCondition(final CpsPathQuery cpsPathQuery,
                                                              final StringBuilder sqlStringBuilder,
                                                              final Map<String, Object> queryParameters,
                                                              final boolean acrossAnchors) {
        if (CpsPathPrefixType.ABSOLUTE.equals(cpsPathQuery.getCpsPathPrefixType())) {
            if (cpsPathQuery.getNormalizedParentPath().isEmpty()) {
                sqlStringBuilder.append(" AND parent_id IS NULL");
            } else {
                if (acrossAnchors) {
                    sqlStringBuilder.append(" AND parent_id IN (SELECT id FROM fragment WHERE xpath = :parentXpath)");
                } else {
                    sqlStringBuilder.append(" AND parent_id = (SELECT id FROM fragment WHERE xpath = :parentXpath"
                            + " AND anchor_id = :anchorId)");
                }
                queryParameters.put("parentXpath", cpsPathQuery.getNormalizedParentPath());
            }
        }
    }

    private static void addPaginationCondition(final StringBuilder sqlStringBuilder,
                                               final Map<String, Object> queryParameters,
                                               final PaginationOption paginationOption) {
        if (PaginationOption.NO_PAGINATION != paginationOption) {
            final Integer offset = (paginationOption.getPageIndex() - 1) * paginationOption.getPageSize();
            sqlStringBuilder.append(" LIMIT :pageSize OFFSET :offset");
            queryParameters.put("pageSize", paginationOption.getPageSize());
            queryParameters.put("offset", offset);
        }
    }

    private static Integer getTextValueAsInt(final CpsPathQuery cpsPathQuery) {
        try {
            return Integer.parseInt(cpsPathQuery.getTextFunctionConditionValue());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static void addLeafConditions(final CpsPathQuery cpsPathQuery, final StringBuilder sqlStringBuilder) {
        if (cpsPathQuery.hasLeafConditions()) {
            sqlStringBuilder.append(" AND (");
            final Queue<String> booleanOperatorsQueue = new LinkedList<>(cpsPathQuery.getBooleanOperators());
            cpsPathQuery.getLeafConditions().forEach(leafCondition -> {
                if (leafCondition.value() instanceof Integer) {
                    sqlStringBuilder.append("(attributes ->> '").append(leafCondition.name()).append("')\\:\\:int");
                    sqlStringBuilder.append(leafCondition.operator());
                    sqlStringBuilder.append(leafCondition.value());
                } else {
                    if ("=".equals(leafCondition.operator())) {
                        final String leafValueAsText = leafCondition.value().toString();
                        sqlStringBuilder.append("attributes ->> '").append(leafCondition.name()).append("'");
                        sqlStringBuilder.append(" = '");
                        sqlStringBuilder.append(EscapeUtils.escapeForSqlStringLiteral(leafValueAsText));
                        sqlStringBuilder.append("'");
                    } else {
                        throw new CpsPathException(" can use only " + leafCondition.operator() + " with integer ");
                    }
                }
                if (!booleanOperatorsQueue.isEmpty()) {
                    sqlStringBuilder.append(" ");
                    sqlStringBuilder.append(booleanOperatorsQueue.poll());
                    sqlStringBuilder.append(" ");
                }
            });
            sqlStringBuilder.append(")");
        }
    }

    private static void addTextFunctionCondition(final CpsPathQuery cpsPathQuery,
                                                 final StringBuilder sqlStringBuilder,
                                                 final Map<String, Object> queryParameters) {
        if (cpsPathQuery.hasTextFunctionCondition()) {
            sqlStringBuilder.append(" AND (");
            sqlStringBuilder.append("attributes @> jsonb_build_object(:textLeafName, :textValue)");
            sqlStringBuilder
                .append(" OR attributes @> jsonb_build_object(:textLeafName, json_build_array(:textValue))");
            queryParameters.put("textLeafName", cpsPathQuery.getTextFunctionConditionLeafName());
            queryParameters.put("textValue", cpsPathQuery.getTextFunctionConditionValue());
            final Integer textValueAsInt = getTextValueAsInt(cpsPathQuery);
            if (textValueAsInt != null) {
                sqlStringBuilder.append(" OR attributes @> jsonb_build_object(:textLeafName, :textValueAsInt)");
                sqlStringBuilder
                    .append(" OR attributes @> jsonb_build_object(:textLeafName, json_build_array(:textValueAsInt))");
                queryParameters.put("textValueAsInt", textValueAsInt);
            }
            sqlStringBuilder.append(")");
        }
    }

    private static void addContainsFunctionCondition(final CpsPathQuery cpsPathQuery,
                                                     final StringBuilder sqlStringBuilder,
                                                     final Map<String, Object> queryParameters) {
        if (cpsPathQuery.hasContainsFunctionCondition()) {
            sqlStringBuilder.append(" AND attributes ->> :containsLeafName LIKE CONCAT('%',:containsValue,'%') ");
            queryParameters.put("containsLeafName", cpsPathQuery.getContainsFunctionConditionLeafName());
            queryParameters.put("containsValue",
                    EscapeUtils.escapeForSqlLike(cpsPathQuery.getContainsFunctionConditionValue()));
        }
    }

    private static void setQueryParameters(final Query query, final Map<String, Object> queryParameters) {
        for (final Map.Entry<String, Object> queryParameter : queryParameters.entrySet()) {
            query.setParameter(queryParameter.getKey(), queryParameter.getValue());
        }
    }

}
