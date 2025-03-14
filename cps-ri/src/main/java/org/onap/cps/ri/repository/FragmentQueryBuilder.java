/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 Nordix Foundation
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
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.onap.cps.api.exceptions.CpsPathException;
import org.onap.cps.cpspath.parser.CpsPathPrefixType;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.FragmentEntity;
import org.onap.cps.ri.utils.EscapeUtils;

@RequiredArgsConstructor
public class FragmentQueryBuilder {
    private static final String DESCENDANT_PATH = "//";

    private final EntityManager entityManager;

    private final StringBuilder sqlStringBuilder = new StringBuilder();
    private final Map<String, Object> queryParameters = new HashMap<>();

    /**
     * Create a sql query to retrieve by anchor and cps path.
     *
     * @param anchorEntity the anchor
     * @param cpsPathQuery the cps path query to be transformed into a sql query
     * @return a executable query object
     */
    public Query getQueryForAnchorAndCpsPath(final AnchorEntity anchorEntity,
                                             final CpsPathQuery cpsPathQuery) {
        addSearchPrefix(cpsPathQuery);
        addWhereClauseForAnchor(anchorEntity);
        addNodeSearchConditions(cpsPathQuery, false);
        addSearchSuffix(cpsPathQuery);
        return getQuery(FragmentEntity.class);
    }

    /**
     * Create a sql query to retrieve by dataspace and cps path.
     *
     * @param dataspaceEntity the dataspace
     * @param cpsPathQuery the cps path query to be transformed into a sql query
     * @return a executable query object
     */
    public Query getQueryForDataspaceAndCpsPath(final DataspaceEntity dataspaceEntity,
                                                final CpsPathQuery cpsPathQuery) {
        addSearchPrefix(cpsPathQuery);
        addWhereClauseForDataspace(dataspaceEntity);
        addNodeSearchConditions(cpsPathQuery, true);
        addSearchSuffix(cpsPathQuery);
        return getQuery(FragmentEntity.class);
    }

    /**
     * Create a sql query to retrieve by anchors and cps path.
     *
     * @param anchorIds    IDs of anchors to search
     * @param cpsPathQuery the cps path query to be transformed into a sql query
     * @return a executable query object
     */
    public Query getQueryForAnchorIdsAndCpsPath(final List<Long> anchorIds, final CpsPathQuery cpsPathQuery) {
        addSearchPrefix(cpsPathQuery);
        addWhereClauseForAnchorIds(anchorIds);
        addNodeSearchConditions(cpsPathQuery, true);
        addSearchSuffix(cpsPathQuery);
        return getQuery(FragmentEntity.class);
    }

    /**
     * Get query for dataspace and cps path, returning anchor ids.
     * @param dataspaceEntity data space entity
     * @param cpsPathQuery cps path query
     * @return query for given dataspace, cps path and pagination parameters
     */
    public Query getQueryForAnchorIdsForPagination(final DataspaceEntity dataspaceEntity,
                                                   final CpsPathQuery cpsPathQuery) {
        sqlStringBuilder.append("SELECT distinct(fragment.anchor_id) FROM fragment");
        addWhereClauseForDataspace(dataspaceEntity);
        addNodeSearchConditions(cpsPathQuery, true);
        sqlStringBuilder.append(" ORDER BY fragment.anchor_id");
        return getQuery(Long.class);
    }

    private Query getQuery(final Class<?> returnType) {
        final Query query = entityManager.createNativeQuery(sqlStringBuilder.toString(), returnType);
        queryParameters.forEach(query::setParameter);
        return query;
    }

    private void addWhereClauseForAnchor(final AnchorEntity anchorEntity) {
        sqlStringBuilder.append(" WHERE anchor_id = :anchorId");
        queryParameters.put("anchorId", anchorEntity.getId());
    }

    private void addWhereClauseForAnchorIds(final List<Long> anchorIdsForPagination) {
        sqlStringBuilder.append(" WHERE anchor_id IN (:anchorIdsForPagination)");
        queryParameters.put("anchorIdsForPagination", anchorIdsForPagination);
    }

    private void addWhereClauseForDataspace(final DataspaceEntity dataspaceEntity) {
        sqlStringBuilder.append(" JOIN anchor ON anchor.id = fragment.anchor_id WHERE dataspace_id = :dataspaceId");
        queryParameters.put("dataspaceId", dataspaceEntity.getId());
    }

    private void addNodeSearchConditions(final CpsPathQuery cpsPathQuery, final boolean acrossAnchors) {
        addAbsoluteParentXpathSearchCondition(cpsPathQuery, acrossAnchors);
        sqlStringBuilder.append(" AND ");
        addXpathSearchCondition(cpsPathQuery, "baseXpath");
        addLeafConditions(cpsPathQuery);
        addTextFunctionCondition(cpsPathQuery);
        addContainsFunctionCondition(cpsPathQuery);
    }

