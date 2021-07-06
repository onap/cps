/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.cpspath.parser;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.onap.cps.cpspath.parser.antlr4.CpsPathLexer;
import org.onap.cps.cpspath.parser.antlr4.CpsPathParser;

@Getter
@Setter(AccessLevel.PACKAGE)
public class CpsPathQuery {

    private CpsPathQueryType cpsPathQueryType;
    private String xpathPrefix;
    private String leafName;
    private Object leafValue;
    private String descendantName;
    private Map<String, Object> leavesData;
    private String ancestorSchemaNodeIdentifier = "";
    private String textFunctionConditionLeafName;
    private String textFunctionConditionValue;

    /**
     * Returns a cps path query.
     *
     * @param cpsPathSource cps path
     * @return a CpsPathQuery object.
     */
    public static CpsPathQuery createFrom(final String cpsPathSource) {
        final var inputStream = CharStreams.fromString(cpsPathSource);
        final var cpsPathLexer = new CpsPathLexer(inputStream);
        final var cpsPathParser = new CpsPathParser(new CommonTokenStream(cpsPathLexer));
        cpsPathParser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                    final int charPositionInLine, final String msg, final RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        final var cpsPathBuilder = new CpsPathBuilder();
        cpsPathParser.addParseListener(cpsPathBuilder);
        cpsPathParser.cpsPath();
        return cpsPathBuilder.build();
    }

    /**
     * Has ancestor axis been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasAncestorAxis() {
        return !(ancestorSchemaNodeIdentifier.isEmpty());
    }

    /**
     * Has text function condition been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasTextFunctionCondition() {
        return !(textFunctionConditionLeafName == null);
    }

}
