/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  Modifications Copyright (C) 2022-2023 TechMahindra Ltd.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDeltaService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.ConcurrencyException
import org.onap.cps.spi.exceptions.DataNodeNotFoundExceptionBatch
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.SessionManagerException
import org.onap.cps.spi.exceptions.SessionTimeoutException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.spi.utils.CpsValidator
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.YangParser
import org.onap.cps.utils.YangParserHelper
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Shared
import spock.lang.Specification
import java.time.OffsetDateTime

class CpsDataServiceImplSpec extends Specification {
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockCpsValidator = Mock(CpsValidator)
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache)
    def mockCpsDeltaService = Mock(CpsDeltaService);

    def objectUnderTest = new CpsDataServiceImpl(mockCpsDataPersistenceService, mockCpsAnchorService, mockCpsValidator, yangParser, mockCpsDeltaService)

    def setup() {
        mockCpsAnchorService.getAnchor(dataspaceName, anchorName) >> anchor
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_1) >> anchor1
        mockCpsAnchorService.getAnchor(dataspaceName, ANCHOR_NAME_2) >> anchor2
    }

    @Shared
    static def ANCHOR_NAME_1 = 'some-anchor-1'
    @Shared
    static def ANCHOR_NAME_2 = 'some-anchor-2'
    def dataspaceName = 'some-dataspace'
    def anchorName = 'some-anchor'
    def schemaSetName = 'some-schema-set'
    def anchor = Anchor.builder().name(anchorName).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    def anchor1 = Anchor.builder().name(ANCHOR_NAME_1).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    def anchor2 = Anchor.builder().name(ANCHOR_NAME_2).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()
    def observedTimestamp = OffsetDateTime.now()

    def 'Saving #scenario data.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with test-tree #scenario data'
            def data = TestUtils.getResourceFileContent(dataFile)
            objectUnderTest.saveData(dataspaceName, anchorName, data, observedTimestamp, contentType)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.storeDataNodes(dataspaceName, anchorName,
                    { dataNode -> dataNode.xpath[0] == '/test-tree' })
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'given parameters'
            scenario | dataFile         | contentType
            'json'   | 'test-tree.json' | ContentType.JSON
            'xml'    | 'test-tree.xml'  | ContentType.XML
    }

    def 'Saving data with error: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with test-tree json data'
            objectUnderTest.saveData(dataspaceName, anchorName, invalidData, observedTimestamp, contentType)
        then: 'a data validation exception is thrown with the correct message'
            def exceptionThrown  = thrown(DataValidationException)
            assert exceptionThrown.message.startsWith(expectedMessage)
        where: 'given parameters'
            scenario        | invalidData     | contentType      || expectedMessage
            'no data nodes' | '{}'            | ContentType.JSON || 'No data nodes'
            'invalid json'  | '{invalid json' | ContentType.JSON || 'Failed to parse json data'
            'invalid xml'   | '<invalid xml'  | ContentType.XML  || 'Failed to parse xml data'
    }

    def 'Saving list element data fragment under Root node.'() {
        given: 'schema set for given anchor and dataspace references bookstore model'
            setupSchemaSetMocks('bookstore.yang')
        when: 'save data method is invoked with list element json data'
            def jsonData = '{"bookstore-address":[{"bookstore-name":"Easons","address":"Dublin,Ireland","postal-code":"D02HA21"}]}'
            objectUnderTest.saveListElements(dataspaceName, anchorName, '/', jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.storeDataNodes(dataspaceName, anchorName,
                { dataNodeCollection ->
                    {
                        assert dataNodeCollection.size() == 1
                        assert dataNodeCollection.collect { it.getXpath() }
                            .containsAll(['/bookstore-address[@bookstore-name=\'Easons\']'])
                    }
                }
            )
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
    }

    def 'Saving child data fragment under existing node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with test-tree json data'
            def jsonData = '{"branch": [{"name": "New"}]}'
            objectUnderTest.saveData(dataspaceName, anchorName, '/test-tree', jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.addChildDataNodes(dataspaceName, anchorName, '/test-tree',
                { dataNode -> dataNode.xpath[0] == '/test-tree/branch[@name=\'New\']' })
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
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
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
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

    def 'Get all data nodes #scenario.'() {
        given: 'persistence service returns data for GET request'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpath, fetchDescendantsOption) >> dataNode
        expect: 'service returns same data if using same parameters'
            objectUnderTest.getDataNodes(dataspaceName, anchorName, xpath, fetchDescendantsOption) == dataNode
        where: 'following parameters were used'
            scenario                                   | xpath   | fetchDescendantsOption                         |   dataNode
            'with root node xpath and descendants'     | '/'     | FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS | [new DataNodeBuilder().withXpath('/xpath-1').build(), new DataNodeBuilder().withXpath('/xpath-2').build()]
            'with root node xpath and no descendants'  | '/'     | FetchDescendantsOption.OMIT_DESCENDANTS        | [new DataNodeBuilder().withXpath('/xpath-1').build(), new DataNodeBuilder().withXpath('/xpath-2').build()]
            'with valid xpath and descendants'         | '/xpath'| FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS | [new DataNodeBuilder().withXpath('/xpath').build()]
            'with valid xpath and no descendants'      | '/xpath'| FetchDescendantsOption.OMIT_DESCENDANTS        | [new DataNodeBuilder().withXpath('/xpath').build()]
    }

    def 'Get all data nodes over multiple xpaths with option #fetchDescendantsOption.'() {
        def xpath1 = '/xpath-1'
        def xpath2 = '/xpath-2'
        def dataNode = [new DataNodeBuilder().withXpath(xpath1).build(), new DataNodeBuilder().withXpath(xpath2).build()]
        given: 'persistence service returns data for get data request'
            mockCpsDataPersistenceService.getDataNodesForMultipleXpaths(dataspaceName, anchorName, [xpath1, xpath2], fetchDescendantsOption) >> dataNode
        expect: 'service returns same data if uses same parameters'
            objectUnderTest.getDataNodesForMultipleXpaths(dataspaceName, anchorName, [xpath1, xpath2], fetchDescendantsOption) == dataNode
        where: 'all fetch options are supported'
            fetchDescendantsOption << [FetchDescendantsOption.OMIT_DESCENDANTS, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS]
    }

    def 'Get delta between 2 anchors'() {
        given: 'some xpath, source and target data nodes'
            def xpath = '/xpath'
            def sourceDataNodes = [new DataNodeBuilder().withXpath(xpath).build()]
            def targetDataNodes = [new DataNodeBuilder().withXpath(xpath).build()]
        when: 'attempt to get delta between 2 anchors'
            objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, ANCHOR_NAME_1, ANCHOR_NAME_2, xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the dataspace and anchor names are validated'
            2 * mockCpsValidator.validateNameCharacters(_)
        and: 'data nodes are fetched using appropriate persistence layer method'
            mockCpsDataPersistenceService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_1, [xpath], FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
            mockCpsDataPersistenceService.getDataNodesForMultipleXpaths(dataspaceName, ANCHOR_NAME_2, [xpath], FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> targetDataNodes
        and: 'appropriate delta service method is invoked once with correct source and target data nodes'
            1 * mockCpsDeltaService.getDeltaReports(sourceDataNodes, targetDataNodes)
    }

    def 'Update data node leaves: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'update data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath, jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.batchUpdateDataLeaves(dataspaceName, anchorName, {dataNode -> dataNode.keySet()[0] == expectedNodeXpath})
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'following parameters were used'
            scenario         | parentNodeXpath | jsonData                        || expectedNodeXpath
            'top level node' | '/'             | '{"test-tree": {"branch": []}}' || '/test-tree'
            'level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}' || '/test-tree/branch[@name=\'Name\']'
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
            scenario                  | jsonData
            'multiple expectedLeaves' | '{"code": "01","name": "some-name"}'
            'one leaf'                | '{"name": "some-name"}'
    }

    def 'Update data nodes in different containers.' () {
        given: 'schema set for given dataspace and anchor refers multipleDataTree model'
            setupSchemaSetMocks('multipleDataTree.yang')
        and: 'json string with multiple data trees'
            def parentNodeXpath = '/'
            def updatedJsonData = '{"first-container":{"a-leaf":"a-new-Value"},"last-container":{"x-leaf":"x-new-value"}}'
        when: 'update operation is performed on multiple data nodes'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath, updatedJsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.batchUpdateDataLeaves(dataspaceName, anchorName, {dataNode -> dataNode.keySet()[index] == expectedNodeXpath})
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'the following parameters were used'
            index | expectedNodeXpath
            0     | '/first-container'
            1     | '/last-container'
    }

    def 'Update Bookstore node leaves and child.' () {
        given: 'a DMI registry model'
            setupSchemaSetMocks('bookstore.yang')
        and: 'json update for a category (parent) and new book (child)'
            def jsonData = '{"categories":[{"code":01,"name":"Romance","books": [{"title": "new"}]}]}'
        when: 'update data method is invoked with json data and parent node xpath'
            objectUnderTest.updateNodeLeavesAndExistingDescendantLeaves(dataspaceName, anchorName, '/bookstore', jsonData, observedTimestamp)
        then: 'the persistence service method is invoked for the category (parent)'
            1 * mockCpsDataPersistenceService.batchUpdateDataLeaves(dataspaceName, anchorName,
                    {updatedDataNodesPerXPath -> updatedDataNodesPerXPath.keySet()
                                                .iterator().next() == "/bookstore/categories[@code='01']"})
        and: 'the persistence service method is invoked for the new book (child)'
            1 * mockCpsDataPersistenceService.batchUpdateDataLeaves(dataspaceName, anchorName,
                {updatedDataNodesPerXPath -> updatedDataNodesPerXPath.keySet()
                    .iterator().next() == "/bookstore/categories[@code='01']/books[@title='new']"})
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
    }

    def 'Replace data node using singular data node: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateDataNodeAndDescendants(dataspaceName, anchorName, parentNodeXpath, jsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName,
                    { dataNode -> dataNode.xpath == expectedNodeXpath})
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'following parameters were used'
            scenario         | parentNodeXpath | jsonData                                           || expectedNodeXpath
            'top level node' | '/'             | '{"test-tree": {"branch": []}}'                    || ['/test-tree']
            'level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}'                    || ['/test-tree/branch[@name=\'Name\']']
            'json list'      | '/test-tree'    | '{"branch": [{"name":"Name1"}, {"name":"Name2"}]}' || ["/test-tree/branch[@name='Name1']", "/test-tree/branch[@name='Name2']"]
    }

    def 'Replace data node using multiple data nodes: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace data method is invoked with a map of xpaths and json data'
            objectUnderTest.updateDataNodesAndDescendants(dataspaceName, anchorName, nodesJsonData, observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName,
                { dataNode -> dataNode.xpath == expectedNodeXpath})
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'following parameters were used'
            scenario         | nodesJsonData                                                                                                        || expectedNodeXpath
            'top level node' | ['/' : '{"test-tree": {"branch": []}}', '/test-tree' : '{"branch": [{"name":"Name"}]}']                              || ["/test-tree", "/test-tree/branch[@name='Name']"]
            'level 2 node'   | ['/test-tree' : '{"branch": [{"name":"Name"}]}', '/test-tree/branch[@name=\'Name\']':'{"nest":{"name":"nestName"}}'] || ["/test-tree/branch[@name='Name']", "/test-tree/branch[@name='Name']/nest"]
            'json list'      | ['/test-tree' : '{"branch": [{"name":"Name1"}, {"name":"Name2"}]}']                                                  || ["/test-tree/branch[@name='Name1']", "/test-tree/branch[@name='Name2']"]
    }

    def 'Replace data node with concurrency exception in persistence layer.'() {
        given: 'the persistence layer throws an concurrency exception'
            def originalException = new ConcurrencyException('message', 'details')
            mockCpsDataPersistenceService.updateDataNodesAndDescendants(*_) >> { throw originalException }
            setupSchemaSetMocks('test-tree.yang')
        when: 'attempt to replace data node'
            objectUnderTest.updateDataNodesAndDescendants(dataspaceName, anchorName, ['/' : '{"test-tree": {}}'] , observedTimestamp)
        then: 'the same exception is thrown up'
            def thrownUp = thrown(ConcurrencyException)
            assert thrownUp == originalException
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
        and: 'the CpsValidator is called on the dataspaceName and AnchorName twice'
            2 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
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
        when: 'delete list data method is invoked with list element json data'
            objectUnderTest.deleteListOrListElement(dataspaceName, anchorName, '/test-tree/branch', observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.deleteListDataNode(dataspaceName, anchorName, '/test-tree/branch')
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
    }

    def 'Delete multiple list elements under existing node.'() {
        when: 'delete multiple list data method is invoked with list element json data'
            objectUnderTest.deleteDataNodes(dataspaceName, anchorName, ['/test-tree/branch[@name="A"]', '/test-tree/branch[@name="B"]'], observedTimestamp)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName, ['/test-tree/branch[@name="A"]', '/test-tree/branch[@name="B"]'])
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
    }

    def 'Delete data node under anchor and dataspace.'() {
        when: 'delete data node method is invoked with correct parameters'
            objectUnderTest.deleteDataNode(dataspaceName, anchorName, '/data-node', observedTimestamp)
        then: 'the persistence service method is invoked with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, '/data-node')
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
    }

    def 'Delete all data nodes for a given anchor and dataspace.'() {
        when: 'delete data nodes method is invoked with correct parameters'
            objectUnderTest.deleteDataNodes(dataspaceName, anchorName, observedTimestamp)
        then: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        and: 'the persistence service method is invoked with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName)
    }

    def 'Delete all data nodes for a given anchor and dataspace with batch exception in persistence layer.'() {
        given: 'a batch exception in persistence layer'
            def originalException = new DataNodeNotFoundExceptionBatch('ds1','a1',[])
            mockCpsDataPersistenceService.deleteDataNodes(*_)  >> { throw originalException }
        when: 'attempt to delete data nodes'
            objectUnderTest.deleteDataNodes(dataspaceName, anchorName, observedTimestamp)
        then: 'the original exception is thrown up'
            def thrownUp = thrown(DataNodeNotFoundExceptionBatch)
            assert thrownUp == originalException
        and: 'the exception details contain the expected data'
            assert thrownUp.details.contains('ds1')
            assert thrownUp.details.contains('a1')
    }

    def 'Delete all data nodes for given dataspace and multiple anchors.'() {
        given: 'schema set for given anchors and dataspace references test tree model'
            setupSchemaSetMocks('test-tree.yang')
            mockCpsAnchorService.getAnchors(dataspaceName, ['anchor1', 'anchor2']) >>
                [new Anchor(name: 'anchor1', dataspaceName: dataspaceName),
                 new Anchor(name: 'anchor2', dataspaceName: dataspaceName)]
        when: 'delete data node method is invoked with correct parameters'
            objectUnderTest.deleteDataNodes(dataspaceName, ['anchor1', 'anchor2'], observedTimestamp)
        then: 'the CpsValidator is called on the dataspace name and the anchor names'
            2 * mockCpsValidator.validateNameCharacters(_)
        and: 'the persistence service method is invoked with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNodes(dataspaceName, _ as Collection<String>)
    }

    def 'Start session.'() {
        when: 'start session method is called'
            objectUnderTest.startSession()
        then: 'the persistence service method to start session is invoked'
            1 * mockCpsDataPersistenceService.startSession()
    }

    def 'Start session with Session Manager Exceptions.'() {
        given: 'the persistence layer throws an Session Manager Exception'
            mockCpsDataPersistenceService.startSession() >> { throw originalException }
        when: 'attempt to start session'
            objectUnderTest.startSession()
        then: 'the original exception is thrown up'
            def thrownUp = thrown(SessionManagerException)
            assert thrownUp == originalException
        where: 'variations of Session Manager Exception are used'
            originalException << [ new SessionManagerException('message','details'),
                                   new SessionManagerException('message','details', new Exception('cause')),
                                   new SessionTimeoutException('message','details', new Exception('cause'))]
    }

    def 'Close session.'(){
        given: 'session Id from calling the start session method'
            def sessionId = objectUnderTest.startSession()
        when: 'close session method is called'
            objectUnderTest.closeSession(sessionId)
        then: 'the persistence service method to close session is invoked'
            1 * mockCpsDataPersistenceService.closeSession(sessionId)
    }

    def 'Lock anchor with no timeout parameter.'(){
        when: 'lock anchor method with no timeout parameter with details of anchor entity to lock'
            objectUnderTest.lockAnchor('some-sessionId', 'some-dataspaceName', 'some-anchorName')
        then: 'the persistence service method to lock anchor is invoked with default timeout'
            1 * mockCpsDataPersistenceService.lockAnchor('some-sessionId', 'some-dataspaceName', 'some-anchorName', 300L)
    }

    def 'Lock anchor with timeout parameter.'(){
        when: 'lock anchor method with timeout parameter is called with details of anchor entity to lock'
            objectUnderTest.lockAnchor('some-sessionId', 'some-dataspaceName', 'some-anchorName', 250L)
        then: 'the persistence service method to lock anchor is invoked with the given timeout'
            1 * mockCpsDataPersistenceService.lockAnchor('some-sessionId', 'some-dataspaceName', 'some-anchorName', 250L)
    }

    def setupSchemaSetMocks(String... yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }

}
