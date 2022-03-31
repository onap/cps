/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

import java.util.HashMap;
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

    final CpsPathQuery cpsPathQuery = new CpsPathQuery();

    final Map<String, Object> leavesData = new HashMap<>();

    final StringBuilder normalizedXpath = new StringBuilder();

    final StringBuilder normalizedAncestorpath = new StringBuilder();

    boolean processingAncestorAxis = false;

    @Override
    public void exitPrefix(final PrefixContext ctx) {
        cpsPathQuery.setXpathPrefix(normalizedXpath.toString());
    }

    @Override
    public void exitIncorrectPrefix(final IncorrectPrefixContext ctx) {
        throw new IllegalStateException("CPS path can only start with one or two slashes (/)");
    }

    @Override
    public void exitLeafCondition(final LeafConditionContext ctx) {
        Object comparisonValue = null;
        if (ctx.IntegerLiteral() != null) {
            comparisonValue = Integer.valueOf(ctx.IntegerLiteral().getText());
        }
        if (ctx.StringLiteral() != null) {
            final boolean wasWrappedInDoubleQuote  = ctx.StringLiteral().getText().startsWith("\"");
            comparisonValue = stripFirstAndLastCharacter(ctx.StringLiteral().getText());
            if (wasWrappedInDoubleQuote) {
                comparisonValue = String.valueOf(comparisonValue).replace("'", "\\'");
            }
        } else if (comparisonValue == null) {
            throw new IllegalStateException("Unsupported comparison value encountered in expression" + ctx.getText());
        }
        leavesData.put(ctx.leafName().getText(), comparisonValue);
        normalizedXpath.append(normalizedXpath.toString().endsWith("[") ? "" : " and ")
                .append("@")
                .append(ctx.leafName().getText())
                .append("='")
                .append(comparisonValue)
                .append("'");
        if (processingAncestorAxis) {
            normalizedAncestorpath.append(normalizedAncestorpath.toString().endsWith("[") ? "" : " and ")
                    .append("@")
                    .append(ctx.leafName().getText())
                    .append("='")
                    .append(comparisonValue)
                    .append("'");
        }
    }

    @Override
    public void exitDescendant(final DescendantContext ctx) {
        cpsPathQuery.setCpsPathPrefixType(DESCENDANT);
        cpsPathQuery.setDescendantName(normalizedXpath.substring(1));
        normalizedXpath.insert(0, "/");
    }

    @Override
    public void enterMultipleLeafConditions(final MultipleLeafConditionsContext ctx)  {
        normalizedXpath.append("[");
        leavesData.clear();
    }

    @Override
    public void exitMultipleLeafConditions(final MultipleLeafConditionsContext ctx) {
        normalizedXpath.append("]");
        cpsPathQuery.setLeavesData(leavesData);
    }

    @Override
    public void enterAncestorAxis(final AncestorAxisContext ctx) {
        processingAncestorAxis = true;
    }

    @Override
    public void exitAncestorAxis(final AncestorAxisContext ctx) {
        if (processingAncestorAxis) {
            cpsPathQuery.setAncestorSchemaNodeIdentifier(normalizedAncestorpath.substring(1));
        }
    }

    @Override
    public void exitTextFunctionCondition(final TextFunctionConditionContext ctx) {
        cpsPathQuery.setTextFunctionConditionLeafName(ctx.leafName().getText());
        cpsPathQuery.setTextFunctionConditionValue(stripFirstAndLastCharacter(ctx.StringLiteral().getText()));
    }

    @Override
    public void enterListElementRef(final CpsPathParser.ListElementRefContext ctx) {
        normalizedXpath.append("[");
        if (processingAncestorAxis) {
            normalizedAncestorpath.append("[");
        }
    }

    @Override
    public void exitListElementRef(final CpsPathParser.ListElementRefContext ctx) {
        normalizedXpath.append("]");
        if (processingAncestorAxis) {
            normalizedAncestorpath.append("]");
        }
    }

    CpsPathQuery build() {
        cpsPathQuery.setNormalizedXpath(normalizedXpath.toString());
        return cpsPathQuery;
    }

    private static String stripFirstAndLastCharacter(final String wrappedString) {
        return wrappedString.substring(1, wrappedString.length() - 1);
    }

    @Override
    public void exitContainerName(final CpsPathParser.ContainerNameContext ctx) {
        normalizedXpath.append("/")
                .append(ctx.getText());
        if (processingAncestorAxis) {
            normalizedAncestorpath.append("/").append(ctx.getText());
        }
    }
}
