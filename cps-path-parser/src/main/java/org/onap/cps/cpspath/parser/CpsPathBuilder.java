/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2023-2026 Deutsche Telekom AG
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
import java.util.Comparator;
import java.util.Iterator;
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

    private static final String EQUALITY_OPERATOR = "=";

    private static final String OR_OPERATOR = "or";

    private static final String ATTRIBUTE_PREFIX = "@";

    private static final String SPACE = " ";

    private final CpsPathQuery cpsPathQuery = new CpsPathQuery();

    private final List<CpsPathQuery.LeafCondition> leafConditions = new ArrayList<>();

    private final StringBuilder normalizedXpathBuilder = new StringBuilder();

    private int startIndexOfAncestorSchemaNodeIdentifier;

    private boolean processingAncestorAxis;

    private final List<String> containerNames = new ArrayList<>();

    private final List<String> booleanOperators = new ArrayList<>();

    private boolean hasOrOperator;

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
        if ("/".equals(normalizedXpathBuilder.toString())) {
            normalizedParentPath = NO_PARENT_PATH;
        } else {
            normalizedParentPath = normalizedXpathBuilder.toString();
        }
        cpsPathQuery.setNormalizedParentPath(normalizedParentPath);
    }

    @Override
    public void exitLeafCondition(final LeafConditionContext ctx) {
        if (ctx.leafName() != null && ctx.comparativeOperators() != null) {
            final String leafName = ctx.leafName().getText();
            final String operator = ctx.comparativeOperators().getText();
            final Object comparisonValue;
            if (ctx.IntegerLiteral() != null) {
                comparisonValue = Integer.valueOf(ctx.IntegerLiteral().getText());
            } else if (ctx.StringLiteral() != null) {
                comparisonValue = unwrapQuotedString(ctx.StringLiteral().getText());
            } else {
                throw new PathParsingException(
                        "Unsupported comparison value encountered in expression:" + ctx.getText(), "");
            }
            leafConditions.add(new CpsPathQuery.LeafCondition(leafName, operator, comparisonValue));
        }
    }

    @Override
    public void exitBooleanOperators(final CpsPathParser.BooleanOperatorsContext ctx) {
        final String operator = ctx.getText();
        booleanOperators.add(operator);
        if (OR_OPERATOR.equals(operator)) {
            hasOrOperator = true;
        }
    }

    @Override
    public void exitDescendant(final DescendantContext ctx) {
        cpsPathQuery.setCpsPathPrefixType(DESCENDANT);
        cpsPathQuery.setDescendantName(normalizedXpathBuilder.substring(2));
    }

    @Override
    public void enterListElementRef(final CpsPathParser.ListElementRefContext ctx) {
        hasOrOperator = false;
        leafConditions.clear();
        booleanOperators.clear();
    }

    @Override
    public void enterMultipleLeafConditions(final MultipleLeafConditionsContext ctx)  {
        hasOrOperator = false;
        leafConditions.clear();
        booleanOperators.clear();
    }

    @Override
    public void exitMultipleLeafConditions(final MultipleLeafConditionsContext ctx) {
        cpsPathQuery.setLeafConditions(new ArrayList<>(leafConditions));
        appendConditionsToNormalizedPathSortIfNeeded();
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
        if (ctx.leafName() != null && ctx.StringLiteral() != null) {
            cpsPathQuery.setTextFunctionConditionLeafName(ctx.leafName().getText());
            cpsPathQuery.setTextFunctionConditionValue(unwrapQuotedString(ctx.StringLiteral().getText()));
        }
    }

    @Override
    public void exitContainsFunctionCondition(final CpsPathParser.ContainsFunctionConditionContext ctx) {
        if (ctx.leafName() != null && ctx.StringLiteral() != null) {
            cpsPathQuery.setContainsFunctionConditionLeafName(ctx.leafName().getText());
            cpsPathQuery.setContainsFunctionConditionValue(unwrapQuotedString(ctx.StringLiteral().getText()));
        }
    }

    @Override
    public void exitListElementRef(final CpsPathParser.ListElementRefContext ctx) {
        appendConditionsToNormalizedPathSortIfNeeded();
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

    private void appendConditionsToNormalizedPathSortIfNeeded() {
        if (!hasOrOperator && isAllEqualsOperators()) {
            leafConditions.sort(Comparator.comparing(CpsPathQuery.LeafCondition::name));
        }
        normalizedXpathBuilder.append(OPEN_BRACKET);
        boolean isStartOfExpression = true;
        final Iterator<String> operatorIterator = booleanOperators.iterator();
        for (final CpsPathQuery.LeafCondition leafCondition : leafConditions) {
            if (!isStartOfExpression) {
                normalizedXpathBuilder.append(SPACE).append(operatorIterator.next()).append(SPACE);
            }
            normalizedXpathBuilder.append(normalizeCondition(leafCondition));
            isStartOfExpression = false;
        }
        normalizedXpathBuilder.append(CLOSE_BRACKET);
    }

    private boolean isAllEqualsOperators() {
        return leafConditions.stream().allMatch(lc -> EQUALITY_OPERATOR.equals(lc.operator()));
    }

    private static String normalizeCondition(final CpsPathQuery.LeafCondition leafCondition) {
        final String normalizedValue;
        if (EQUALITY_OPERATOR.equals(leafCondition.operator())) {
            normalizedValue = wrapValueInSingleQuotes(leafCondition.value());
        } else {
            normalizedValue = leafCondition.value().toString();
        }
        return ATTRIBUTE_PREFIX + leafCondition.name() + leafCondition.operator() + normalizedValue;
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
