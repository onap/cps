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

package org.onap.cps.ncmp.impl.inventory

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryApiParameters
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.ConditionApiProperties
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.spi.model.ConditionProperties
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class NetworkCmProxyInventoryFacadeSpec extends Specification {

    def mockCmHandleRegistrationService = Mock(CmHandleRegistrationService)
    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockParameterizedCmHandleQueryService = Mock(ParameterizedCmHandleQueryService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def trustLevelPerCmHandle = [:]

    def objectUnderTest = new NetworkCmProxyInventoryFacade(mockCmHandleRegistrationService, mockCmHandleQueryService, mockParameterizedCmHandleQueryService, mockInventoryPersistence, spiedJsonObjectMapper, trustLevelPerCmHandle)

    def 'Update DMI Registration'() {
        given: 'an (updated) dmi plugin registration'
            def dmiPluginRegistration = Mock(DmiPluginRegistration)
        when: 'the registration is submitted '
           objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the call is delegated to the cm handle registration service'
            1 * mockCmHandleRegistrationService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
    }

    def 'Execute cm handle id search for inventory'() {
        given: 'a ConditionApiProperties object'
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllProperties'
            conditionProperties.conditionParameters = [ [ 'some-key' : 'some-value' ] ]
            def cmHandleQueryServiceParameters = new CmHandleQueryServiceParameters()
            cmHandleQueryServiceParameters.cmHandleQueryParameters = [conditionProperties] as List<ConditionProperties>
        and: 'the system returns an set of cmHandle ids'
            mockParameterizedCmHandleQueryService.queryCmHandleIdsForInventory(*_) >> [ 'cmHandle1', 'cmHandle2' ]
        when: 'executing the search'
            def result = objectUnderTest.executeParameterizedCmHandleIdSearch(cmHandleQueryServiceParameters)
        then: 'the result returns the correct 2 elements'
            assert result.size() == 2
            assert result.contains('cmHandle1')
            assert result.contains('cmHandle2')
    }

    def 'Get all cm handle IDs by DMI plugin identifier.' () {
        given: 'cm handle queries service returns cm handles'
            1 * mockCmHandleQueryService.getCmHandleIdsByDmiPluginIdentifier('some-dmi-plugin-identifier') >> ['cm-handle-1','cm-handle-2']
        when: 'cm handle Ids are requested with dmi plugin identifier'
            def result = objectUnderTest.getAllCmHandleIdsByDmiPluginIdentifier('some-dmi-plugin-identifier')
        then: 'the result size is correct'
            assert result.size() == 2
        and: 'the result returns the correct details'
            assert result.containsAll('cm-handle-1','cm-handle-2')
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
                lockReason: CompositeState.LockReason.builder().lockReasonCategory(LockReasonCategory.MODULE_SYNC_FAILED).details('lock details').build(),
                lastUpdateTime: 'some-timestamp',
                dataSyncEnabled: false,
                dataStores: dataStores())
            def dmiProperties = [new YangModelCmHandle.Property('Book', 'Romance Novel')]
            def publicProperties = [new YangModelCmHandle.Property('Public Book', 'Public Romance Novel')]
            def moduleSetTag = 'some-module-set-tag'
            def alternateId = 'some-alternate-id'
            def yangModelCmHandle = new YangModelCmHandle(id: 'ch-1', dmiServiceName: dmiServiceName, dmiProperties: dmiProperties,
                 publicProperties: publicProperties, compositeState: compositeState, moduleSetTag: moduleSetTag, alternateId: alternateId)
            1 * mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle
        and: 'a trust level for the cm handle in the cache'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        when: 'getting cm handle details for a given cm handle id from ncmp service'
            def result = objectUnderTest.getNcmpServiceCmHandle('ch-1')
        then: 'the result is a ncmpServiceCmHandle'
            assert result.class == NcmpServiceCmHandle.class
        and: 'the cm handle contains the cm handle id'
            assert result.cmHandleId == 'ch-1'
        and: 'the cm handle contains the alternate id'
            assert result.alternateId == 'some-alternate-id'
        and: 'the cm handle contains the module-set-tag'
            assert result.moduleSetTag == 'some-module-set-tag'
        and: 'the cm handle contains the DMI Properties'
            assert result.dmiProperties ==[ Book:'Romance Novel' ]
        and: 'the cm handle contains the public Properties'
            assert result.publicProperties == [ "Public Book":'Public Romance Novel' ]
        and: 'the cm handle contains the cm handle composite state'
            assert result.compositeState == compositeState
        and: 'the cm handle contains the trust level from the cache'
            assert result.currentTrustLevel == TrustLevel.COMPLETE
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
            assert result == [ 'public prop' : 'some public prop' ]
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
            assert result == compositeState
    }

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
        and: 'query cm handle method returns two cm handles'
            mockParameterizedCmHandleQueryService.queryCmHandles(
                spiedJsonObjectMapper.convertToValueType(cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class))
                >> [new NcmpServiceCmHandle(cmHandleId: 'ch-0'), new NcmpServiceCmHandle(cmHandleId: 'ch-1')]
        and: ' a trust level for ch-1'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        when: 'execute cm handle search is called'
            def result = objectUnderTest.executeCmHandleSearch(cmHandleQueryApiParameters)
        then: 'result consists of the two cm handles returned by the CPS Data Service'
            assert result.size() == 2
            assert result[0].cmHandleId == 'ch-0'
            assert result[1].cmHandleId == 'ch-1'
        and: 'only ch-1 has a trust level'
            assert result[0].currentTrustLevel == null
            assert result[1].currentTrustLevel == TrustLevel.COMPLETE
    }

    def 'Set Cm Handle Data Sync flag.'() {
        when: 'setting data sync enabled flag'
            objectUnderTest.setDataSyncEnabled('ch-1',true)
        then: 'call is delegated to the cm handle registration service'
            mockCmHandleRegistrationService.setDataSyncEnabled('ch-1', true)
    }

    def dataStores() {
        CompositeState.DataStores.builder().operationalDataStore(CompositeState.Operational.builder()
                .dataStoreSyncState(DataStoreSyncState.NONE_REQUESTED)
                .lastSyncTime('some-timestamp').build()).build()
    }
}
