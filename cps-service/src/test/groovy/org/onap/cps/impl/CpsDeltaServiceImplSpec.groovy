/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Deutsche Telekom AG
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
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.Anchor
import org.onap.cps.api.model.DataNode
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.CpsValidator
import org.onap.cps.utils.DataMapper
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.PrefixResolver
import org.onap.cps.utils.YangParser
import org.onap.cps.utils.YangParserHelper
import org.onap.cps.utils.deltareport.DeltaReportExecutor
import org.onap.cps.utils.deltareport.DeltaReportGenerator
import org.onap.cps.utils.deltareport.DeltaReportHelper
import org.onap.cps.utils.deltareport.GroupedDeltaReportGenerator
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDeltaServiceImplSpec extends Specification {

    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsValidator = Mock(CpsValidator)
    def mockDeltaReportExecutor = Mock(DeltaReportExecutor)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)
    def dataNodeFactory = new DataNodeFactoryImpl(yangParser)
    def mockPrefixResolver = Mock(PrefixResolver)
    def dataMapper = new DataMapper(mockCpsAnchorService, mockPrefixResolver)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def deltaReportHelper = new DeltaReportHelper()
    def deltaReportGenerator = new DeltaReportGenerator(deltaReportHelper)
    def groupedDeltaReportGenerator = new GroupedDeltaReportGenerator(deltaReportHelper)
    def objectUnderTest = new CpsDeltaServiceImpl(mockDeltaReportExecutor, mockCpsAnchorService, mockCpsValidator, mockCpsDataService, dataNodeFactory, dataMapper, jsonObjectMapper, deltaReportGenerator, groupedDeltaReportGenerator)

    static def bookstoreDataNodeWithParentXpath = [new DataNode(xpath: '/bookstore', leaves: ['bookstore-name': 'Easons'])]
    static def bookstoreDataNodeWithChildXpath = [new DataNode(xpath: '/bookstore/categories[@code=\'02\']', leaves: ['code': '02', 'name': 'Kids'])]
    static def bookstoreDataNodesWithChildXpathAndNoLeaves = [new DataNode(xpath: '/bookstore/categories[@code=\'02\']')]
    static def bookstoreDataAsMapForParentNode = [bookstore: ['bookstore-name': 'Easons']]
    static def bookstoreDataAsMapForChildNode = [categories: ['code': '02', 'name': 'Kids']]
    static def bookstoreJsonForParentNode = '{"bookstore":{"bookstore-name":"My Store"}}'
    static def bookstoreJsonForChildNode = '{"categories":[{"name":"Child","code":"02"}]}'
    static def sourceDataNode = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-leaf-as-source-data'])]
    static def sourceDataNodeWithChild = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-leaf-as-source-data'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-leaf-as-source-data'])])]
    static def sourceDataNodeWithoutLeafData = [new DataNode(xpath: '/parent')]
    static def targetDataNode = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-leaf-as-target-data'])]
    static def targetDataNodeWithChild = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-leaf-as-target-data'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-leaf-as-target-data'])])]
    static def targetDataNodeWithoutLeafData = [new DataNode(xpath: '/parent')]
    static def sourceDataNodeWithMultipleLeaves = [new DataNode(xpath: '/parent', leaves: ['leaf-1': 'leaf-1-in-source', 'leaf-2': 'leaf-2-in-source'])]
    static def targetDataNodeWithMultipleLeaves = [new DataNode(xpath: '/parent', leaves: ['leaf-1': 'leaf-1-in-target', 'leaf-2': 'leaf-2-in-target'])]
    static def expectedParentSourceData = ['parent':['parent-leaf':'parent-leaf-as-source-data']]
    static def expectedParentTargetData = ['parent':['parent-leaf':'parent-leaf-as-target-data']]

    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender
    def applicationContext = new AnnotationConfigApplicationContext()

    @Shared
    static def ANCHOR_NAME_1 = 'some-anchor-1'
    static def ANCHOR_NAME_2 = 'some-anchor-2'
    def dataspaceName = 'some-dataspace'
    def schemaSetName = 'some-schema-set'
    def anchor1 = Anchor.builder().name(ANCHOR_NAME_1).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    def anchor2 = Anchor.builder().name(ANCHOR_NAME_2).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    static def GROUPING_ENABLED = true
    static def GROUPING_DISABLED = false

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

    def 'Get Delta between 2 anchors where data node is #scenario'() {
        given: 'xpath to get delta'
            def xpath = '/'
        when: 'attempt to get delta between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, OMIT_DESCENDANTS, groupDataNodes)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], OMIT_DESCENDANTS) >> sourceDataNodes
        and: 'cps data service is invoked again to return target data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], OMIT_DESCENDANTS) >> targetDataNodes
        and: 'the delta report contains the expected information'
            deltaReport.size() == 1
            deltaReport[0].action.equals(expectedAction)
            deltaReport[0].xpath.equals('/parent')
            deltaReport[0].sourceData == expectedSourceData
            deltaReport[0].targetData == expectedTargetData
        where: 'following data was used'
            scenario                         | sourceDataNodes          | targetDataNodes         | groupDataNodes    || expectedAction | expectedSourceData                                                                                            | expectedTargetData
            'added with grouping disabled'   | []                       | targetDataNode          | GROUPING_DISABLED || 'create'       | null                                                                                                          | ['parent':['parent-leaf':'parent-leaf-as-target-data']]
            'removed with grouping disabled' | sourceDataNode           | []                      | GROUPING_DISABLED || 'remove'       | ['parent':['parent-leaf':'parent-leaf-as-source-data']]                                                       | null
            'updated with grouping disabled' | sourceDataNode           | targetDataNode          | GROUPING_DISABLED || 'replace'      | ['parent':['parent-leaf':'parent-leaf-as-source-data']]                                                       | ['parent':['parent-leaf':'parent-leaf-as-target-data']]
            'added with grouping enabled'    | []                       | targetDataNodeWithChild | GROUPING_ENABLED  || 'create'       | null                                                                                                          | ['parent':['parent-leaf': 'parent-leaf-as-target-data', 'child':['child-leaf': 'child-leaf-as-target-data']]]
            'removed with grouping enabled'  | sourceDataNodeWithChild  | []                      | GROUPING_ENABLED  || 'remove'       | ['parent':['parent-leaf': 'parent-leaf-as-source-data', 'child':['child-leaf': 'child-leaf-as-source-data']]] | null
            'updated with grouping enabled'  | sourceDataNode           | targetDataNode          | GROUPING_ENABLED  || 'replace'      | ['parent':['parent-leaf': 'parent-leaf-as-source-data']]                                                      | ['parent':['parent-leaf': 'parent-leaf-as-target-data']]
    }

    def 'Get delta between 2 anchors with invalid xpath'() {
        given: 'an invalid xpath'
            def invalidXpath = '/test[invalid'
        when: 'attempt to get delta between 2 anchors with invalid xpath'
            objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, invalidXpath, INCLUDE_ALL_DESCENDANTS, GROUPING_DISABLED)
        then: 'DataValidationException is thrown'
            def exception = thrown(DataValidationException)
            assert exception.message == 'Invalid xpath: /test[invalid'
    }

    def 'Delta Report between parent nodes with children where data node is #scenario without grouping of data nodes'() {
        given: 'root node xpath and expected source and target data'
            def xpath = '/'
        when: 'attempt to get delta between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS, GROUPING_DISABLED)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'cps data service is invoked again to return target data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodes
        and: 'delta report contains correct number of entries'
            deltaReport.size() == 2
        and: 'the delta report contains expected details for parent node'
            assert deltaReport[0].action == expectedAction
            assert deltaReport[0].xpath == '/parent'
            assert deltaReport[0].sourceData == expectedSourceDataForParent
            assert deltaReport[0].targetData == expectedTargetDataForParent
        and: 'the delta report contains expected details for child node'
            assert deltaReport[1].action == expectedAction
            assert deltaReport[1].xpath == '/parent/child'
            assert deltaReport[1].sourceData == expectedSourceDataForChild
            assert deltaReport[1].targetData == expectedTargetDataForChild
        where: 'the following data is used'
            scenario  | sourceDataNodes           | targetDataNodes           || expectedAction | expectedSourceDataForParent | expectedTargetDataForParent | expectedSourceDataForChild                            | expectedTargetDataForChild
            'added'   | []                        | targetDataNodeWithChild() || 'create'       | null                        | expectedParentTargetData    | null                                                  | ['child':['child-leaf': 'child-leaf-as-target-data']]
            'removed' | sourceDataNodeWithChild() | []                        || 'remove'       | expectedParentSourceData    | null                        | ['child':['child-leaf': 'child-leaf-as-source-data']] | null
            'updated' | sourceDataNodeWithChild() | targetDataNodeWithChild() || 'replace'      | expectedParentSourceData    | expectedParentTargetData    | ['child':['child-leaf': 'child-leaf-as-source-data']] | ['child':['child-leaf': 'child-leaf-as-target-data']]
    }

    def 'Delta Report between parent nodes with children where parent is updated and child node is #scenario with grouping of data nodes'() {
        given: 'root node xpath and expected source and target data'
            def xpath = '/'
        when: 'attempt to get delta between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS, GROUPING_ENABLED)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'cps data service is invoked again to return target data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodes
        and: 'delta report contains correct number of entries'
            deltaReport.size() == 2
        and: 'the delta report contains expected details for parent node'
            assert deltaReport[0].action == 'replace'
            assert deltaReport[0].xpath == '/parent'
            assert deltaReport[0].sourceData == expectedParentSourceData
            assert deltaReport[0].targetData == expectedParentTargetData
        and: 'the delta report contains expected details for child node'
            assert deltaReport[1].action == expectedChildAction
            assert deltaReport[1].xpath == expectedChildXpath
            assert deltaReport[1].sourceData == expectedSourceDataForChild
            assert deltaReport[1].targetData == expectedTargetDataForChild
        where: 'the following data is used'
            scenario  | sourceDataNodes         | targetDataNodes         || expectedChildAction | expectedChildXpath | expectedSourceDataForChild                            | expectedTargetDataForChild
            'added'   | sourceDataNode          | targetDataNodeWithChild || 'create'            | '/parent'          | null                                                  | ['child':['child-leaf': 'child-leaf-as-target-data']]
            'removed' | sourceDataNodeWithChild | targetDataNode          || 'remove'            | '/parent'          | ['child':['child-leaf':'child-leaf-as-source-data']]  | null
            'updated' | sourceDataNodeWithChild | targetDataNodeWithChild || 'replace'           | '/parent/child'    | ['child':['child-leaf': 'child-leaf-as-source-data']] | ['child':['child-leaf': 'child-leaf-as-target-data']]
    }

    def 'Delta report between leaves, #scenario'() {
    given: 'xpath to fetch delta between two anchors'
        def xpath = '/'
    when: 'attempt to get delta between 2 anchors'
        def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS, GROUPING_DISABLED)
    then: 'cps data service is invoked and returns source data nodes'
        mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
    and: 'cps data service is invoked again to return target data nodes'
        mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodes
    and: 'the delta report contains expected "replace" action'
        assert deltaReport[0].action.equals('replace')
    and: 'the delta report contains expected xpath'
        assert deltaReport[0].xpath == '/parent'
    and: 'the delta report contains expected source and target data'
        assert deltaReport[0].sourceData == expectedSourceData
        assert deltaReport[0].targetData == expectedTargetData
    where: 'the following data was used'
        scenario                                           | sourceDataNodes                  | targetDataNodes                  || expectedSourceData                                                    | expectedTargetData
        'source and target data nodes have leaves'         | sourceDataNode                   | targetDataNode                   || ['parent':['parent-leaf':'parent-leaf-as-source-data']]               | ['parent':['parent-leaf':'parent-leaf-as-target-data']]
        'only source data node has leaves'                 | sourceDataNode                   | targetDataNodeWithoutLeafData    || ['parent':['parent-leaf':'parent-leaf-as-source-data']]               | null
        'only target data node has leaves'                 | sourceDataNodeWithoutLeafData    | targetDataNode                   || null                                                                  | ['parent':['parent-leaf':'parent-leaf-as-target-data']]
        'source and target data node with multiple leaves' | sourceDataNodeWithMultipleLeaves | targetDataNodeWithMultipleLeaves || ['parent':['leaf-1':'leaf-1-in-source', 'leaf-2':'leaf-2-in-source']] | ['parent':['leaf-1':'leaf-1-in-target', 'leaf-2':'leaf-2-in-target']]
    }

    def 'Get delta between data nodes for updated data, where source and target data nodes have no leaves '() {
        given: 'xpath to get delta between anchors'
            def xpath = '/'
        when: 'attempt to get delta between 2 data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS, GROUPING_DISABLED)
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
            def deltaReport = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, ANCHOR_NAME_1, xpath, yangResourceContentPerName, jsonData, INCLUDE_ALL_DESCENDANTS, GROUPING_DISABLED)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'source data nodes are rebuilt (to match the data type with target data nodes)'
            dataNodeFactory.createDataNodesWithAnchorXpathAndNodeData(anchor1, xpath, jsonObjectMapper.asJsonString(sourceDataNodesAsMap), ContentType.JSON)
        and: 'data node factory method is invoked to build target data nodes using user provided schema'
            dataNodeFactory.createDataNodesWithYangResourceXpathAndNodeData(yangResourceContentPerName, xpath, jsonData, ContentType.JSON)
        and: 'delta report contains expected xpath, action, source and target data'
            deltaReport[0].getXpath() == expectedNodeXpath
            deltaReport[0].getAction().equals('replace')
            deltaReport[0].getSourceData().equals(expectedSourceData)
            deltaReport[0].getTargetData().equals(expectedTargetData)
        where: 'following data was used'
            scenario          | xpath                                 | sourceDataNodes                  | sourceDataNodesAsMap            | jsonData                   || expectedNodeXpath                    | expectedSourceData                            | expectedTargetData
            'root node xpath' | '/'                                   | bookstoreDataNodeWithParentXpath | bookstoreDataAsMapForParentNode | bookstoreJsonForParentNode || '/bookstore'                         | ['bookstore':['bookstore-name':'Easons']]     | ['bookstore':['bookstore-name':'My Store']]
            'parent xpath'    | '/bookstore'                          | bookstoreDataNodeWithParentXpath | bookstoreDataAsMapForParentNode | bookstoreJsonForParentNode || '/bookstore'                         | ['bookstore':['bookstore-name':'Easons']]     | ['bookstore':['bookstore-name':'My Store']]
            'non-root xpath'  | '/bookstore/categories[@code=\'02\']' | bookstoreDataNodeWithChildXpath  | bookstoreDataAsMapForChildNode  | bookstoreJsonForChildNode  || '/bookstore/categories[@code=\'02\']'| ['categories':[['code':'02', 'name':'Kids']]] | ['categories':[['code':'02', 'name':'Child']]]
    }

    def 'Get delta between anchor and payload by using schema from anchor #scenario'() {
        given: 'schema set for a given dataspace and anchor'
            setupSchemaSetMocks('bookstore.yang')
        when: 'attempt to get delta between an anchor and a JSON payload'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, ANCHOR_NAME_1, xpath, [:], jsonData, INCLUDE_ALL_DESCENDANTS, GROUPING_DISABLED)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'source data nodes are rebuilt (to match the data type with target data nodes)'
            dataNodeFactory.createDataNodesWithAnchorXpathAndNodeData(anchor1, xpath, jsonObjectMapper.asJsonString(sourceDataNodesAsMap), ContentType.JSON)
        and: 'data node factory method is invoked to build target data nodes using schema details fetched from anchor name'
            dataNodeFactory.createDataNodesWithAnchorXpathAndNodeData(anchor1, xpath, jsonData, ContentType.JSON)
        and: 'delta report contains expected xpath, action, source and target data'
            deltaReport[0].getXpath() == expectedNodeXpath
            deltaReport[0].getAction().equals('replace')
            deltaReport[0].getSourceData().equals(expectedSourceData)
            deltaReport[0].getTargetData().equals(expectedTargetData)
        where: 'following data was used'
             scenario         | xpath                                 | sourceDataNodes                  | sourceDataNodesAsMap            | jsonData                   || expectedNodeXpath                     | expectedSourceData                            | expectedTargetData
            'root node xpath' | '/'                                   | bookstoreDataNodeWithParentXpath | bookstoreDataAsMapForParentNode | bookstoreJsonForParentNode || '/bookstore'                          | ['bookstore':['bookstore-name':'Easons']]     | ['bookstore':['bookstore-name':'My Store']]
            'parent xpath'    | '/bookstore'                          | bookstoreDataNodeWithParentXpath | bookstoreDataAsMapForParentNode | bookstoreJsonForParentNode || '/bookstore'                          | ['bookstore':['bookstore-name':'Easons']]     | ['bookstore':['bookstore-name':'My Store']]
            'non-root xpath'  | '/bookstore/categories[@code=\'02\']' | bookstoreDataNodeWithChildXpath  | bookstoreDataAsMapForChildNode  | bookstoreJsonForChildNode  || '/bookstore/categories[@code=\'02\']' | ['categories':[['code':'02', 'name':'Kids']]] | ['categories':[['code':'02', 'name':'Child']]]
    }

    def 'Delta between anchor and payload error scenario #scenario'() {
        given: 'schema set for given anchor and dataspace references bookstore model'
            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap('bookstore.yang')
            setupSchemaSetMocksForDelta(yangResourceContentPerName)
        when: 'attempt to get delta between anchor and payload'
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, ANCHOR_NAME_1, xpath, yangResourceContentPerName, jsonData, INCLUDE_ALL_DESCENDANTS, GROUPING_DISABLED)
        then: 'expected exception is thrown'
            thrown(DataValidationException)
        where: 'following parameters were used'
            scenario                                   | xpath                                 | jsonData
            'invalid json data with root node xpath'   | '/'                                   | '{"some-key": "some-value"'
            'empty json data with root node xpath'     | '/'                                   | '{}'
            'invalid json data with parent node xpath' | '/bookstore'                          | '{"some-key": "some-value"'
            'empty json data with parent node xpath'   | '/bookstore'                          | '{}'
            'empty json data with xpath'               | '/bookstore/categories[@code=\'02\']' | '{}'
    }

    def 'Delta Report between identical nodes, with grouping of data nodes #scenario'() {
        given: 'parent node xpath'
            def xpath = '/parent'
        when: 'attempt to get delta between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS, groupDataNodes)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodeWithoutLeafData
        and: 'cps data service is invoked again to return target data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodeWithoutLeafData
        and: 'the delta report contains expected details for parent node and child node'
            assert deltaReport.isEmpty()
        where:
            scenario   | groupDataNodes
            'enabled'  | GROUPING_ENABLED
            'disabled' | GROUPING_DISABLED
    }

    def 'Delta Report between data nodes with list node xpath where leaf data is #scenario with grouping of data nodes enabled'() {
        given: 'root node xpath'
            def xpath = '/'
        when: 'attempt to get delta between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, INCLUDE_ALL_DESCENDANTS, GROUPING_ENABLED)
        then: 'cps data service is invoked and returns source data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'cps data service is invoked again to return target data nodes'
            mockCpsDataService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], INCLUDE_ALL_DESCENDANTS) >> targetDataNodes
        and: 'delta report contains correct number of entries'
            deltaReport.size() == 1
        and: 'the delta report contains expected details for updated node'
            assert deltaReport[0].action == 'replace'
            assert deltaReport[0].xpath == "/bookstore/categories[@code='02']"
            assert deltaReport[0].sourceData == expectedSourceData
            assert deltaReport[0].targetData == expectedTargetData
        where: 'the following data is used'
            scenario  | sourceDataNodes                             | targetDataNodes                             || expectedSourceData                               | expectedTargetData
            'added'   | bookstoreDataNodeWithChildXpath             | bookstoreDataNodesWithChildXpathAndNoLeaves || ['categories': [['code': '02', 'name': 'Kids']]] | null
            'removed' | bookstoreDataNodesWithChildXpathAndNoLeaves | bookstoreDataNodeWithChildXpath             || null                                             | ['categories': [['code': '02', 'name': 'Kids']]]
    }

    def 'Apply changes from a delta report to an anchor'() {
        given: 'delta report as JSON string'
            def deltaReportJson = '[{"action":"replace","xpath":"/bookstore","sourceData":{"bookstore":{"bookstore-name":"Easons"}},"targetData":{"bookstore":{"bookstore-name":"My Store"}}}]'
        when: 'an attempt to apply the delta report to the anchor'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'utility class to apply the delta report is invoked with expected parameters'
            1 * mockDeltaReportExecutor.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
    }

    def setupSchemaSetMocks(yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).schemaContext()
        mockYangTextSchemaSourceSet.schemaContext() >> schemaContext
    }

    def setupSchemaSetMocksForDelta(yangResourceContentPerName) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockTimedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourceContentPerName) >> mockYangTextSchemaSourceSet
        mockYangTextSchemaSourceSetCache.get(_, _) >> mockYangTextSchemaSourceSet
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceContentPerName).schemaContext()
        mockYangTextSchemaSourceSet.schemaContext() >> schemaContext
    }

    def sourceDataNodeWithChild() {
        [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-leaf-as-source-data'],
            childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-leaf-as-source-data'])])]
    }

    def targetDataNodeWithChild() {
        [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-leaf-as-target-data'],
            childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-leaf-as-target-data'])])]
    }

}
