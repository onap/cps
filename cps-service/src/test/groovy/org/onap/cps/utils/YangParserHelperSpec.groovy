/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
import org.onap.cps.spi.api.exceptions.DataValidationException
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode
import spock.lang.Specification

class YangParserHelperSpec extends Specification {

    def objectUnderTest = new YangParserHelper()
    def validateOnly = true
    def validateAndParse = false

    def 'Parsing a valid multicontainer Json String.'() {
        given: 'a yang model (file)'
            def jsonData = TestUtils.getResourceFileContent('multiple-object-data.json')
        and: 'a model for that data'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('multipleDataTree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'the json data is parsed'
            def result = objectUnderTest.parseData(ContentType.JSON, jsonData, schemaContext, '', validateAndParse)
        then: 'a ContainerNode holding collection of normalized nodes is returned'
            result.body().getAt(index) instanceof NormalizedNode == true
        then: 'qualified name of children created is as expected'
            result.body().getAt(index).getIdentifier().nodeType == QName.create('org:onap:ccsdk:multiDataTree', '2020-09-15', nodeName)
        where:
            index | nodeName
            0     | 'first-container'
            1     | 'last-container'
    }

    def 'Parsing a valid #scenario String.'() {
        given: 'a yang model (file)'
            def fileData = TestUtils.getResourceFileContent(contentFile)
        and: 'a model for that data'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'the data is parsed'
            NormalizedNode result = objectUnderTest.parseData(contentType, fileData, schemaContext, '', validateAndParse)
        then: 'the result is a normalized node of the correct type'
            if (revision) {
                result.identifier.nodeType == QName.create(namespace, revision, localName)
            } else {
                result.identifier.nodeType == QName.create(namespace, localName)
            }
        where:
            scenario | contentFile      | contentType      | namespace                                 | revision     | localName
            'JSON'   | 'bookstore.json' | ContentType.JSON | 'org:onap:ccsdk:sample'                   | '2020-09-15' | 'bookstore'
            'XML'    | 'bookstore.xml'  | ContentType.XML  | 'urn:ietf:params:xml:ns:netconf:base:1.0' | ''           | 'bookstore'
    }

    def 'Parsing invalid data: #description.'() {
        given: 'a yang model (file)'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'invalid data is parsed'
            objectUnderTest.parseData(contentType, invalidData, schemaContext, '', validateAndParse)
        then: 'an exception is thrown'
            thrown(DataValidationException)
        where: 'the following invalid data is provided'
            invalidData                                                                          | contentType      | description
            '{incomplete json'                                                                   | ContentType.JSON | 'incomplete json'
            '{"test:bookstore": {"address": "Parnell st." }}'                                    | ContentType.JSON | 'json with un-modelled data'
            '{" }'                                                                               | ContentType.JSON | 'json with syntax exception'
            '<data>'                                                                             | ContentType.XML  | 'incomplete xml'
            '<data><bookstore><bookstore-anything>blabla</bookstore-anything></bookstore</data>' | ContentType.XML  | 'xml with invalid model'
            ''                                                                                   | ContentType.XML  | 'empty xml'
    }

    def 'Parsing data fragment by xpath for #scenario.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        when: 'json string is parsed'
            def result = objectUnderTest.parseData(contentType, nodeData, schemaContext, parentNodeXpath, validateAndParse)
        then: 'a ContainerNode holding collection of normalized nodes is returned'
            result.body().getAt(0) instanceof NormalizedNode == true
        then: 'result represents a node of expected type'
            result.body().getAt(0).getIdentifier().nodeType == QName.create('org:onap:cps:test:test-tree', '2020-02-02', nodeName)
        where:
            scenario                         | contentType      | nodeData                                                                                                                                                                                                      | parentNodeXpath                       || nodeName
            'JSON list element as container' | ContentType.JSON | '{ "branch": { "name": "B", "nest": { "name": "N", "birds": ["bird"] } } }'                                                                                                                                   | '/test-tree'                          || 'branch'
            'JSON list element within list'  | ContentType.JSON | '{ "branch": [{ "name": "B", "nest": { "name": "N", "birds": ["bird"] } }] }'                                                                                                                                 | '/test-tree'                          || 'branch'
            'JSON container element'         | ContentType.JSON | '{ "nest": { "name": "N", "birds": ["bird"] } }'                                                                                                                                                              | '/test-tree/branch[@name=\'Branch\']' || 'nest'
            'XML element test tree'          | ContentType.XML  | '<?xml version=\'1.0\' encoding=\'UTF-8\'?><branch xmlns="org:onap:cps:test:test-tree"><name>Left</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch>'                                       | '/test-tree'                          || 'branch'
            'XML element branch xpath'       | ContentType.XML  | '<?xml version=\'1.0\' encoding=\'UTF-8\'?><branch xmlns="org:onap:cps:test:test-tree"><name>Left</name><nest><name>Small</name><birds>Sparrow</birds><birds>Robin</birds></nest></branch>'                   | '/test-tree'                          || 'branch'
            'XML container element'          | ContentType.XML  | '<?xml version=\'1.0\' encoding=\'UTF-8\'?><nest xmlns="org:onap:cps:test:test-tree"><name>Small</name><birds>Sparrow</birds></nest>'                                                                         | '/test-tree/branch[@name=\'Branch\']' || 'nest'
    }

