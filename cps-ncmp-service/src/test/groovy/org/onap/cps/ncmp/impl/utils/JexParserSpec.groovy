/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.utils

import spock.lang.Specification

class JexParserSpec extends Specification {

    def 'Parsing list of json expressions with #scenario.'() {
        when: 'the parser gets a (muti-line) json expressions'
            def result = JexParser.toXpaths(jsonExpressions)
        then: 'the expected xpaths are returned'
            assert result == ['/SubNetwork[id="SN1"]']
        where: 'Following expressions are used'
            scenario               | jsonExpressions
            'single segment'       | '/SubNetwork[id="SN1"]'
            'trimmed segment'      | '  /SubNetwork[id="SN1"]  '
            'duplicate segments'   | '/SubNetwork[id="SN1"]\n/SubNetwork[id="SN1"]'
            'comment with segment' | '&&ignore this\n/SubNetwork[id="SN1"]'
    }

    def 'Extracts xpaths from json expressions, ignored expressions: #scenario.'() {
        when: 'the parser gets a (muti-line) json expressions'
            def result = JexParser.toXpaths(jsonExpressions)
        then: 'the result is empty'
            assert result.isEmpty()
        where: 'Following expressions are used'
            scenario            | jsonExpressions
            'null input'        | null
            'comments only'     | '&&text only comment'
            'commented out FDN' | '&&/SubNetwork[id="SN1"]/ManagedElement[id="ME1"]'
    }

    def 'Convert xpaths to json expressions.'() {
        given: 'list of xpaths'
            def xpaths = ['/SubNetwork[id="SN1"]', '/ManagedElement']
        when: 'parser gets xpaths'
            def result = JexParser.toJsonExpressionsAsString(xpaths)
        then: 'the expected multi-line json expression returned'
            assert result == '/SubNetwork[id="SN1"]\n/ManagedElement'
    }

    def 'Extracts fdn from xpath with #scenario.'() {
        when: 'the parser extracts the fdn'
            def result = JexParser.extractFdnPrefix(xpath)
        then: 'the expected FDN is returned'
            assert result.orElse(null) == expectedFdn
        where: 'Following xpaths are used'
            scenario                                          | xpath                                            || expectedFdn
            'single segment'                                  | '/SubNetwork[id="SN1"]'                          || '/SubNetwork=SN1'
            'two segments'                                    | '/SubNetwork[id="SN1"]/ManagedElement[id="ME1"]' || '/SubNetwork=SN1/ManagedElement=ME1'
            'segment and mo without id'                       | '/SubNetwork[id="SN1"]/attributes'               || '/SubNetwork=SN1'
            'segment and mos without id'                      | '/SubNetwork[id="SN1"]/attributes/vendorName'    || '/SubNetwork=SN1'
            'segment and mo with other attribute expressions' | '/SubNetwork[id="SN1"]/vendor[name="V1"]'        || '/SubNetwork=SN1'
            'segment followed by wildcard'                    | '/SubNetwork[id="SN1"]/*'                        || '/SubNetwork=SN1'
    }

    def 'Extracts fdn from xpath, ignored expressions: #scenario.'() {
        when: 'the parser extracts fdns'
            def result = JexParser.extractFdnPrefix(xpaths)
        then: 'the result is empty'
            assert result.isEmpty()
        where: 'Following xpaths are used'
            scenario        | xpaths
            'blank'         | ''
            'root'          | '/'
            'no IDs at all' | '/SubNetwork/attribute'
    }
}




