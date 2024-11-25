/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  Modifications Copyright (C) 2022-2024 TechMahindra Ltd.
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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDeltaService
import org.onap.cps.events.CpsDataUpdateEventsService
import org.onap.cps.impl.utils.CpsValidator
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.ConcurrencyException
import org.onap.cps.spi.exceptions.DataNodeNotFoundExceptionBatch
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.SessionManagerException
import org.onap.cps.spi.exceptions.SessionTimeoutException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.ContentType
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

import static org.onap.cps.events.model.Data.Operation.DELETE

class CpsDataServiceImplSpec extends Specification {
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockCpsValidator = Mock(CpsValidator)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)
    def mockCpsDeltaService = Mock(CpsDeltaService);
    def mockDataUpdateEventsService = Mock(CpsDataUpdateEventsService)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockPrefixResolver = Mock(PrefixResolver)

    def objectUnderTest = new CpsDataServiceImpl(mockCpsDataPersistenceService, mockDataUpdateEventsService, mockCpsAnchorService,
            mockCpsValidator, yangParser, mockCpsDeltaService, jsonObjectMapper, mockPrefixResolver)

    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender
    def applicationContext = new AnnotationConfigApplicationContext()

    def setup() {
        mockCpsAnchorService.getAnchor(dataspaceName, anchorName) >> anchor
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
            'invalid json'  | '{invalid json' | ContentType.JSON || 'Data Validation Failed'
            'invalid xml'   | '<invalid xml'  | ContentType.XML  || 'Data Validation Failed'
    }

    def 'Saving list element data fragment under Root node.'() {
        given: 'schema set for given anchor and dataspace references bookstore model'
            setupSchemaSetMocks('bookstore.yang')
        when: 'save data method is invoked with list element json data'
            def jsonData = '{"bookstore-address":[{"bookstore-name":"Easons","address":"Dublin,Ireland","postal-code":"D02HA21"}]}'
            objectUnderTest.saveListElements(dataspaceName, anchorName, '/', jsonData, observedTimestamp, ContentType.JSON)
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

    def 'Saving list element data fragment under existing JSON/XML node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with list element data'
            objectUnderTest.saveListElements(dataspaceName, anchorName, '/test-tree', data, observedTimestamp, contentType)
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
        where:
            data                                                                                                                        | contentType
            '{"branch": [{"name": "A"}, {"name": "B"}]}'                                                                                | ContentType.JSON
            '<test-tree xmlns="org:onap:cps:test:test-tree"><branch><name>A</name></branch><branch><name>B</name></branch></test-tree>' | ContentType.XML
    }

    def 'Saving empty list element data fragment for JSON/XML data.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'save data method is invoked with an empty list'
            objectUnderTest.saveListElements(dataspaceName, anchorName, '/test-tree', data, observedTimestamp, contentType)
        then: 'invalid data exception is thrown'
            thrown(DataValidationException)
        where:
            data                                       | contentType
            '{"branch": []}'                           | ContentType.JSON
            '<test-tree><branch></branch></test-tree>' | ContentType.XML
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

    def 'Get delta between anchor and payload with user provided schema #scenario'() {
        given: 'user provided schema set '
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            setupSchemaSetMocksForDelta(yangResourcesNameToContentMap)
        when: 'attempt to get delta between an anchor and a JSON payload'
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, yangResourcesNameToContentMap, jsonData, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'dataspacename and anchor names are validated'
            1 * mockCpsValidator.validateNameCharacters(['some-dataspace', 'some-anchor'])
        and: 'source data nodes are fetched using appropriate persistence layer method'
            1 * mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'appropriate delta service method is invoked once with correct source and target data nodes'
            1 * mockCpsDeltaService.getDeltaReports({sourceDataNodesRebuilt -> sourceDataNodesRebuilt.xpath[0] == expectedNodeXpath}, {targetDataNodes -> targetDataNodes.xpath[0] == expectedNodeXpath})
        where: 'following data was used'
            scenario             | xpath                               | sourceDataNodes                                                                                          | jsonData                                       || expectedNodeXpath
            'root node xpath'    | '/'                                 | [new DataNodeBuilder().withXpath('/bookstore').build()]                                                  | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
            'parent xpath'       | '/bookstore'                        | [new DataNodeBuilder().withXpath('/bookstore').build()]                                                  | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
            'non-root xpath'     | '/bookstore/categories[@code="02"]' | [new DataNodeBuilder().withXpath('/bookstore/categories[@code="02"]').withLeaves(["code":"02"]).build()] | '{"categories":[{"name":"kids","code":"02"}]}' || '/bookstore/categories[@code=\'02\']'
    }

    def 'Get delta between anchor and payload by using schema from anchor #scenario'() {
        given: 'schema set for a given dataspace and anchor'
            setupSchemaSetMocks("bookstore.yang")
        when: 'attempt to get delta between an anchor and a JSON payload'
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, [:], jsonData, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'dataspacename and anchor names are validated'
            1 * mockCpsValidator.validateNameCharacters(['some-dataspace', 'some-anchor'])
        and: 'source data nodes are fetched using appropriate persistence layer method'
            1 * mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> sourceDataNodes
        and: 'appropriate delta service method is invoked once with correct source and target data nodes'
            1 * mockCpsDeltaService.getDeltaReports({sourceDataNodesRebuilt -> sourceDataNodesRebuilt.xpath[0] == expectedNodeXpath}, {targetDataNodes -> targetDataNodes.xpath[0] == expectedNodeXpath})
        where: 'following data was used'
            scenario          | xpath                               | sourceDataNodes                                                                                          | jsonData                                       || expectedNodeXpath
            'root node xpath' | '/'                                 | [new DataNodeBuilder().withXpath('/bookstore').build()]                                                  | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
            'parent xpath'    | '/bookstore'                        | [new DataNodeBuilder().withXpath('/bookstore').build()]                                                  | '{"bookstore":{"bookstore-name":"Easons"}}'    || '/bookstore'
            'non-root xpath'  | '/bookstore/categories[@code="02"]' | [new DataNodeBuilder().withXpath('/bookstore/categories[@code="02"]').withLeaves(["code":"02"]).build()] | '{"categories":[{"name":"kids","code":"02"}]}' || '/bookstore/categories[@code=\'02\']'
    }

    def 'Delta between anchor and payload error scenario #scenario'() {
        given: 'schema set for given anchor and dataspace references bookstore model'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            setupSchemaSetMocksForDelta(yangResourcesNameToContentMap)
        when: 'attempt to get delta between anchor and payload'
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, yangResourcesNameToContentMap, jsonData, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'expected exception is thrown'
            thrown(DataValidationException)
        where: 'following parameters were used'
            scenario                                   | xpath                               | jsonData
            'invalid json data with root node xpath'   | '/'                                 | '{"some-key": "some-value"'
            'empty json data with root node xpath'     | '/'                                 | '{}'
            'invalid json data with parent node xpath' | '/bookstore'                        | '{"some-key": "some-value"'
            'empty json data with parent node xpath'   | '/bookstore'                        | '{}'
            'empty json data with xpath'               | "/bookstore/categories[@code='02']" | '{}'
    }

    def 'Update data node leaves: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'update data method is invoked with node data #nodeData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath, nodeData, observedTimestamp, contentType)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.batchUpdateDataLeaves(dataspaceName, anchorName, {dataNode -> dataNode.keySet()[0] == expectedNodeXpath})
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'following parameters were used'
            scenario                       | parentNodeXpath | nodeData                             || expectedNodeXpath                   | contentType
            'JSON content: top level node' | '/'             | '{"test-tree": {"branch": []}}'      || '/test-tree'                        | ContentType.JSON
            'JSON content: level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}'      || '/test-tree/branch[@name=\'Name\']' | ContentType.JSON
            'XML  content: level 2 node'   | '/test-tree'    | '<branch><name>Name</name></branch>' || '/test-tree/branch[@name=\'Name\']' | ContentType.XML
    }

    def 'Update list-element data node with : #scenario.'() {
        given: 'schema set for given anchor and dataspace references bookstore model'
            setupSchemaSetMocks('bookstore.yang')
        when: 'update data method is invoked with node data #nodeData and parent node xpath'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, '/bookstore/categories[@code=2]',
                nodeData, observedTimestamp, contentType)
        then: 'the persistence service method is invoked with correct parameters'
            thrown(DataValidationException)
        where: 'following parameters were used'
            scenario                                || nodeData                               | contentType
            'JSON content: multiple expectedLeaves' || '{"code": "03","name": "some-name"}'   | ContentType.JSON
            'JSON content: one leaf'                || '{"name": "some-name"}'                | ContentType.JSON
            'XML content: multiple expectedLeaves'  || '<code>1</code><name>some-name</name>' | ContentType.XML
    }

    def 'Update data nodes in different containers.' () {
        given: 'schema set for given dataspace and anchor refers multipleDataTree model'
            setupSchemaSetMocks('multipleDataTree.yang')
        and: 'json string with multiple data trees'
            def parentNodeXpath = '/'
            def updatedJsonData = '{"first-container":{"a-leaf":"a-new-Value"},"last-container":{"x-leaf":"x-new-value"}}'
        when: 'update operation is performed on multiple data nodes'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath, updatedJsonData, observedTimestamp, ContentType.JSON)
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

    def 'Replace data node using singular JSON data node: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateDataNodeAndDescendants(dataspaceName, anchorName, parentNodeXpath, jsonData, observedTimestamp, ContentType.JSON)
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

    def 'Replace data node using singular XML data node: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace data method is invoked with XML data #xmlData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateDataNodeAndDescendants(dataspaceName, anchorName, parentNodeXpath, xmlData, observedTimestamp, ContentType.XML)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName,
                { dataNode -> dataNode.xpath == expectedNodeXpath })
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'following parameters were used'
            scenario       | parentNodeXpath | xmlData                                                                                                                                  || expectedNodeXpath
            'level 2 node' | '/test-tree'    | '<branch><name>Name</name></branch>'                                                                                                     || ['/test-tree/branch[@name=\'Name\']']
            'xml list'     | '/test-tree'    | '<test-tree xmlns="org:onap:cps:test:test-tree"><branch><name>Name1</name></branch>' + '<branch><name>Name2</name></branch></test-tree>' || ["/test-tree/branch[@name='Name1']", "/test-tree/branch[@name='Name2']"]
    }

    def 'Replace data node using multiple JSON data nodes: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace data method is invoked with a map of xpaths and json data'
            objectUnderTest.updateDataNodesAndDescendants(dataspaceName, anchorName, nodeDataPerXPath, observedTimestamp, ContentType.JSON)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName,
                { dataNode -> dataNode.xpath == expectedNodeXpath})
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'following parameters were used'
            scenario         | nodeDataPerXPath                                                                                                     || expectedNodeXpath
            'top level node' | ['/' : '{"test-tree": {"branch": []}}', '/test-tree' : '{"branch": [{"name":"Name"}]}']                              || ["/test-tree", "/test-tree/branch[@name='Name']"]
            'level 2 node'   | ['/test-tree' : '{"branch": [{"name":"Name"}]}', '/test-tree/branch[@name=\'Name\']':'{"nest":{"name":"nestName"}}'] || ["/test-tree/branch[@name='Name']", "/test-tree/branch[@name='Name']/nest"]
            'json list'      | ['/test-tree' : '{"branch": [{"name":"Name1"}, {"name":"Name2"}]}']                                                  || ["/test-tree/branch[@name='Name1']", "/test-tree/branch[@name='Name2']"]
    }

    def 'Replace data node using multiple XML data nodes: #scenario.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace data method is invoked with a map of xpaths and XML data'
            objectUnderTest.updateDataNodesAndDescendants(dataspaceName, anchorName, nodeXmlDataPerXPath, observedTimestamp, ContentType.XML)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName,
                { dataNode -> dataNode.xpath == expectedNodeXpath })
        and: 'the CpsValidator is called on the dataspaceName and AnchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'following parameters were used'
            scenario         | nodeXmlDataPerXPath                                                                                                                                      || expectedNodeXpath
            'top level node' | ['/test-tree': '<branch><name>Name</name></branch>']                                                                                                     || ["/test-tree/branch[@name='Name']"]
            'level 2 node'   | ['/test-tree': '<branch><name>Name</name></branch>', '/test-tree/branch[@name=\'Name\']': '<nest><name>nestName</name></nest>']                          || ["/test-tree/branch[@name='Name']", "/test-tree/branch[@name='Name']/nest"]
            'xml list'       | ['/test-tree': '<test-tree xmlns="org:onap:cps:test:test-tree"><branch><name>Name1</name></branch>' + '<branch><name>Name2</name></branch></test-tree>'] || ["/test-tree/branch[@name='Name1']", "/test-tree/branch[@name='Name2']"]
    }

    def 'Replace data node with concurrency exception in persistence layer.'() {
        given: 'the persistence layer throws an concurrency exception'
            def originalException = new ConcurrencyException('message', 'details')
            mockCpsDataPersistenceService.updateDataNodesAndDescendants(*_) >> { throw originalException }
            setupSchemaSetMocks('test-tree.yang')
        when: 'attempt to replace data node'
            objectUnderTest.updateDataNodesAndDescendants(dataspaceName, anchorName, ['/' : '{"test-tree": {}}'] , observedTimestamp, ContentType.JSON)
        then: 'the same exception is thrown up'
            def thrownUp = thrown(ConcurrencyException)
            assert thrownUp == originalException
    }

    def 'Replace list content data fragment JSON  under parent node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace list data method is invoked with list element json data'
            def jsonData = '{"branch": [{"name": "A"}, {"name": "B"}]}'
            objectUnderTest.replaceListContent(dataspaceName, anchorName, '/test-tree', jsonData, observedTimestamp, ContentType.JSON)
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

    def 'Replace list content data fragment XML under parent node.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace list data method is invoked with list element xml data'
            def nodeData = '<branch><name>A</name></branch>'
            objectUnderTest.replaceListContent(dataspaceName, anchorName, '/test-tree', nodeData, observedTimestamp, ContentType.XML)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.replaceListContent(dataspaceName, anchorName, '/test-tree',
                { dataNodeCollection ->
                    {
                        assert dataNodeCollection.size() == 1
                        assert dataNodeCollection.collect { it.getXpath() }
                            .containsAll(['/test-tree/branch[@name=\'A\']'])
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
            objectUnderTest.replaceListContent(dataspaceName, anchorName, '/test-tree', jsonData, observedTimestamp, ContentType.JSON)
        then: 'invalid data exception is thrown'
            thrown(DataValidationException)
    }

    def 'Replace whole list content XML with empty list element.'() {
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'replace list data method is invoked with xml empty list'
            def nodeData = '[]'
            objectUnderTest.replaceListContent(dataspaceName, anchorName, '/test-tree', nodeData, observedTimestamp, ContentType.XML)
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
            def anchor1 = new Anchor(name: 'anchor1', dataspaceName: dataspaceName)
            def anchor2 = new Anchor(name: 'anchor2', dataspaceName: dataspaceName)
            mockCpsAnchorService.getAnchors(dataspaceName, ['anchor1', 'anchor2']) >> [anchor1, anchor2]
        when: 'delete data node method is invoked with correct parameters'
            objectUnderTest.deleteDataNodes(dataspaceName, ['anchor1', 'anchor2'], observedTimestamp)
        then: 'the CpsValidator is called on the dataspace name and the anchor names'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName)
            1 * mockCpsValidator.validateNameCharacters(['anchor1', 'anchor2'])
        and: 'the persistence service method is invoked with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNodes(dataspaceName, _ as Collection<String>)
        and: 'a data update event is sent for each anchor'
            1 * mockDataUpdateEventsService.publishCpsDataUpdateEvent(anchor1, '/', DELETE, observedTimestamp)
            1 * mockDataUpdateEventsService.publishCpsDataUpdateEvent(anchor2, '/', DELETE, observedTimestamp)
    }

    def "Validating #scenario when dry run is enabled."() {
        given: 'schema set for given anchors and dataspace references bookstore model'
            setupSchemaSetMocks('bookstore.yang')
        when: 'validating the data with the given parameters'
            objectUnderTest.validateData(dataspaceName, anchorName, parentNodeXpath, data,contentType)
        then: 'the appropriate yang parser method is invoked with correct parameters'
            yangParser.validateData(contentType, data, anchor, xpath)
        where: 'the following parameters were used'
            scenario                     | parentNodeXpath | xpath        | contentType      | data
        'JSON data with root node xpath' | '/'             | ''           | ContentType.JSON | '{"bookstore":{"bookstore-name":"Easons"}}'
        'JSON data with specific xpath'  | '/bookstore'    | '/bookstore' | ContentType.JSON | '{"bookstore-name":"Easons"}'
        'XML data with specific xpath'   | '/bookstore'    | '/bookstore' | ContentType.XML  | '<bookstore-name>Easons</bookstore-name>'
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

    def 'Exception is thrown while publishing the notification.'(){
        given: 'schema set for given anchor and dataspace references test-tree model'
            setupSchemaSetMocks('test-tree.yang')
        when: 'publisher set to throw an exception'
            mockDataUpdateEventsService.publishCpsDataUpdateEvent(_, _, _, _) >> { throw new Exception("publishing failed")}
        and: 'an update event is performed'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, '/', '{"test-tree": {"branch": []}}', observedTimestamp, ContentType.JSON)
        then: 'the exception is not bubbled up'
            noExceptionThrown()
        and: "the exception message is logged"
            def logs = loggingListAppender.list.toString()
            assert logs.contains('Failed to send message to notification service')
    }
    def setupSchemaSetMocks(String... yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }

    def setupSchemaSetMocksForDelta(Map<String, String> yangResourcesNameToContentMap) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockTimedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourcesNameToContentMap) >> mockYangTextSchemaSourceSet
        mockYangTextSchemaSourceSetCache.get(_, _) >> mockYangTextSchemaSourceSet
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }

}
