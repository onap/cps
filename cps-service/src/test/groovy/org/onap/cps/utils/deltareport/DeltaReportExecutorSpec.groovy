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

package org.onap.cps.utils.deltareport

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.DataNodeFactory
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.Anchor
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.model.DeltaReport
import org.onap.cps.impl.DataNodeFactoryImpl
import org.onap.cps.impl.DeltaReportBuilder
import org.onap.cps.impl.YangTextSchemaSourceSetCache
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.XmlObjectMapper
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
    def xmlObjectMapper = new XmlObjectMapper()
    def objectUnderTest = new DeltaReportExecutor(mockCpsAnchorService, mockCpsDataPersistenceService, dataNodeFactory, jsonObjectMapper, xmlObjectMapper)
    def mockDataNodeFactory = Mock(DataNodeFactory)
    def objectUnderTestWithMock = new DeltaReportExecutor(mockCpsAnchorService, mockCpsDataPersistenceService, mockDataNodeFactory, jsonObjectMapper, xmlObjectMapper)

    @Shared
    static def ANCHOR_NAME_1 = 'some-anchor-1'
    static def ANCHOR_NAME_2 = 'some-anchor-2'
    static def bookstoreCategoryNode = [new DataNode(xpath: '/bookstore/categories[@code=\'1\']', leaves: ['code': '1', 'name': 'Children'])]
    static def bookstoreBookNode = [new DataNode(xpath: '/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']', leaves: ['price': 20, 'title': 'Matilda'])]
    static def REMOVE_JSON = '[{"action":"remove","xpath":"/bookstore"}]'
    static def CREATE_JSON = '[{"action":"create","xpath":"/bookstore"}]'
    static def CREATE_LIST_JSON = '[{"action":"create","xpath":"/bookstore/categories[@code=\'1\']"}]'
    static def CREATE_PARENT_LIST_JSON = '[{"action":"create","xpath":"/bookstore-address[@bookstore-name=\'Easons\']"}]'
    static def REPLACE_JSON = '[{"action":"replace","xpath":"/bookstore/categories[@code=\'1\']"}]'
    static def REMOVE_XML = '<deltaReports><deltaReport><action>remove</action><xpath>/bookstore</xpath><sourceData><categories><code>1</code></categories></sourceData></deltaReport></deltaReports>'
    static def CREATE_XML = '<deltaReports><deltaReport><action>create</action><xpath>/bookstore</xpath><targetData><categories><code>1</code></categories></targetData></deltaReport></deltaReports>'
    static def CREATE_LIST_XML = '<deltaReports><deltaReport><action>create</action><xpath>/bookstore/categories[@code=\'1\']</xpath><targetData><books><price>20</price></books></targetData></deltaReport></deltaReports>'
    static def CREATE_PARENT_LIST_XML = '<deltaReports><deltaReport><action>create</action><xpath>/bookstore-address[@bookstore-name=\'Easons\']</xpath><targetData><bookstore-address><bookstore-name>Easons</bookstore-name></bookstore-address></targetData></deltaReport></deltaReports>'
    static def REPLACE_XML = '<deltaReports><deltaReport><action>replace</action><xpath>/bookstore/categories[@code=\'1\']</xpath><sourceData><categories><code>1</code><name>Children</name></categories></sourceData><targetData><categories><code>1</code><name>Kids</name></categories></targetData></deltaReport></deltaReports>'

    def dataspaceName = 'some-dataspace'
    def schemaSetName = 'some-schema-set'
    def anchor1 = Anchor.builder().name(ANCHOR_NAME_1).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    def anchor2 = Anchor.builder().name(ANCHOR_NAME_2).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()

    def setup() {
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_1) >> anchor1
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_2) >> anchor2
    }

    def 'Perform delete operation on existing data under an anchor using delta report'() {
        given: 'data nodes'
            def dataNodes = [new DataNode(xpath: '/bookstore/categories[@code=\'1\']',childDataNodes: [new DataNode(xpath: '/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']')])]
            def xpathsToDelete = dataNodes*.xpath
        when: 'attempt to apply delta using the delta report'
            objectUnderTestWithMock.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReport, contentType)
        then: 'data nodes are built from the source data of delta report'
            mockDataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, '/bookstore', _, contentType) >> dataNodes
        and: 'appropriate cps data service method is invoked with expected parameters to delete data nodes'
            1 * mockCpsDataPersistenceService.deleteDataNodes(dataspaceName, ANCHOR_NAME_1, xpathsToDelete)
        where:
            scenario | contentType       | deltaReport
            'JSON'   | ContentType.JSON  | REMOVE_JSON
            'XML'    | ContentType.XML   | REMOVE_XML
    }

    def 'Perform create operation on existing data under an anchor using delta report to add a node with #scenario'() {
        when: 'attempt to apply delta using the delta report'
            objectUnderTestWithMock.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReport, contentType)
        then: 'data nodes are built from the target data of delta report'
            mockDataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, expectedXpath, _, contentType) >> dataNodesInput
        and: 'appropriate cps data service method is invoked with expected parameters to create data nodes'
            1 * mockCpsDataPersistenceService.addListElements(dataspaceName, ANCHOR_NAME_1, expectedXpath, { it*.xpath == dataNodesInput*.xpath && it*.leaves == dataNodesInput*.leaves})
        where:
            scenario                   | contentType      | deltaReport      | dataNodesInput        || expectedXpath
            'JSON and xpath'           | ContentType.JSON | CREATE_JSON      | bookstoreCategoryNode || '/bookstore'
            'JSON and list node xpath' | ContentType.JSON | CREATE_LIST_JSON | bookstoreBookNode     || '/bookstore/categories[@code=\'1\']'
            'XML and xpath'            | ContentType.XML  | CREATE_XML       | bookstoreCategoryNode || '/bookstore'
            'XML and list node xpath'  | ContentType.XML  | CREATE_LIST_XML  | bookstoreBookNode     || '/bookstore/categories[@code=\'1\']'
    }

    def 'Perform create operation on existing data under an anchor using delta report to add a parent list node with #scenario'() {
        given: 'data nodes'
            def dataNodes = [new DataNode(xpath: '/bookstore-address[@bookstore-name=\'Easons\']',leaves: ['bookstore-name': 'Easons'])]
        when: 'attempt to apply delta using the delta report'
            objectUnderTestWithMock.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReport, contentType)
        then: 'data nodes are built from the target data of delta report'
            mockDataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, '/', _, contentType) >> dataNodes
        and: 'appropriate cps data service method is invoked with expected parameters to create data nodes'
            1 * mockCpsDataPersistenceService.storeDataNodes(dataspaceName, ANCHOR_NAME_1, dataNodesToBeStored -> {
                dataNodesToBeStored*.xpath == dataNodes*.xpath && dataNodesToBeStored*.leaves == dataNodes*.leaves })
        where:
            scenario | contentType      | deltaReport
            'JSON'   | ContentType.JSON | CREATE_PARENT_LIST_JSON
            'XML'    | ContentType.XML  | CREATE_PARENT_LIST_XML
    }

    def 'Perform replace operation on existing data under an anchor using delta report'() {
        given: 'data nodes'
            def dataNodes = [new DataNode(xpath: "/bookstore/categories[@code='1']",leaves: ['code': '1', 'name': 'Children'],childDataNodes: [new DataNode(xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']",leaves: ['price': 30, 'title': 'Matilda'])])]
        when: 'attempt to apply delta using the delta report'
            objectUnderTestWithMock.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReport, contentType)
        then: 'data nodes are built from the target data of delta report'
            mockDataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, '/bookstore', _, contentType) >> dataNodes
        and: 'cps data service is invoked with expected parameters to update data nodes'
            1 * mockCpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, ANCHOR_NAME_1,{
                dataNodesToBeStored -> dataNodesToBeStored*.xpath == dataNodes*.xpath && dataNodesToBeStored*.leaves == dataNodes*.leaves })
        where:
            scenario | contentType       | deltaReport
            'JSON'   | ContentType.JSON  | REPLACE_JSON
            'XML'    | ContentType.XML   | REPLACE_XML
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
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson,ContentType.JSON)
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
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson, ContentType.JSON)
        then: 'expected exception is thrown'
            def thrownException = thrown(DataValidationException)
            assert thrownException.message == 'Error while parsing xpath expression \'/invalid[\'.'
            assert thrownException.details == 'Failed to parse at line 1 due to no viable alternative at input \'[\''
    }

    def 'Apply delta report with an unsupported action'() {
        given: 'delta report as JSON string'
            def deltaReportJson = '[{"action":"invalidAction","xpath":"/bookstore","targetData":{"categories":[{"code":"1"}]}}]'
        when: 'attempt to apply delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson, ContentType.JSON)
        then: 'expected exception is thrown with correct details'
            def thrownException = thrown(DataValidationException)
            assert thrownException.message == 'Invalid \'action\' in delta report.'
            assert thrownException.details.contains('invalidAction')
            assert thrownException.details.contains('/bookstore')
    }

    def setupSchemaSetMocks(yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).schemaContext()
        mockYangTextSchemaSourceSet.schemaContext() >> schemaContext
    }
}
