/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd
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

package org.onap.cps.query.parser;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySelectWhere {

    private List<String> selectFields;
    private List<WhereCondition> whereConditions;
    private List<String> whereBooleanOperators;

    public boolean hasWhereConditions() {
        return whereConditions != null && !whereConditions.isEmpty();
    }

    public record WhereCondition(String name, String operator, Object value) { }

    public static QuerySelectWhere createFrom(final String select, final String where) {
        return QuerySelectWhereUtil.getQuerySelectWhere(select, where);
    }
}
