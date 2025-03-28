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
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.Anchor
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.model.DeltaReport
import org.onap.cps.cpspath.parser.CpsPathUtil
import org.onap.cps.impl.DataNodeFactoryImpl
import org.onap.cps.impl.DeltaReportBuilder
import org.onap.cps.impl.YangTextSchemaSourceSetCache
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
    def mockCpsDataService = Mock(CpsDataService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)
    def dataNodeFactory = new DataNodeFactoryImpl(yangParser)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def objectUnderTest = new DeltaReportExecutor(mockCpsAnchorService, mockCpsDataService, dataNodeFactory, jsonObjectMapper)

    @Shared
    static def ANCHOR_NAME_1 = 'some-anchor-1'
    static def ANCHOR_NAME_2 = 'some-anchor-2'
    static def NO_TIMESTAMP = null
    def dataspaceName = 'some-dataspace'
    def schemaSetName = 'some-schema-set'
    def anchor1 = Anchor.builder().name(ANCHOR_NAME_1).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    def anchor2 = Anchor.builder().name(ANCHOR_NAME_2).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()

    def setup() {
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_1) >> anchor1
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_2) >> anchor2
    }

    def 'Perform delete operation on existing data under an anchor using delta report'() {
        given: 'schema mocks and delta report in JSON format'
            setupSchemaSetMocks('bookstore.yang')
            def deltaReportJson = '[{"action":"remove","xpath":"/bookstore","sourceData":{"categories":[{"code":"1","name":"Children","books":[{"title":"Matilda"}]}]}}]'
        and: 'delta report constructed from JSON'
            def deltaReport = new DeltaReportBuilder().actionRemove().withXpath('/bookstore').withSourceData('categories': [['code': '1', 'name': 'Children', 'books': [['title': 'Matilda']]]]).build()
        and: 'source data as JSON string from delta report'
            def sourceData = jsonObjectMapper.asJsonString(deltaReport.getSourceData())
        and: 'expected data nodes with child nodes to delete'
            def dataNodes = [new DataNode(xpath: '/bookstore/categories[@code=\'1\']', childDataNodes: [new DataNode(xpath: '/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']')])]
            def xpathsToDelete = dataNodes*.xpath
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report in JSON format is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'data nodes are built from the source data of delta report'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, deltaReport.getXpath(), sourceData, ContentType.JSON) >> [dataNodes]
        and: 'appropriate cps data service method is invoked with expected parameters to delete data nodes'
            1 * mockCpsDataService.deleteDataNodes(dataspaceName, ANCHOR_NAME_1, xpathsToDelete, NO_TIMESTAMP)
    }

    def 'Perform create operation on existing data under an anchor using delta report to add a node with #scenario'() {
        given: 'schema mocks and delta report in JSON format'
            setupSchemaSetMocks('bookstore.yang')
        and: 'target data as JSON string from delta report'
            def targetData = jsonObjectMapper.asJsonString(deltaReport.getTargetData())
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report in JSON format is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'appropriate cps data service method is invoked with expected parameters to create data nodes'
            1 * mockCpsDataService.saveListElements(dataspaceName, ANCHOR_NAME_1, expectedXpath, targetData, NO_TIMESTAMP, ContentType.JSON)
        where:
            scenario                 | deltaReportJson                                                                                                                    | deltaReport                                                                                                                                                        || expectedXpath
            'xpath'                  | '[{"action":"create","xpath":"/bookstore","targetData":{"categories":[{"code":"1","name":"Children"}]}}]'                          | new DeltaReportBuilder().actionCreate().withXpath('/bookstore').withTargetData(['categories': [['code': '1', 'name': 'Children']]]).build()                        || '/bookstore'
            'list node xpath'        | '[{"action":"create","xpath":"/bookstore/categories[@code=\'1\']","targetData":{"books":[{"price":20,"title":"Matilda"}]}}]'       | new DeltaReportBuilder().actionCreate().withXpath('/bookstore/categories[@code=\'1\']').withTargetData(['books': [['price': 20, 'title': 'Matilda']]]).build()     || '/bookstore/categories[@code=\'1\']'
            'parent list node xpath' | '[{"action":"create","xpath":"/bookstore-address[@bookstore-name=\'Easons\']","targetData":{"address":{"street":"Main Street"}}}]' | new DeltaReportBuilder().actionCreate().withXpath('/bookstore-address[@bookstore-name=\'Easons\']').withTargetData(['address': ['street': 'Main Street']]).build() || '/'
    }

    def 'Perform replace operation on existing data under an anchor using delta report'() {
        given: 'schema mocks and delta report in JSON format'
            setupSchemaSetMocks('bookstore.yang')
            def deltaReportJson = '[{"action":"replace","xpath":"/bookstore/categories[@code=\'1\']","sourceData":{"books":[{"price":20,"title":"Matilda"}]},"targetData":{"books":[{"price":30,"title":"Matilda"}]}}]'
        and: 'delta report constructed from JSON'
            def deltaReport = new DeltaReportBuilder().actionReplace().withXpath('/bookstore/categories[@code=\'1\']').withSourceData(['books': [['price': 20, 'title': 'Matilda']]]).withTargetData(['books': [['price': 30, 'title': 'Matilda']]]).build()
        and: 'the parent node xpath is fetched from delta report'
            def parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(deltaReport.getXpath())
        and: 'target data as JSON is fetched from delta report'
            def targetData = jsonObjectMapper.asJsonString(deltaReport.getTargetData())
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report in JSON format is converted to a list of DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'cps data service is invoked with expected parameters to delete data nodes by using their xpaths'
            1 * mockCpsDataService.updateNodeLeavesAndExistingDescendantLeaves(dataspaceName, ANCHOR_NAME_1, parentNodeXpath, targetData, NO_TIMESTAMP)
    }

    def 'Batch operation using delta report rolls back in case of a semantically invalid Delta Report'() {
        given: 'schema mocks and a semantically invalid delta report in JSON format'
            setupSchemaSetMocks('bookstore.yang')
            def deltaReportJson = '[{"action":"create","xpath":"/bookstore/categories[@code=\'100\']","targetData":{"categories":[{"code":"100","name":"Funny"}]}},{"action":"remove","xpath":"/bookstore/categories[@code=\'4\']","sourceData":{"categorie":[{"code":"4","name":"Computing"}]}}]'
        and: 'delta report object with invalid data for remove operation'
            def deltaReport = [new DeltaReportBuilder().actionCreate().withXpath('/bookstore/categories[@code=\'100\']').withTargetData(['categories': [['code': '100', 'name': 'Funny']]]).build(), new DeltaReportBuilder().actionRemove().withXpath('/bookstore/categories[@code=\'4\'').withSourceData(['categorie': ['code': '4', 'name': 'Computing']]).build()]
        and: 'appropriate data is fetched from delta report'
            def xpathForCreateOperation = deltaReport[0].xpath
            def targetDataForCreateOperation = jsonObjectMapper.asJsonString(deltaReport[0].targetData)
            def xpathForDeleteOperation = deltaReport[1].xpath
            def sourceDataForDeleteOperation = jsonObjectMapper.asJsonString(deltaReport[1].sourceData)
        when: 'attempt to apply delta using the delta report'
            objectUnderTest.applyChangesInDeltaReport(dataspaceName, ANCHOR_NAME_1, deltaReportJson)
        then: 'the delta report in JSON format is converted to DeltaReport objects'
            jsonObjectMapper.convertToJsonArray(deltaReportJson, DeltaReport) >> [deltaReport]
        and: 'the create operation is attempted and succeeds'
            1 * mockCpsDataService.saveListElements(dataspaceName, ANCHOR_NAME_1, xpathForCreateOperation, targetDataForCreateOperation, NO_TIMESTAMP, ContentType.JSON)
        and: 'the remove operation fails due to invalid data, causing rollback'
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor1, xpathForDeleteOperation, sourceDataForDeleteOperation, ContentType.JSON) >> {throw new DataValidationException('Data Validation Failed')}
        then: 'a DataValidationException is thrown'
            thrown(DataValidationException)
    }

    def setupSchemaSetMocks(yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }
}
