/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd
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

package org.onap.cps.utils.deltareport

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.Anchor
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.model.DeltaReport
import org.onap.cps.cpspath.parser.CpsPathUtil
import org.onap.cps.impl.DataNodeFactoryImpl
import org.onap.cps.impl.DeltaReportBuilder
import org.onap.cps.impl.YangTextSchemaSourceSetCache
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.YangParser
import org.onap.cps.utils.YangParserHelper
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Shared
import spock.lang.Specification

class DeltaReportExecutorSpec extends Specification {

    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)
    def dataNodeFactory = new DataNodeFactoryImpl(yangParser)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def objectUnderTest = new DeltaReportExecutor(mockCpsAnchorService, mockCpsDataPersistenceService, dataNodeFactory, jsonObjectMapper)

    @Shared
    static def ANCHOR_NAME_1 = 'some-anchor-1'
    static def ANCHOR_NAME_2 = 'some-anchor-2'
    def dataspaceName = 'some-dataspace'
    def schemaSetName = 'some-schema-set'
    def anchor1 = Anchor.builder().name(ANCHOR_NAME_1).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    def anchor2 = Anchor.builder().name(ANCHOR_NAME_2).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()

    def setup() {
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_1) >> anchor1
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_2) >> anchor2
    }

    def 'Perform delete operation on existing data under an anchor using delta report'() {
        given: 'schema mocks and delta report as JSON'
            setupSchemaSetMocks('bookstore.yang')
            def deltaReportJson = '[{"action":"remove","xpath":"/bookstore","sourceData":{"categories":[{"code":"1","name":"Children","books":[{"title":"Matilda"}]}]}}]'
        and: 'delta report constructed from JSON'
            def deltaReport = new DeltaReportBuilder().actionRemove().withXpath('/bookstore').withSourceData('categories': [['code': '1', 'name': 'Children', 'books': [['title': 'Matilda']]]]).build()
        and: 'source data as JSON string from delta report'
            def sourceData = jsonObjectMapper.asJsonString(deltaReport.getSourceData())
        and: 'expected data nodes to delete'
            def dataNodes = [new DataNode(xpath: '/bookstore/categories[@code=\'1\']', childDataNodes: [new DataNode(xpath: '/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']')])]
            def xpathsToDelete = dataNodes*.xpath
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report in JSON format is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'data nodes are built from the source data of delta report'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, deltaReport.getXpath(), sourceData, ContentType.JSON) >> [dataNodes]
        and: 'appropriate cps data service method is invoked with expected parameters to delete data nodes'
            1 * mockCpsDataPersistenceService.deleteDataNodes(dataspaceName, ANCHOR_NAME_1, xpathsToDelete)
    }

    def 'Perform create operation on existing data under an anchor using delta report to add a node with #scenario'() {
        given: 'schema mocks and test data'
            setupSchemaSetMocks('bookstore.yang')
            def testData = setupTestData(scenario)
        and: 'delta report as JSON'
            def deltaReportJson = testData.deltaReportJson
        and: 'delta report constructed from JSON'
            def deltaReport = testData.deltaReport
        and: 'data nodes to be created'
            def dataNodes = testData.dataNodes
        and: 'target data as JSON string from delta report'
            def targetData = jsonObjectMapper.asJsonString(deltaReport.getTargetData())
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report as JSON string is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'data nodes are built from the target data of delta report'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, expectedXpath, targetData, ContentType.JSON) >> dataNodes
        and: 'appropriate cps data service method is invoked with expected parameters to create data nodes'
            1 * mockCpsDataPersistenceService.addListElements(dataspaceName, ANCHOR_NAME_1, expectedXpath, { it*.xpath == dataNodes*.xpath && it*.leaves == dataNodes*.leaves })
        where:
            scenario          || expectedXpath
            'xpath'           || '/bookstore'
            'list node xpath' || '/bookstore/categories[@code=\'1\']'
    }

    def 'Perform create operation on existing data under an anchor using delta report to add a parent list node'() {
        given: 'schema mocks and delta report in JSON format'
            setupSchemaSetMocks('bookstore.yang')
        and: 'delta report as JSON'
            def deltaReportJson = '[{"action":"create","xpath":"/bookstore-address[@bookstore-name=\'Easons\']","targetData":{"bookstore-address":[{"bookstore-name":"Easons"}]}}]'
        and: 'delta report constructed from JSON'
            def deltaReport = new DeltaReportBuilder().actionCreate().withXpath('/bookstore-address[@bookstore-name=\'Easons\']').withTargetData(['bookstore-address': [['bookstore-name': 'Easons']]]).build()
        and: 'target data as JSON string from delta report'
            def targetData = jsonObjectMapper.asJsonString(deltaReport.getTargetData())
        and: 'data nodes created from delta report'
            def dataNodesCreatedFromDeltaReport = [new DataNode(xpath: '/bookstore-address[@bookstore-name=\'Easons\']', leaves: ['bookstore-name':'Easons'])]
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report as JSON string is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'data nodes are built from the target data of delta report'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, '/', targetData, ContentType.JSON) >> dataNodesCreatedFromDeltaReport
        and: 'appropriate cps data service method is invoked with expected parameters to create data nodes'
            1 * mockCpsDataPersistenceService.storeDataNodes(dataspaceName, ANCHOR_NAME_1, dataNodesToBeStored ->
            { dataNodesToBeStored*.xpath == dataNodesCreatedFromDeltaReport*.xpath &&
                dataNodesToBeStored*.leaves == dataNodesCreatedFromDeltaReport*.leaves })
        }

    def 'Perform replace operation on existing data under an anchor using delta report'() {
        given: 'schema mocks'
            setupSchemaSetMocks('bookstore.yang')
        and: 'delta report as JSON string with parent and child data nodes'
            def deltaReportJson = '[{"action":"replace","xpath":"/bookstore/categories[@code=\'1\']","sourceData":{"categories":[{"code":"1","name":"Children","books":[{"title":"Matilda","price":20}]}]},"targetData":{"categories":[{"code":"1","name":"Children","books":[{"title":"Matilda","price":30}]}]}}]'
        and: 'delta report constructed from JSON'
            def deltaReport = new DeltaReportBuilder().actionReplace().withXpath('/bookstore/categories[@code=\'1\']').withSourceData(['categories': [['code': '1', 'name': 'Children', 'books': [['title': 'Matilda', 'price': 20]]]]]).withTargetData(['categories': [['code': '1', 'name': 'Children', 'books': [['title': 'Matilda', 'price': 30]]]]]).build()
        and: 'the parent node xpath is fetched from delta report'
            def parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(deltaReport.getXpath())
        and: 'target data as JSON string is fetched from delta report'
            def targetData = jsonObjectMapper.asJsonString(deltaReport.getTargetData())
        and: 'parent and child nodes to be updated'
         def dataNodesCreatedFromDeltaReport = [new DataNode(xpath: '/bookstore/categories[@code=\'1\']', leaves: ['code': '1', 'name': 'Children'], childDataNodes: [new DataNode(xpath: '/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']', leaves: ['price': 30, 'title': 'Matilda'])])]
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report as JSON string is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'data nodes are built from the target data of delta report'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, parentNodeXpath, targetData, ContentType.JSON) >> dataNodesCreatedFromDeltaReport
        and: 'cps data service is invoked with expected parameters to update data nodes'
            1 * mockCpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, ANCHOR_NAME_1, dataNodesToBeStored ->
            { dataNodesToBeStored*.xpath == dataNodesCreatedFromDeltaReport*.xpath &&
                dataNodesToBeStored*.leaves == dataNodesCreatedFromDeltaReport*.leaves })
    }

    def 'Batch operation using delta report rolls back in case of a semantically invalid Delta Report'() {
        given: 'schema mocks and a semantically invalid delta report as JSON string (code: xxx is invalid value for key code)'
            setupSchemaSetMocks('bookstore.yang')
            def deltaReportJson = '[{"action":"create","xpath":"/bookstore","targetData":{"categories":[{"code":"xxx","name":"Funny"}]}},{"action":"remove","xpath":"/bookstore","sourceData":{"categorie":[{"code":"4","name":"Computing"}]}}]'
        and: 'delta report object with invalid data for remove operation (code: xxx is invalid value for key code)'
            def deltaReport = [new DeltaReportBuilder().actionCreate().withXpath('/bookstore').withTargetData(['categories': [['code': 'xxx', 'name': 'Funny']]]).build(), new DeltaReportBuilder().actionRemove().withXpath('/bookstore').withSourceData(['categorie': ['code': '4', 'name': 'Computing']]).build()]
        and: 'data nodes to be created'
            def dataNodesToAdd = [new DataNode(xpath: '/bookstore/categories[@code=\'100\']', leaves: ['code': '100', 'name': 'Funny'])]
        and: 'appropriate data is fetched from delta report'
            def xpathForCreateOperation = deltaReport[0].xpath
            def targetDataForCreateOperation = jsonObjectMapper.asJsonString(deltaReport[0].targetData)
            def xpathForDeleteOperation = deltaReport[1].xpath
            def sourceDataForDeleteOperation = jsonObjectMapper.asJsonString(deltaReport[1].sourceData)
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report in JSON format is converted to DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> deltaReport
        and: 'data nodes are built from the target data of create operation in delta report'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, xpathForCreateOperation, targetDataForCreateOperation, ContentType.JSON) >> dataNodesToAdd
        and: 'the create operation is attempted and succeeds'
           1 * mockCpsDataPersistenceService.addListElements(dataspaceName, ANCHOR_NAME_1, xpathForCreateOperation, { it })
        and: 'the remove operation fails due to invalid data, causing rollback'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, xpathForDeleteOperation, sourceDataForDeleteOperation, ContentType.JSON) >> {throw new DataValidationException('Data Validation Failed')}
        and: 'a DataValidationException is thrown'
            thrown(DataValidationException)
    }
    def 'Apply delta report with an invalid xpath'() {
        given: 'delta report as JSON string with an invalid xpath for #action action'
            def deltaReportJson = '[{"action":"create","xpath":"/invalid[","targetData":{"data":[{"key":"value"}]}}]'
        when: 'attempt to apply delta'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'DataValidationException is thrown'
            def exception = thrown(DataValidationException)
            assert exception.message == 'Error while parsing xpath expression \'/invalid[\'.'
            assert exception.details == 'failed to parse at line 1 due to no viable alternative at input \'[\''
    }

    def 'Apply delta report with an unsupported action'() {
        given: 'delta report as JSON string'
            def deltaReportJson = '[{"action":"unsupported","xpath":"/bookstore","targetData":{"categories":[{"code":"1"}]}}]'
        when: 'attempt to apply delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'DataValidationException is thrown with correct details'
            def exception = thrown(DataValidationException)
            assert exception.message == 'Invalid \'action\' in delta report.'
            assert exception.details == 'Unsupported action \'unsupported\' at xpath: /bookstore. Valid actions are: \'create\', \'remove\' or \'replace\'.'
    }

    def setupSchemaSetMocks(yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).schemaContext()
        mockYangTextSchemaSourceSet.schemaContext() >> schemaContext
    }

    def setupTestData(scenario) {
        if (scenario == 'xpath') {
            return [
                deltaReportJson: '[{"action":"create","xpath":"/bookstore","targetData":{"categories":[{"code":"1","name":"Children"}]}}]',
                deltaReport: new DeltaReportBuilder().actionCreate().withXpath('/bookstore').withTargetData(['categories': [['code': '1', 'name': 'Children']]]).build(),
                dataNodes: [new DataNode(xpath: '/bookstore/categories[@code=\'1\']', leaves: ['code': '1', 'name': 'Children'])]
            ]
        }
        return [
            deltaReportJson: '[{"action":"create","xpath":"/bookstore/categories[@code=\'1\']","targetData":{"books":[{"price":20,"title":"Matilda"}]}}]',
            deltaReport: new DeltaReportBuilder().actionCreate().withXpath('/bookstore/categories[@code=\'1\']').withTargetData(['books': [['price': 20, 'title': 'Matilda']]]).build(),
            dataNodes: [new DataNode(xpath: '/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']', leaves: ['price':20, 'title':'Matilda'])]
        ]
    }
}
