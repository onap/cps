/*
 * ============LICENSE_START=======================================================
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
package org.onap.cps.ncmp.api.impl

import spock.lang.Specification

class JsonUtilsSpec extends Specification  {
    def 'Remove redundant escape characters.'() {
        expect: 'removing redundant escape characters returns the correct output for #scenario'
            JsonUtils.removeRedundantEscapeCharacters(input) == expectedOutput
        where: 'the following input is used'
            scenario                               | input                                      || expectedOutput
            'two lines'                            | 'line1\\nline2'                            || 'line1\nline2'
            'a string inside quotes'               | 'a \\"word in quotes\\"'                   || 'a "word in quotes"'
            'quotes inside quotes (double escape)' | '\\"quotes \\\\\\"inside\\\\\\" quotes\\"' || '"quotes \\"inside\\" quotes"'  // human readable:  "quotes \"inside\" quotes"
    }
    def 'Remove wrapping tokens.'() {
        expect: 'removing wrapping tokens returns the correct output for #scenario'
            JsonUtils.removeWrappingTokens(input) == expectedOutput
        where: 'the following input is used'
            scenario                           | input    || expectedOutput
            'a string in quotes'               | '"abc"'  || 'abc'
            'a string in apostrophes'          | "'abc'"  || 'abc'
            'a string inside any other tokens' | 'abcde'  || 'bcd'
    }
}

