/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2025 TechMahindra Ltd.
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

package org.onap.cps.cpspath.parser;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.onap.cps.cpspath.parser.antlr4.CpsPathLexer;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CpsPathUtil {

    public static final String ROOT_NODE_XPATH = "/";
    public static final String NO_PARENT_PATH = "";

    /**
     * Returns a normalized xpath path query.
     *
     * @param xpathSource xpath
     * @return a normalized xpath String.
     */
    public static String getNormalizedXpath(final String xpathSource) {
        if (ROOT_NODE_XPATH.equals(xpathSource)) {
            return NO_PARENT_PATH;
        }
        return getCpsPathBuilder(xpathSource, null).build().getNormalizedXpath();
    }

    /**
     * Returns the parent xpath.
     *
     * @param xpathSource xpath
     * @return the parent xpath String.
     */
    public static String getNormalizedParentXpath(final String xpathSource) {
        return getCpsPathBuilder(xpathSource, null).build().getNormalizedParentPath();
    }

    public static List<String> getXpathNodeIdSequence(final String xpathSource) {
        return getCpsPathBuilder(xpathSource, null).build().getContainerNames();
    }

    /**
     * Returns boolean indicating xpath is an absolute path to a list element.
     *
     * @param xpathSource xpath
     * @return true if xpath is an absolute path to a list element
     */
    public static boolean isPathToListElement(final String xpathSource) {
        final CpsPathQuery cpsPathQuery = getCpsPathBuilder(xpathSource, null).build();
        return cpsPathQuery.isPathToListElement();
    }

    /**
     * Returns a cps path query.
     *
     * @param cpsPathSource cps path
     * @return a CpsPathQuery object.
     */

    public static CpsPathQuery getCpsPathQuery(final String cpsPathSource) {
        return getCpsPathBuilder(cpsPathSource, null).build();
    }
    /**
     * Returns a SQL query and parameters for the given cps path and where clause.
     *
     * @param cpsPathSource cps path
     * @param whereClause where clause
     * @return a SqlQueryResult object containing the SQL query and parameters
     */
    public static SqlQueryResult getSqlQuery(final String cpsPathSource, final String whereClause) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();
        CpsPathQuery cpsPathQuery;
        // Handle wildcard paths (e.g., /bookstore%)
        if (cpsPathSource != null && cpsPathSource.contains("%")) {
            sql.append("xpath LIKE ?");
            params.add(cpsPathSource);
            // Parse whereClause if present
            if (whereClause != null && !whereClause.trim().isEmpty()) {
                cpsPathQuery = getCpsPathBuilder("/dummy", whereClause).build();
                appendWhereClauseConditions(sql, params, cpsPathQuery);
            }
        } else {
            // Handle standard cpsPath parsing
            cpsPathQuery = getCpsPathBuilder(cpsPathSource, whereClause).build();
            sql.append("xpath LIKE ?");
            String xpathPattern = cpsPathQuery.getNormalizedXpath().replaceAll("\\[@[^\\]]*\\]", "%");
            if (cpsPathQuery.getCpsPathPrefixType() == CpsPathPrefixType.DESCENDANT) {
                xpathPattern = "%" + xpathPattern;
            }
            params.add(xpathPattern);
            // Add leaf conditions from cpsPath (Integer or String)
            if (cpsPathQuery.hasLeafConditions()) {
                for (int i = 0; i < cpsPathQuery.getLeafConditions().size(); i++) {
                    final CpsPathQuery.LeafCondition condition = cpsPathQuery.getLeafConditions().get(i);
                    sql.append(" AND ");
                    appendSqlCondition(sql, params, condition.name(), condition.operator(), condition.value());
                    if (i < cpsPathQuery.getBooleanOperators().size()) {
                        sql.append(" ").append(cpsPathQuery.getBooleanOperators().get(i).toUpperCase());
                    }
                }
            }
            // Add text function condition
            if (cpsPathQuery.hasTextFunctionCondition()) {
                sql.append(" AND attributes->>'").append(cpsPathQuery.getTextFunctionConditionLeafName()).append("' = ?");
                params.add(cpsPathQuery.getTextFunctionConditionValue());
            }
            // Add contains function condition
            if (cpsPathQuery.hasContainsFunctionCondition()) {
                sql.append(" AND attributes->>'").append(cpsPathQuery.getContainsFunctionConditionLeafName()).append("' ILIKE ?");
                params.add("%" + cpsPathQuery.getContainsFunctionConditionValue() + "%");
            }
            // Add attribute axis condition
            if (cpsPathQuery.hasAttributeAxis()) {
                sql.append(" AND attributes->>'").append(cpsPathQuery.getAttributeAxisAttributeName()).append("' IS NOT NULL");
            }
            // Add ancestor axis condition
            if (cpsPathQuery.hasAncestorAxis()) {
                sql.append(" AND xpath LIKE '%").append(cpsPathQuery.getAncestorSchemaNodeIdentifier()).append("%'");
            }
            // Add where clause conditions
            appendWhereClauseConditions(sql, params, cpsPathQuery);
        }
        return new SqlQueryResult(sql.toString(), params);
    }
    private static void appendWhereClauseConditions(final StringBuilder sql, final List<Object> params, final CpsPathQuery cpsPathQuery) {
        if (cpsPathQuery.hasWhereConditions()) {
            StringBuilder whereSql = new StringBuilder();
            boolean firstCondition = true;
            int operatorIndex = 0;
            for (int i = 0; i < cpsPathQuery.getWhereConditions().size(); i++) {
                final CpsPathQuery.WhereCondition condition = cpsPathQuery.getWhereConditions().get(i);
                if (condition.operator().equals("NOT")) {
                    whereSql.append(" NOT (");
                    continue;
                }
                if (!firstCondition && operatorIndex < cpsPathQuery.getWhereBooleanOperators().size()) {
                    whereSql.append(" ").append(cpsPathQuery.getWhereBooleanOperators().get(operatorIndex).toUpperCase()).append(" ");
                    operatorIndex++;
                }
                appendSqlCondition(whereSql, params, condition.name(), condition.operator(), condition.value());
                firstCondition = false;
            }
            // Close any open NOT expressions
            int notCount = (int) cpsPathQuery.getWhereConditions().stream().filter(c -> c.operator().equals("NOT")).count();
            for (int i = 0; i < notCount; i++) {
                whereSql.append(")");
            }
            if (whereSql.length() > 0) {
                sql.append(" AND ").append(whereSql);
            }
        }
    }
    private static void appendSqlCondition(final StringBuilder sql, final List<Object> params, final String name, final String operator, final Object value) {
        if (operator.equals("NOT")) {
            return; // Handled in whereSql construction
        }
        final boolean isNumeric = value instanceof Number;
        final String sqlField = isNumeric ? "CAST(attributes->>'" + name + "' AS NUMERIC)" : "attributes->>'" + name + "'";
        final String sqlOperator = operator.equalsIgnoreCase("LIKE") ? "ILIKE" : operator.toUpperCase();
        sql.append(sqlField).append(" ").append(sqlOperator).append(" ?");
        params.add(sqlOperator.equals("ILIKE") ? "%" + value + "%" : value);
    }
    private static CpsPathBuilder getCpsPathBuilder(final String cpsPathSource, final String whereClause) {
        final String input = whereClause != null && !whereClause.trim().isEmpty() ? cpsPathSource + " " + whereClause : cpsPathSource;
        final CharStream inputStream = CharStreams.fromString(input);
        final CpsPathLexer cpsPathLexer = new CpsPathLexer(inputStream);
        final CpsPathParser cpsPathParser = new CpsPathParser(new CommonTokenStream(cpsPathLexer));
        cpsPathParser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                    final int charPositionInLine, final String msg, final RecognitionException e) {
                throw new PathParsingException("failed to parse at line " + line + " due to " + msg,
                        e == null ? "" : e.getMessage());
            }
        });
        final CpsPathBuilder cpsPathBuilder = new CpsPathBuilder();
        cpsPathParser.addParseListener(cpsPathBuilder);
        cpsPathParser.query();
        return cpsPathBuilder;
    }
    public record SqlQueryResult(String sql, List<Object> params) { }
}