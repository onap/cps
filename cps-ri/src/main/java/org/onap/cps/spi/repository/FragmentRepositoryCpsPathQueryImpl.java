/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.jetbrains.annotations.Nullable;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathQueryType;
import org.onap.cps.spi.entities.FragmentEntity;

public class FragmentRepositoryCpsPathQueryImpl implements FragmentRepositoryCpsPathQuery {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public List<FragmentEntity> executeCpsPathQuery(final int anchorId, final CpsPathQuery cpsPathQuery) {
        final var sqlStringBuilder = new StringBuilder("SELECT * FROM FRAGMENT WHERE anchor_id = :anchorId");
        final Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put("anchorId", anchorId);
        if (CpsPathQueryType.XPATH_LEAF_VALUE.equals(cpsPathQuery.getCpsPathQueryType())) {
            sqlStringBuilder.append(" AND (xpath = (:xpath) OR xpath LIKE CONCAT(:xpath,'\\[@%]'))");
            sqlStringBuilder.append(" AND attributes @> jsonb_build_object(:leafName , :leafValue)");
            queryParameters.put("xpath", cpsPathQuery.getXpathPrefix());
            queryParameters.put("leafName", cpsPathQuery.getLeafName());
            queryParameters.put("leafValue", cpsPathQuery.getLeafValue());
        } else if (CpsPathQueryType.XPATH_HAS_DESCENDANT_WITH_LEAF_VALUES.equals(cpsPathQuery.getCpsPathQueryType())) {
            sqlStringBuilder.append(" AND (xpath LIKE CONCAT('%/',:descendantName)");
            sqlStringBuilder.append(" OR xpath LIKE CONCAT('%/', :descendantName,'\\[@%]'))");
            sqlStringBuilder.append(" AND attributes @> :leafDataAsJson\\:\\:jsonb");
            queryParameters.put("descendantName", cpsPathQuery.getDescendantName());
            queryParameters.put("leafDataAsJson", GSON.toJson(cpsPathQuery.getLeavesData()));
        } else {
            queryParameters.put("descendantName", cpsPathQuery.getDescendantName());
            sqlStringBuilder.append(" AND xpath LIKE CONCAT('%/',:descendantName)");
        }

        addTextFunctionCondition(cpsPathQuery, sqlStringBuilder, queryParameters);
        final var query = entityManager.createNativeQuery(sqlStringBuilder.toString(), FragmentEntity.class);
        setQueryParameters(query, queryParameters);
        return query.getResultList();
    }

    @Nullable
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
