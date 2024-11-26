/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Deutsche Telekom AG
 *  Modifications Copyright (c) 2023-2024 Nordix Foundation
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.w3c.dom.DOMException
import org.xml.sax.SAXParseException
import spock.lang.Specification

import static org.onap.cps.utils.XmlFileUtils.convertDataMapsToXml

class XmlFileUtilsSpec extends Specification {

    def 'Parse a valid xml content #scenario'() {
        given: 'YANG model schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'the xml data is parsed'
            def parsedXmlContent = XmlFileUtils.prepareXmlContent(xmlData, schemaContext)
        then: 'the result xml is wrapped by root node defined in YANG schema'
            assert parsedXmlContent == expectedOutput
        where:
            scenario                 | xmlData                                                                   || expectedOutput
            'without root data node' | '<?xml version="1.0" encoding="UTF-8"?><class> </class>'                  || '<?xml version="1.0" encoding="UTF-8"?><stores xmlns="urn:ietf:params:xml:ns:netconf:base:1.0"><class> </class></stores>'
            'with root data node'    | '<?xml version="1.0" encoding="UTF-8"?><stores><class> </class></stores>' || '<?xml version="1.0" encoding="UTF-8"?><stores><class> </class></stores>'
            'no xml header'          | '<stores><class> </class></stores>'                                       || '<stores><class> </class></stores>'
    }

    def 'Parse a invalid xml content'() {
        given: 'YANG model schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'attempt to parse invalid xml'
            XmlFileUtils.prepareXmlContent('invalid-xml', schemaContext)
        then: 'a Sax Parser exception is thrown'
            thrown(SAXParseException)
    }

    def 'Parse a xml content with XPath container #scenario'() {
        given: 'YANG model schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        and: 'Parent schema node by xPath'
            def parentSchemaNode = YangParserHelper.getDataSchemaNodeAndIdentifiersByXpath(xPath, schemaContext).get('dataSchemaNode')
        when: 'the XML data is parsed'
            def parsedXmlContent = XmlFileUtils.prepareXmlContent(xmlData, parentSchemaNode, xPath)
        then: 'the result XML is wrapped by xPath defined parent root node'
            assert parsedXmlContent == expectedOutput
        where:
            scenario                 | xmlData                                                                                                                                                                                    | xPath                                 || expectedOutput
            'XML element test tree'  | '<?xml version="1.0" encoding="UTF-8"?><test-tree xmlns="org:onap:cps:test:test-tree"><branch><name>Left</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch></test-tree>' | '/test-tree'                          || '<?xml version="1.0" encoding="UTF-8"?><test-tree xmlns="org:onap:cps:test:test-tree"><branch><name>Left</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch></test-tree>'
            'without root data node' | '<?xml version="1.0" encoding="UTF-8"?><nest xmlns="org:onap:cps:test:test-tree"><name>Small</name><birds>Sparrow</birds></nest>'                                                          | '/test-tree/branch[@name=\'Branch\']' || '<?xml version="1.0" encoding="UTF-8"?><branch xmlns="org:onap:cps:test:test-tree"><name>Branch</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch>'
    }

    def 'Convert data maps to XML #scenario'() {
        when: 'data maps are converted to XML'
            def result = convertDataMapsToXml(dataMaps)
        then: 'the result contains the expected XML'
            assert result == expectedXmlOutput
        where:
            scenario                              | dataMaps                                                                                                                                 || expectedXmlOutput
            'single XML branch'                   | [['branch': ['name': 'Left', 'nest': ['name': 'Small', 'birds': ['Sparrow', 'Owl']]]]]                                                   || '<branch><name>Left</name><nest><name>Small</name><birds>Sparrow</birds><birds>Owl</birds></nest></branch>'
            'nested XML branch'                   | [['test-tree': [branch: [name: 'Left', nest: [name: 'Small', birds: 'Sparrow']]]]]                                                       || '<test-tree><branch><name>Left</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch></test-tree>'
            'list of branch within a test tree'   | [['test-tree': [branch: [[name: 'Left', nest: [name: 'Small', birds: 'Sparrow']], [name: 'Right', nest: [name: 'Big', birds: 'Owl']]]]]] || '<test-tree><branch><name>Left</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch><branch><name>Right</name><nest><name>Big</name><birds>Owl</birds></nest></branch></test-tree>'
            'list of birds under a nest'          | [['nest': ['name': 'Small', 'birds': ['Sparrow']]]]                                                                                      || '<nest><name>Small</name><birds>Sparrow</birds></nest>'
    }

    def 'Convert data maps to XML with null or empty maps and lists'() {
        when: 'data maps with empty content are converted to XML'
            def result = convertDataMapsToXml(dataMaps)
        then: 'the result contains the expected XML or handles nulls correctly'
            assert result == expectedXmlOutput
        where:
            scenario                         | dataMaps                                                                                    || expectedXmlOutput
            'null entry in map'              | [['branch': []]]                                                                            || '<branch/>'
            'XML Content list is empty'      | [['nest': ['name': 'Small', 'birds': [null]]]]                                              || '<nest><name>Small</name><birds/></nest>'
            'XML with mixed content in list' | [['branch': ['name': 'Left', 'nest': ['name': 'Small', 'birds': [null, 'Sparrow']]]]]       || '<branch><name>Left</name><nest><name>Small</name><birds/><birds>Sparrow</birds></nest></branch>'
            'list with null object'          | [['branch': [name: 'Left', nest: [name: 'Small', birds: [null]]]]]                          || '<branch><name>Left</name><nest><name>Small</name><birds/></nest></branch>'
            'list containing null values'    | [['branch': [null, null, null]]]                                                            || '<branch/><branch/><branch/>'
            'nested map with null values'    | [['test-tree': [branch: [name: 'Left', nest: null]]]]                                       || '<test-tree><branch><name>Left</name><nest/></branch></test-tree>'
            'mixed list with null values'    | [['branch': ['name': 'Left', 'nest': ['name': 'Small', 'birds': [null, 'Sparrow', null]]]]] || '<branch><name>Left</name><nest><name>Small</name><birds/><birds>Sparrow</birds><birds/></nest></branch>'
    }

    def 'Converting data maps to xml with no data'() {
        given: 'A list of maps where entry is null'
            def dataMapWithNull = [null]
        when: 'convert the dataMaps to XML'
            convertDataMapsToXml(dataMapWithNull)
        then: 'a validation exception is thrown'
            def exception = thrown(DataValidationException)
        and: 'the cause is a null pointer exception'
            assert exception.cause instanceof NullPointerException
    }

    def 'Converting data maps to xml with document syntax error'() {
        given: 'A list of maps with an invalid entry'
            def dataMap = [['invalid<tag>': 'value']]
        when: 'convert the dataMaps to XML'
            convertDataMapsToXml(dataMap)
        then: 'a validation exception is thrown'
            def exception = thrown(DataValidationException)
        and: 'the cause is a document object model exception'
            assert exception.cause instanceof DOMException

    }

}