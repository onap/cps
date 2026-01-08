/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2025 Deutsche Telekom AG
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
        return getCpsPathBuilder(xpathSource).build().getNormalizedXpath();
    }

    /**
     * Returns the parent xpath.
     *
     * @param xpathSource xpath
     * @return the parent xpath String.
     */
    public static String getNormalizedParentXpath(final String xpathSource) {
        return getCpsPathBuilder(xpathSource).build().getNormalizedParentPath();
    }

    public static List<String> getXpathNodeIdSequence(final String xpathSource) {
        return getCpsPathBuilder(xpathSource).build().getContainerNames();
    }

    /**
     * Returns boolean indicating xpath is an absolute path to a list element.
     *
     * @param xpathSource xpath
     * @return true if xpath is an absolute path to a list element
     */
    public static boolean isPathToListElement(final String xpathSource) {
        final CpsPathQuery cpsPathQuery = getCpsPathBuilder(xpathSource).build();
        return cpsPathQuery.isPathToListElement();
    }

    /**
     * Returns a cps path query.
     *
     * @param cpsPathSource cps path
     * @return a CpsPathQuery object.
     */

    public static CpsPathQuery getCpsPathQuery(final String cpsPathSource) {
        return getCpsPathBuilder(cpsPathSource).build();
    }

    private static CpsPathBuilder getCpsPathBuilder(final String cpsPathSource) {
        final CharStream inputStream = CharStreams.fromString(cpsPathSource);
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
        cpsPathParser.cpsPath();
        return cpsPathBuilder;
    }
}
