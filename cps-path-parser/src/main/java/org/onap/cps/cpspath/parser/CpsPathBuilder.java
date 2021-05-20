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
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser;

public class CpsPathBuilder extends CpsPathBaseListener {

    final CpsPathQuery cpsPathQuery = new CpsPathQuery();

    final Map<String, Object> leavesData = new HashMap<>();

    private Object lastValue = null;

    @Override
    public void exitPrefix(final CpsPathParser.PrefixContext ctx) {
        cpsPathQuery.setXpathPrefix(ctx.getText());
    }

    @Override
    public void exitIntValue(final CpsPathParser.IntValueContext ctx) {
        lastValue = Integer.valueOf(ctx.getText());
    }

    @Override
    public void exitStringValue(final CpsPathParser.StringValueContext ctx) {
        lastValue = stripFirstAndLastCharacter(ctx.getText());
    }

    @Override
    public void exitLeafCondition(final CpsPathParser.LeafConditionContext ctx) {
        leavesData.put(ctx.leafName().getText(), lastValue);
    }

    @Override
    public void enterSingleValueCondition(final CpsPathParser.SingleValueConditionContext ctx) {
       leavesData.clear();
    }

    @Override
    public void enterMultipleValueConditions(final CpsPathParser.MultipleValueConditionsContext ctx) {
        leavesData.clear();
    }

    @Override
    public void exitSingleValueCondition(final CpsPathParser.SingleValueConditionContext ctx) {
        final String leafName = ctx.leafCondition().leafName().getText();
        cpsPathQuery.setLeafName(leafName);
        cpsPathQuery.setLeafValue(leavesData.get(leafName));
    }

    @Override
    public void exitCpsPathWithSingleLeafCondition(final CpsPathParser.CpsPathWithSingleLeafConditionContext ctx) {
        cpsPathQuery.setCpsPathQueryType(CpsPathQueryType.XPATH_LEAF_VALUE);
    }

    @Override
    public void exitCpsPathWithDescendant(final CpsPathParser.CpsPathWithDescendantContext ctx) {
        cpsPathQuery.setCpsPathQueryType(CpsPathQueryType.XPATH_HAS_DESCENDANT_ANYWHERE);
        cpsPathQuery.setDescendantName(cpsPathQuery.getXpathPrefix().substring(1));
    }

    @Override
    public void exitCpsPathWithDescendantAndLeafConditions(
        final CpsPathParser.CpsPathWithDescendantAndLeafConditionsContext ctx) {
        cpsPathQuery.setCpsPathQueryType(CpsPathQueryType.XPATH_HAS_DESCENDANT_WITH_LEAF_VALUES);
        cpsPathQuery.setDescendantName(cpsPathQuery.getXpathPrefix().substring(1));
        cpsPathQuery.setLeavesData(leavesData);
    }

    @Override
    public void exitAncestorAxis(final CpsPathParser.AncestorAxisContext ctx) {
        cpsPathQuery.setAncestorSchemaNodeIdentifier(ctx.ancestorPath().getText());
    }

    CpsPathQuery build() {
        return cpsPathQuery;
    }

    private static String stripFirstAndLastCharacter(final String wrappedString) {
        return wrappedString.substring(1, wrappedString.length() - 1);
    }

}
