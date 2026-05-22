/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2023 Deutsche Telekom AG
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

import static org.onap.cps.cpspath.parser.CpsPathPrefixType.DESCENDANT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.onap.cps.cpspath.parser.antlr4.CpsPathBaseListener;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.AncestorAxisContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.DescendantContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.LeafConditionContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.MultipleLeafConditionsContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.PrefixContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.TextFunctionConditionContext;

public class CpsPathBuilder extends CpsPathBaseListener {

    private static final String NO_PARENT_PATH = "";

    private static final String OPEN_BRACKET = "[";

    private static final String CLOSE_BRACKET = "]";

    private final CpsPathQuery cpsPathQuery = new CpsPathQuery();

    private final List<CpsPathQuery.LeafCondition> leafConditions = new ArrayList<>();

    private final StringBuilder normalizedXpathBuilder = new StringBuilder();

    private int startIndexOfAncestorSchemaNodeIdentifier = 0;

    private boolean processingAncestorAxis = false;

    private final List<String> containerNames = new ArrayList<>();

    private final List<String> booleanOperators = new ArrayList<>();

    private int startIndexOfMultipleLeafConditions = 0;

    private boolean insideListElementRef = false;

    private final List<CpsPathQuery.LeafCondition> pendingListRefConditions = new ArrayList<>();

    private final List<String> pendingListRefBooleanOperators = new ArrayList<>();

    private int startIndexOfListElementRef = 0;

    @Override
    public void exitSlash(final CpsPathParser.SlashContext ctx) {
        normalizedXpathBuilder.append("/");
    }

    @Override
    public void exitPrefix(final PrefixContext ctx) {
        cpsPathQuery.setXpathPrefix(normalizedXpathBuilder.toString());
    }

    @Override
    public void exitParent(final CpsPathParser.ParentContext ctx) {
        final String normalizedParentPath;
        if (normalizedXpathBuilder.toString().equals("/")) {
            normalizedParentPath = NO_PARENT_PATH;
        } else {
            normalizedParentPath = normalizedXpathBuilder.toString();
        }
        cpsPathQuery.setNormalizedParentPath(normalizedParentPath);
    }

    @Override
    public void exitLeafCondition(final LeafConditionContext ctx) {
        final String leafName = ctx.leafName().getText();
        final String operator = ctx.comparativeOperators().getText();
        final Object comparisonValue;
        if (ctx.IntegerLiteral() != null) {
            comparisonValue = Integer.valueOf(ctx.IntegerLiteral().getText());
        } else if (ctx.StringLiteral() != null) {
            comparisonValue = unwrapQuotedString(ctx.StringLiteral().getText());
        } else {
            throw new PathParsingException("Unsupported comparison value encountered in expression" + ctx.getText());
        }
        leafContext(leafName, operator, comparisonValue);
    }

    @Override
    public void exitBooleanOperators(final CpsPathParser.BooleanOperatorsContext ctx) {
        if (insideListElementRef) {
            pendingListRefBooleanOperators.add(ctx.getText());
        } else {
            booleanOperators.add(ctx.getText());
        }
    }

    @Override
    public void exitDescendant(final DescendantContext ctx) {
        cpsPathQuery.setCpsPathPrefixType(DESCENDANT);
        cpsPathQuery.setDescendantName(normalizedXpathBuilder.substring(2));
    }

    @Override
    public void enterMultipleLeafConditions(final MultipleLeafConditionsContext ctx)  {
        normalizedXpathBuilder.append(OPEN_BRACKET);
        startIndexOfMultipleLeafConditions = normalizedXpathBuilder.length();
        leafConditions.clear();
        booleanOperators.clear();
    }

    @Override
    public void exitMultipleLeafConditions(final MultipleLeafConditionsContext ctx) {
        final List<String> formattedConditions = new ArrayList<>(leafConditions.size());
        for (final CpsPathQuery.LeafCondition leafCondition : leafConditions) {
            formattedConditions.add(formatCondition(leafCondition.name(), leafCondition.operator(),
                    leafCondition.value()));
        }
        if (isAllEqualityConditionsWithAndOperators(leafConditions, booleanOperators)) {
            Collections.sort(formattedConditions);
            normalizedXpathBuilder.replace(startIndexOfMultipleLeafConditions, normalizedXpathBuilder.length(),
                    String.join(" and ", formattedConditions));
        } else {
            for (int i = 0; i < formattedConditions.size(); i++) {
                if (i > 0) {
                    normalizedXpathBuilder.append(" ").append(booleanOperators.get(i - 1)).append(" ");
                }
                normalizedXpathBuilder.append(formattedConditions.get(i));
            }
        }
        normalizedXpathBuilder.append(CLOSE_BRACKET);
        cpsPathQuery.setLeafConditions(leafConditions);
    }

