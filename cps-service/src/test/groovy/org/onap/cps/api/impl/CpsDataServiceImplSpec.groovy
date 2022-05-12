/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
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

package org.onap.cps.api.impl

import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAdminService
import org.onap.cps.notification.NotificationService
import org.onap.cps.notification.Operation
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.CmHandleQueryParameters
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

import java.time.OffsetDateTime

class CpsDataServiceImplSpec extends Specification {
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockNotificationService = Mock(NotificationService)

    def objectUnderTest = new CpsDataServiceImpl(mockCpsDataPersistenceService, mockCpsAdminService,
            mockYangTextSchemaSourceSetCache, mockNotificationService)

    def setup() {
        mockCpsAdminService.getAnchor(dataspaceName, anchorName) >> anchor
    }

    def dataspaceName = 'some-dataspace'
    def anchorName = 'some-anchor'
    def schemaSetName = 'some-schema-set'
    def anchor = Anchor.builder().name(anchorName).schemaSetName(schemaSetName).build()
    def observedTimestamp = OffsetDateTime.now()

    def 'Saving json data.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with test-tree json data'
            def jsonData = TestUtils.getResourceFileContent('test-tree.json')
            objectUnderTest.saveData(dataspaceName, anchorName, jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.storeDataNode(dataspaceName, anchorName,
                { dataNode -> dataNode.xpath == '/test-tree' })
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, '/', Operation.CREATE)
    }

    def 'Saving json data with invalid #scenario.'() {
        when: 'save data method is invoked with invalid #scenario'
            objectUnderTest.saveData(dataspaceName, anchorName, _ as String, observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsDataPersistenceService.storeDataNode(_, _, _)
        and: 'data updated event is not sent to notification service'
            0 * mockNotificationService.processDataUpdatedEvent(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Saving child data fragment under existing node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with test-tree json data'
            def jsonData = '{"branch": [{"name": "New"}]}'
            objectUnderTest.saveData(dataspaceName, anchorName, '/test-tree', jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.addChildDataNode(dataspaceName, anchorName, '/test-tree',
                { dataNode -> dataNode.xpath == '/test-tree/branch[@name=\'New\']' })
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, '/test-tree', Operation.CREATE)
    }

    def 'Saving child data fragment under existing node with invalid #scenario.'() {
        when: 'save data method is invoked with test-tree and an invalid #scenario'
            objectUnderTest.saveData(dataspaceName, anchorName, '/test-tree', _ as String, observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsDataPersistenceService.addChildDataNode(_, _, _,_)
        and: 'data updated event is not sent to notification service'
            0 * mockNotificationService.processDataUpdatedEvent(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Saving list element data fragment under existing node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with list element json data'
            def jsonData = '{"branch": [{"name": "A"}, {"name": "B"}]}'
            objectUnderTest.saveListElements(dataspaceName, anchorName, '/test-tree', jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.addListElements(dataspaceName, anchorName, '/test-tree',
                { dataNodeCollection ->
                    {
                        assert dataNodeCollection.size() == 2
                        assert dataNodeCollection.collect { it.getXpath() }
                            .containsAll(['/test-tree/branch[@name=\'A\']', '/test-tree/branch[@name=\'B\']'])
                    }
                }
            )
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, '/test-tree', Operation.UPDATE)
    }

    def 'Saving empty list element data fragment.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with an empty list'
            def jsonData = '{"branch": []}'
            objectUnderTest.saveListElements(dataspaceName, anchorName, '/test-tree', jsonData, observedTimestamp)
        then: 'invalid data exception is thrown'
            thrown(DataValidationException)
    }

    def 'Saving list element data fragment with invalid #scenario.'() {
        when: 'save data method is invoked with an invalid #scenario'
            objectUnderTest.saveListElements(dataspaceName, anchorName, '/test-tree', _ as String, observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'add list elements persistence method is not invoked'
            0 * mockCpsDataPersistenceService.addListElements(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Get data node with option #fetchDescendantsOption.'() {
        def xpath = '/xpath'
        def dataNode = new DataNodeBuilder().withXpath(xpath).build()
        given: 'persistence service returns data for get data request'
            mockCpsDataPersistenceService.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption) >> dataNode
        expect: 'service returns same data if uses same parameters'
            objectUnderTest.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption) == dataNode
        where: 'all fetch options are supported'
            fetchDescendantsOption << FetchDescendantsOption.values()
    }

    def 'Get data node with option invalid #scenario.'() {
        when: 'get data node is invoked with #scenario'
            objectUnderTest.getDataNode(dataspaceName, anchorName, '/test-tree', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'get data node persistence service is not invoked'
            0 * mockCpsDataPersistenceService.getDataNode(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Update data node leaves: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'update data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath, jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataLeaves(dataspaceName, anchorName, expectedNodeXpath, leaves)
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, parentNodeXpath, Operation.UPDATE)
        where: 'following parameters were used'
            scenario         | parentNodeXpath | jsonData                        || expectedNodeXpath                   | leaves
            'top level node' | '/'             | '{"test-tree": {"branch": []}}' || '/test-tree'                        | Collections.emptyMap()
            'level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}' || '/test-tree/branch[@name=\'Name\']' | ['name': 'Name']
    }

    def 'Update data node with invalid #scenario.'() {
        when: 'update data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, '/', '{"test-tree": {"branch": []}}', observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsDataPersistenceService.updateDataLeaves(_, _, _, _)
        and: 'data updated event is not sent to notification service'
            0 * mockNotificationService.processDataUpdatedEvent(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Update list-element data node with : #scenario.'() {
        given: 'schema set for given anchor and dataspace references bookstore model'
            setupSchemaSetMocks('bookstore.yang')
        when: 'update data method is invoked with json data #jsonData and parent node xpath'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, '/bookstore/categories[@code=2]',
                jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            thrown(DataValidationException)
        where: 'following parameters were used'
            scenario          | jsonData
            'multiple expectedLeaves' | '{"code": "01","name": "some-name"}'
            'one leaf'        | '{"name": "some-name"}'
    }

    def 'Update Bookstore node leaves' () {
        given: 'a DMI registry model'
            setupSchemaSetMocks('bookstore.yang')
        and: 'the expected json string'
            def jsonData = '{"categories":[{"code":01,"name":"Romance"}]}'
        when: 'update data method is invoked with json data and parent node xpath'
            objectUnderTest.updateNodeLeavesAndExistingDescendantLeaves(dataspaceName, anchorName,
                '/bookstore', jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataLeaves(dataspaceName, anchorName,
                "/bookstore/categories[@code='01']", ['name':'Romance', 'code': '01'])
        and: 'the data updated event is sent to the notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, '/bookstore', Operation.UPDATE)
    }

    def 'Update Bookstore node leaves with invalid #scenario' () {
        when: 'update data method is invoked with an invalid #scenario'
            objectUnderTest.updateNodeLeavesAndExistingDescendantLeaves(dataspaceName, anchorName,
                '/bookstore', _ as String, observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsDataPersistenceService.updateDataLeaves(_, _, _, _)
        and: 'the data updated event is not sent to the notification service'
            0 * mockNotificationService.processDataUpdatedEvent(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }


    def 'Replace data node: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.replaceNodeTree(dataspaceName, anchorName, parentNodeXpath, jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.replaceDataNodeTree(dataspaceName, anchorName,
                { dataNode -> dataNode.xpath == expectedNodeXpath })
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, parentNodeXpath, Operation.UPDATE)
        where: 'following parameters were used'
            scenario         | parentNodeXpath | jsonData                        || expectedNodeXpath
            'top level node' | '/'             | '{"test-tree": {"branch": []}}' || '/test-tree'
            'level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}' || '/test-tree/branch[@name=\'Name\']'
    }

    def 'Replace data node with invalid #scenario.'() {
        when: 'replace data method is invoked with invalid #scenario'
            objectUnderTest.replaceNodeTree(dataspaceName, anchorName, '/', _ as String, observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsDataPersistenceService.replaceDataNodeTree(_, _,_)
        and: 'data updated event is not sent to notification service'
            0 * mockNotificationService.processDataUpdatedEvent(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Replace list content data fragment under parent node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace list data method is invoked with list element json data'
            def jsonData = '{"branch": [{"name": "A"}, {"name": "B"}]}'
            objectUnderTest.replaceListContent(dataspaceName, anchorName, '/test-tree', jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.replaceListContent(dataspaceName, anchorName, '/test-tree',
                { dataNodeCollection ->
                    {
                        assert dataNodeCollection.size() == 2
                        assert dataNodeCollection.collect { it.getXpath() }
                            .containsAll(['/test-tree/branch[@name=\'A\']', '/test-tree/branch[@name=\'B\']'])
                    }
                }
            )
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, '/test-tree', Operation.UPDATE)
    }

    def 'Replace whole list content with empty list element.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace list data method is invoked with empty list'
            def jsonData = '{"branch": []}'
            objectUnderTest.replaceListContent(dataspaceName, anchorName, '/test-tree', jsonData, observedTimestamp)
        then: 'invalid data exception is thrown'
            thrown(DataValidationException)
    }

    def 'Replace whole list content with an invalid #scenario.'() {
        when: 'replace list data method is invoked with invalid #scenario'
            objectUnderTest.replaceListContent(dataspaceName, anchorName, '/test-tree', _ as Collection<DataNode>, observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsDataPersistenceService.replaceListContent(_, _,_)
        and: 'data updated event is not sent to notification service'
            0 * mockNotificationService.processDataUpdatedEvent(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Delete list element under existing node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'delete list data method is invoked with list element json data'
            objectUnderTest.deleteListOrListElement(dataspaceName, anchorName, '/test-tree/branch', observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.deleteListDataNode(dataspaceName, anchorName, '/test-tree/branch')
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, '/test-tree/branch', Operation.DELETE)
    }


    def 'Delete list element with an invalid #scenario.'() {
        when: 'delete list data method is invoked with with invalid #scenario'
            objectUnderTest.deleteDataNode(dataspaceName, anchorName, '/data-node', observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsDataPersistenceService.deleteListDataNode(_, _, _)
        and: 'data updated event is not sent to notification service'
            0 * mockNotificationService.processDataUpdatedEvent(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Delete data node under anchor and dataspace.'() {
        given: 'schema set for given anchor and dataspace references test tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'delete data node method is invoked with correct parameters'
            objectUnderTest.deleteDataNode(dataspaceName, anchorName, '/data-node', observedTimestamp)
        then: 'the persistence service method is invoked with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, '/data-node')
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, '/data-node', Operation.DELETE)
    }

    def 'Delete data node with an invalid #scenario.'() {
        when: 'delete data node method is invoked with invalid #scenario'
            objectUnderTest.deleteDataNode(dataspaceName, anchorName, '/data-node', observedTimestamp)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsDataPersistenceService.deleteDataNode(_, _, _)
        and: 'data updated event is not sent to notification service'
            0 * mockNotificationService.processDataUpdatedEvent(_, _, _, _)
        where: 'the following parameters are used'
            scenario                    | dataspaceName                 | anchorName
            'dataspace name'            | 'dataspace names with spaces' | 'anchorName'
            'anchor name'               | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name' | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Delete all data nodes for a given anchor and dataspace.'() {
        given: 'schema set for given anchor and dataspace references test tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'delete data node method is invoked with correct parameters'
            objectUnderTest.deleteDataNodes(dataspaceName, anchorName, observedTimestamp)
        then: 'the persistence service method is invoked with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName)
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(anchor, observedTimestamp, '/', Operation.DELETE)

    }

    def setupSchemaSetMocks(String... yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }

    def 'start session'() {
        when: 'start session method is called'
            objectUnderTest.startSession()
        then: 'the persistence service method to start session is invoked'
            1 * mockCpsDataPersistenceService.startSession()
    }

    def 'close session'(){
        given: 'session Id from calling the start session method'
            def sessionId = objectUnderTest.startSession()
        when: 'close session method is called'
            objectUnderTest.closeSession(sessionId)
        then: 'the persistence service method to close session is invoked'
            1 * mockCpsDataPersistenceService.closeSession(sessionId)
    }

    def 'lock anchor with no timeout parameter'(){
        when: 'lock anchor method with no timeout parameter with details of anchor entity to lock'
            objectUnderTest.lockAnchor('some-sessionId', 'some-dataspaceName', 'some-anchorName')
        then: 'the persistence service method to lock anchor is invoked with default timeout'
            1 * mockCpsDataPersistenceService.lockAnchor('some-sessionId', 'some-dataspaceName',
                    'some-anchorName', 300L)
    }

    def 'lock anchor with timeout parameter'(){
        when: 'lock anchor method with timeout parameter is called with details of anchor entity to lock'
            objectUnderTest.lockAnchor('some-sessionId', 'some-dataspaceName',
                    'some-anchorName', 250L)
        then: 'the persistence service method to lock anchor is invoked with the given timeout'
            1 * mockCpsDataPersistenceService.lockAnchor('some-sessionId', 'some-dataspaceName',
                    'some-anchorName', 250L)
    }

    def 'Query CM Handles.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
        when: 'query cm handles is invoked'
            objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'associated persistence service method is invoked with correct parameter'
            1 * mockCpsDataPersistenceService.queryCmHandles(cmHandleQueryParameters)
    }
}
