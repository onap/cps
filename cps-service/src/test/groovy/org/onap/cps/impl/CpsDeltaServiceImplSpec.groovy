/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 TechMahindra Ltd.
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
import org.onap.cps.api.model.DeltaReport
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.DataMapper
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.PrefixResolver
import org.onap.cps.utils.YangParser
import org.onap.cps.utils.YangParserHelper
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime

class CpsDeltaServiceImplSpec extends Specification {

    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)
    def dataNodeFactory = new DataNodeFactoryImpl(yangParser)
    def mockPrefixResolver = Mock(PrefixResolver)
    def dataMapper = new DataMapper(mockCpsAnchorService, mockPrefixResolver)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def objectUnderTest = new CpsDeltaServiceImpl(mockCpsAnchorService, mockCpsDataService, dataNodeFactory, dataMapper, jsonObjectMapper)

    static def bookstoreDataNodeWithParentXpath = [new DataNode(xpath: '/bookstore', leaves: ['bookstore-name': 'Easons'])]
    static def bookstoreDataNodeWithChildXpath = [new DataNode(xpath: '/bookstore/categories[@code=\'02\']', leaves: ['code': '02', 'name': 'Kids'])]
    static def bookstoreDataAsMapForParentNode = [bookstore: ['bookstore-name': 'Easons']]
    static def bookstoreDataAsMapForChildNode = [categories: ['code': '02', 'name': 'Kids']]
    static def bookstoreJsonForParentNode = '{"bookstore":{"bookstore-name":"My Store"}}'
    static def bookstoreJsonForChildNode = '{"categories":[{"name":"Child","code":"02"}]}'

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
            scenario          | xpath                                 | sourceDataNodes                  | sourceDataNodesAsMap            | jsonData                   || expectedNodeXpath                    | expectedSourceData          | expectedTargetData
            'root node xpath' | '/'                                   | bookstoreDataNodeWithParentXpath | bookstoreDataAsMapForParentNode | bookstoreJsonForParentNode || '/bookstore'                         | ['bookstore-name':'Easons'] | ['bookstore-name':'My Store']
            'parent xpath'    | '/bookstore'                          | bookstoreDataNodeWithParentXpath | bookstoreDataAsMapForParentNode | bookstoreJsonForParentNode || '/bookstore'                         | ['bookstore-name':'Easons'] | ['bookstore-name':'My Store']
            'non-root xpath'  | '/bookstore/categories[@code=\'02\']' | bookstoreDataNodeWithChildXpath  | bookstoreDataAsMapForChildNode  | bookstoreJsonForChildNode  || '/bookstore/categories[@code=\'02\']'| ['name':'Kids']             | ['name':'Child']
    }

    def 'Get delta between anchor and payload by using schema from anchor #scenario'() {
        given: 'schema set for a given dataspace and anchor'
            setupSchemaSetMocks('bookstore.yang')
        when: 'attempt to get delta between an anchor and a JSON payload'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, ANCHOR_NAME_1, xpath, [:], jsonData, INCLUDE_ALL_DESCENDANTS)
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
             scenario         | xpath                                 | sourceDataNodes                  | sourceDataNodesAsMap            | jsonData                   || expectedNodeXpath                     | expectedSourceData          | expectedTargetData
            'root node xpath' | '/'                                   | bookstoreDataNodeWithParentXpath | bookstoreDataAsMapForParentNode | bookstoreJsonForParentNode || '/bookstore'                          | ['bookstore-name':'Easons'] | ['bookstore-name':'My Store']
            'parent xpath'    | '/bookstore'                          | bookstoreDataNodeWithParentXpath | bookstoreDataAsMapForParentNode | bookstoreJsonForParentNode || '/bookstore'                          | ['bookstore-name':'Easons'] | ['bookstore-name':'My Store']
            'non-root xpath'  | '/bookstore/categories[@code=\'02\']' | bookstoreDataNodeWithChildXpath  | bookstoreDataAsMapForChildNode  | bookstoreJsonForChildNode  || '/bookstore/categories[@code=\'02\']' | ['name':'Kids']             | ['name':'Child']
    }

    def 'Delta between anchor and payload error scenario #scenario'() {
        given: 'schema set for given anchor and dataspace references bookstore model'
            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap('bookstore.yang')
            setupSchemaSetMocksForDelta(yangResourceContentPerName)
        when: 'attempt to get delta between anchor and payload'
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, ANCHOR_NAME_1, xpath, yangResourceContentPerName, jsonData, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
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

    def 'Perform delete operation on existing data under an anchor using delta report'() {
        given: 'schema mocks, delta report in JSON format and observed time stamp'
            setupSchemaSetMocks('bookstore.yang')
            def deltaReportJson = '[{"action":"remove","xpath":"/bookstore/categories[@code=\'1\']","sourceData":{"book-store:books":[{"price":20,"title":"Matilda"}]}}]'
            def observedTimestamp = OffsetDateTime.now()
        and: 'delta report constructed from JSON'
            def deltaReport = new DeltaReportBuilder().actionRemove().withXpath('/bookstore/categories[@code=\'1\']').withSourceData(['book-store:books': [['price': 20, 'title': 'Matilda']]]).build()
        and: 'source data nodes from delta report'
            def sourceData = jsonObjectMapper.asJsonString(deltaReport.getSourceData())
        and: 'expected data nodes'
            def dataNodes = new DataNodeBuilder().withXpath('/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']').buildCollection()
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyDelta(dataspaceName, ANCHOR_NAME_1, deltaReportJson, observedTimestamp)
        then: 'the delta report in JSON format is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'data nodes are built from the source data of delta report'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, deltaReport.getXpath(), sourceData, ContentType.JSON) >> dataNodes
        and: 'cps data service is invoked to delete data nodes by using their xpaths'
            1 * mockCpsDataService.deleteDataNodes(dataspaceName, ANCHOR_NAME_1,  { xpaths -> xpaths == ["/bookstore/categories[@code='1']/books[@title='Matilda']"] }, observedTimestamp)
    }

    def 'Perform create operation on existing data under an anchor using delta report'() {
        given: 'schema mocks, delta report in JSON format and observed time stamp'
            setupSchemaSetMocks('bookstore.yang')
            def deltaReportJson = '[{"action":"create","xpath":"/bookstore/categories[@code=\'1\']","targetData":{"book-store:books":[{"price":20,"title":"Matilda"}]}}]'
            def observedTimestamp = OffsetDateTime.now()
        and: 'delta report constructed from JSON'
            def deltaReport = new DeltaReportBuilder().actionCreate().withXpath('/bookstore/categories[@code=\'1\']').withTargetData(['book-store:books': [['price': 20, 'title': 'Matilda']]]).build()
        and: 'target data nodes from delta report'
            def targetData = jsonObjectMapper.asJsonString(deltaReport.getTargetData())
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyDelta(dataspaceName, ANCHOR_NAME_1, deltaReportJson, observedTimestamp)
        then: 'the delta report in JSON format is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'cps data service is invoked to create data nodes by using their xpaths'
            1 * mockCpsDataService.saveListElements(dataspaceName, ANCHOR_NAME_1, deltaReport.getXpath(), targetData, observedTimestamp, ContentType.JSON)
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
