/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.spi.repository;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.cpspath.parser.CpsPathPrefixType;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.CpsPathException;
import org.onap.cps.spi.utils.EscapeUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class FragmentQueryBuilder {
    private static final AnchorEntity ACROSS_ALL_ANCHORS = null;

    @PersistenceContext
    private EntityManager entityManager;

    private final JsonObjectMapper jsonObjectMapper;

    /**
     * Create a sql query to retrieve by anchor(id) and cps path.
     *
     * @param anchorEntity the anchor
     * @param cpsPathQuery the cps path query to be transformed into a sql query
     * @return a executable query object
     */
    public Query getQueryForAnchorAndCpsPath(final AnchorEntity anchorEntity, final CpsPathQuery cpsPathQuery) {
        return getQueryForDataspaceOrAnchorAndCpsPath(anchorEntity.getDataspace(), anchorEntity, cpsPathQuery);
    }

    /**
     * Create a sql query to retrieve by cps path.
     *
     * @param dataspaceEntity the dataspace
     * @param cpsPathQuery the cps path query to be transformed into a sql query
     * @return a executable query object
     */
    public Query getQueryForDataspaceAndCpsPath(final DataspaceEntity dataspaceEntity,
                                                final CpsPathQuery cpsPathQuery) {
        return getQueryForDataspaceOrAnchorAndCpsPath(dataspaceEntity, ACROSS_ALL_ANCHORS, cpsPathQuery);
    }

    private Query getQueryForDataspaceOrAnchorAndCpsPath(final DataspaceEntity dataspaceEntity,
                                                         final AnchorEntity anchorEntity,
                                                         final CpsPathQuery cpsPathQuery) {
        final StringBuilder sqlStringBuilder = new StringBuilder();
        final Map<String, Object> queryParameters = new HashMap<>();

        if (anchorEntity == ACROSS_ALL_ANCHORS) {
            sqlStringBuilder.append("SELECT fragment.* FROM fragment JOIN anchor ON anchor.id = fragment.anchor_id"
                + " WHERE dataspace_id = :dataspaceId");
            queryParameters.put("dataspaceId", dataspaceEntity.getId());
        } else {
            sqlStringBuilder.append("SELECT * FROM fragment WHERE anchor_id = :anchorId");
            queryParameters.put("anchorId", anchorEntity.getId());
        }
        addXpathSearch(cpsPathQuery, sqlStringBuilder, queryParameters);
        addLeafConditions(cpsPathQuery, sqlStringBuilder);
        addTextFunctionCondition(cpsPathQuery, sqlStringBuilder, queryParameters);
        addContainsFunctionCondition(cpsPathQuery, sqlStringBuilder, queryParameters);

        final Query query = entityManager.createNativeQuery(sqlStringBuilder.toString(), FragmentEntity.class);
        setQueryParameters(query, queryParameters);
        return query;
    }

    private static void addXpathSearch(final CpsPathQuery cpsPathQuery,
                                       final StringBuilder sqlStringBuilder,
                                       final Map<String, Object> queryParameters) {
        sqlStringBuilder.append(" AND (");
        sqlStringBuilder.append("xpath LIKE :escapedXpath ");
        sqlStringBuilder.append("OR (xpath LIKE :escapedXpath||'[@%]' AND xpath NOT LIKE :escapedXpath||'[@%]/%[@%]')");
        sqlStringBuilder.append(")");
        if (CpsPathPrefixType.ABSOLUTE.equals(cpsPathQuery.getCpsPathPrefixType())) {
            queryParameters.put("escapedXpath", EscapeUtils.escapeForSqlLike(cpsPathQuery.getXpathPrefix()));
        } else {
            queryParameters.put("escapedXpath", "%/" + EscapeUtils.escapeForSqlLike(cpsPathQuery.getDescendantName()));
        }
    }

    private static Integer getTextValueAsInt(final CpsPathQuery cpsPathQuery) {
        try {
            return Integer.parseInt(cpsPathQuery.getTextFunctionConditionValue());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private void addLeafConditions(final CpsPathQuery cpsPathQuery, final StringBuilder sqlStringBuilder) {
        if (cpsPathQuery.hasLeafConditions()) {
            queryLeafConditions(cpsPathQuery, sqlStringBuilder);
        }
    }

    private void queryLeafConditions(final CpsPathQuery cpsPathQuery, final StringBuilder sqlStringBuilder) {
        sqlStringBuilder.append(" AND (");
        final Queue<String> booleanOperatorsQueue = new LinkedList<>(cpsPathQuery.getBooleanOperators());
        final Queue<String> comparativeOperatorQueue = new LinkedList<>(cpsPathQuery.getComparativeOperators());
        cpsPathQuery.getLeavesData().entrySet().forEach(entry -> {
            final String nextComparativeOperator = comparativeOperatorQueue.poll();
            if (entry.getValue() instanceof Integer) {
                sqlStringBuilder.append("(attributes ->> ");
                sqlStringBuilder.append("'").append(entry.getKey()).append("')\\:\\:int");
                sqlStringBuilder.append(" ").append(nextComparativeOperator).append(" ");
                sqlStringBuilder.append("'").append(jsonObjectMapper.asJsonString(entry.getValue())).append("'");
            } else {
                if ("=".equals(nextComparativeOperator)) {
                    sqlStringBuilder.append(" attributes @> ");
                    sqlStringBuilder.append("'");
                    sqlStringBuilder.append(jsonObjectMapper.asJsonString(entry));
                    sqlStringBuilder.append("'");
                } else {
                    throw new CpsPathException(" can use only " + nextComparativeOperator + " with integer ");
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
