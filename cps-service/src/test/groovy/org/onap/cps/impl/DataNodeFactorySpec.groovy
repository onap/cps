/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG
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

package org.onap.cps.impl

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.Anchor
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.YangParser
import org.onap.cps.utils.YangParserHelper
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

class DataNodeFactorySpec extends Specification {

    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)
    def objectUnderTest = new DataNodeFactoryImpl(yangParser)

    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender
    def applicationContext = new AnnotationConfigApplicationContext()

    def dataspaceName = 'some-dataspace'
    def anchorName = 'some-anchor'
    def schemaSetName = 'some-schema-set'
    def anchor = Anchor.builder().name(anchorName).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()

    def setup() {
        mockCpsAnchorService.getAnchor(dataspaceName, anchorName) >> anchor
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
        applicationContext.refresh()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(DataNodeFactoryImpl.class)).detachAndStopAllAppenders()
        applicationContext.close()
    }

    def 'Create data nodes using anchor and map of xpath to #scenario'() {
        given:'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'attempt to create data nodes'
            def dataNodes = objectUnderTest.createDataNodesWithAnchorAndXpathToNodeData(anchor, xpathToNodeData, contentType)
        then: 'expected number of data nodes are created'
            dataNodes.size() == expectedDataNodes
        and: 'data nodes have expected xpaths'
            dataNodes.stream().map { it.getXpath() }.toList().containsAll(expectedXpaths)
        where: 'the following data was used'
            scenario    | xpathToNodeData                                                                         | contentType      || expectedDataNodes | expectedXpaths
            'JSON Data' | ['/' : "{'test-tree': {'branch': []}}", '/test-tree' : "{'branch': [{'name':'Name'}]}"] | ContentType.JSON || 2                 | ['/test-tree', "/test-tree/branch[@name='Name']"]
            'XML Data'  | ['/test-tree' : '<branch><name>Name</name></branch>']                                   | ContentType.XML  || 1                 | ["/test-tree/branch[@name='Name']"]
    }

    def 'Create data nodes using anchor, xpath and #scenario string'() {
        given:'xpath, json string and schema set for given anchor and dataspace references test-tree model'
            def xpath = '/'
            def nodeData = TestUtils.getResourceFileContent(data)
            setupSchemaSetMocks('test-tree.yang')
        when: 'attempt to create data nodes'
            def dataNodes = objectUnderTest.createDataNodesWithAnchorXpathAndNodeData(anchor, xpath, nodeData, contentType)
        then: 'expected number of data nodes are created'
            dataNodes.size() == 1
        and: 'data nodes have expected xpaths'
            dataNodes[0].getXpath() == '/test-tree'
        where: 'the following data was used'
            scenario | data             | contentType
            'JSON'   | 'test-tree.json' | ContentType.JSON
            'XML'    | 'test-tree.xml'  | ContentType.XML
    }

    def 'Building data nodes using anchor, xpath and #scenario'() {
        given:'xpath, invalid json string and schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'attempt to create data nodes'
            objectUnderTest.createDataNodesWithAnchorXpathAndNodeData(anchor, '/test-tree', invalidData, contentType)
        then: 'expected number of data nodes are created'
            def exceptionThrown = thrown(DataValidationException)
            assert exceptionThrown.message.startsWith(expectedMessage)
        where:
            scenario        | invalidData     | contentType      || expectedMessage
            'no data nodes' | '{}'            | ContentType.JSON || 'No Data Nodes'
            'invalid json'  | '{invalid json' | ContentType.JSON || 'Data Validation Failed'
            'invalid xml'   | '<invalid xml'  | ContentType.XML  || 'Data Validation Failed'
    }

    def 'Create data nodes using anchor, parent node xpath and #scenario string'() {
        given:'parent node xpath, json string and schema set for given anchor and dataspace references test-tree model'
            def parentXpath = '/test-tree'
            setupSchemaSetMocks('test-tree.yang')
        when: 'attempt to create data nodes'
            def dataNodes = objectUnderTest.createDataNodesWithAnchorParentXpathAndNodeData(anchor, parentXpath, nodeData, contentType)
        then: 'expected number of data nodes are created'
            dataNodes.size() == 1
        and: 'data nodes have expected xpaths'
            dataNodes[0].getXpath() == "/test-tree/branch[@name='A']"
        where: 'the following data was used'
            scenario | nodeData                                                                                     | contentType
            'JSON'   | '{"branch": [{"name": "A"}]}'                                                                | ContentType.JSON
            'XML'    | '<test-tree xmlns="org:onap:cps:test:test-tree"><branch><name>A</name></branch></test-tree>' | ContentType.XML
    }

    def 'Create data nodes using anchor, parent node xpath and invalid #scenario string'() {
        given:'parent node xpath, invalid json string and schema set for given anchor and dataspace references test-tree model'
            def parentXpath = '/test-tree'
            setupSchemaSetMocks('test-tree.yang')
        when: 'attempt to create data nodes'
            objectUnderTest.createDataNodesWithAnchorParentXpathAndNodeData(anchor, parentXpath, invalidData, contentType)
        then: 'expected number of data nodes are created'
            def exceptionThrown = thrown(DataValidationException)
            assert exceptionThrown.message.startsWith(expectedMessage)
        where:
            scenario        | invalidData                                | contentType      || expectedMessage
            'no data nodes' | '{"branch": []}'                           | ContentType.JSON || 'No Data Nodes'
            'invalid json'  | '<test-tree><branch></branch></test-tree>' | ContentType.JSON || 'Data Validation Failed'
    }

    def 'Create data nodes using schema, xpath and #scenario string'() {
        given:'xpath, json string and schema set for given anchor and dataspace references bookstore model'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            setupSchemaSetMocksForDelta(yangResourcesNameToContentMap)
        when: 'attempt to create data nodes'
            def dataNodes = objectUnderTest.createDataNodesWithYangResourceXpathAndNodeData(yangResourcesNameToContentMap, '/', nodeData, contentType)
        then: 'expected number of data nodes are created'
            dataNodes.size() == 1
        and: 'data nodes have expected xpath'
            dataNodes[0].getXpath() == '/bookstore'
        where: 'the following data was used'
            scenario | nodeData                                                                                         | contentType
            'JSON'   | '{"bookstore":{"bookstore-name":"Easons"}}'                                                      | ContentType.JSON
            'XML'    | "<bookstore xmlns=\"org:onap:ccsdk:sample\"><bookstore-name>Easons</bookstore-name></bookstore>" | ContentType.XML
    }

    def 'Create data nodes using schema, xpath and invalid #scenario string'() {
        given:'xpath, invalid json string and schema set for given anchor and dataspace references bookstore model'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            setupSchemaSetMocksForDelta(yangResourcesNameToContentMap)
        when: 'attempt to create data nodes'
            objectUnderTest.createDataNodesWithYangResourceXpathAndNodeData(yangResourcesNameToContentMap, '/', invalidData, contentType)
        then: 'expected number of data nodes are created'
            def exceptionThrown = thrown(DataValidationException)
            assert exceptionThrown.message.startsWith(expectedMessage)
        where:
            scenario        | invalidData                                     | contentType      || expectedMessage
            'no json nodes' | '{}'                                            | ContentType.JSON || 'No Data Nodes'
            'no xml nodes'  | '"<bookstore xmlns=\"org:onap:ccsdk:sample\"/>' | ContentType.XML  || 'Data Validation Failed'
            'invalid json'  | '{invalid'                                      | ContentType.JSON || 'Data Validation Failed'
            'invalid xml'   | '<invalid'                                      | ContentType.XML  || 'Data Validation Failed'
    }

    def setupSchemaSetMocks(String... yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).schemaContext()
        mockYangTextSchemaSourceSet.schemaContext() >> schemaContext
    }

    def setupSchemaSetMocksForDelta(Map<String, String> yangResourcesNameToContentMap) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockTimedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourcesNameToContentMap) >> mockYangTextSchemaSourceSet
        mockYangTextSchemaSourceSetCache.get(_, _) >> mockYangTextSchemaSourceSet
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap).schemaContext()
        mockYangTextSchemaSourceSet.schemaContext() >> schemaContext
    }
}
