/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
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
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(CpsPathBuilder.class.getName());

    private static final String OPEN_BRACKET = "[";

    private static final String CLOSE_BRACKET = "]";

    private final CpsPathQuery cpsPathQuery = new CpsPathQuery();

    private final List<CpsPathQuery.DataLeaf> leavesData = new ArrayList<>();

    private final StringBuilder normalizedXpathBuilder = new StringBuilder();

    private final StringBuilder normalizedAncestorPathBuilder = new StringBuilder();

    private boolean processingAncestorAxis = false;

    private final List<String> containerNames = new ArrayList<>();

    private final List<String> booleanOperators = new ArrayList<>();

    private final List<String> comparativeOperators = new ArrayList<>();

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
        final Object comparisonValue;
        if (ctx.IntegerLiteral() != null) {
            comparisonValue = Integer.valueOf(ctx.IntegerLiteral().getText());
        } else if (ctx.StringLiteral() != null) {
            comparisonValue = unwrapQuotedString(ctx.StringLiteral().getText());
        } else {
            throw new PathParsingException("Unsupported comparison value encountered in expression" + ctx.getText());
        }
        leafContext(ctx.leafName(), comparisonValue);
    }

    @Override
    public void exitBooleanOperators(final CpsPathParser.BooleanOperatorsContext ctx) {
        booleanOperators.add(ctx.getText());
    }

    @Override
    public void exitComparativeOperators(final CpsPathParser.ComparativeOperatorsContext ctx) {
        comparativeOperators.add(ctx.getText());
    }

    @Override
    public void exitDescendant(final DescendantContext ctx) {
        cpsPathQuery.setCpsPathPrefixType(DESCENDANT);
        cpsPathQuery.setDescendantName(normalizedXpathBuilder.substring(1));
        normalizedXpathBuilder.insert(0, "/");
    }

    @Override
    public void enterMultipleLeafConditions(final MultipleLeafConditionsContext ctx)  {
        normalizedXpathBuilder.append(OPEN_BRACKET);
        leavesData.clear();
        booleanOperators.clear();
        comparativeOperators.clear();
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
        cpsPathQuery.setBooleanOperators(booleanOperators);
        cpsPathQuery.setComparativeOperators(comparativeOperators);
        if (cpsPathQuery.hasAncestorAxis() && cpsPathQuery.getXpathPrefix()
                .endsWith("/" + cpsPathQuery.getAncestorSchemaNodeIdentifier())) {
            cpsPathQuery.setAncestorSchemaNodeIdentifier("");
            logger.warning("Ancestor axis ignored because it is of same type as the target.");
        }
        return cpsPathQuery;
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

    private void leafContext(final CpsPathParser.LeafNameContext ctx, final Object comparisonValue) {
        leavesData.add(new CpsPathQuery.DataLeaf(ctx.getText(), comparisonValue));
        appendCondition(normalizedXpathBuilder, ctx.getText(), comparisonValue);
        if (processingAncestorAxis) {
            appendCondition(normalizedAncestorPathBuilder, ctx.getText(), comparisonValue);
        }
    }

    private void appendCondition(final StringBuilder currentNormalizedPathBuilder, final String name,
                                 final Object value) {
        final char lastCharacter = currentNormalizedPathBuilder.charAt(currentNormalizedPathBuilder.length() - 1);
        final boolean isStartOfExpression = lastCharacter == '[';
        if (!isStartOfExpression) {
            currentNormalizedPathBuilder.append(" ").append(getLastElement(booleanOperators)).append(" ");
        }
        currentNormalizedPathBuilder.append("@")
                                    .append(name)
                                    .append(getLastElement(comparativeOperators))
                                    .append("'")
                                    .append(value.toString().replace("'", "''"))
                                    .append("'");
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

    private static String stripFirstAndLastCharacter(final String wrappedString) {
        return wrappedString.substring(1, wrappedString.length() - 1);
    }
}
