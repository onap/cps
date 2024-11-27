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

package org.onap.cps.cpspath.parser;

import static org.onap.cps.cpspath.parser.CpsPathPrefixType.DESCENDANT;

import java.util.ArrayList;
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
        booleanOperators.add(ctx.getText());
    }

    @Override
    public void exitDescendant(final DescendantContext ctx) {
        cpsPathQuery.setCpsPathPrefixType(DESCENDANT);
        cpsPathQuery.setDescendantName(normalizedXpathBuilder.substring(2));
    }

    @Override
    public void enterMultipleLeafConditions(final MultipleLeafConditionsContext ctx)  {
        normalizedXpathBuilder.append(OPEN_BRACKET);
        leafConditions.clear();
        booleanOperators.clear();
    }

    @Override
    public void exitMultipleLeafConditions(final MultipleLeafConditionsContext ctx) {
        normalizedXpathBuilder.append(CLOSE_BRACKET);
        cpsPathQuery.setLeafConditions(leafConditions);
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
    }

    @Override
    public void exitListElementRef(final CpsPathParser.ListElementRefContext ctx) {
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
        leafConditions.add(new CpsPathQuery.LeafCondition(leafName, operator, comparisonValue));
        appendCondition(normalizedXpathBuilder, leafName, operator, comparisonValue);
    }

    private void appendCondition(final StringBuilder currentNormalizedPathBuilder, final String name,
                                 final String operator, final Object value) {
        final char lastCharacter = currentNormalizedPathBuilder.charAt(currentNormalizedPathBuilder.length() - 1);
        final boolean isStartOfExpression = lastCharacter == '[';
        if (!isStartOfExpression) {
            currentNormalizedPathBuilder.append(" ").append(getLastElement(booleanOperators)).append(" ");
        }
        currentNormalizedPathBuilder.append("@").append(name).append(operator);
        if (operator.equals("=")) {
            currentNormalizedPathBuilder.append(wrapValueInSingleQuotes(value));
        } else {
            currentNormalizedPathBuilder.append(value);
        }
    }

    private static String getLastElement(final List<String> listOfStrings) {
        return listOfStrings.get(listOfStrings.size() - 1);
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
