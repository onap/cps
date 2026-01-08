/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2024 Deutsche Telekom AG
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

package org.onap.cps.ncmp.impl.inventory

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.model.ModuleDefinition
import org.onap.cps.api.model.ModuleReference
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.models.CmHandleMigrationDetail
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.CpsValidator
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NO_TIMESTAMP

class InventoryPersistenceImplSpec extends Specification {

    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    def mockCpsDataService = Mock(CpsDataService)

    def mockCpsModuleService = Mock(CpsModuleService)

    def mockCpsAnchorService = Mock(CpsAnchorService)

    def mockCpsValidator = Mock(CpsValidator)

    def mockCmHandleIdPerAlternateId = Mock(IMap)

    def objectUnderTest = Spy(new InventoryPersistenceImpl(mockCpsValidator, spiedJsonObjectMapper, mockCpsAnchorService, mockCpsModuleService, mockCpsDataService, mockCmHandleIdPerAlternateId))

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    def cmHandleId = 'ch-1'
    def updates = [
            new CmHandleMigrationDetail("ch-1", "READY", "some-dmi-properties"),
            new CmHandleMigrationDetail("ch-2", "DELETING", "some-dmi-properties")
    ]
    def alternateId = 'some-alternate-id'
    def leaves = ["id":cmHandleId, "alternateId":alternateId,"dmi-service-name":"common service name","dmi-data-service-name":"data service name","dmi-model-service-name":"model service name"]
    def xpath = "/dmi-registry/cm-handles[@id='ch-1']"

    def cmHandleId2 = 'another-cm-handle'
    def xpath2 = "/dmi-registry/cm-handles[@id='another-cm-handle']"

    def dataNode = new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/additional-properties[@name='myAdditionalProperty']", leaves: leaves)

    @Shared
    def childDataNodesForCmHandleWithAllProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/additional-properties[@name='myAdditionalProperty']", leaves: ["name":"myAdditionalProperty", "value":"myAdditionalValue"]),
                                                      new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/public-properties[@name='myPublicProperty']", leaves: ["name":"myPublicProperty","value":"myPublicValue"])]

