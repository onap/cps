/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.onap.cps.cpspath.parser.CpsPathPrefixType;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.utils.JsonObjectMapper;

@RequiredArgsConstructor
public class FragmentRepositoryCpsPathQueryImpl implements FragmentRepositoryCpsPathQuery {

    public static final String SIMILAR_TO_ABSOLUTE_PATH_PREFIX = "%/";
    public static final String SIMILAR_TO_OPTIONAL_LIST_INDEX_POSTFIX = "(\\[[^/]*])?";

    @PersistenceContext
    private EntityManager entityManager;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    public List<FragmentEntity> findByAnchorAndCpsPath(final int anchorId, final CpsPathQuery cpsPathQuery) {
        final StringBuilder sqlStringBuilder = new StringBuilder("SELECT * FROM FRAGMENT WHERE anchor_id = :anchorId");
        final Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put("anchorId", anchorId);
        sqlStringBuilder.append(" AND xpath SIMILAR TO :xpathRegex");
        final String xpathRegex = getSimilarToXpathSqlRegex(cpsPathQuery);
        queryParameters.put("xpathRegex", xpathRegex);
        if (cpsPathQuery.hasLeafConditions()) {
            sqlStringBuilder.append(" AND attributes @> :leafDataAsJson\\:\\:jsonb");
            queryParameters.put("leafDataAsJson", jsonObjectMapper.asJsonString(
                    cpsPathQuery.getLeavesData()));
        }

        addTextFunctionCondition(cpsPathQuery, sqlStringBuilder, queryParameters);
        final var query = entityManager.createNativeQuery(sqlStringBuilder.toString(), FragmentEntity.class);
        setQueryParameters(query, queryParameters);
        return query.getResultList();
    }

    private static String getSimilarToXpathSqlRegex(final CpsPathQuery cpsPathQuery) {
        final var xpathRegexBuilder = new StringBuilder();
        if (CpsPathPrefixType.ABSOLUTE.equals(cpsPathQuery.getCpsPathPrefixType())) {
            xpathRegexBuilder.append(escapeXpath(cpsPathQuery.getXpathPrefix()));
        } else {
            xpathRegexBuilder.append(SIMILAR_TO_ABSOLUTE_PATH_PREFIX);
            xpathRegexBuilder.append(escapeXpath(cpsPathQuery.getDescendantName()));
        }
        xpathRegexBuilder.append(SIMILAR_TO_OPTIONAL_LIST_INDEX_POSTFIX);
        return xpathRegexBuilder.toString();
    }

    private static String escapeXpath(final String xpath) {
        // See https://jira.onap.org/browse/CPS-500 for limitations of this basic escape mechanism
        return xpath.replace("[@", "\\[@");
    }

    private static Integer getTextValueAsInt(final CpsPathQuery cpsPathQuery) {
        try {
            return Integer.parseInt(cpsPathQuery.getTextFunctionConditionValue());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static void addTextFunctionCondition(final CpsPathQuery cpsPathQuery, final StringBuilder sqlStringBuilder,
                                                 final Map<String, Object> queryParameters) {
        if (cpsPathQuery.hasTextFunctionCondition()) {
            sqlStringBuilder.append(" AND (");
            sqlStringBuilder.append("attributes @> jsonb_build_object(:textLeafName, :textValue)");
            sqlStringBuilder
                .append(" OR attributes @> jsonb_build_object(:textLeafName, json_build_array(:textValue))");
            queryParameters.put("textLeafName", cpsPathQuery.getTextFunctionConditionLeafName());
            queryParameters.put("textValue", cpsPathQuery.getTextFunctionConditionValue());
            final var textValueAsInt = getTextValueAsInt(cpsPathQuery);
            if (textValueAsInt != null) {
                sqlStringBuilder.append(" OR attributes @> jsonb_build_object(:textLeafName, :textValueAsInt)");
                sqlStringBuilder
                    .append(" OR attributes @> jsonb_build_object(:textLeafName, json_build_array(:textValueAsInt))");
                queryParameters.put("textValueAsInt", textValueAsInt);
            }
            sqlStringBuilder.append(")");
        }
    }

    private static void setQueryParameters(final Query query, final Map<String, Object> queryParameters) {
        for (final Map.Entry<String, Object> queryParameter : queryParameters.entrySet()) {
            query.setParameter(queryParameter.getKey(), queryParameter.getValue());
        }
    }

}
