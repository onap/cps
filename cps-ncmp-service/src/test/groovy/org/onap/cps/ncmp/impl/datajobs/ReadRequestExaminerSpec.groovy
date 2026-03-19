/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs

import spock.lang.Specification

class ReadRequestExaminerSpec extends Specification {

    def objectUnderTest = new ReadRequestExaminer()

    def 'Split data node selector with #scenario.'() {
        when: 'the data node selector is split'
            def result = objectUnderTest.splitDataNodeSelector(dataNodeSelector)
        then: 'the expected selectors are returned'
            assert result == expectedSelectors
        where:
            scenario                    | dataNodeSelector          || expectedSelectors
            'single selector'           | 'fdn1'                   || ['fdn1']
            'newline delimiter'         | 'fdn1\nfdn2'             || ['fdn1', 'fdn2']
            'OR delimiter'              | 'fdn1 OR fdn2'           || ['fdn1 ', ' fdn2']
            'mixed delimiters'          | 'fdn1\nfdn2 OR fdn3'    || ['fdn1', 'fdn2 ', ' fdn3']
            'multiple newlines'         | 'fdn1\nfdn2\nfdn3'      || ['fdn1', 'fdn2', 'fdn3']
            'multiple OR delimiters'    | 'fdn1 OR fdn2 OR fdn3'  || ['fdn1 ', ' fdn2 ', ' fdn3']
    }
}