    def 'Parsing json data fragment by xpath error scenario: #scenario.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        when: 'json string is parsed'
            objectUnderTest.parseData(ContentType.JSON, '{"nest": {"name" : "Nest", "birds": ["bird"]}}', schemaContext, parentNodeXpath, validateAndParse)
        then: 'expected exception is thrown'
            thrown(DataValidationException)
        where:
            scenario                             | parentNodeXpath
            'xpath has no identifiers'           | '/'
            'xpath has no valid identifiers'     | '/[@name=\'Name\']'
            'invalid parent path'                | '/test-bush'
            'another invalid parent path'        | '/test-tree/branch[@name=\'Branch\']/nest/name/last-name'
            'fragment does not belong to parent' | '/test-tree/'
    }

    def 'Parsing json data with invalid json string: #description.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        when: 'malformed json string is parsed'
            objectUnderTest.parseData(ContentType.JSON, invalidJson, schemaContext, '', validateAndParse)
        then: 'an exception is thrown'
            thrown(DataValidationException)
        where: 'the following malformed json is provided'
            description                                          | invalidJson
            'malformed json string with unterminated array data' | '{bookstore={categories=[{books=[{authors=[Iain M. Banks]}]}]}}'
            'incorrect json'                                     | '{" }'
    }

    def 'Parsing json data with space.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        and: 'some json data with space in the array elements'
            def jsonDataWithSpacesInArrayElement = TestUtils.getResourceFileContent('bookstore.json')
        when: 'that json data is parsed'
            objectUnderTest.parseData(ContentType.JSON, jsonDataWithSpacesInArrayElement, schemaContext, '', validateAndParse)
        then: 'no exception thrown'
            noExceptionThrown()
    }

    def 'Converting xPath to nodeId for #scenario.'() {
        when: 'xPath is parsed'
            def result = objectUnderTest.xpathToNodeIdSequence(xPath)
        then: 'result represents an array of expected identifiers'
            assert result == expectedNodeIdentifier
        where: 'the following parameters are used'
            scenario                                       | xPath                                                               || expectedNodeIdentifier
            'container xpath'                              | '/test-tree'                                                        || ['test-tree']
            'xpath contains list attribute'                | '/test-tree/branch[@name=\'Branch\']'                               || ['test-tree','branch']
            'xpath contains list attributes with /'        | '/test-tree/branch[@name=\'/Branch\']/categories[@id=\'/broken\']'  || ['test-tree','branch','categories']
    }

    def 'Validating #scenario xpath String.'() {
        given: 'a data model (file) is provided'
            def fileData = TestUtils.getResourceFileContent(contentFile)
        and: 'the schema context is built for that data model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'the data is parsed to be validated'
            objectUnderTest.parseData(contentType, fileData, schemaContext,  parentNodeXpath, validateOnly)
        then: 'no exception is thrown'
            noExceptionThrown()
        where:
            scenario                   | parentNodeXpath | contentFile                      | contentType
            'JSON without parent node' | ''              | 'bookstore.json'                 | ContentType.JSON
            'JSON with parent node'    | '/bookstore'    | 'bookstore-categories-data.json' | ContentType.JSON
            'XML without parent node'  | ''              | 'bookstore.xml'                  | ContentType.XML
            'XML with parent node'     | '/bookstore'    | 'bookstore-categories-data.xml'  | ContentType.XML
    }

}
