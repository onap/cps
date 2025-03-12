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
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.DataNodeFactory
import org.onap.cps.api.model.Anchor
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.PrefixResolver
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
    def mockPrefixResolver = Mock(PrefixResolver)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def objectUnderTest = new CpsDeltaServiceImpl(mockCpsAnchorService, mockCpsDataService, mockDataNodeFactory, jsonObjectMapper, mockPrefixResolver)

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

//

//
//    def 'Get delta between anchor and payload by using schema from anchor #scenario'() {
//        given: 'schema set for a given dataspace and anchor'
//            setupSchemaSetMocks("bookstore.yang")
//        when: 'attempt to get delta between an anchor and a JSON payload'
//            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, [:], jsonData, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
//        then: 'dataspacename and anchor names are validated'
//            1 * mockCpsValidator.validateNameCharacters(['some-dataspace', 'some-anchor'])
//        and: 'source data nodes are fetched using appropriate persistence layer method'
//            1 * mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
//        and: 'appropriate delta service method is invoked once with correct source and target data nodes'
//            1 * mockCpsDeltaService.getDeltaReports({sourceDataNodesRebuilt -> sourceDataNodesRebuilt.xpath[0] == expectedNodeXpath}, {targetDataNodes -> targetDataNodes.xpath[0] == expectedNodeXpath})
//        where: 'following data was used'
//            scenario          | xpath                               | sourceDataNodes                                                                                          | jsonData                                       || expectedNodeXpath
//            'root node xpath' | '/'                                 | [new DataNodeBuilder().withXpath('/bookstore').build()]                                                  | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
//            'parent xpath'    | '/bookstore'                        | [new DataNodeBuilder().withXpath('/bookstore').build()]                                                  | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
//            'non-root xpath'  | '/bookstore/categories[@code="02"]' | [new DataNodeBuilder().withXpath('/bookstore/categories[@code="02"]').withLeaves(["code":"02"]).build()] | '{"categories":[{"name":"kids","code":"02"}]}' || '/bookstore/categories[@code=\'02\']'
//    }
//
//    def 'Delta between anchor and payload error scenario #scenario'() {
//        given: 'schema set for given anchor and dataspace references bookstore model'
//            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap('bookstore.yang')
//            setupSchemaSetMocksForDelta(yangResourceContentPerName)
//        when: 'attempt to get delta between anchor and payload'
//            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, yangResourceContentPerName, jsonData, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
//        then: 'expected exception is thrown'
//            thrown(DataValidationException)
//        where: 'following parameters were used'
//            scenario                                   | xpath                               | jsonData
//            'invalid json data with root node xpath'   | '/'                                 | '{"some-key": "some-value"'
//            'empty json data with root node xpath'     | '/'                                 | '{}'
//            'invalid json data with parent node xpath' | '/bookstore'                        | '{"some-key": "some-value"'
//            'empty json data with parent node xpath'   | '/bookstore'                        | '{}'
//            'empty json data with xpath'               | "/bookstore/categories[@code='02']" | '{}'
//    }
//
//    def setupSchemaSetMocks(String... yangResources) {
//        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
//        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
//        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
//        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
//        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
//    }
//


    def 'Get Delta between 2 anchors for #scenario'() {
        given: 'some xpath, source and target data nodes'
            def xpath = '/'
        when: 'attempt to get delta between 2 anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS)
        then: 'the dataspace and anchor names are validated'
            1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and:
            1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodes
        and: 'the result contains the expected delta report'
            result.size() == 1
            result[0].action.equals(expectedAction)
            result[0].xpath.equals('/parent')
            result[0].sourceData == expectedSourceData
            result[0].targetData == expectedTargetData
        where:
            scenario               | sourceDataNodes            | targetDataNodes            || expectedAction | expectedSourceData                          | expectedTargetData
            'Data node is added'   | []                         | targetDataNodeWithLeafData || 'create'       | null                                        | ['parent-leaf': 'parent-payload-in-target']
            'Data node is removed' | sourceDataNodeWithLeafData | []                         || 'remove'       | ['parent-leaf': 'parent-payload-in-source'] | null
            'Data node is updated' | sourceDataNodeWithLeafData | targetDataNodeWithLeafData || 'replace'      | ['parent-leaf': 'parent-payload-in-source'] |['parent-leaf': 'parent-payload-in-target']
    }

        def 'Delta Report for node with a child node'() {
        given: 'Two data nodes'
            def xpath = '/'
            def sourceDataNode  = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-payload'])])]
            def targetDataNode  = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-updated'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-payload-updated'])])]
            when: 'attempt to get delta between 2 anchors'
                def result = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS)
            then: 'the dataspace and anchor names are validated'
                1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNode
            and:
                1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNode
            and: 'the delta report contains expected details for parent node'
            assert result[0].action.equals('replace')
            assert result[0].xpath == '/parent'
            assert result[0].sourceData == ['parent-leaf': 'parent-payload']
            assert result[0].targetData == ['parent-leaf': 'parent-payload-updated']
        and: 'the delta report contains expected details for child node'
            assert result[1].action.equals('replace')
            assert result[1].xpath == '/parent/child'
            assert result[1].sourceData == ['child-leaf': 'child-payload']
            assert result[1].targetData == ['child-leaf': 'child-payload-updated']
    }

        def 'Delta report between leaves, #scenario'() {
            def xpath = '/'
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS)
        then: 'the dataspace and anchor names are validated'
            1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNode
        and:
            1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNode
        and: 'the delta report contains expected "replace" action'
            assert result[0].action.equals('replace')
        and: 'the delta report contains expected xpath'
            assert result[0].xpath == '/parent'
        and: 'the delta report contains expected source and target data'
            assert result[0].sourceData == expectedSourceData
            assert result[0].targetData == expectedTargetData
        where: 'the following data was used'
            scenario                                           | sourceDataNode                   | targetDataNode                   || expectedSourceData                                           | expectedTargetData
            'source and target data nodes have leaves'         | sourceDataNodeWithLeafData       | targetDataNodeWithLeafData       || ['parent-leaf': 'parent-payload-in-source']                  | ['parent-leaf': 'parent-payload-in-target']
            'only source data node has leaves'                 | sourceDataNodeWithLeafData       | targetDataNodeWithoutLeafData    || ['parent-leaf': 'parent-payload-in-source']                  | null
            'only target data node has leaves'                 | sourceDataNodeWithoutLeafData    | targetDataNodeWithLeafData       || null                                                         | ['parent-leaf': 'parent-payload-in-target']
            'source and target dsta node with multiple leaves' | sourceDataNodeWithMultipleLeaves | targetDataNodeWithMultipleLeaves || ['leaf-1': 'leaf-1-in-source', 'leaf-2': 'leaf-2-in-source'] | ['leaf-1': 'leaf-1-in-target', 'leaf-2': 'leaf-2-in-target']
    }

        def 'Get delta between data nodes for updated data, where source and target data nodes have no leaves '() {
            def xpath = '/'
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS)
        then: 'the dataspace and anchor names are validated'
            1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodeWithoutLeafData
        and:
            1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodeWithoutLeafData
        then: 'the delta report is empty'
            assert result.isEmpty()
    }

