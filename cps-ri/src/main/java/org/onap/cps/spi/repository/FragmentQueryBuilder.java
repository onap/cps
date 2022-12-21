/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.cpspath.parser.CpsPathPrefixType;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class FragmentQueryBuilder {
    private static final String REGEX_ABSOLUTE_PATH_PREFIX = ".*\\/";
    private static final String REGEX_OPTIONAL_LIST_INDEX_POSTFIX = "(\\[@(?!.*\\[).*?])?";
    private static final String REGEX_DESCENDANT_PATH_POSTFIX = "(\\/.*)?";
    private static final String REGEX_END_OF_INPUT = "$";

    @PersistenceContext
    private EntityManager entityManager;

    private final JsonObjectMapper jsonObjectMapper;

    /**
     * Create a sql query to retrieve by anchor(id) and cps path.
     *
     * @param anchorId the id of the anchor
     * @param cpsPathQuery the cps path query to be transformed into a sql query
     * @return a executable query object
     */
    public Query getQueryForAnchorAndCpsPath(final int anchorId, final CpsPathQuery cpsPathQuery) {
        final StringBuilder sqlStringBuilder = new StringBuilder("SELECT * FROM FRAGMENT WHERE anchor_id = :anchorId");
        final Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put("anchorId", anchorId);
        sqlStringBuilder.append(" AND xpath ~ :xpathRegex");
        final String xpathRegex = getXpathSqlRegex(cpsPathQuery, false);
        queryParameters.put("xpathRegex", xpathRegex);
        if (cpsPathQuery.hasLeafConditions()) {
            sqlStringBuilder.append(" AND attributes @> :leafDataAsJson\\:\\:jsonb");
            queryParameters.put("leafDataAsJson", jsonObjectMapper.asJsonString(
                cpsPathQuery.getLeavesData()));
        }

        addTextFunctionCondition(cpsPathQuery, sqlStringBuilder, queryParameters);
        final Query query = entityManager.createNativeQuery(sqlStringBuilder.toString(), FragmentEntity.class);
        setQueryParameters(query, queryParameters);
        return query;
    }

    /**
     * Create a regular expression (string) for xpath based on the given cps path query.
     *
     * @param cpsPathQuery  the cps path query to determine the required regular expression
     * @param includeDescendants include descendants yes or no
     * @return a string representing the required regular expression
     */
    public static String getXpathSqlRegex(final CpsPathQuery cpsPathQuery, final boolean includeDescendants) {
        final StringBuilder xpathRegexBuilder = new StringBuilder();
        if (CpsPathPrefixType.ABSOLUTE.equals(cpsPathQuery.getCpsPathPrefixType())) {
            xpathRegexBuilder.append(escapeXpath(cpsPathQuery.getXpathPrefix()));
        } else {
            xpathRegexBuilder.append(REGEX_ABSOLUTE_PATH_PREFIX);
            xpathRegexBuilder.append(escapeXpath(cpsPathQuery.getDescendantName()));
        }
        xpathRegexBuilder.append(REGEX_OPTIONAL_LIST_INDEX_POSTFIX);
        if (includeDescendants) {
            xpathRegexBuilder.append(REGEX_DESCENDANT_PATH_POSTFIX);
        }
        xpathRegexBuilder.append(REGEX_END_OF_INPUT);
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

    private static void setQueryParameters(final Query query, final Map<String, Object> queryParameters) {
        for (final Map.Entry<String, Object> queryParameter : queryParameters.entrySet()) {
            query.setParameter(queryParameter.getKey(), queryParameter.getValue());
        }
    }

}
