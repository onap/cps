/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

import spock.lang.Specification

class CpsPathBuilderSpec extends Specification{

    def 'Verify ancestor axis handling in XPath parsing'() {
        given: 'A CpsPathBuilder instance initialized for XPath parsing'
            def xpath = '//parent-node/leaf-name[text()="some-value"]/ancestor::parent-node'
        when: 'XPath is parsed and builder is used'
            def result = parseXPathAndBuild(xpath)
        then: 'ancestor schema node identifier is correctly handled'
            assert result.ancestorSchemaNodeIdentifier == ''
    }

    def parseXPathAndBuild(xpath) {
        def cpsPathBuilder = CpsPathUtil.getCpsPathBuilder(xpath)
        cpsPathBuilder.build()
    }
}