    private void addXpathSearchCondition(final CpsPathQuery cpsPathQuery, final String parameterName) {
        queryParameters.put(parameterName, escapeXpathForSqlLike(cpsPathQuery));
        final String sqlForXpathLikeContainerOrList = """
                (
                  (xpath LIKE :${xpathParamName})
                  OR
                  (xpath LIKE :${xpathParamName}||'[@%]' AND xpath NOT LIKE :${xpathParamName}||'[@%]/%[@%]')
                )
                """;
        sqlStringBuilder.append(substitute(sqlForXpathLikeContainerOrList, Map.of("xpathParamName", parameterName)));
    }

    /**
     * Returns a pattern suitable for use in an SQL LIKE expression, matching the xpath (absolute or descendant).
     * For an absolute path such as "/bookstore/categories[@name='10% off']",
     *  the output would be "/bookstore/categories[@name='10\% off']".
     * For a descendant path such as "//categories[@name='10% off']",
     *  the output would be "%/categories[@name='10\% off']".
     * Note: percent sign '%' means match anything in SQL LIKE, while underscore '_' means match any single character.
     *
     * @param cpsPathQuery Cps Path Query
     * @return a pattern suitable for use in an SQL LIKE expression.
     */
    private static String escapeXpathForSqlLike(final CpsPathQuery cpsPathQuery) {
        if (CpsPathPrefixType.ABSOLUTE.equals(cpsPathQuery.getCpsPathPrefixType())) {
            return EscapeUtils.escapeForSqlLike(cpsPathQuery.getXpathPrefix());
        } else {
            return "%/" + EscapeUtils.escapeForSqlLike(cpsPathQuery.getDescendantName());
        }
    }

    private void addAbsoluteParentXpathSearchCondition(final CpsPathQuery cpsPathQuery, final boolean acrossAnchors) {
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

    private static Integer getTextValueAsInt(final CpsPathQuery cpsPathQuery) {
        try {
            return Integer.parseInt(cpsPathQuery.getTextFunctionConditionValue());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private void addLeafConditions(final CpsPathQuery cpsPathQuery) {
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

    private void addTextFunctionCondition(final CpsPathQuery cpsPathQuery) {
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

    private void addContainsFunctionCondition(final CpsPathQuery cpsPathQuery) {
        if (cpsPathQuery.hasContainsFunctionCondition()) {
            sqlStringBuilder.append(" AND attributes ->> :containsLeafName LIKE CONCAT('%',:containsValue,'%') ");
            queryParameters.put("containsLeafName", cpsPathQuery.getContainsFunctionConditionLeafName());
            queryParameters.put("containsValue",
                    EscapeUtils.escapeForSqlLike(cpsPathQuery.getContainsFunctionConditionValue()));
        }
    }

    private void addSearchPrefix(final CpsPathQuery cpsPathQuery) {
        if (cpsPathQuery.hasAncestorAxis()) {
            sqlStringBuilder.append("""
                WITH RECURSIVE ancestors AS (
                    SELECT parentFragment.* FROM fragment parentFragment
                    WHERE parentFragment.id IN (
                        SELECT parent_id FROM fragment""");
        } else {
            sqlStringBuilder.append("SELECT fragment.* FROM fragment");
        }
    }

    private void addSearchSuffix(final CpsPathQuery cpsPathQuery) {
        if (cpsPathQuery.hasAncestorAxis()) {
            sqlStringBuilder.append("""
                          )
                          UNION
                            SELECT fragment.*
                            FROM fragment
                            JOIN ancestors ON ancestors.parent_id = fragment.id
                        )
                        SELECT * FROM ancestors
                        WHERE""");

            final String ancestorPath = DESCENDANT_PATH + cpsPathQuery.getAncestorSchemaNodeIdentifier();
            final CpsPathQuery ancestorCpsPathQuery = CpsPathUtil.getCpsPathQuery(ancestorPath);
            addAncestorNodeSearchCondition(ancestorCpsPathQuery);
        }
    }

    private void addAncestorNodeSearchCondition(final CpsPathQuery ancestorCpsPathQuery) {
        if (ancestorCpsPathQuery.hasLeafConditions()) {
            final String pathWithoutSlashes = ancestorCpsPathQuery.getNormalizedXpath().substring(2);
            queryParameters.put("ancestorXpath", "%/" + EscapeUtils.escapeForSqlLike(pathWithoutSlashes));
            sqlStringBuilder.append(" xpath LIKE :ancestorXpath");
        } else {
            addXpathSearchCondition(ancestorCpsPathQuery, "ancestorXpath");
        }
    }

    private static <V> String substitute(final String template, final Map<String, V> valueMap) {
        final StringSubstitutor stringSubstitutor = new StringSubstitutor(valueMap);
        return stringSubstitutor.replace(template);
    }

}