    private static boolean isAllEqualityConditionsWithAndOperators(
            final List<CpsPathQuery.LeafCondition> conditions, final List<String> operators) {
        for (final CpsPathQuery.LeafCondition condition : conditions) {
            if (!"=".equals(condition.operator())) {
                return false;
            }
        }
        for (final String operator : operators) {
            if (!"and".equals(operator)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void enterAncestorAxis(final AncestorAxisContext ctx) {
        processingAncestorAxis = true;
        normalizedXpathBuilder.append("/ancestor::");
        startIndexOfAncestorSchemaNodeIdentifier = normalizedXpathBuilder.length();
    }

    @Override
    public void exitAncestorAxis(final AncestorAxisContext ctx) {
        processingAncestorAxis = false;
        cpsPathQuery.setAncestorSchemaNodeIdentifier(
                normalizedXpathBuilder.substring(startIndexOfAncestorSchemaNodeIdentifier));
    }

    @Override
    public void exitAttributeAxis(final CpsPathParser.AttributeAxisContext ctx) {
        final String attributeName = ctx.leafName().getText();
        normalizedXpathBuilder.append("/@").append(attributeName);
        cpsPathQuery.setAttributeAxisAttributeName(attributeName);
    }

    @Override
    public void exitTextFunctionCondition(final TextFunctionConditionContext ctx) {
        cpsPathQuery.setTextFunctionConditionLeafName(ctx.leafName().getText());
        cpsPathQuery.setTextFunctionConditionValue(unwrapQuotedString(ctx.StringLiteral().getText()));
    }

    @Override
    public void exitContainsFunctionCondition(final CpsPathParser.ContainsFunctionConditionContext ctx) {
        cpsPathQuery.setContainsFunctionConditionLeafName(ctx.leafName().getText());
        cpsPathQuery.setContainsFunctionConditionValue(unwrapQuotedString(ctx.StringLiteral().getText()));
    }

    @Override
    public void enterListElementRef(final CpsPathParser.ListElementRefContext ctx) {
        normalizedXpathBuilder.append(OPEN_BRACKET);
        startIndexOfListElementRef = normalizedXpathBuilder.length();
        insideListElementRef = true;
        pendingListRefConditions.clear();
        pendingListRefBooleanOperators.clear();
    }

    @Override
    public void exitListElementRef(final CpsPathParser.ListElementRefContext ctx) {
        insideListElementRef = false;
        final List<String> formattedConditions = new ArrayList<>(pendingListRefConditions.size());
        for (final CpsPathQuery.LeafCondition leafCondition : pendingListRefConditions) {
            formattedConditions.add(formatCondition(leafCondition.name(), leafCondition.operator(),
                    leafCondition.value()));
        }
        if (isAllEqualityConditionsWithAndOperators(pendingListRefConditions, pendingListRefBooleanOperators)) {
            Collections.sort(formattedConditions);
            normalizedXpathBuilder.replace(startIndexOfListElementRef, normalizedXpathBuilder.length(),
                    String.join(" and ", formattedConditions));
        } else {
            for (int i = 0; i < formattedConditions.size(); i++) {
                if (i > 0) {
                    normalizedXpathBuilder.append(" ").append(pendingListRefBooleanOperators.get(i - 1)).append(" ");
                }
                normalizedXpathBuilder.append(formattedConditions.get(i));
            }
        }
        normalizedXpathBuilder.append(CLOSE_BRACKET);
    }

    CpsPathQuery build() {
        cpsPathQuery.setNormalizedXpath(normalizedXpathBuilder.toString());
        cpsPathQuery.setContainerNames(containerNames);
        cpsPathQuery.setBooleanOperators(booleanOperators);
        return cpsPathQuery;
    }

    @Override
    public void exitContainerName(final CpsPathParser.ContainerNameContext ctx) {
        final String containerName = ctx.getText();
        normalizedXpathBuilder.append(containerName);
        if (!processingAncestorAxis) {
            containerNames.add(containerName);
        }
    }

    private void leafContext(final String leafName, final String operator, final Object comparisonValue) {
        final CpsPathQuery.LeafCondition leafCondition = new CpsPathQuery.LeafCondition(leafName, operator,
                comparisonValue);
        if (insideListElementRef) {
            pendingListRefConditions.add(leafCondition);
        } else {
            leafConditions.add(leafCondition);
        }
    }

    private static String formatCondition(final String name, final String operator, final Object value) {
        final String formattedValue = operator.equals("=") ? wrapValueInSingleQuotes(value) : value.toString();
        return "@" + name + operator + formattedValue;
    }

    private static String unwrapQuotedString(final String wrappedString) {
        final boolean wasWrappedInSingleQuote = wrappedString.startsWith("'");
        final String value = stripFirstAndLastCharacter(wrappedString);
        if (wasWrappedInSingleQuote) {
            return value.replace("''", "'");
        } else {
            return value.replace("\"\"", "\"");
        }
    }

    private static String wrapValueInSingleQuotes(final Object value) {
        return "'" + value.toString().replace("'", "''") + "'";
    }

    private static String stripFirstAndLastCharacter(final String wrappedString) {
        return wrappedString.substring(1, wrappedString.length() - 1);
    }
}
