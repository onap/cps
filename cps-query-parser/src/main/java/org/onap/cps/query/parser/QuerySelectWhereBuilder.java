
/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd
 *  Modifications Copyright (C) 2025 xAI
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
 *  SPDX-License-LicenseIdentifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.query.parser;

import org.onap.cps.query.parser.antlr4.QuerySelectWhereBaseListener;
import org.onap.cps.query.parser.antlr4.QuerySelectWhereParser;
import java.util.ArrayList;
import java.util.List;
import org.onap.cps.cpspath.parser.PathParsingException;

public class QuerySelectWhereBuilder extends QuerySelectWhereBaseListener {

    private final QuerySelectWhere querySelectWhere = new QuerySelectWhere();
    private final List<String> selectFields = new ArrayList<>();
    private final List<QuerySelectWhere.WhereCondition> whereConditions = new ArrayList<>();
    private final List<String> booleanOperators = new ArrayList<>();

    @Override
    public void exitSelectClause(final QuerySelectWhereParser.SelectClauseContext ctx) {
        querySelectWhere.setSelectFields(selectFields);
    }

    @Override
    public void exitIdentifier(final QuerySelectWhereParser.IdentifierContext ctx) {
        if (ctx.getParent() instanceof QuerySelectWhereParser.SelectClauseContext) {
            selectFields.add(ctx.getText());
        }
    }

    @Override
    public void exitPredicate(final QuerySelectWhereParser.PredicateContext ctx) {
        final String name = ctx.identifier().getText();
        final String operator = ctx.comparativeOperator().getText();
        final Object value;
        if (ctx.value().IntegerLiteral() != null) {
            value = Integer.valueOf(ctx.value().IntegerLiteral().getText());
        } else if (ctx.value().DecimalLiteral() != null) {
            value = Double.valueOf(ctx.value().DecimalLiteral().getText());
        } else if (ctx.value().StringLiteral() != null) {
            value = unwrapQuotedString(ctx.value().StringLiteral().getText());
        } else {
            throw new PathParsingException("Unsupported value in where condition: " + ctx.getText());
        }
        whereConditions.add(new QuerySelectWhere.WhereCondition(name, operator, value));
    }

    @Override
    public void exitBooleanOperator(final QuerySelectWhereParser.BooleanOperatorContext ctx) {
        booleanOperators.add(ctx.getText());
    }

    @Override
    public void exitWhereClause(final QuerySelectWhereParser.WhereClauseContext ctx) {
        querySelectWhere.setWhereConditions(whereConditions);
        querySelectWhere.setWhereBooleanOperators(booleanOperators);
    }

    public QuerySelectWhere build() {
        return querySelectWhere;
    }

    private static String unwrapQuotedString(final String wrappedString) {
        final String value = wrappedString.substring(1, wrappedString.length() - 1);
        return value.replace("''", "'");
    }
}
