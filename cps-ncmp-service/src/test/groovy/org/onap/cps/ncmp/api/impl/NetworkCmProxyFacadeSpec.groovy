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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.ParameterizedCmHandleQueryService
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.CompositeState
import org.onap.cps.ncmp.api.impl.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters
import org.onap.cps.ncmp.api.models.CmResourceAddress
import org.onap.cps.ncmp.api.models.ConditionApiProperties
import org.onap.cps.ncmp.api.models.DataOperationDefinition
import org.onap.cps.ncmp.api.models.DataOperationRequest
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.util.stream.Collectors

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.OperationType.CREATE
import static org.onap.cps.ncmp.api.impl.operations.OperationType.UPDATE

class NetworkCmProxyFacadeSpec extends Specification {

    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def mockParameterizedCmHandleQueryService = Mock(ParameterizedCmHandleQueryService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCmHandleRegistrationService = Mock(CmHandleRegistrationService)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    def NO_TOPIC = null
    def NO_REQUEST_ID = null
    def NO_AUTH_HEADER = null
    def OPTIONS_PARAM = '(a=1,b=2)'

    def objectUnderTest = new NetworkCmProxyFacade(spiedJsonObjectMapper, mockDmiDataOperations, mockParameterizedCmHandleQueryService,
            mockCpsDataService, mockCmHandleRegistrationService, mockInventoryPersistence)

    def cmHandleXPath = "/dmi-registry/cm-handles[@id='testCmHandle']"

    def dataNode = [new DataNode(leaves: ['id': 'some-cm-handle', 'dmi-service-name': 'testDmiService'])]

    def 'Write resource data for pass-through running from DMI using POST.'() {
        given: 'cpsDataService returns valid datanode'
            mockDataNode()
        when: 'write resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json', null)
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                    CREATE, '{some-json}', 'application/json', null)
                >> { new ResponseEntity<>(HttpStatus.CREATED) }
    }

    def 'Get resource data from DMI.'() {
        given: 'cpsDataService returns valid data node'
            mockDataNode()
        and: 'some cm resource address'
            def cmResourceAddress = new CmResourceAddress('some datastore', 'some CM Handle', 'some resource Id')
        and: 'get resource data from DMI is called'
            mockDmiDataOperations.getResourceDataFromDmi(cmResourceAddress, OPTIONS_PARAM, NO_TOPIC, NO_REQUEST_ID, NO_AUTH_HEADER) >>
                    Mono.just(new ResponseEntity<>('dmi-response', HttpStatus.OK))
        when: 'get resource data operational for the given cm resource address is called'
            def response = objectUnderTest.getResourceDataForCmHandle(cmResourceAddress, OPTIONS_PARAM, NO_TOPIC, NO_REQUEST_ID, NO_AUTH_HEADER).block()
        then: 'DMI returns a json response'
            assert response == 'dmi-response'
    }

    def 'Get resource data for operational (cached) data.'() {
        given: 'CPS Data service returns some object(s)'
            mockCpsDataService.getDataNodes(OPERATIONAL.datastoreName, 'testCmHandle', 'testResourceId', FetchDescendantsOption.OMIT_DESCENDANTS) >> ['First Object', 'other Object']
        and: 'a cm resource address for the same datastore, cm handle and resource id'
            def cmResourceAddress = new CmResourceAddress(OPERATIONAL.datastoreName, 'testCmHandle', 'testResourceId')
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataForCmHandle(cmResourceAddress, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'get resource data returns teh first object from the data service'
            assert response == 'First Object'
    }

    def 'Execute (async) data operation for #datastoreName from DMI.'() {
        given: 'cpsDataService returns valid data node'
            def dataOperationRequest = getDataOperationRequest(datastoreName)
        when: 'request resource data for data operation is called'
            objectUnderTest.executeDataOperationForCmHandles('some topic', dataOperationRequest, 'requestId', NO_AUTH_HEADER)
        then: 'request resource data for data operation returns expected response'
            1 * mockDmiDataOperations.requestResourceDataFromDmi('some topic', dataOperationRequest, 'requestId', NO_AUTH_HEADER)
        where: 'the following data stores are used'
            datastoreName << [PASSTHROUGH_RUNNING.datastoreName, PASSTHROUGH_OPERATIONAL.datastoreName]
    }

    def 'Getting Yang Resources.'() {
        when: 'yang resources is called'
            objectUnderTest.getYangResourcesModuleReferences('some-cm-handle')
        then: 'CPS module services is invoked for the correct dataspace and cm handle'
            1 * mockInventoryPersistence.getYangResourcesModuleReferences('some-cm-handle')
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
            def moduleSetTag = 'some-module-set-tag'
            def alternateId = 'some-alternate-id'
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', dmiServiceName: dmiServiceName,
                    dmiProperties: dmiProperties, publicProperties: publicProperties, compositeState: compositeState,
                    moduleSetTag: moduleSetTag, alternateId: alternateId)
            1 * mockInventoryPersistence.getYangModelCmHandle('some-cm-handle') >> yangModelCmHandle
        when: 'getting cm handle details for a given cm handle id from ncmp service'
            def result = objectUnderTest.getNcmpServiceCmHandle('some-cm-handle')
        then: 'the result is a ncmpServiceCmHandle'
            result.class == NcmpServiceCmHandle.class
        and: 'the cm handle contains the cm handle id'
            result.cmHandleId == 'some-cm-handle'
        and: 'the cm handle contains the alternate id'
            result.alternateId == 'some-alternate-id'
        and: 'the cm handle contains the module-set-tag'
            result.moduleSetTag == 'some-module-set-tag'
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
                '{some-json}', 'application/json', NO_AUTH_HEADER)
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                    UPDATE, '{some-json}', 'application/json', NO_AUTH_HEADER)
                >> { new ResponseEntity<>(HttpStatus.OK) }
    }
