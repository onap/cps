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

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.jetbrains.annotations.Nullable;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.spi.entities.FragmentEntity;


public class FragmentRepositoryCustomImpl implements FragmentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<FragmentEntity> executeCpsPathQuery(final int anchorId, final CpsPathQuery cpsPathQuery) {
        final var sqlStringBuilder = new StringBuilder("SELECT * FROM FRAGMENT WHERE ");
        sqlStringBuilder.append("anchor_id = :anchorId AND xpath LIKE CONCAT('%/',:descendantName)");
        final Integer textValueAsInt = getTextValueAsInt(cpsPathQuery);
        addTextFunctionCondition(cpsPathQuery, sqlStringBuilder, textValueAsInt);
        final var query = entityManager.createNativeQuery(sqlStringBuilder.toString(), FragmentEntity.class);
        setQueryParameters(query, anchorId, cpsPathQuery, textValueAsInt);
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
                                                 final Integer textValueAsInt) {
        if (cpsPathQuery.hasTextFunctionCondition()) {
            sqlStringBuilder.append("AND (");
            sqlStringBuilder.append("attributes @> jsonb_build_object(:textLeafName, :textValue)");
            sqlStringBuilder
                .append(" OR attributes @> jsonb_build_object(:textLeafName, json_build_array(:textValue))");
            if (textValueAsInt != null) {
                sqlStringBuilder.append(" OR attributes @> jsonb_build_object(:textLeafName, :textValueAsInt)");
                sqlStringBuilder
                    .append(" OR attributes @> jsonb_build_object(:textLeafName, json_build_array(:textValueAsInt))");
            }
            sqlStringBuilder.append(")");
        }
    }

    private static void setQueryParameters(final Query query, final int anchorId, final CpsPathQuery cpsPathQuery,
                                           final Integer textValueAsInt) {
        query.setParameter("anchorId", anchorId);
        query.setParameter("descendantName", cpsPathQuery.getDescendantName());
        if (cpsPathQuery.hasTextFunctionCondition()) {
            query.setParameter("textLeafName", cpsPathQuery.getTextFunctionConditionLeafName());
            query.setParameter("textValue", cpsPathQuery.getTextFunctionConditionValue());
            if (textValueAsInt != null) {
                query.setParameter("textValueAsInt", textValueAsInt);
            }
        }
    }

}
