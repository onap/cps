/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.impl

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.DataNodeFactory
import org.onap.cps.api.model.Anchor
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.DataMapper
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Shared
import spock.lang.Specification

class CpsDeltaServiceImplSpec extends Specification{

    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockDataNodeFactory = Mock(DataNodeFactory)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockDataMapper = Mock(DataMapper)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def objectUnderTest = new CpsDeltaServiceImpl(mockCpsAnchorService, mockCpsDataService, mockDataNodeFactory, jsonObjectMapper, mockDataMapper)

    static def sourceDataNodeWithLeafData = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-in-source'])]
    static def sourceDataNodeWithoutLeafData = [new DataNode(xpath: '/parent')]
    static def targetDataNodeWithLeafData = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-in-target'])]
    static def targetDataNodeWithoutLeafData = [new DataNode(xpath: '/parent')]
    static def sourceDataNodeWithMultipleLeaves = [new DataNode(xpath: '/parent', leaves: ['leaf-1': 'leaf-1-in-source', 'leaf-2': 'leaf-2-in-source'])]
    static def targetDataNodeWithMultipleLeaves = [new DataNode(xpath: '/parent', leaves: ['leaf-1': 'leaf-1-in-target', 'leaf-2': 'leaf-2-in-target'])]

    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender
    def applicationContext = new AnnotationConfigApplicationContext()

    @Shared
    static def ANCHOR_NAME_1 = 'some-anchor-1'
    static def ANCHOR_NAME_2 = 'some-anchor-2'
    static def INCLUDE_ALL_DESCENDANTS = FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
    def dataspaceName = 'some-dataspace'
    def schemaSetName = 'some-schema-set'
    def anchor1 = Anchor.builder().name(ANCHOR_NAME_1).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    def anchor2 = Anchor.builder().name(ANCHOR_NAME_2).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()

