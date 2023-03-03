/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FragmentNativeRepositoryImpl implements FragmentNativeRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public void deleteByAnchorIdAndXpaths(final int anchorId, final Collection<String> xpaths) {
        final String queryString =
            "DELETE FROM fragment f WHERE f.anchor_id = ? AND (f.xpath IN (:parameterPlaceholders))";
        executeUpdateWithAnchorIdAndCollection(queryString, anchorId, xpaths);
    }

    @Override
    public void deleteListsByAnchorIdAndXpaths(final int anchorId, final Collection<String> listXpaths) {
        final Collection<String> listXpathPatterns =
            listXpaths.stream().map(listXpath -> listXpath + "[%").collect(Collectors.toSet());
        final String queryString =
            "DELETE FROM fragment f WHERE f.anchor_id = ? AND (f.xpath LIKE ANY (array[:parameterPlaceholders]))";
        executeUpdateWithAnchorIdAndCollection(queryString, anchorId, listXpathPatterns);
    }

    // Accept security hotspot as placeholders in SQL query are created internally, not from user input.
    @SuppressWarnings("squid:S2077")
    private void executeUpdateWithAnchorIdAndCollection(final String sqlTemplate, final int anchorId,
                                                        final Collection<String> collection) {
        if (!collection.isEmpty()) {
            final String parameterPlaceholders = String.join(",", Collections.nCopies(collection.size(), "?"));
            final String queryStringWithParameterPlaceholders =
                sqlTemplate.replaceFirst(":parameterPlaceholders\\b", parameterPlaceholders);

            final Query query = entityManager.createNativeQuery(queryStringWithParameterPlaceholders);
            query.setParameter(1, anchorId);
            int parameterIndex = 2;
            for (final String parameterValue : collection) {
                query.setParameter(parameterIndex++, parameterValue);
            }
            query.executeUpdate();
        }
    }

}