//        def 'Get delta between anchor and payload with user provided schema #scenario'() {
//        given: 'user provided schema set '
//            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap('bookstore.yang')
//            setupSchemaSetMocksForDelta(yangResourceContentPerName)
//            def xpath = '/'
//        when: 'attempt to get delta between an anchor and a JSON payload'
//            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, ANCHOR_NAME_1, xpath, yangResourceContentPerName, jsonData, INCLUDE_ALL_DESCENDANTS)
//        then: 'the dataspace and anchor names are validated'
//            1 * mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodeWithoutLeafData
//        and: 'source data nodes are fetched using appropriate persistence layer method'
//            1 * mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
//        and: 'appropriate delta service method is invoked once with correct source and target data nodes'
//            1 * mockCpsDeltaService.getDeltaReports({sourceDataNodesRebuilt -> sourceDataNodesRebuilt.xpath[0] == expectedNodeXpath}, {targetDataNodes -> targetDataNodes.xpath[0] == expectedNodeXpath})
//        where: 'following data was used'
//            scenario             | xpath                               | sourceDataNodes                                                                                          | jsonData                                       || expectedNodeXpath
//            'root node xpath'    | '/'                                 | [new DataNodeBuilder().withXpath('/bookstore').build()]                                                  | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
//            'parent xpath'       | '/bookstore'                        | [new DataNodeBuilder().withXpath('/bookstore').build()]                                                  | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
//            'non-root xpath'     | '/bookstore/categories[@code="02"]' | [new DataNodeBuilder().withXpath('/bookstore/categories[@code="02"]').withLeaves(["code":"02"]).build()] | '{"categories":[{"name":"kids","code":"02"}]}' || '/bookstore/categories[@code=\'02\']'
//    }

        def setupSchemaSetMocksForDelta(Map<String, String> yangResourceContentPerName) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockTimedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourceContentPerName) >> mockYangTextSchemaSourceSet
        mockYangTextSchemaSourceSetCache.get(_, _) >> mockYangTextSchemaSourceSet
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceContentPerName).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }
}
