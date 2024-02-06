/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ncmp.api.impl

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.OperationType.CREATE
import static org.onap.cps.ncmp.api.impl.operations.OperationType.UPDATE

import org.onap.cps.ncmp.api.impl.utils.CmHandleIdMapper
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService
import org.onap.cps.ncmp.api.impl.events.lcm.LcmEventsCmHandleStateHandler
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevelManager
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.CompositeState
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory
import org.onap.cps.ncmp.api.impl.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.models.DataOperationDefinition
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters
import org.onap.cps.ncmp.api.models.ConditionApiProperties
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.models.DataOperationRequest
import org.onap.cps.spi.exceptions.CpsException
import org.onap.cps.spi.model.ConditionProperties
import spock.lang.Shared
import java.util.stream.Collectors
import org.onap.cps.utils.JsonObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class NetworkCmProxyDataServiceImplSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def nullNetworkCmProxyDataServicePropertyHandler = null
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockCmHandleQueries = Mock(CmHandleQueries)
    def mockDmiPluginRegistration = Mock(DmiPluginRegistration)
    def mockCpsCmHandlerQueryService = Mock(NetworkCmProxyCmHandleQueryService)
    def mockLcmEventsCmHandleStateHandler = Mock(LcmEventsCmHandleStateHandler)
    def stubModuleSyncStartedOnCmHandles = Stub(IMap<String, Object>)
    def stubTrustLevelPerDmiPlugin = Stub(Map<String, TrustLevel>)
    def mockTrustLevelManager = Mock(TrustLevelManager)
    def mockCmHandleIdMapper = Mock(CmHandleIdMapper)
    def mockModuleSetTagCache = [:]

    def NO_TOPIC = null
    def NO_REQUEST_ID = null
    @Shared
    def OPTIONS_PARAM = '(a=1,b=2)'
    @Shared
    def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'test-cm-handle-id')

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(
            spiedJsonObjectMapper,
            mockDmiDataOperations,
            nullNetworkCmProxyDataServicePropertyHandler,
            mockInventoryPersistence,
            mockCmHandleQueries,
            mockCpsCmHandlerQueryService,
            mockLcmEventsCmHandleStateHandler,
            mockCpsDataService,
            mockCpsModuleService,
            stubModuleSyncStartedOnCmHandles,
            stubTrustLevelPerDmiPlugin,
            mockTrustLevelManager,
            mockCmHandleIdMapper,
            mockModuleSetTagCache)

    def cmHandleXPath = "/dmi-registry/cm-handles[@id='testCmHandle']"

    def dataNode = [new DataNode(leaves: ['id': 'some-cm-handle', 'dmi-service-name': 'testDmiService'])]

    def 'Write resource data for pass-through running from DMI using POST.'() {
        given: 'cpsDataService returns valid datanode'
            mockDataNode()
        when: 'write resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                    CREATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.CREATED) }
    }

    def 'Get resource data for pass-through operational from DMI.'() {
        given: 'cpsDataService returns valid data node'
            mockDataNode()
        and: 'get resource data from DMI is called'
            mockDmiDataOperations.getResourceDataFromDmi(PASSTHROUGH_OPERATIONAL.datastoreName,'testCmHandle', 'testResourceId', OPTIONS_PARAM, NO_TOPIC, NO_REQUEST_ID) >>
                    new ResponseEntity<>('dmi-response', HttpStatus.OK)
        when: 'get resource data operational for cm-handle is called'
            def response = objectUnderTest.getResourceDataForCmHandle(PASSTHROUGH_OPERATIONAL.datastoreName, 'testCmHandle', 'testResourceId', OPTIONS_PARAM, NO_TOPIC, NO_REQUEST_ID)
        then: 'DMI returns a json response'
            assert response == 'dmi-response'
    }

    def 'Get resource data for pass-through running from DMI.'() {
        given: 'cpsDataService returns valid data node'
            mockDataNode()
        and: 'DMI returns valid response and data'
            mockDmiDataOperations.getResourceDataFromDmi(PASSTHROUGH_RUNNING.datastoreName, 'testCmHandle', 'testResourceId', OPTIONS_PARAM, NO_TOPIC, NO_REQUEST_ID) >>
                    new ResponseEntity<>('{dmi-response}', HttpStatus.OK)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataForCmHandle(PASSTHROUGH_RUNNING.datastoreName, 'testCmHandle', 'testResourceId', OPTIONS_PARAM, NO_TOPIC, NO_REQUEST_ID)
        then: 'get resource data returns expected response'
            assert response == '{dmi-response}'
    }

    def 'Get resource data for operational (cached) data.'() {
        given: 'CPS Data service returns some object(s)'
            mockCpsDataService.getDataNodes(OPERATIONAL.datastoreName, 'testCmHandle', 'testResourceId', FetchDescendantsOption.OMIT_DESCENDANTS) >> ['First Object', 'other Object']
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataForCmHandle(OPERATIONAL.datastoreName, 'testCmHandle', 'testResourceId', FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'get resource data returns teh first object from the data service'
            assert response == 'First Object'
    }

    def 'Execute (async) data operation for #datastoreName from DMI.'() {
        given: 'cpsDataService returns valid data node'
            def dataOperationRequest = getDataOperationRequest(datastoreName)
        when: 'request resource data for data operation is called'
            objectUnderTest.executeDataOperationForCmHandles('some topic', dataOperationRequest, 'requestId')
        then: 'request resource data for data operation returns expected response'
            1 * mockDmiDataOperations.requestResourceDataFromDmi('some topic', dataOperationRequest, 'requestId')
        where: 'the following data stores are used'
            datastoreName << [PASSTHROUGH_RUNNING.datastoreName, PASSTHROUGH_OPERATIONAL.datastoreName]
    }

    def 'Getting Yang Resources.'() {
        when: 'yang resources is called'
            objectUnderTest.getYangResourcesModuleReferences('some-cm-handle')
        then: 'CPS module services is invoked for the correct dataspace and cm handle'
            1 * mockInventoryPersistence.getYangResourcesModuleReferences('some-cm-handle')
    }

    def 'Delete unused yang resource modules.'() {
            when: 'cm handles batch deletion is called'
                objectUnderTest.batchDeleteCmHandlesFromDbAndModuleSyncMap(Collections.emptyList())
            then: 'CPS module services is invoked for deletion of unused yang resource modules'
                1 * mockCpsModuleService.deleteUnusedYangResourceModules()
    }

    def 'Get a cm handle.'() {
        given: 'the system returns a yang modelled cm handle'
            def dmiServiceName = 'some service name'
            def compositeState = new CompositeState(cmHandleState: CmHandleState.ADVISED,
                lockReason: CompositeState.LockReason.builder().lockReasonCategory(LockReasonCategory.MODULE_SYNC_FAILED).details("lock details").build(),
                lastUpdateTime: 'some-timestamp',
                dataSyncEnabled: false,
                dataStores: dataStores())
            def dmiProperties = [new YangModelCmHandle.Property('Book', 'Romance Novel')]
            def publicProperties = [new YangModelCmHandle.Property('Public Book', 'Public Romance Novel')]
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', dmiServiceName: dmiServiceName,
                dmiProperties: dmiProperties, publicProperties: publicProperties, compositeState: compositeState)
            1 * mockInventoryPersistence.getYangModelCmHandle('some-cm-handle') >> yangModelCmHandle
        when: 'getting cm handle details for a given cm handle id from ncmp service'
            def result = objectUnderTest.getNcmpServiceCmHandle('some-cm-handle')
        then: 'the result is a ncmpServiceCmHandle'
            result.class == NcmpServiceCmHandle.class
        and: 'the cm handle contains the cm handle id'
            result.cmHandleId == 'some-cm-handle'
        and: 'the cm handle contains the DMI Properties'
            result.dmiProperties ==[ Book:'Romance Novel' ]
        and: 'the cm handle contains the public Properties'
            result.publicProperties == [ "Public Book":'Public Romance Novel' ]
        and: 'the cm handle contains the cm handle composite state'
            result.compositeState == compositeState

    }

    def 'Get cm handle public properties'() {
        given: 'a yang modelled cm handle'
            def dmiProperties = [new YangModelCmHandle.Property('prop', 'some DMI property')]
            def publicProperties = [new YangModelCmHandle.Property('public prop', 'some public prop')]
            def yangModelCmHandle = new YangModelCmHandle(id:'some-cm-handle', dmiServiceName: 'some service name', dmiProperties: dmiProperties, publicProperties: publicProperties)
        and: 'the system returns this yang modelled cm handle'
            1 * mockInventoryPersistence.getYangModelCmHandle('some-cm-handle') >> yangModelCmHandle
        when: 'getting cm handle public properties for a given cm handle id from ncmp service'
            def result = objectUnderTest.getCmHandlePublicProperties('some-cm-handle')
        then: 'the result returns the correct data'
            result == [ 'public prop' : 'some public prop' ]
    }

    def 'Execute cm handle id search for inventory'() {
        given: 'a ConditionApiProperties object'
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllProperties'
            conditionProperties.conditionParameters = [ [ 'some-key' : 'some-value' ] ]
            def conditionServiceProps = new CmHandleQueryServiceParameters()
            conditionServiceProps.cmHandleQueryParameters = [conditionProperties] as List<ConditionProperties>
        and: 'the system returns an set of cmHandle ids'
            mockCpsCmHandlerQueryService.queryCmHandleIdsForInventory(*_) >> [ 'cmHandle1', 'cmHandle2' ]
        when: 'getting cm handle id set for a given dmi property'
            def result = objectUnderTest.executeCmHandleIdSearchForInventory(conditionServiceProps)
        then: 'the result returns the correct 2 elements'
            assert result.size() == 2
            assert result.contains('cmHandle1')
            assert result.contains('cmHandle2')
    }

    def 'Get cm handle composite state'() {
        given: 'a yang modelled cm handle'
            def compositeState = new CompositeState(cmHandleState: CmHandleState.ADVISED,
                lockReason: CompositeState.LockReason.builder().lockReasonCategory(LockReasonCategory.MODULE_SYNC_FAILED).details("lock details").build(),
                lastUpdateTime: 'some-timestamp',
                dataSyncEnabled: false,
                dataStores: dataStores())
            def dmiProperties = [new YangModelCmHandle.Property('prop', 'some DMI property')]
            def publicProperties = [new YangModelCmHandle.Property('public prop', 'some public prop')]
            def yangModelCmHandle = new YangModelCmHandle(id:'some-cm-handle', dmiServiceName: 'some service name', dmiProperties: dmiProperties, publicProperties: publicProperties, compositeState: compositeState)
        and: 'the system returns this yang modelled cm handle'
            1 * mockInventoryPersistence.getYangModelCmHandle('some-cm-handle') >> yangModelCmHandle
        when: 'getting cm handle composite state for a given cm handle id from ncmp service'
            def result = objectUnderTest.getCmHandleCompositeState('some-cm-handle')
        then: 'the result returns the correct data'
            result == compositeState
    }

    def 'Update resource data for pass-through running from dmi using POST #scenario DMI properties.'() {
        given: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', UPDATE,
                '{some-json}', 'application/json')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                    UPDATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.OK) }
    }

    def 'Verify modules and create anchor params.'() {
        given: 'dmi plugin registration return created cm handles'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'service1', dmiModelPlugin: 'service1',
                    dmiDataPlugin: 'service2')
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
            mockDmiPluginRegistration.getCreatedCmHandles() >> [ncmpServiceCmHandle]
        when: 'parse and create cm handle in dmi registration then sync module'
            objectUnderTest.parseAndProcessCreatedCmHandlesInRegistration(mockDmiPluginRegistration)
        then: 'system persists the cm handle state'
            1 * mockLcmEventsCmHandleStateHandler.initiateStateAdvised(_) >> {
                args -> {
                          def cmHandleStatePerCmHandle = (args[0] as Collection)
                          cmHandleStatePerCmHandle.each {
                            assert it.id == 'test-cm-handle-id'
                          }
                    }
            }
    }
    
    def 'Execute cm handle id search'() {
        given: 'valid CmHandleQueryApiParameters input'
            def cmHandleQueryApiParameters = new CmHandleQueryApiParameters()
            def conditionApiProperties = new ConditionApiProperties()
            conditionApiProperties.conditionName = 'hasAllModules'
            conditionApiProperties.conditionParameters = [[moduleName: 'module-name-1']]
            cmHandleQueryApiParameters.cmHandleQueryParameters = [conditionApiProperties]
        and: 'query cm handle method return with a data node list'
            mockCpsCmHandlerQueryService.queryCmHandleIds(
                    spiedJsonObjectMapper.convertToValueType(cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class))
                    >> ['cm-handle-id-1']
        when: 'execute cm handle search is called'
            def result = objectUnderTest.executeCmHandleIdSearch(cmHandleQueryApiParameters)
        then: 'result is the same collection as returned by the CPS Data Service'
            assert result == ['cm-handle-id-1']
    }

    def 'Getting module definitions by module'() {
        when: 'get module definitions is performed with module name'
            objectUnderTest.getModuleDefinitionsByCmHandleAndModule('some-cm-handle', 'some-module', '2021-08-04')
        then: 'ncmp inventory persistence service is invoked once with correct parameters'
            1 * mockInventoryPersistence.getModuleDefinitionsByCmHandleAndModule('some-cm-handle', 'some-module', '2021-08-04')
    }

    def 'Getting module definitions by cm handle id'() {
        when: 'get module definitions is performed with cm handle id'
            objectUnderTest.getModuleDefinitionsByCmHandleId('some-cm-handle')
        then: 'ncmp inventory persistence service is invoked once with correct parameter'
            1 * mockInventoryPersistence.getModuleDefinitionsByCmHandleId('some-cm-handle')
    }

    def 'Execute cm handle search'() {
        given: 'valid CmHandleQueryApiParameters input'
            def cmHandleQueryApiParameters = new CmHandleQueryApiParameters()
            def conditionApiProperties = new ConditionApiProperties()
            conditionApiProperties.conditionName = 'hasAllModules'
            conditionApiProperties.conditionParameters = [[moduleName: 'module-name-1']]
            cmHandleQueryApiParameters.cmHandleQueryParameters = [conditionApiProperties]
        and: 'query cm handle method return with a data node list'
            mockCpsCmHandlerQueryService.queryCmHandles(
                    spiedJsonObjectMapper.convertToValueType(cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class))
                    >> [new NcmpServiceCmHandle(cmHandleId: 'cm-handle-id-1')]
        when: 'execute cm handle search is called'
            def result = objectUnderTest.executeCmHandleSearch(cmHandleQueryApiParameters)
        then: 'result is the same collection as returned by the CPS Data Service'
            assert result.stream().map(d -> d.cmHandleId).collect(Collectors.toSet()) == ['cm-handle-id-1'] as Set
    }

    def 'Set Cm Handle Data Sync Enabled Flag where data sync flag is  #scenario'() {
        given: 'an existing cm handle composite state'
            def compositeState = new CompositeState(cmHandleState: CmHandleState.READY, dataSyncEnabled: initialDataSyncEnabledFlag,
                dataStores: CompositeState.DataStores.builder()
                    .operationalDataStore(CompositeState.Operational.builder()
                        .dataStoreSyncState(initialDataSyncState)
                        .build()).build())
        and: 'get cm handle state returns the composite state for the given cm handle id'
            mockInventoryPersistence.getCmHandleState('some-cm-handle-id') >> compositeState
        when: 'set data sync enabled is called with the data sync enabled flag set to #dataSyncEnabledFlag'
            objectUnderTest.setDataSyncEnabled('some-cm-handle-id', dataSyncEnabledFlag)
        then: 'the data sync enabled flag is set to #dataSyncEnabled'
            compositeState.dataSyncEnabled == dataSyncEnabledFlag
        and: 'the data store sync state is set to #expectedDataStoreSyncState'
            compositeState.dataStores.operationalDataStore.dataStoreSyncState == expectedDataStoreSyncState
        and: 'the cps data service to delete data nodes is invoked the expected number of times'
            deleteDataNodeExpectedNumberOfInvocation * mockCpsDataService.deleteDataNode(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'some-cm-handle-id', '/netconf-state', _)
        and: 'the inventory persistence service to update node leaves is called with the correct values'
            saveCmHandleStateExpectedNumberOfInvocations * mockInventoryPersistence.saveCmHandleState('some-cm-handle-id', compositeState)
        where: 'the following data sync enabled flag is used'
            scenario                                              | dataSyncEnabledFlag | initialDataSyncEnabledFlag | initialDataSyncState               || expectedDataStoreSyncState         | deleteDataNodeExpectedNumberOfInvocation | saveCmHandleStateExpectedNumberOfInvocations
            'enabled'                                             | true                | false                      | DataStoreSyncState.NONE_REQUESTED  || DataStoreSyncState.UNSYNCHRONIZED  | 0                                        | 1
            'disabled'                                            | false               | true                       | DataStoreSyncState.UNSYNCHRONIZED  || DataStoreSyncState.NONE_REQUESTED  | 0                                        | 1
            'disabled where sync-state is currently SYNCHRONIZED' | false               | true                       | DataStoreSyncState.SYNCHRONIZED    || DataStoreSyncState.NONE_REQUESTED  | 1                                        | 1
            'is set to existing flag state'                       | true                | true                       | DataStoreSyncState.UNSYNCHRONIZED  || DataStoreSyncState.UNSYNCHRONIZED  | 0                                        | 0
    }

    def 'Set cm Handle Data Sync Enabled flag with following cm handle not in ready state exception' () {
        given: 'a cm handle composite state'
            def compositeState = new CompositeState(cmHandleState: CmHandleState.ADVISED, dataSyncEnabled: false)
        and: 'get cm handle state returns the composite state for the given cm handle id'
            mockInventoryPersistence.getCmHandleState('some-cm-handle-id') >> compositeState
        when: 'set data sync enabled is called with the data sync enabled flag set to true'
            objectUnderTest.setDataSyncEnabled('some-cm-handle-id', true)
        then: 'the expected exception is thrown'
            thrown(CpsException)
        and: 'the inventory persistence service to update node leaves is not invoked'
            0 * mockInventoryPersistence.saveCmHandleState(_, _)
    }

    def 'Get all cm handle IDs by DMI plugin identifier.' () {
        given: 'cm handle queries service returns cm handles'
            1 * mockCmHandleQueries.getCmHandleIdsByDmiPluginIdentifier('some-dmi-plugin-identifier') >> ['cm-handle-1','cm-handle-2']
        when: 'cm handle Ids are requested with dmi plugin identifier'
            def result = objectUnderTest.getAllCmHandleIdsByDmiPluginIdentifier('some-dmi-plugin-identifier')
        then: 'the result size is correct'
            assert result.size() == 2
        and: 'the result returns the correct details'
            assert result.containsAll('cm-handle-1','cm-handle-2')
    }

    def dataStores() {
        CompositeState.DataStores.builder()
            .operationalDataStore(CompositeState.Operational.builder()
                .dataStoreSyncState(DataStoreSyncState.NONE_REQUESTED)
                .lastSyncTime('some-timestamp').build()).build()
    }

    def mockDataNode() {
        mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
    }

    def getDataOperationRequest(datastore) {
        def dataOperationRequest = new DataOperationRequest()
        def dataOperationDefinitions = new ArrayList()
        dataOperationDefinitions.add(getDataOperationDefinition(datastore))
        dataOperationRequest.setDataOperationDefinitions(dataOperationDefinitions)
        return dataOperationRequest
    }

    def getDataOperationDefinition(datastore) {
        def dataOperationDefinition = new DataOperationDefinition()
        dataOperationDefinition.setOperation("read")
        dataOperationDefinition.setOperationId("operational-12")
        dataOperationDefinition.setDatastore(datastore)
        def targetIds = new ArrayList()
        targetIds.add("some-cm-handle")
        dataOperationDefinition.setCmHandleIds(targetIds)
        return dataOperationDefinition
    }
}
