/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2023 Tech Mahindra.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onap.cps.cpspath.parser.antlr4.CpsPathBaseListener;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.AncestorAxisContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.DescendantContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.IncorrectPrefixContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.LeafConditionContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.MultipleLeafConditionsContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.PrefixContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.TextFunctionConditionContext;

public class CpsPathBuilder extends CpsPathBaseListener {

    private static final String OPEN_BRACKET = "[";

    private static final String CLOSE_BRACKET = "]";

    final CpsPathQuery cpsPathQuery = new CpsPathQuery();

    final Map<String, Object> leavesData = new HashMap<>();

    final StringBuilder normalizedXpathBuilder = new StringBuilder();

    final StringBuilder normalizedAncestorPathBuilder = new StringBuilder();

    boolean processingAncestorAxis = false;

    private List<String> containerNames = new ArrayList<>();

    final List<CpsPathOperatorType> operators = new ArrayList<>();

    @Override
    public void exitInvalidPostFix(final CpsPathParser.InvalidPostFixContext ctx) {
        throw new PathParsingException(ctx.getText());
    }

    @Override
    public void exitPrefix(final PrefixContext ctx) {
        cpsPathQuery.setXpathPrefix(normalizedXpathBuilder.toString());
    }

    @Override
    public void exitParent(final CpsPathParser.ParentContext ctx) {
        cpsPathQuery.setNormalizedParentPath(normalizedXpathBuilder.toString());
    }

    @Override
    public void exitIncorrectPrefix(final IncorrectPrefixContext ctx) {
        throw new PathParsingException("CPS path can only start with one or two slashes (/)");
    }

    @Override
    public void exitLeafCondition(final LeafConditionContext ctx) {
        Object comparisonValue = null;
        if (ctx.IntegerLiteral() != null) {
            comparisonValue = Integer.valueOf(ctx.IntegerLiteral().getText());
        }
        if (ctx.StringLiteral() != null) {
            final boolean wasWrappedInDoubleQuote = ctx.StringLiteral().getText().startsWith("\"");
            comparisonValue = stripFirstAndLastCharacter(ctx.StringLiteral().getText());
            if (wasWrappedInDoubleQuote) {
                comparisonValue = String.valueOf(comparisonValue).replace("'", "\\'");
            }
        } else if (comparisonValue == null) {
            throw new PathParsingException("Unsupported comparison value encountered in expression" + ctx.getText());
        }
        leavesData.put(ctx.leafName().getText(), comparisonValue);
        final char lastCharacter = normalizedXpathBuilder.charAt(normalizedXpathBuilder.length() - 1);
        final String operatorName = operators.toString().replace("[", "").replace("]", "");
        final String[] operatorValues = operatorName.split(",");
        for (final String operatorValue : operatorValues) {
            normalizedXpathBuilder.append(lastCharacter == '[' ? "" : " " + operatorValue + " ");
        }
        normalizedXpathBuilder.append("@").append(ctx.leafName().getText()).append("='").append(comparisonValue)
                              .append("'");
        if (processingAncestorAxis) {
            normalizedAncestorPathBuilder.append(lastCharacter == '[' ? "" : " and ");
            normalizedAncestorPathBuilder.append("@").append(ctx.leafName().getText()).append("='");
            normalizedAncestorPathBuilder.append(comparisonValue).append("'");
        }
    }

    @Override
    public void exitBooleanOperators(final CpsPathParser.BooleanOperatorsContext ctx) {
        if (ctx.getText().equalsIgnoreCase("or")) {
            operators.add(CpsPathOperatorType.or);
        }
        if (ctx.getText().equalsIgnoreCase("and")) {
            operators.add(CpsPathOperatorType.and);
        }
        cpsPathQuery.setOperatorName(operators);
    }

    @Override
    public void exitDescendant(final DescendantContext ctx) {
        cpsPathQuery.setCpsPathPrefixType(DESCENDANT);
        cpsPathQuery.setDescendantName(normalizedXpathBuilder.substring(1));
        normalizedXpathBuilder.insert(0, "/");
    }

    @Override
    public void enterMultipleLeafConditions(final MultipleLeafConditionsContext ctx) {
        normalizedXpathBuilder.append(OPEN_BRACKET);
        leavesData.clear();
    }

    @Override
    public void exitMultipleLeafConditions(final MultipleLeafConditionsContext ctx) {
        normalizedXpathBuilder.append(CLOSE_BRACKET);
        cpsPathQuery.setLeavesData(leavesData);
    }

    @Override
    public void enterAncestorAxis(final AncestorAxisContext ctx) {
        processingAncestorAxis = true;
    }

    @Override
    public void exitAncestorAxis(final AncestorAxisContext ctx) {
        cpsPathQuery.setAncestorSchemaNodeIdentifier(normalizedAncestorPathBuilder.substring(1));
        processingAncestorAxis = false;
    }

    @Override
    public void exitTextFunctionCondition(final TextFunctionConditionContext ctx) {
        cpsPathQuery.setTextFunctionConditionLeafName(ctx.leafName().getText());
        cpsPathQuery.setTextFunctionConditionValue(stripFirstAndLastCharacter(ctx.StringLiteral().getText()));
    }

    @Override
    public void enterListElementRef(final CpsPathParser.ListElementRefContext ctx) {
        normalizedXpathBuilder.append(OPEN_BRACKET);
        if (processingAncestorAxis) {
            normalizedAncestorPathBuilder.append(OPEN_BRACKET);
        }
    }

    @Override
    public void exitListElementRef(final CpsPathParser.ListElementRefContext ctx) {
        normalizedXpathBuilder.append(CLOSE_BRACKET);
        if (processingAncestorAxis) {
            normalizedAncestorPathBuilder.append(CLOSE_BRACKET);
        }
    }

    CpsPathQuery build() {
        cpsPathQuery.setNormalizedXpath(normalizedXpathBuilder.toString());
        cpsPathQuery.setContainerNames(containerNames);
        return cpsPathQuery;
    }

    private static String stripFirstAndLastCharacter(final String wrappedString) {
        return wrappedString.substring(1, wrappedString.length() - 1);
    }

    @Override
    public void exitContainerName(final CpsPathParser.ContainerNameContext ctx) {
        final String containerName = ctx.getText();
        normalizedXpathBuilder.append("/")
                              .append(containerName);
        containerNames.add(containerName);
        if (processingAncestorAxis) {
            normalizedAncestorPathBuilder.append("/").append(containerName);
        }
    }
}
