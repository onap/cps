/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.inventory

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.spi.CascadeDeleteAllowed
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.ModuleDefinition
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class InventoryPersistenceSpec extends Specification {

    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    def mockCpsDataService = Mock(CpsDataService)

    def mockCpsModuleService = Mock(CpsModuleService)

    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)

    def mockCpsAdminPersistenceService = Mock(CpsAdminPersistenceService)

    def objectUnderTest = new InventoryPersistence(spiedJsonObjectMapper, mockCpsDataService, mockCpsModuleService,
            mockCpsDataPersistenceService, mockCpsAdminPersistenceService)

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    def cmHandleId = 'some-cm-handle'
    def leaves = ["dmi-service-name":"common service name","dmi-data-service-name":"data service name","dmi-model-service-name":"model service name"]
    def xpath = "/dmi-registry/cm-handles[@id='some-cm-handle']"

    @Shared
    def childDataNodesForCmHandleWithAllProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/additional-properties[@name='name1']", leaves: ["name":"name1", "value":"value1"]),
                                                      new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/public-properties[@name='name2']", leaves: ["name":"name2","value":"value2"])]

    @Shared
    def childDataNodesForCmHandleWithDMIProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some-cm-handle']/additional-properties[@name='name1']", leaves: ["name":"name1", "value":"value1"])]

    @Shared
    def childDataNodesForCmHandleWithPublicProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some-cm-handle']/public-properties[@name='name2']", leaves: ["name":"name2","value":"value2"])]

    @Shared
    def childDataNodesForCmHandleWithState = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some-cm-handle']/state", leaves: ['cm-handle-state': 'ADVISED'])]

    def "Retrieve CmHandle using datanode with #scenario."() {
        given: 'the cps data service returns a data node from the DMI registry'
            def dataNode = new DataNode(childDataNodes:childDataNodes, leaves: leaves)
            mockCpsDataPersistenceService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', xpath, INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'retrieving the yang modelled cm handle'
            def result = objectUnderTest.getYangModelCmHandle(cmHandleId)
        then: 'the result has the correct id and service names'
            result.id == cmHandleId
            result.dmiServiceName == 'common service name'
            result.dmiDataServiceName == 'data service name'
            result.dmiModelServiceName == 'model service name'
        and: 'the expected DMI properties'
            result.dmiProperties == expectedDmiProperties
            result.publicProperties == expectedPublicProperties
        and: 'the state details are returned'
            result.compositeState.cmHandleState == expectedCompositeState
        where: 'the following parameters are used'
            scenario                    | childDataNodes                                || expectedDmiProperties                               || expectedPublicProperties                              || expectedCompositeState
            'no properties'             | []                                            || []                                                  || []                                                    || null
            'DMI and public properties' | childDataNodesForCmHandleWithAllProperties    || [new YangModelCmHandle.Property("name1", "value1")] || [new YangModelCmHandle.Property("name2", "value2")] || null
            'just DMI properties'       | childDataNodesForCmHandleWithDMIProperties    || [new YangModelCmHandle.Property("name1", "value1")] || []                                                    || null
            'just public properties'    | childDataNodesForCmHandleWithPublicProperties || []                                                  || [new YangModelCmHandle.Property("name2", "value2")]   || null
            'with state details'        | childDataNodesForCmHandleWithState            || []                                                  || []                                                    || CmHandleState.ADVISED
    }

    def "Retrieve CmHandle using datanode with invalid CmHandle id."() {
        when: 'retrieving the yang modelled cm handle with an invalid id'
            def result = objectUnderTest.getYangModelCmHandle('cm handle id with spaces')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the result is not returned'
            result == null
    }

    def "Handling missing service names as null CPS-1043."() {
        given: 'the cps data service returns a data node from the DMI registry with empty child and leaf attributes'
            def dataNode = new DataNode(childDataNodes:[], leaves: [:])
            mockCpsDataPersistenceService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', xpath, INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'retrieving the yang modelled cm handle'
            def result = objectUnderTest.getYangModelCmHandle(cmHandleId)
        then: 'the service names ae returned as null'
            result.dmiServiceName == null
            result.dmiDataServiceName == null
            result.dmiModelServiceName == null
    }

    def 'Get a Cm Handle Composite State'() {
        given: 'a valid cm handle id'
            def cmHandleId = 'Some-Cm-Handle'
            def dataNode = new DataNode(leaves: ['cm-handle-state': 'ADVISED'])
        and: 'cps data service returns a valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']/state', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get cm handle state is invoked'
            def result = objectUnderTest.getCmHandleState(cmHandleId)
        then: 'result has returned the correct cm handle state'
            result.cmHandleState == CmHandleState.ADVISED
    }

    def 'Update Cm Handle with #scenario State'() {
        given: 'a cm handle and a composite state'
            def cmHandleId = 'Some-Cm-Handle'
            def compositeState = new CompositeState(cmHandleState: cmHandleState, lastUpdateTime: formattedDateAndTime)
        when: 'update cm handle state is invoked with the #scenario state'
            objectUnderTest.saveCmHandleState(cmHandleId, compositeState)
        then: 'update node leaves is invoked with the correct params'
            1 * mockCpsDataService.updateDataNodeAndDescendants('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']', expectedJsonData, _ as OffsetDateTime)
        where: 'the following states are used'
            scenario    | cmHandleState          || expectedJsonData
            'READY'     | CmHandleState.READY    || '{"state":{"cm-handle-state":"READY","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
            'LOCKED'    | CmHandleState.LOCKED   || '{"state":{"cm-handle-state":"LOCKED","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
            'DELETING'  | CmHandleState.DELETING || '{"state":{"cm-handle-state":"DELETING","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
    }

    def 'Update Cm Handles with #scenario States'() {
        given: 'a map of cm handles composite states'
            def compositeState1 = new CompositeState(cmHandleState: cmHandleState, lastUpdateTime: formattedDateAndTime)
            def compositeState2 = new CompositeState(cmHandleState: cmHandleState, lastUpdateTime: formattedDateAndTime)
        when: 'update cm handle state is invoked with the #scenario state'
            def cmHandleStateMap = ['Some-Cm-Handle1' : compositeState1, 'Some-Cm-Handle2' : compositeState2]
            objectUnderTest.saveCmHandleStates(cmHandleStateMap)
        then: 'update node leaves is invoked with the correct params'
            1 * mockCpsDataService.updateDataNodesAndDescendants('NCMP-Admin', 'ncmp-dmi-registry', cmHandlesJsonDataMap, _ as OffsetDateTime)
        where: 'the following states are used'
            scenario    | cmHandleState          || cmHandlesJsonDataMap
            'READY'     | CmHandleState.READY    || ['/dmi-registry/cm-handles[@id=\'Some-Cm-Handle1\']':'{"state":{"cm-handle-state":"READY","last-update-time":"2022-12-31T20:30:40.000+0000"}}', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle2\']':'{"state":{"cm-handle-state":"READY","last-update-time":"2022-12-31T20:30:40.000+0000"}}']
            'LOCKED'    | CmHandleState.LOCKED   || ['/dmi-registry/cm-handles[@id=\'Some-Cm-Handle1\']':'{"state":{"cm-handle-state":"LOCKED","last-update-time":"2022-12-31T20:30:40.000+0000"}}', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle2\']':'{"state":{"cm-handle-state":"LOCKED","last-update-time":"2022-12-31T20:30:40.000+0000"}}']
            'DELETING'  | CmHandleState.DELETING || ['/dmi-registry/cm-handles[@id=\'Some-Cm-Handle1\']':'{"state":{"cm-handle-state":"DELETING","last-update-time":"2022-12-31T20:30:40.000+0000"}}', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle2\']':'{"state":{"cm-handle-state":"DELETING","last-update-time":"2022-12-31T20:30:40.000+0000"}}']
    }

    def 'Get module definitions'() {
        given: 'cps module service returns a collection of module definitions'
            def moduleDefinitions = [new ModuleDefinition('moduleName','revision','content')]
            mockCpsModuleService.getModuleDefinitionsByAnchorName('NFP-Operational','some-cmHandle-Id') >> moduleDefinitions
        when: 'get module definitions by cmHandle is invoked'
            def result = objectUnderTest.getModuleDefinitionsByCmHandleId('some-cmHandle-Id')
        then: 'the returned result are the same module definitions as returned from the module service'
            assert result == moduleDefinitions
    }

    def 'Get module references'() {
        given: 'cps module service returns a collection of module references'
            def moduleReferences = [new ModuleReference('moduleName','revision','namespace')]
            mockCpsModuleService.getYangResourcesModuleReferences('NFP-Operational','some-cmHandle-Id') >> moduleReferences
        when: 'get yang resources module references by cmHandle is invoked'
            def result = objectUnderTest.getYangResourcesModuleReferences('some-cmHandle-Id')
        then: 'the returned result is a collection of module definitions'
            assert result == moduleReferences
    }

    def 'Save Cmhandle'() {
        given: 'cmHandle represented as Yang Model'
            def yangModelCmHandle = new YangModelCmHandle(id: 'cmhandle', dmiProperties: [], publicProperties: [])
        when: 'the method to save cmhandle is called'
            objectUnderTest.saveCmHandle(yangModelCmHandle)
        then: 'the data service method to save list elements is called once'
            1 * mockCpsDataService.saveListElements('NCMP-Admin','ncmp-dmi-registry','/dmi-registry',_,null) >> {
                args -> {
                    assert args[3].startsWith('{"cm-handles":[{"id":"cmhandle","additional-properties":[],"public-properties":[]}]}')
                }
            }
    }

    def 'Save Multiple Cmhandles'() {
        given: 'cmHandle represented as Yang Model'
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'cmhandle1', dmiProperties: [], publicProperties: [])
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'cmhandle2', dmiProperties: [], publicProperties: [])
        when: 'the method to save cmhandle is called'
            objectUnderTest.saveCmHandles([yangModelCmHandle1, yangModelCmHandle2])
        then: 'the data service method to save list elements is called once'
            1 * mockCpsDataService.saveListElementsCollection('NCMP-Admin','ncmp-dmi-registry','/dmi-registry',_,null) >> {
                args -> {
                    def jsonDataList = (args[3] as List)
                    (jsonDataList[0] as String).contains('cmhandle1')
                    (jsonDataList[0] as String).contains('cmhandle2')
                }
            }
    }

    def 'Delete list or list elements'() {
        when: 'the method to delete list or list elements is called'
            objectUnderTest.deleteListOrListElement('sample xPath')
        then: 'the data service method to save list elements is called once'
            1 * mockCpsDataService.deleteListOrListElement('NCMP-Admin','ncmp-dmi-registry','sample xPath',null)
    }

    def 'Delete schema set with a valid schema set name'() {
        when: 'the method to delete schema set is called with valid schema set name'
            objectUnderTest.deleteSchemaSetWithCascade('validSchemaSetName')
        then: 'the module service to delete schemaSet is invoked once'
            1 * mockCpsModuleService.deleteSchemaSet('NFP-Operational', 'validSchemaSetName', CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED)
    }

    def 'Delete schema set with an invalid schema set name'() {
        when: 'the method to delete schema set is called with an invalid schema set name'
            objectUnderTest.deleteSchemaSetWithCascade('invalid SchemaSet name')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the module service to delete schemaSet is not called'
            0 * mockCpsModuleService.deleteSchemaSet('NFP-Operational', 'sampleSchemaSetName', CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED)
    }

    def 'Get data node via xPath'() {
        when: 'the method to get data nodes is called'
            objectUnderTest.getDataNode('sample xPath')
        then: 'the data persistence service method to get data node is invoked once'
            1 * mockCpsDataPersistenceService.getDataNode('NCMP-Admin','ncmp-dmi-registry','sample xPath', INCLUDE_ALL_DESCENDANTS)
    }

    def 'Get cmHandle data node'() {
        given: 'expected xPath to get cmHandle data node'
            def expectedXPath = '/dmi-registry/cm-handles[@id=\'sample cmHandleId\']';
        when: 'the method to get data nodes is called'
            objectUnderTest.getCmHandleDataNode('sample cmHandleId')
        then: 'the data persistence service method to get cmHandle data node is invoked once with expected xPath'
            1 * mockCpsDataPersistenceService.getDataNode('NCMP-Admin','ncmp-dmi-registry',expectedXPath, INCLUDE_ALL_DESCENDANTS)
    }

    def 'Query anchors'() {
        when: 'the method to query anchors is called'
            objectUnderTest.queryAnchors(['sample-module-name'])
        then: 'the admin persistence service method to query anchors is invoked once with the same parameter'
            1 * mockCpsAdminPersistenceService.queryAnchors('NFP-Operational',['sample-module-name'])
    }

    def 'Get anchors'() {
        when: 'the method to get anchors with no parameters is called'
            objectUnderTest.getAnchors()
        then: 'the admin persistence service method to query anchors is invoked once with a specific dataspace name'
            1 * mockCpsAdminPersistenceService.getAnchors('NFP-Operational')
    }

    def 'Replace list content'() {
        when: 'replace list content method is called with xpath and data nodes collection'
            objectUnderTest.replaceListContent('sample xpath', [new DataNode()])
        then: 'the cps data service method to replace list content is invoked once with same parameters'
            1 * mockCpsDataService.replaceListContent('NCMP-Admin', 'ncmp-dmi-registry',
                    'sample xpath', [new DataNode()], NO_TIMESTAMP);
    }

    def 'Delete data node via xPath'() {
        when: 'Delete data node method is called with xpath as parameter'
            objectUnderTest.deleteDataNode('sample dataNode xpath')
        then: 'the cps data service method to delete data node is invoked once with the same xPath'
            1 * mockCpsDataService.deleteDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    'sample dataNode xpath', NO_TIMESTAMP);
    }

}