    @Shared
    def childDataNodesForCmHandleWithAdditionalProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='ch-1']/additional-properties[@name='myAdditionalProperty']", leaves: ["name":"myAdditionalProperty", "value":"myAdditionalValue"])]

    @Shared
    def childDataNodesForCmHandleWithPublicProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='ch-1']/public-properties[@name='myPublicProperty']", leaves: ["name":"myPublicProperty","value":"myPublicValue"])]

    @Shared
    def childDataNodesForCmHandleWithState = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='ch-1']/state", leaves: ['cm-handle-state': 'ADVISED'])]

    def 'Retrieve CmHandle using datanode with #scenario.'() {
        given: 'the cps data service returns a data node from the DMI registry'
            def dataNode = new DataNode(childDataNodes:childDataNodes, leaves: leaves)
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, xpath, INCLUDE_ALL_DESCENDANTS) >> [dataNode]
        when: 'retrieving the yang modelled cm handle'
            def result = objectUnderTest.getYangModelCmHandle(cmHandleId)
        then: 'the result has the correct id and service names'
            result.id == cmHandleId
            result.dmiServiceName == 'common service name'
            result.dmiDataServiceName == 'data service name'
            result.dmiModelServiceName == 'model service name'
        and: 'the expected additional properties'
            result.additionalProperties.name == expectedAdditionalProperties
        and: 'the expected public properties'
            result.publicProperties.name == expectedPublicProperties
        and: 'the state details are returned'
            result.compositeState.cmHandleState == expectedCompositeState
        and: 'the CM Handle ID is validated'
            1 * mockCpsValidator.validateNameCharacters(cmHandleId)
        where: 'the following parameters are used'
            scenario                           | childDataNodes                                    || expectedAdditionalProperties || expectedPublicProperties || expectedCompositeState
            'no properties'                    | []                                                || []                           || []                       || null
            'additional and public properties' | childDataNodesForCmHandleWithAllProperties        || ["myAdditionalProperty"]     || ["myPublicProperty"]     || null
            'just additional properties'       | childDataNodesForCmHandleWithAdditionalProperties || ["myAdditionalProperty"]     || []                       || null
            'just public properties'           | childDataNodesForCmHandleWithPublicProperties     || []                           || ["myPublicProperty"]     || null
            'with state details'               | childDataNodesForCmHandleWithState                || []                           || []                       || CmHandleState.ADVISED
    }

    def 'Handling missing service names as null.'() {
        given: 'the cps data service returns a data node from the DMI registry with empty child and leaf attributes'
            def dataNode = new DataNode(childDataNodes:[], leaves: ['id':cmHandleId])
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, xpath, INCLUDE_ALL_DESCENDANTS) >> [dataNode]
        when: 'retrieving the yang modelled cm handle'
            def result = objectUnderTest.getYangModelCmHandle(cmHandleId)
        then: 'the service names are returned as null'
            result.dmiServiceName == null
            result.dmiDataServiceName == null
            result.dmiModelServiceName == null
        and: 'the CM Handle ID is validated'
            1 * mockCpsValidator.validateNameCharacters(cmHandleId)
    }

    def 'Retrieve multiple YangModelCmHandles using cm handle ids.'() {
        given: 'the cps data service returns 2 data nodes from the DMI registry'
            def dataNodes = [new DataNode(xpath: xpath, leaves: ['id': cmHandleId]), new DataNode(xpath: xpath2, leaves: ['id': cmHandleId2])]
            mockCpsDataService.getDataNodesForMultipleXpaths(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, [xpath, xpath2] , INCLUDE_ALL_DESCENDANTS) >> dataNodes
        when: 'retrieving the yang modelled cm handles'
            def results = objectUnderTest.getYangModelCmHandles([cmHandleId, cmHandleId2])
        then: 'verify both have returned and cm handle Ids are correct'
            assert results.size() == 2
            assert results.id.containsAll([cmHandleId, cmHandleId2])
    }

    def 'YangModelCmHandles are not returned for invalid cm handle ids.'() {
        given: 'invalid cm handle id throws a data validation exception'
            mockCpsValidator.validateNameCharacters('Invalid Cm Handle Id') >> {throw new DataValidationException('','')}
        and: 'empty collection is returned as no valid cm handle ids are given'
            mockCpsDataService.getDataNodesForMultipleXpaths(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, [] , INCLUDE_ALL_DESCENDANTS) >> []
        when: 'retrieving the yang modelled cm handles'
            def results = objectUnderTest.getYangModelCmHandles(['Invalid Cm Handle Id'])
        then: 'no YangModelCmHandle is returned'
            assert results.size() == 0
    }

    def 'Get a Cm Handle Composite State.'() {
        given: 'a valid cm handle id'
            def cmHandleId = 'ch-1'
            def dataNode = new DataNode(leaves: ['cm-handle-state': 'ADVISED'])
        and: 'cps data service returns a valid data node'
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    '/dmi-registry/cm-handles[@id=\'ch-1\']/state', INCLUDE_ALL_DESCENDANTS) >> [dataNode]
        when: 'get cm handle state is invoked'
            def result = objectUnderTest.getCmHandleState(cmHandleId)
        then: 'result has returned the correct cm handle state'
            result.cmHandleState == CmHandleState.ADVISED
        and: 'the CM Handle ID is validated'
            1 * mockCpsValidator.validateNameCharacters(cmHandleId)
    }

    def 'Update Cm Handle with #scenario State.'() {
        given: 'a cm handle and a composite state'
            def cmHandleId = 'ch-1'
            def compositeState = new CompositeState(cmHandleState: cmHandleState, lastUpdateTime: formattedDateAndTime)
        when: 'update cm handle state is invoked with the #scenario state'
            objectUnderTest.saveCmHandleState(cmHandleId, compositeState)
        then: 'update node leaves is invoked with the correct params'
            1 * mockCpsDataService.updateDataNodeAndDescendants(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, '/dmi-registry/cm-handles[@id=\'ch-1\']', expectedJsonData, _ as OffsetDateTime, ContentType.JSON)
        where: 'the following states are used'
            scenario    | cmHandleState          || expectedJsonData
            'READY'     | CmHandleState.READY    || '{"state":{"cm-handle-state":"READY","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
            'LOCKED'    | CmHandleState.LOCKED   || '{"state":{"cm-handle-state":"LOCKED","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
            'DELETING'  | CmHandleState.DELETING || '{"state":{"cm-handle-state":"DELETING","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
    }

    def 'Update Cm Handles with #scenario States.'() {
        given: 'a map of cm handles composite states'
            def compositeState1 = new CompositeState(cmHandleState: cmHandleState, lastUpdateTime: formattedDateAndTime)
            def compositeState2 = new CompositeState(cmHandleState: cmHandleState, lastUpdateTime: formattedDateAndTime)
        and: 'alternate id cache contains the given cm handle reference'
            mockCmHandleIdPerAlternateId.containsKey(_) >> true
        when: 'update cm handle state is invoked with the #scenario state'
            def cmHandleStateMap = ['ch-11' : compositeState1, 'ch-12' : compositeState2]
            objectUnderTest.saveCmHandleStateBatch(cmHandleStateMap)
        then: 'update node leaves is invoked with the correct params'
            1 * mockCpsDataService.updateDataNodesAndDescendants(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cmHandlesJsonDataMap, _ as OffsetDateTime, ContentType.JSON)
        where: 'the following states are used'
            scenario    | cmHandleState          || cmHandlesJsonDataMap
            'READY'     | CmHandleState.READY    || ['/dmi-registry/cm-handles[@id=\'ch-11\']':'{"state":{"cm-handle-state":"READY","last-update-time":"2022-12-31T20:30:40.000+0000"}}', '/dmi-registry/cm-handles[@id=\'ch-12\']':'{"state":{"cm-handle-state":"READY","last-update-time":"2022-12-31T20:30:40.000+0000"}}']
            'LOCKED'    | CmHandleState.LOCKED   || ['/dmi-registry/cm-handles[@id=\'ch-11\']':'{"state":{"cm-handle-state":"LOCKED","last-update-time":"2022-12-31T20:30:40.000+0000"}}', '/dmi-registry/cm-handles[@id=\'ch-12\']':'{"state":{"cm-handle-state":"LOCKED","last-update-time":"2022-12-31T20:30:40.000+0000"}}']
            'DELETING'  | CmHandleState.DELETING || ['/dmi-registry/cm-handles[@id=\'ch-11\']':'{"state":{"cm-handle-state":"DELETING","last-update-time":"2022-12-31T20:30:40.000+0000"}}', '/dmi-registry/cm-handles[@id=\'ch-12\']':'{"state":{"cm-handle-state":"DELETING","last-update-time":"2022-12-31T20:30:40.000+0000"}}']
    }

    def 'Update cm handle states when #scenario in alternate id cache.'() {
        given: 'a map of cm handles composite states'
            def compositeState = new CompositeState(cmHandleState: CmHandleState.ADVISED, lastUpdateTime: formattedDateAndTime)
            def cmHandleStateMap = ['ch-1' : compositeState]
        and: 'alternate id cache returns #scenario'
            mockCmHandleIdPerAlternateId.containsKey(_) >> keyExists
            mockCmHandleIdPerAlternateId.containsValue(_) >> valueExists
        when: 'we update the state of a cm handle when #scenario'
            objectUnderTest.saveCmHandleStateBatch(cmHandleStateMap)
        then: 'update node leaves is invoked correct number of times'
            expectedCalls * mockCpsDataService.updateDataNodesAndDescendants(*_)
        where: 'the following cm handle ids are used'
            scenario            | keyExists | valueExists || expectedCalls
            'id exists as key'  | true      | false       || 1
            'id exists as value'| false     | true        || 1
            'id does not exist' | false     | false       || 0

    }

    def 'Getting module definitions by module.'() {
        given: 'cps module service returns module definition for module name'
            def moduleDefinitions = [new ModuleDefinition('moduleName','revision','content')]
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,'some-cmHandle-Id', 'some-module', '2024-01-25') >> moduleDefinitions
        when: 'get module definitions is invoked with module name'
            def result = objectUnderTest.getModuleDefinitionsByCmHandleAndModule('some-cmHandle-Id', 'some-module', '2024-01-25')
        then: 'returned result are the same module definitions as returned from module service'
            assert result == moduleDefinitions
        and: 'cm handle id and module name validated'
            1 * mockCpsValidator.validateNameCharacters('some-cmHandle-Id', 'some-module')
    }

    def 'Getting module definitions with cm handle id.'() {
        given: 'cps module service returns module definitions for cm handle id'
            def moduleDefinitions = [new ModuleDefinition('moduleName','revision','content')]
            mockCpsModuleService.getModuleDefinitionsByAnchorName(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,'some-cmHandle-Id') >> moduleDefinitions
        when: 'get module definitions is invoked with cm handle id'
            def result = objectUnderTest.getModuleDefinitionsByCmHandleId('some-cmHandle-Id')
        then: 'the returned result are the same module definitions as returned from the module service'
            assert result == moduleDefinitions
    }

    def 'Get module references.'() {
        given: 'cps module service returns a collection of module references'
            def moduleReferences = [new ModuleReference('moduleName','revision','namespace')]
            mockCpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,'some-cmHandle-Id') >> moduleReferences
        when: 'get yang resources module references by cmHandle is invoked'
            def result = objectUnderTest.getYangResourcesModuleReferences('some-cmHandle-Id')
        then: 'the returned result is a collection of module definitions'
            assert result == moduleReferences
        and: 'the CM Handle ID is validated'
            1 * mockCpsValidator.validateNameCharacters('some-cmHandle-Id')
    }

    def 'Save Cmhandle.'() {
        given: 'cmHandle represented as Yang Model'
            def yangModelCmHandle = new YangModelCmHandle(id: 'cmhandle', additionalProperties: [], publicProperties: [])
        when: 'the method to save cmhandle is called'
            objectUnderTest.saveCmHandle(yangModelCmHandle)
        then: 'the data service method to save list elements is called once'
            1 * mockCpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                    _,null, ContentType.JSON) >> {
                args -> {
                    assert args[3].startsWith('{"cm-handles":[{"id":"cmhandle","additional-properties":[],"public-properties":[]}]}')
                }
            }
    }

    def 'Save Multiple Cmhandles.'() {
        given: 'cm handles represented as Yang Model'
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'cmhandle1')
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'cmhandle2')
        when: 'the cm handles are saved'
            objectUnderTest.saveCmHandleBatch([yangModelCmHandle1, yangModelCmHandle2])
        then: 'CPS Data Service persists both cm handles as a batch'
            1 * mockCpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    NCMP_DMI_REGISTRY_PARENT, _,null, ContentType.JSON) >> {
                args -> {
                    def jsonData = (args[3] as String)
                    jsonData.contains('cmhandle1')
                    jsonData.contains('cmhandle2')
                }
            }
    }

    def 'Delete list or list elements.'() {
        when: 'the method to delete list or list elements is called'
            objectUnderTest.deleteListOrListElement('sample xPath')
        then: 'the data service method to save list elements is called once'
            1 * mockCpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,'sample xPath',null)
    }

    def 'Get data node via xPath.'() {
        when: 'the method to get data nodes is called'
            objectUnderTest.getDataNode('sample xPath')
        then: 'the data persistence service method to get data node is invoked once'
            1 * mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,'sample xPath', INCLUDE_ALL_DESCENDANTS)
    }

    def 'Get cmHandle data node.'() {
        given: 'expected xPath to get cmHandle data node'
            def expectedXPath = '/dmi-registry/cm-handles[@id=\'sample cmHandleId\']'
        when: 'the method to get data nodes is called'
            objectUnderTest.getCmHandleDataNodeByCmHandleId('sample cmHandleId', INCLUDE_ALL_DESCENDANTS)
        then: 'the data persistence service method to get cmHandle data node is invoked once with expected xPath'
            1 * mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, expectedXPath, INCLUDE_ALL_DESCENDANTS)
    }

    def 'Get CM handle ids for CM Handles that has given module names.'() {
        when: 'the method to get cm handles is called'
            objectUnderTest.getCmHandleReferencesWithGivenModules(['sample-module-name'], false)
        then: 'the admin persistence service method to query anchors is invoked once with the same parameter'
            1 * mockCpsAnchorService.queryAnchorNames(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, ['sample-module-name'])
    }

    def 'Get Alternate Ids for CM Handles that has given module names.'() {
        given: 'cps anchor service returns a CM-handle ID for the given module name'
            mockCpsAnchorService.queryAnchorNames(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, ['sample-module-name']) >> ['ch-1']
        and: 'cps data service returns some data nodes for the given CM-handle ID'
            def dataNodes = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='ch-1']", leaves: ['id': 'ch-1', 'alternate-id': 'alt-1'])]
            mockCpsDataService.getDataNodesForMultipleXpaths(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, ["/dmi-registry/cm-handles[@id='ch-1']"], OMIT_DESCENDANTS) >> dataNodes
        when: 'the method to get cm-handle references by modules is called (outputting alternate IDs)'
            def result = objectUnderTest.getCmHandleReferencesWithGivenModules(['sample-module-name'], true)
        then: 'the result contains the correct alternate Id'
            assert result == ['alt-1'] as Set
    }

    def 'Replace list content.'() {
        when: 'replace list content method is called with xpath and data nodes collection'
            objectUnderTest.replaceListContent('sample xpath', [new DataNode()])
        then: 'the cps data service method to replace list content is invoked once with same parameters'
            1 * mockCpsDataService.replaceListContent(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,'sample xpath', [new DataNode()], NO_TIMESTAMP);
    }

    def 'Delete data node via xPath.'() {
        when: 'Delete data node method is called with xpath as parameter'
            objectUnderTest.deleteDataNode('sample dataNode xpath')
        then: 'the cps data service method to delete data node is invoked once with the same xPath'
            1 * mockCpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, 'sample dataNode xpath', NO_TIMESTAMP);
    }

    def 'Delete multiple data nodes via xPath.'() {
        when: 'Delete data nodes method is called with multiple xpaths as parameters'
            objectUnderTest.deleteDataNodes(['xpath1', 'xpath2'])
        then: 'the cps data service method to delete data nodes is invoked once with the same xPaths'
            1 * mockCpsDataService.deleteDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, ['xpath1', 'xpath2'], NO_TIMESTAMP);
    }

    def 'CM handle exists.'() {
        given: 'data service returns a datanode with correct cm handle id'
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, xpath, OMIT_DESCENDANTS) >> [dataNode]
        expect: 'cm handle exists for given cm handle id'
            assert true == objectUnderTest.isExistingCmHandleId(cmHandleId)
    }

    def 'CM handle does not exist (data service returns empty collection).'() {
        given: 'data service returns an empty datanode'
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, xpath, OMIT_DESCENDANTS) >> []
        expect: 'false is returned for non-existent cm handle'
            assert false == objectUnderTest.isExistingCmHandleId(cmHandleId)
    }

    def 'CM handle does not exist (data service throws).'() {
        given: 'data service throws an exception'
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, "/dmi-registry/cm-handles[@id='non-existent-cm-handle']", OMIT_DESCENDANTS) >> {throw new DataNodeNotFoundException('','')}
        expect: 'false is returned for non-existent cm handle'
            assert false == objectUnderTest.isExistingCmHandleId('non-existent-cm-handle')
    }

    def 'Delete anchors.'() {
        when: 'Deleting some anchors'
            objectUnderTest.deleteAnchors(['anchor1' ,'anchor2'])
        then: 'The call is delegated to the anchor service with teh correct parameters'
            mockCpsAnchorService.deleteAnchors(NCMP_DATASPACE_NAME ,['anchor1' ,'anchor2'])
    }

    def 'Get Yang Model CM Handles without properties.'() {
        given: 'the cps data service returns 2 data nodes from the DMI registry (omitting descendants)'
            def dataNodes = [new DataNode(xpath: xpath, leaves: ['id': cmHandleId]), new DataNode(xpath: xpath2, leaves: ['id': cmHandleId2])]
            mockCpsDataService.getDataNodesForMultipleXpaths(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, [xpath, xpath2] , OMIT_DESCENDANTS) >> dataNodes
        when: 'retrieving cm handles without properties'
            def result = objectUnderTest.getYangModelCmHandlesWithoutProperties([cmHandleId, cmHandleId2])
        then: 'The cm handles from the data service are returned'
            assert result.size() == 2
            assert result.id.containsAll([cmHandleId, cmHandleId2])
    }

    def 'Update Cm Handle Field.'(){
        when: 'update is called.'
            objectUnderTest.updateCmHandleField('ch-1', 'my field', 'my new value')
        then: 'call is delegated to updateCmHandleFields'
            1 * objectUnderTest.updateCmHandleFields('my field', ['ch-1':'my new value'])
    }

    def 'Bulk update cm handle state.'(){
        when: 'bulk update is called'
            objectUnderTest.cmHandleBulkMigrate(updates)
        then: 'call is made to update the fileds of the cm handle'
        1 * mockCpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT, { jsonString ->
            jsonString.contains('"cm-handle-state":"READY"') && jsonString.contains('"cm-handle-state":"DELETING"')
        }, _, ContentType.JSON)
    }

    def 'Bulk update with empty list.'() {
        when: 'bulk update is called with empty list'
            objectUnderTest.cmHandleBulkMigrate([])
        then: 'no database call is made'
            0 * mockCpsDataService.updateNodeLeaves(*_)
    }

}

