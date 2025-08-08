/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd
 *  Modifications Copyright (C) 2025 xAI
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
 *  SPDX-License-LicenseIdentifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.query.parser;

import org.onap.cps.query.parser.antlr4.QuerySelectWhereLexer;
import org.onap.cps.query.parser.antlr4.QuerySelectWhereParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.onap.cps.cpspath.parser.PathParsingException;

public class QuerySelectWhereUtil {

    public static QuerySelectWhere getQuerySelectWhere(final String select, final String where) {
        final String queryString = "SELECT " + select + (where != null && !where.isEmpty() ? " WHERE " + where : "");
        final QuerySelectWhereLexer lexer = new QuerySelectWhereLexer(CharStreams.fromString(queryString));
        final QuerySelectWhereParser parser = new QuerySelectWhereParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                    final int charPositionInLine, final String msg, final RecognitionException e) {
                throw new PathParsingException("Failed to parse query at line " + line + ": " + msg,
                        e == null ? "" : e.getMessage());
            }
        });
        final QuerySelectWhereBuilder builder = new QuerySelectWhereBuilder();
        parser.addParseListener(builder);
        parser.query();
        return builder.build();
    }
}