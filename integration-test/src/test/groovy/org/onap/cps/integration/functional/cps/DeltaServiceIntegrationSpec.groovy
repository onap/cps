/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG
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

package org.onap.cps.integration.functional.cps

import org.onap.cps.api.CpsDeltaService
import org.onap.cps.api.exceptions.AnchorNotFoundException
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.exceptions.DataspaceNotFoundException
import org.onap.cps.api.model.DeltaReport
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.integration.base.FunctionalSpecBase

class DeltaServiceIntegrationSpec extends FunctionalSpecBase {
    CpsDeltaService objectUnderTest
    def originalCountBookstoreChildNodes
    def originalCountXmlBookstoreChildNodes
    def originalCountBookstoreTopLevelListNodes

    static def INCLUDE_ALL_DESCENDANTS = FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
    static def OMIT_DESCENDANTS = FetchDescendantsOption.OMIT_DESCENDANTS
    static def DIRECT_CHILDREN_ONLY = FetchDescendantsOption.DIRECT_CHILDREN_ONLY
    def NO_GROUPING = false

    def setup() {
        objectUnderTest = cpsDeltaService
        originalCountBookstoreChildNodes = countDataNodesInBookstore()
        originalCountBookstoreTopLevelListNodes = countTopLevelListDataNodesInBookstore()
        originalCountXmlBookstoreChildNodes = countXmlDataNodesInBookstore()
    }

    def 'Get delta between 2 anchors'() {
        when: 'attempt to get delta report between anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_5, '/', OMIT_DESCENDANTS, NO_GROUPING)
        and: 'report is ordered based on xpath'
            result = result.toList().sort { it.xpath }
        then: 'delta report contains expected number of changes'
            result.size() == 3
        and: 'delta report contains REPLACE action with expected xpath'
            assert result[0].getAction() == 'replace'
            assert result[0].getXpath() == '/bookstore'
        and: 'delta report contains CREATE action with expected xpath'
            assert result[1].getAction() == 'create'
            assert result[1].getXpath() == '/bookstore-address[@bookstore-name=\'Crossword Bookstores\']'
        and: 'delta report contains REMOVE action with expected xpath'
            assert result[2].getAction() == 'remove'
            assert result[2].getXpath() == '/bookstore-address[@bookstore-name=\'Easons-1\']'
    }

    def 'Get delta between 2 anchors returns empty response when #scenario'() {
        when: 'attempt to get delta report between anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, targetAnchor, xpath, INCLUDE_ALL_DESCENDANTS, NO_GROUPING)
        then: 'delta report is empty'
            assert result.isEmpty()
        where: 'following data was used'
            scenario                              | targetAnchor       | xpath
        'anchors with identical data are queried' | BOOKSTORE_ANCHOR_4 | '/'
        'same anchor name is passed as parameter' | BOOKSTORE_ANCHOR_3 | '/'
        'non existing xpath'                      | BOOKSTORE_ANCHOR_5 | '/non-existing-xpath'
    }

