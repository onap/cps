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

    def 'Parsing single JSON Expression with #scenario.'() {
        when: 'the parser extracts FDNs'
            def result = JexParser.extractFdnsFromLocationPaths(locationPath)
        then: 'only the expected top-level absolute paths with id is returned'
            assert result[0] == expectedFdn
        where: 'Following expressions are used'
            scenario                                          | locationPath                                     || expectedFdn
            'single segment'                                  | '/SubNetwork[id="SN1"]'                          || '/SubNetwork=SN1'
            'two segments'                                    | '/SubNetwork[id="SN1"]/ManagedElement[id="ME1"]' || '/SubNetwork=SN1/ManagedElement=ME1'
            'segment and mo without id'                       | '/SubNetwork[id="SN1"]/attributes]'              || '/SubNetwork=SN1'
            'segment and mos without id'                      | '/SubNetwork[id="SN1"]/attributes/vendorName'    || '/SubNetwork=SN1'
            'segment and mo with other attribute expressions' | '/SubNetwork[id="SN1"]/vendor[name="V1"]'        || '/SubNetwork=SN1'
            'segment followed by wildcard'                    | '/SubNetwork[id="SN1"]/*'                        || '/SubNetwork=SN1'
    }

    def 'Parsing multiple JSON Expressions.'() {
        given: 'multiple JSON expressions with multiple absolute paths, attributes, and filters'
            def locationPath = """
            /SubNetwork[id="SN1"]/ManagedElement
            /SubNetwork[id="SN2"]
            """
        when: 'the parser extracts FDNs'
            def result = JexParser.extractFdnsFromLocationPaths(locationPath)
        then: 'the expected paths with ids are returned'
            assert result.size() == 2
            assert result.containsAll(['/SubNetwork=SN1', '/SubNetwork=SN2'])
    }

    def 'Parsing multiple JSON Expressions with duplicate results.'() {
        given: 'multiple JSON expressions with multiple absolute paths, attributes, and filters'
            def locationPath = """
            /SubNetwork[id="SN1"]/ManagedElement
            /SubNetwork[id="SN1"]
            """
        when: 'the parser extracts FDNs'
            def result = JexParser.extractFdnsFromLocationPaths(locationPath)
        then: 'only one unique path with id is returned'
            assert result == ['/SubNetwork=SN1']
    }

    def 'Ignored expressions #scenario.'() {
        when: 'the parser extracts FDNs'
            def result = JexParser.extractFdnsFromLocationPaths(locationPath)
        then: 'the result is empty'
            assert result.isEmpty()
        where: 'Following expressions are used'
            scenario            | locationPath
            'comments'          | '&&text only comment'
            'commented out FDN' | '&&/SubNetwork[id="SN1"]/ManagedElement[id="ME1"]'
            'blank'             | ''
            'root'              | '/'
            'no IDs at all'     | '/SubNetwork/attribute'
            'null'              | null
    }
}