//TODO: verify belwo test in covered by CmHandleRegistrationServiceSpec
//    def 'Verify modules and create anchor params.'() {
//        given: 'dmi plugin registration return created cm handles'
//            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'service1', dmiModelPlugin: 'service1',
//                    dmiDataPlugin: 'service2')
//            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
//            mockDmiPluginRegistration.getCreatedCmHandles() >> [ncmpServiceCmHandle]
//        and: 'no rejected cm handles because of alternate ids'
//            mockAlternateIdChecker.getIdsOfCmHandlesWithRejectedAlternateId(*_) >> []
//        when: 'parse and create cm handle in dmi registration then sync module'
//            mockDmiPluginRegistration.createdCmHandles = ['test-cm-handle-id']
//            objectUnderTest.processCreatedCmHandles(mockDmiPluginRegistration, new DmiPluginRegistrationResponse())
//        then: 'system persists the cm handle state'
//            1 * mockLcmEventsCmHandleStateHandler.initiateStateAdvised(_) >> {
//                args -> {
//                          def cmHandleStatePerCmHandle = (args[0] as Collection)
//                          cmHandleStatePerCmHandle.each {
//                            assert it.id == 'test-cm-handle-id'
//                          }
//                    }
//            }
//    }

    def 'Execute cm handle id search'() {
        given: 'valid CmHandleQueryApiParameters input'
            def cmHandleQueryApiParameters = new CmHandleQueryApiParameters()
            def conditionApiProperties = new ConditionApiProperties()
            conditionApiProperties.conditionName = 'hasAllModules'
            conditionApiProperties.conditionParameters = [[moduleName: 'module-name-1']]
            cmHandleQueryApiParameters.cmHandleQueryParameters = [conditionApiProperties]
        and: 'query cm handle method return with a data node list'
            mockParameterizedCmHandleQueryService.queryCmHandleIds(
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
            mockParameterizedCmHandleQueryService.queryCmHandles(
                    spiedJsonObjectMapper.convertToValueType(cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class))
                    >> [new NcmpServiceCmHandle(cmHandleId: 'cm-handle-id-1')]
        when: 'execute cm handle search is called'
            def result = objectUnderTest.executeCmHandleSearch(cmHandleQueryApiParameters)
        then: 'result is the same collection as returned by the CPS Data Service'
            assert result.stream().map(d -> d.cmHandleId).collect(Collectors.toSet()) == ['cm-handle-id-1'] as Set
    }

    def 'Set Cm Handle Data Sync flag.'() {
        when: 'setting data sync enabled flag'
            objectUnderTest.setDataSyncEnabled('ch-1',true)
        then: 'call is delegated to the cm handle registration service'
            mockCmHandleRegistrationService.setDataSyncEnabled('ch-1', true)
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
        dataOperationDefinition.setOperation('read')
        dataOperationDefinition.setOperationId('"operational-12')
        dataOperationDefinition.setDatastore(datastore)
        def targetIds = new ArrayList()
        targetIds.add('some-cm-handle')
        dataOperationDefinition.setCmHandleIds(targetIds)
        return dataOperationDefinition
    }
}