    def 'Get delta between anchors error scenario: #scenario'() {
        when: 'attempt to get delta between anchors'
            objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, sourceAnchor, targetAnchor, '/some-xpath', INCLUDE_ALL_DESCENDANTS, NO_GROUPING)
        then: 'expected exception is thrown'
            thrown(expectedException)
        where: 'following data was used'
                    scenario                               | dataspaceName               | sourceAnchor          | targetAnchor          || expectedException
            'invalid dataspace name'                       | 'Invalid dataspace'         | 'not-relevant'        | 'not-relevant'        || DataValidationException
            'invalid anchor 1 name'                        | FUNCTIONAL_TEST_DATASPACE_3 | 'invalid anchor'      | 'not-relevant'        || DataValidationException
            'invalid anchor 2 name'                        | FUNCTIONAL_TEST_DATASPACE_3 | BOOKSTORE_ANCHOR_3    | 'invalid anchor'      || DataValidationException
            'non-existing dataspace'                       | 'non-existing'              | 'not-relevant1'       | 'not-relevant2'       || DataspaceNotFoundException
            'non-existing dataspace with same anchor name' | 'non-existing'              | 'not-relevant'        | 'not-relevant'        || DataspaceNotFoundException
            'non-existing anchor 1'                        | FUNCTIONAL_TEST_DATASPACE_3 | 'non-existing-anchor' | 'not-relevant'        || AnchorNotFoundException
            'non-existing anchor 2'                        | FUNCTIONAL_TEST_DATASPACE_3 | BOOKSTORE_ANCHOR_3    | 'non-existing-anchor' || AnchorNotFoundException
    }

    def 'Get delta between anchors for remove action, where source data node #scenario'() {
        when: 'attempt to get delta between leaves of data nodes present in 2 anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_5, BOOKSTORE_ANCHOR_3, parentNodeXpath, INCLUDE_ALL_DESCENDANTS, NO_GROUPING)
        then: 'expected action is present in delta report'
            assert result.get(0).getAction() == 'remove'
        where: 'following data was used'
            scenario                     | parentNodeXpath
            'has leaves and child nodes' | '/bookstore/categories[@code=\'6\']'
            'has leaves only'            | '/bookstore/categories[@code=\'5\']/books[@title=\'Book 11\']'
            'has child data node only'   | '/bookstore/support-info/contact-emails'
            'is empty'                   | '/bookstore/container-without-leaves'
    }

    def 'Get delta between anchors for "create" action, where target data node #scenario'() {
        when: 'attempt to get delta between leaves of data nodes present in 2 anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_5, parentNodeXpath, INCLUDE_ALL_DESCENDANTS, NO_GROUPING)
        then: 'the expected action is present in delta report'
            result.get(0).getAction() == 'create'
        and: 'the expected xapth is present in delta report'
            result.get(0).getXpath() == parentNodeXpath
        where: 'following data was used'
            scenario                     | parentNodeXpath
            'has leaves and child nodes' | '/bookstore/categories[@code=\'6\']'
            'has leaves only'            | '/bookstore/categories[@code=\'5\']/books[@title=\'Book 11\']'
            'has child data node only'   | '/bookstore/support-info/contact-emails'
            'is empty'                   | '/bookstore/container-without-leaves'
    }

    def 'Get delta between anchors when leaves of existing data nodes are updated,: #scenario'() {
        when: 'attempt to get delta between leaves of existing data nodes'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, xpath, OMIT_DESCENDANTS, NO_GROUPING)
        then: 'expected action is "replace"'
            assert result[0].getAction() == 'replace'
        and: 'the payload has expected leaf values'
            def sourceData = result[0].getSourceData()
            def targetData = result[0].getTargetData()
            assert sourceData.equals(expectedSourceValue)
            assert targetData.equals(expectedTargetValue)
        where: 'following data was used'
            scenario                           | sourceAnchor       | targetAnchor       | xpath                                                         || expectedSourceValue                         | expectedTargetValue
            'leaf is updated in target anchor' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | '/bookstore'                                                  || ['bookstore':['bookstore-name':'Easons-1']] | ['bookstore':['bookstore-name': 'Crossword Bookstores']]
            'leaf is removed in target anchor' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | '/bookstore/categories[@code=\'5\']/books[@title=\'Book 1\']' || ['books':[['price':1, 'title':'Book 1']]]   | null
            'leaf is added in target anchor'   | BOOKSTORE_ANCHOR_5 | BOOKSTORE_ANCHOR_3 | '/bookstore/categories[@code=\'5\']/books[@title=\'Book 1\']' || null                                        | ['books':[['title':'Book 1', 'price':1]]]
    }

    def 'Get delta between anchors when child data nodes under existing parent data nodes are updated: #scenario'() {
        when: 'attempt to get delta between leaves of existing data nodes'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, xpath, DIRECT_CHILDREN_ONLY, NO_GROUPING)
        then: 'expected action is "replace"'
            assert result[0].getAction() == 'replace'
        and: 'the delta report has expected child node xpaths'
            def deltaReportEntities = getDeltaReportEntities(result)
            def childNodeXpathsInDeltaReport = deltaReportEntities.get('xpaths')
            assert childNodeXpathsInDeltaReport.contains(expectedChildNodeXpath)
        where: 'following data was used'
            scenario                                          | sourceAnchor       | targetAnchor       | xpath                 || expectedChildNodeXpath
            'source and target anchors have child data nodes' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | '/bookstore/premises' || '/bookstore/premises/addresses[@house-number=\'2\' and @street=\'Main Street\']'
            'removed child data nodes in target anchor'       | BOOKSTORE_ANCHOR_5 | BOOKSTORE_ANCHOR_3 | '/bookstore'          || '/bookstore/support-info'
            'added  child data nodes in target anchor'        | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | '/bookstore'          || '/bookstore/support-info'
    }

    def 'Get delta between anchors where source and target data nodes have leaves and child data nodes'() {
        given: 'parent node xpath and expected data in delta report'
            def parentNodeXpath = '/bookstore/categories[@code=\'1\']'
            def expectedSourceDataInParentNode = ['categories':[['code':'1', 'name':'Children']]]
            def expectedTargetDataInParentNode = ['categories':[['code':'1', 'name':'Kids']]]
            def expectedSourceDataInChildNode = [['books':[['lang':'English', 'title':'The Gruffalo']]], ['books':[['editions':[1988, 2000], 'price':20, 'title':'Matilda']]]]
            def expectedTargetDataInChildNode = [['books':[['lang':'English/German', 'title':'The Gruffalo']]], ['books':[['price':200, 'editions':[1988, 2000, 2023], 'title':'Matilda']]]]
        when: 'attempt to get delta between leaves of existing data nodes'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_5, parentNodeXpath, INCLUDE_ALL_DESCENDANTS, NO_GROUPING)
            def deltaReportEntities = getDeltaReportEntities(result)
        then: 'expected action is "replace"'
            assert result[0].getAction() == 'replace'
        and: 'the payload has expected parent node xpath'
            assert deltaReportEntities.get('xpaths').contains(parentNodeXpath)
        and: 'delta report has expected source and target data'
            assert deltaReportEntities.get('sourcePayload').contains(expectedSourceDataInParentNode)
            assert deltaReportEntities.get('targetPayload').contains(expectedTargetDataInParentNode)
        and: 'the delta report also has expected child node xpaths'
            assert deltaReportEntities.get('xpaths').containsAll(['/bookstore/categories[@code=\'1\']/books[@title=\'The Gruffalo\']', '/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']'])
        and: 'the delta report also has expected source and target data of child nodes'
            assert deltaReportEntities.get('sourcePayload').containsAll(expectedSourceDataInChildNode)
            assert deltaReportEntities.get('targetPayload').containsAll(expectedTargetDataInChildNode)
    }

    def 'Get delta between anchor and JSON payload'() {
        when: 'attempt to get delta report between anchor and JSON payload'
            def jsonPayload = '{\"book-store:bookstore\":{\"bookstore-name\":\"Crossword Bookstores\"},\"book-store:bookstore-address\":{\"address\":\"Bangalore, India\",\"postal-code\":\"560062\",\"bookstore-name\":\"Crossword Bookstores\"}}'
            def result = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, '/', [:], jsonPayload, OMIT_DESCENDANTS, NO_GROUPING)
        then: 'delta report contains expected number of changes'
            result.size() == 3
        and: 'delta report contains "replace" action with expected xpath'
            assert result[0].getAction() == 'replace'
            assert result[0].getXpath() == '/bookstore'
        and: 'delta report contains "remove" action with expected xpath'
            assert result[1].getAction() == 'remove'
            assert result[1].getXpath() == '/bookstore-address[@bookstore-name=\'Easons-1\']'
        and: 'delta report contains "create" action with expected xpath'
            assert result[2].getAction() == 'create'
            assert result[2].getXpath() == '/bookstore-address[@bookstore-name=\'Crossword Bookstores\']'
    }

    def 'Get delta between anchor and payload returns empty response when JSON payload is identical to anchor data'() {
        when: 'attempt to get delta report between anchor and JSON payload (replacing the string Easons with Easons-1 because the data in JSON file is modified, to append anchor number, during the setup process of the integration tests)'
            def jsonPayload = readResourceDataFile('bookstore/bookstoreData.json').replace('Easons', 'Easons-1')
            def result = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, '/', [:], jsonPayload, INCLUDE_ALL_DESCENDANTS, NO_GROUPING)
        then: 'delta report is empty'
            assert result.isEmpty()
    }

    def 'Get delta between anchor and payload error scenario: #scenario'() {
        when: 'attempt to get delta between anchor and json payload'
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, sourceAnchor, xpath, [:], jsonPayload, INCLUDE_ALL_DESCENDANTS, NO_GROUPING)
        then: 'expected exception is thrown'
            thrown(expectedException)
        where: 'following data was used'
                scenario                               | dataspaceName               | sourceAnchor          | xpath        | jsonPayload   || expectedException
        'invalid dataspace name'                       | 'Invalid dataspace'         | 'not-relevant'        | '/'          | '{some-json}' || DataValidationException
        'invalid anchor name'                          | FUNCTIONAL_TEST_DATASPACE_3 | 'invalid anchor'      | '/'          | '{some-json}' || DataValidationException
        'non-existing dataspace'                       | 'non-existing'              | 'not-relevant'        | '/'          | '{some-json}' || DataspaceNotFoundException
        'non-existing anchor'                          | FUNCTIONAL_TEST_DATASPACE_3 | 'non-existing-anchor' | '/'          | '{some-json}' || AnchorNotFoundException
        'empty json payload with root node xpath'      | FUNCTIONAL_TEST_DATASPACE_3 | BOOKSTORE_ANCHOR_3    | '/'          | ''            || DataValidationException
        'empty json payload with non-root node xpath'  | FUNCTIONAL_TEST_DATASPACE_3 | BOOKSTORE_ANCHOR_3    | '/bookstore' | ''            || DataValidationException
    }

    def getDeltaReportEntities(List<DeltaReport> deltaReport) {
        def xpaths = []
        def action = []
        def sourcePayload = []
        def targetPayload = []
        deltaReport.each {
            delta -> xpaths.add(delta.getXpath())
                action.add(delta.getAction())
                sourcePayload.add(delta.getSourceData())
                targetPayload.add(delta.getTargetData())
        }
        return ['xpaths':xpaths, 'action':action, 'sourcePayload':sourcePayload, 'targetPayload':targetPayload]
    }

    def countDataNodesInBookstore() {
        return countDataNodesInTree(cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', INCLUDE_ALL_DESCENDANTS))
    }

    def countTopLevelListDataNodesInBookstore() {
        return countDataNodesInTree(cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/', INCLUDE_ALL_DESCENDANTS))
    }

    def countXmlDataNodesInBookstore() {
        return countDataNodesInTree(cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_4, BOOKSTORE_ANCHOR_6, '/bookstore', INCLUDE_ALL_DESCENDANTS))
    }

}
