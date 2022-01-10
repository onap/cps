/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
import org.onap.cps.api.CpsModuleService
import org.onap.cps.notification.NotificationService
import org.onap.cps.notification.Operation
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.Anchor
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

    def objectUnderTest = new CpsDataServiceImpl()

    def setup() {
        objectUnderTest.cpsDataPersistenceService = mockCpsDataPersistenceService
        objectUnderTest.cpsAdminService = mockCpsAdminService
        objectUnderTest.yangTextSchemaSourceSetCache = mockYangTextSchemaSourceSetCache
        objectUnderTest.notificationService = mockNotificationService
    }

    def dataspaceName = 'some dataspace'
    def anchorName = 'some anchor'
    def schemaSetName = 'some schema set'
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
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, '/', Operation.CREATE)
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
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, '/test-tree', Operation.CREATE)
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
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, '/test-tree', Operation.UPDATE)
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

    def 'Update data node leaves: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'update data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath, jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataLeaves(dataspaceName, anchorName, expectedNodeXpath, leaves)
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, parentNodeXpath, Operation.UPDATE)
        where: 'following parameters were used'
            scenario         | parentNodeXpath | jsonData                        || expectedNodeXpath                   | leaves
            'top level node' | '/'             | '{"test-tree": {"branch": []}}' || '/test-tree'                        | Collections.emptyMap()
            'level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}' || '/test-tree/branch[@name=\'Name\']' | ['name': 'Name']
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

    def 'Update cm-handle properties' () {
        given: 'a dmi registry model'
            setupSchemaSetMocks('dmi-registry.yang')
        and: 'the expected json string'
            def jsonData = '{"cm-handles":[{"id":"cmHandle001", "additional-properties":[{"name":"P1"}]}]}'
        when: 'update data method is invoked with json data and parent node xpath'
            objectUnderTest.updateNodeLeavesAndExistingDescendantLeaves(dataspaceName, anchorName,
                '/dmi-registry', jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataLeaves(dataspaceName, anchorName,
                "/dmi-registry/cm-handles[@id='cmHandle001']", ['id': 'cmHandle001'])
        and: 'the data updated event is sent to the notification service'
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, '/dmi-registry', Operation.UPDATE)
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
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, parentNodeXpath, Operation.UPDATE)
        where: 'following parameters were used'
            scenario         | parentNodeXpath | jsonData                        || expectedNodeXpath
            'top level node' | '/'             | '{"test-tree": {"branch": []}}' || '/test-tree'
            'level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}' || '/test-tree/branch[@name=\'Name\']'
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
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, '/test-tree', Operation.UPDATE)
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

    def 'Delete list element under existing node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'delete list data method is invoked with list element json data'
            objectUnderTest.deleteListOrListElement(dataspaceName, anchorName, '/test-tree/branch', observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.deleteListDataNode(dataspaceName, anchorName, '/test-tree/branch')
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, '/test-tree/branch', Operation.DELETE)
    }

    def 'Delete data node under anchor and dataspace.'() {
        given: 'schema set for given anchor and dataspace references test tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'delete data node method is invoked with correct parameters'
            objectUnderTest.deleteDataNode(dataspaceName, anchorName, '/data-node', observedTimestamp)
        then: 'the persistence service method is invoked with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, '/data-node')
        and: 'data updated event is sent to notification service'
            1 * mockNotificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, '/data-node', Operation.DELETE)
    }

    def setupSchemaSetMocks(String... yangResources) {
        def anchor = Anchor.builder().name(anchorName).schemaSetName(schemaSetName).build()
        mockCpsAdminService.getAnchor(dataspaceName, anchorName) >> anchor
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }
}
