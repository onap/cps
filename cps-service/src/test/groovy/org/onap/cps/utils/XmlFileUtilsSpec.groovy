/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Deutsche Telekom AG
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
package org.onap.cps.utils

import org.onap.cps.TestUtils
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class XmlFileUtilsSpec extends Specification {
    def 'Parse a valid xml content #scenario'(){
        when: 'YANG model schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        and: 'a data for that model'
            def parsedXmlContent = XmlFileUtils.prepareXmlContent(xmlData, schemaContext)
        then: 'the result was not changed'
            parsedXmlContent == output
        where:
            scenario                        | xmlData                                                                   | output
            'without root data node'        | '<?xml version="1.0" encoding="UTF-8"?><class> </class>'                  | '<?xml version="1.0" encoding="UTF-8"?><stores xmlns="urn:ietf:params:xml:ns:netconf:base:1.0"><class> </class></stores>'
            'with root data node'           | '<?xml version="1.0" encoding="UTF-8"?><stores><class> </class></stores>' | '<?xml version="1.0" encoding="UTF-8"?><stores><class> </class></stores>'
            'no xml header'                 | '<stores><class> </class></stores>'                                       | '<stores><class> </class></stores>'
    }

    def 'Parse a xml content with XPath container #scenario'() {
        when: 'YANG model schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
            def parentSchemaNode = YangUtils.getDataSchemaNodeByXpath(xPath, schemaContext)
        and: 'parse xml content'
            def parsedXmlContent = XmlFileUtils.prepareXmlContent(xmlData, parentSchemaNode, xPath)
        then: 'the result'
            parsedXmlContent == output
        where:
            scenario                 | xmlData                                                                                                                                                                                    | xPath                                 | output
            'XML element test tree'  | '<?xml version="1.0" encoding="UTF-8"?><test-tree xmlns="org:onap:cps:test:test-tree"><branch><name>Left</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch></test-tree>' | '/test-tree'                          | '<?xml version="1.0" encoding="UTF-8"?><test-tree xmlns="org:onap:cps:test:test-tree"><branch><name>Left</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch></test-tree>'
            'without root data node' | '<?xml version="1.0" encoding="UTF-8"?><nest xmlns="org:onap:cps:test:test-tree"><name>Small</name><birds>Sparrow</birds></nest>'                                                          | '/test-tree/branch[@name=\'Branch\']' | '<?xml version="1.0" encoding="UTF-8"?><branch xmlns="org:onap:cps:test:test-tree"><name>Branch</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch>'


    }

}