    def setup() {
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_1) >> anchor1
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_2) >> anchor2
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
        applicationContext.refresh()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CpsDataServiceImpl.class)).detachAndStopAllAppenders()
        applicationContext.close()
    }

    def 'Get Delta between 2 anchors for #scenario'() {
        given: 'xpath to get delta'
            def xpath = '/'
        when: 'attempt to get delta between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'cps data service is invoked again to return target data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodes
        and: 'the delta report contains the expected information'
            deltaReport.size() == 1
            deltaReport[0].action.equals(expectedAction)
            deltaReport[0].xpath.equals('/parent')
            deltaReport[0].sourceData == expectedSourceData
            deltaReport[0].targetData == expectedTargetData
        where: 'following data was used'
            scenario               | sourceDataNodes            | targetDataNodes            || expectedAction | expectedSourceData                          | expectedTargetData
            'Data node is added'   | []                         | targetDataNodeWithLeafData || 'create'       | null                                        | ['parent-leaf': 'parent-payload-in-target']
            'Data node is removed' | sourceDataNodeWithLeafData | []                         || 'remove'       | ['parent-leaf': 'parent-payload-in-source'] | null
            'Data node is updated' | sourceDataNodeWithLeafData | targetDataNodeWithLeafData || 'replace'      | ['parent-leaf': 'parent-payload-in-source'] |['parent-leaf': 'parent-payload-in-target']
    }

    def 'Delta Report between parent nodes containing child nodes'() {
        given: 'Two data nodes and xpath'
            def xpath = '/'
            def sourceDataNode  = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-payload'])])]
            def targetDataNode  = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-updated'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-payload-updated'])])]
        when: 'attempt to get delta between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNode
        and: 'cps data service is invoked again to return target data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNode
        and: 'the delta report contains expected details for parent node'
            assert deltaReport[0].action.equals('replace')
            assert deltaReport[0].xpath == '/parent'
            assert deltaReport[0].sourceData == ['parent-leaf': 'parent-payload']
            assert deltaReport[0].targetData == ['parent-leaf': 'parent-payload-updated']
        and: 'the delta report contains expected details for child node'
            assert deltaReport[1].action.equals('replace')
            assert deltaReport[1].xpath == '/parent/child'
            assert deltaReport[1].sourceData == ['child-leaf': 'child-payload']
            assert deltaReport[1].targetData == ['child-leaf': 'child-payload-updated']
    }

    def 'Delta report between leaves, #scenario'() {
    given: 'xpath to fetch delta between two anchors'
        def xpath = '/'
    when: 'attempt to get delta between 2 anchors'
        def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS)
    then: 'cps data service is invoked and returns source data nodes'
        mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNode
    and: 'cps data service is invoked again to return target data nodes'
        mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNode
    and: 'the delta report contains expected "replace" action'
        assert deltaReport[0].action.equals('replace')
    and: 'the delta report contains expected xpath'
        assert deltaReport[0].xpath == '/parent'
    and: 'the delta report contains expected source and target data'
        assert deltaReport[0].sourceData == expectedSourceData
        assert deltaReport[0].targetData == expectedTargetData
    where: 'the following data was used'
        scenario                                           | sourceDataNode                   | targetDataNode                   || expectedSourceData                                           | expectedTargetData
        'source and target data nodes have leaves'         | sourceDataNodeWithLeafData       | targetDataNodeWithLeafData       || ['parent-leaf': 'parent-payload-in-source']                  | ['parent-leaf': 'parent-payload-in-target']
        'only source data node has leaves'                 | sourceDataNodeWithLeafData       | targetDataNodeWithoutLeafData    || ['parent-leaf': 'parent-payload-in-source']                  | null
        'only target data node has leaves'                 | sourceDataNodeWithoutLeafData    | targetDataNodeWithLeafData       || null                                                         | ['parent-leaf': 'parent-payload-in-target']
        'source and target dsta node with multiple leaves' | sourceDataNodeWithMultipleLeaves | targetDataNodeWithMultipleLeaves || ['leaf-1': 'leaf-1-in-source', 'leaf-2': 'leaf-2-in-source'] | ['leaf-1': 'leaf-1-in-target', 'leaf-2': 'leaf-2-in-target']
    }

    def 'Get delta between data nodes for updated data, where source and target data nodes have no leaves '() {
        given: 'xpath to get delta between anchors'
            def xpath = '/'
        when: 'attempt to get delta between 2 data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodeWithoutLeafData
        and: 'cps data service is invoked again to return target data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodeWithoutLeafData
        then: 'the delta report is empty'
            assert deltaReport.isEmpty()
    }

    def 'Get delta between anchor and payload with user provided schema #scenario'() {
        given: 'user provided schema set '
            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap('bookstore.yang')
            setupSchemaSetMocksForDelta(yangResourceContentPerName)
        when: 'attempt to get delta between an anchor and a JSON payload'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, ANCHOR_NAME_1, xpath, yangResourceContentPerName, jsonData, INCLUDE_ALL_DESCENDANTS)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'data node factory method is invoked to build target data nodes using user provided schema'
            mockDataNodeFactory.createDataNodesWithYangResourceXpathAndNodeData(yangResourceContentPerName, xpath, jsonData, ContentType.JSON) >> targetDataNodes
        and: 'delta report contains expected xpath'
            deltaReport[0].getXpath() == expectedNodeXpath
        where: 'following data was used'
            scenario          | xpath                               | sourceDataNodes                                                                    | targetDataNodes                                            | jsonData                                       || expectedNodeXpath
            'root node xpath' | '/'                                 | [new DataNode(xpath: '/bookstore', leaves: ["bookstore-name":"Easons"])]           | [new DataNode(xpath: '/bookstore')]                        | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
            'parent xpath'    | '/bookstore'                        | [new DataNode(xpath: '/bookstore', leaves: ["bookstore-name":"Easons"])]           | [new DataNode(xpath: '/bookstore')]                        | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
            'non-root xpath'  | '/bookstore/categories[@code="02"]' | [new DataNode(xpath: '/bookstore/categories[@code="02"]', leaves: ["code": "02"])] | [new DataNode(xpath: '/bookstore/categories[@code="02"]')] | '{"categories":[{"name":"kids","code":"02"}]}' || '/bookstore/categories[@code="02"]'
    }

    def 'Get delta between anchor and payload by using schema from anchor #scenario'() {
        given: 'schema set for a given dataspace and anchor'
            setupSchemaSetMocks("bookstore.yang")
        when: 'attempt to get delta between an anchor and a JSON payload'
            def result = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, ANCHOR_NAME_1, xpath, [:], jsonData, INCLUDE_ALL_DESCENDANTS)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'data node factory method is invoked to build target data nodes using schema details fetched from anchor name'
            mockDataNodeFactory.createDataNodesWithAnchorXpathAndNodeData(anchor1, xpath, jsonData, ContentType.JSON) >> targetDataNodes
        and:
            result[0].getXpath() == expectedNodeXpath
        where: 'following data was used'
            scenario          | xpath                               | sourceDataNodes                                                                    | targetDataNodes                                            | jsonData                                        || expectedNodeXpath
            'root node xpath' | '/'                                 | [new DataNode(xpath: '/bookstore', leaves: ["bookstore-name": "Easons"])]          | [new DataNode(xpath: '/bookstore', leaves: ["bookstore-name":"Easons-2"])]                        | '{"bookstore":{"bookstore-name":"Easons-2"}}'   || '/bookstore'
            'parent xpath'    | '/bookstore'                        | [new DataNode(xpath: '/bookstore', leaves: ["bookstore-name": "Easons"])]          | [new DataNode(xpath: '/bookstore')]                        | '{"bookstore":{"bookstore-name":"Easons-2"}}'   || '/bookstore'
            'non-root xpath'  | '/bookstore/categories[@code="02"]' | [new DataNode(xpath: '/bookstore/categories[@code="02"]', leaves: ["code": "02"])] | [new DataNode(xpath: '/bookstore/categories[@code="02"]')] | '{"categories":[{"name":"Child","code":"02"}]}' || '/bookstore/categories[@code="02"]'
    }

    def setupSchemaSetMocks(String... yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }

    def setupSchemaSetMocksForDelta(Map<String, String> yangResourceContentPerName) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockTimedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourceContentPerName) >> mockYangTextSchemaSourceSet
        mockYangTextSchemaSourceSetCache.get(_, _) >> mockYangTextSchemaSourceSet
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceContentPerName).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }
}
