/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
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

import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters
import org.onap.cps.ncmp.api.models.ConditionApiProperties
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.ncmp.api.inventory.sync.ModuleSyncService
import org.onap.cps.spi.model.CmHandleQueryParameters
import org.onap.cps.spi.model.ConditionProperties
import spock.lang.Shared

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.CREATE
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.UPDATE

import org.onap.cps.utils.JsonObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAdminService
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
    def mockCpsAdminService = Mock(CpsAdminService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def nullNetworkCmProxyDataServicePropertyHandler = null
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockModuleSyncService = Mock(ModuleSyncService)
    def mockDmiPluginRegistration = Mock(DmiPluginRegistration)
    def mockCpsCmHandlerQueryService = Mock(NetworkCmProxyCmHandlerQueryService)

    def NO_TOPIC = null
    def NO_REQUEST_ID = null
    @Shared
    def OPTIONS_PARAM = '(a=1,b=2)'
    @Shared
    def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'some-cm-handle-id')

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockCpsDataService, spiedJsonObjectMapper, mockDmiDataOperations,
        mockCpsModuleService, mockCpsAdminService, nullNetworkCmProxyDataServicePropertyHandler, mockInventoryPersistence,
        mockModuleSyncService, mockCpsCmHandlerQueryService)

    def cmHandleXPath = "/dmi-registry/cm-handles[@id='testCmHandle']"

    def dataNode = new DataNode(leaves: ['id': 'some-cm-handle', 'dmi-service-name': 'testDmiService'])

    def 'Write resource data for pass-through running from DMI using POST.'() {
        given: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'write resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                CREATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.CREATED) }
    }

    def 'Write resource data for pass-through running from DMI using an invalid id.'() {
        when: 'write resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('invalid cm handle name',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
        then: 'exception is thrown'
            thrown(DataValidationException.class)
        and: 'DMI is not invoked'
            0 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi(_, _, _, _, _)
    }

    def 'Get resource data for pass-through operational from DMI.'() {
        given: 'get data node is called'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'get resource data from DMI is called'
            mockDmiDataOperations.getResourceDataFromDmi(
                'testCmHandle',
                'testResourceId',
                OPTIONS_PARAM,
                PASSTHROUGH_OPERATIONAL,
                NO_REQUEST_ID,
                NO_TOPIC) >> new ResponseEntity<>('dmi-response', HttpStatus.OK)
        when: 'get resource data operational for cm-handle is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                'testResourceId',
                OPTIONS_PARAM,
                NO_TOPIC,
                NO_REQUEST_ID)
        then: 'DMI returns a json response'
            response == 'dmi-response'
    }

    def 'Get resource data for pass-through running from DMI.'() {
        given: 'cpsDataService returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns valid response and data'
            mockDmiDataOperations.getResourceDataFromDmi('testCmHandle',
                'testResourceId',
                OPTIONS_PARAM,
                PASSTHROUGH_RUNNING,
                NO_REQUEST_ID,
                NO_TOPIC) >> new ResponseEntity<>('{dmi-response}', HttpStatus.OK)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId',
                OPTIONS_PARAM,
                NO_TOPIC,
                NO_REQUEST_ID)
        then: 'get resource data returns expected response'
            response == '{dmi-response}'
    }

    def 'Getting Yang Resources.'() {
        when: 'yang resources is called'
            objectUnderTest.getYangResourcesModuleReferences('some-cm-handle')
        then: 'CPS module services is invoked for the correct dataspace and cm handle'
            1 * mockCpsModuleService.getYangResourcesModuleReferences('NFP-Operational','some-cm-handle')
    }

    def 'Getting Yang Resources with an invalid #scenario.'() {
        when: 'yang resources is called'
            objectUnderTest.getYangResourcesModuleReferences('invalid cm handle with spaces')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'CPS module services is not invoked'
            0 * mockCpsModuleService.getYangResourcesModuleReferences(*_)
    }

    def 'Get a cm handle.'() {
        given: 'the system returns a yang modelled cm handle'
            def dmiServiceName = 'some service name'
            def dmiProperties = [new YangModelCmHandle.Property('Book', 'Romance Novel')]
            def publicProperties = [new YangModelCmHandle.Property('Public Book', 'Public Romance Novel')]
            def compositeState = new CompositeState(cmHandleState: 'ADVISED')
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', dmiServiceName: dmiServiceName,
                dmiProperties: dmiProperties, publicProperties: publicProperties, compositeState: compositeState)
            1 * mockInventoryPersistence.getYangModelCmHandle('some-cm-handle') >> yangModelCmHandle
        when: 'getting cm handle details for a given cm handle id from ncmp service'
            def result = objectUnderTest.getNcmpServiceCmHandle('some-cm-handle')
        then: 'the result returns the correct data'
            result.cmHandleId == 'some-cm-handle'
            result.dmiProperties ==[ Book:'Romance Novel' ]
            result.publicProperties == [ "Public Book":'Public Romance Novel' ]
            result.compositeState.cmHandleState == CmHandleState.ADVISED
    }

    def 'Get a cm handle with an invalid id.'() {
        when: 'getting cm handle details for a given cm handle id with an invalid name'
            objectUnderTest.getNcmpServiceCmHandle('invalid cm handle with spaces')
        then: 'an exception is thrown'
            thrown(DataValidationException)
        and: 'the yang model cm handle retriever is not invoked'
            0 * mockInventoryPersistence.getYangModelCmHandle(*_)
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

    def 'Get cm handle public properties with an invalid id.'() {
        when: 'getting cm handle details for a given cm handle id with an invalid name'
            objectUnderTest.getCmHandlePublicProperties('invalid cm handle with spaces')
        then: 'an exception is thrown'
            thrown(DataValidationException)
        and: 'the yang model cm handle retriever is not invoked'
            0 * mockInventoryPersistence.getYangModelCmHandle(*_)
    }

    def 'Update resource data for pass-through running from dmi using POST #scenario DMI properties.'() {
        given: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', UPDATE,
                '{some-json}', 'application/json')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                UPDATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.OK) }
    }

    def 'Verify modules and create anchor params'() {
        given: 'dmi plugin registration return created cm handles'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'service1', dmiModelPlugin: 'service1',
                dmiDataPlugin: 'service2')
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
            mockDmiPluginRegistration.getCreatedCmHandles() >> [ncmpServiceCmHandle]
        when: 'parse and create cm handle in dmi registration then sync module'
            objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(mockDmiPluginRegistration)
        then: 'validate params for creating anchor and list elements'
            1 * mockCpsDataService.saveListElements('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry', '{"cm-handles":[{"id":"some-cm-handle-id",' +
                '"additional-properties":[],"public-properties":[]}]}', null)
            1 * mockCpsAdminService.createAnchor('NFP-Operational', null,
                'some-cm-handle-id')
    }

    def 'Execute cm handle id search'(){
        given: 'valid CmHandleQueryApiParameters input'
            def cmHandleQueryApiParameters = new CmHandleQueryApiParameters()
            def conditionApiProperties = new ConditionApiProperties()
            conditionApiProperties.conditionName = 'hasAllModules'
            conditionApiProperties.conditionParameters = [[moduleName:'module-name-1']]
            cmHandleQueryApiParameters.cmHandleQueryParameters = [conditionApiProperties]
        and: 'valid CmHandleQueryParameters input'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllModules'
            conditionProperties.conditionParameters = [[moduleName:'module-name-1']]
            cmHandleQueryParameters.cmHandleQueryParameters = [conditionProperties]
        and: 'query cm handle method return with a data node list'
            mockCpsCmHandlerQueryService.queryCmHandles(cmHandleQueryParameters) >> [ new DataNode(leaves: [id:'cm-handle-id-1'] )]
        when: 'execute cm handle search is called'
            def result = objectUnderTest.executeCmHandleIdSearch(cmHandleQueryApiParameters)
        then: 'result is the same collection as returned by the CPS Data Service'
            assert result == ['cm-handle-id-1'] as Set
    }
}
