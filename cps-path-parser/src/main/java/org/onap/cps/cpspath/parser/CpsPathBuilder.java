package org.onap.cps.cpspath.parser;
/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

import java.util.HashMap;
import java.util.Map;
import org.onap.cps.cpspath.parser.antlr4.CpsPathBaseListener;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.AncestorAxisContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.CpsPathWithDescendantAndLeafConditionsContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.CpsPathWithDescendantContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.CpsPathWithSingleLeafConditionContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.LeafConditionContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.MultipleValueConditionsContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.PrefixContext;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser.SingleValueConditionContext;

public class CpsPathBuilder extends CpsPathBaseListener {

    final CpsPathQuery cpsPathQuery = new CpsPathQuery();

    final Map<String, Object> leavesData = new HashMap<>();

    @Override
    public void exitPrefix(final PrefixContext ctx) {
        cpsPathQuery.setXpathPrefix(ctx.getText());
    }

    @Override
    public void exitLeafCondition(final LeafConditionContext ctx) {
        Object comparisonValue = null;
        if (ctx.IntegerLiteral() != null) {
            comparisonValue = Integer.valueOf(ctx.IntegerLiteral().getText());
        }
        if (ctx.StringLiteral() != null) {
            comparisonValue = stripFirstAndLastCharacter(ctx.StringLiteral().getText());
        } else if (comparisonValue == null) {
            throw new IllegalStateException("Unsupported comparison value encountered in expression" + ctx.getText());
        }
        leavesData.put(ctx.leafName().getText(), comparisonValue);
    }

    @Override
    public void enterSingleValueCondition(final SingleValueConditionContext ctx) {
        leavesData.clear();
    }

    @Override
    public void enterMultipleValueConditions(final MultipleValueConditionsContext ctx) {
        leavesData.clear();
    }

    @Override
    public void exitSingleValueCondition(final SingleValueConditionContext ctx) {
        final String leafName = ctx.leafCondition().leafName().getText();
        cpsPathQuery.setLeafName(leafName);
        cpsPathQuery.setLeafValue(leavesData.get(leafName));
    }

    @Override
    public void exitCpsPathWithSingleLeafCondition(final CpsPathWithSingleLeafConditionContext ctx) {
        cpsPathQuery.setCpsPathQueryType(CpsPathQueryType.XPATH_LEAF_VALUE);
    }

    @Override
    public void exitCpsPathWithDescendant(final CpsPathWithDescendantContext ctx) {
        cpsPathQuery.setCpsPathQueryType(CpsPathQueryType.XPATH_HAS_DESCENDANT_ANYWHERE);
        cpsPathQuery.setDescendantName(cpsPathQuery.getXpathPrefix().substring(1));
    }

    @Override
    public void exitCpsPathWithDescendantAndLeafConditions(
        final CpsPathWithDescendantAndLeafConditionsContext ctx) {
        cpsPathQuery.setCpsPathQueryType(CpsPathQueryType.XPATH_HAS_DESCENDANT_WITH_LEAF_VALUES);
        cpsPathQuery.setDescendantName(cpsPathQuery.getXpathPrefix().substring(1));
        cpsPathQuery.setLeavesData(leavesData);
    }

    @Override
    public void exitAncestorAxis(final AncestorAxisContext ctx) {
        cpsPathQuery.setAncestorSchemaNodeIdentifier(ctx.ancestorPath().getText());
    }

    CpsPathQuery build() {
        return cpsPathQuery;
    }

    private static String stripFirstAndLastCharacter(final String wrappedString) {
        return wrappedString.substring(1, wrappedString.length() - 1);
    }

}
