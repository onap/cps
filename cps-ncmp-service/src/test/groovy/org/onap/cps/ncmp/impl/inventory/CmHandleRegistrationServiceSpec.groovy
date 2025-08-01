/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory

import com.hazelcast.map.IMap
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.exceptions.AlreadyDefinedException
import org.onap.cps.api.exceptions.CpsException
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.ncmp.api.exceptions.DmiRequestException
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.api.inventory.models.UpgradedCmHandles
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandler
import org.onap.cps.ncmp.impl.inventory.trustlevel.TrustLevelManager
import spock.lang.Specification

import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_FOUND
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_ALREADY_EXIST
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_INVALID_ID
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR
import static org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse.Status
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME

class CmHandleRegistrationServiceSpec extends Specification {

    def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'some-cm-handle-id')
    def mockNetworkCmProxyDataServicePropertyHandler = Mock(CmHandleRegistrationServicePropertyHandler)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockCmHandleQueries = Mock(CmHandleQueryService)
    def mockLcmEventsCmHandleStateHandler = Mock(LcmEventsCmHandleStateHandler)
    def mockCpsDataService = Mock(CpsDataService)
    def mockModuleSyncStartedOnCmHandles = Mock(IMap<String, Object>)
    def mockTrustLevelManager = Mock(TrustLevelManager)
    def mockAlternateIdChecker = Mock(AlternateIdChecker)
    def mockCmHandleIdPerAlternateId = Mock(IMap)

    def objectUnderTest = Spy(new CmHandleRegistrationService(
        mockNetworkCmProxyDataServicePropertyHandler, mockInventoryPersistence, mockCpsDataService, mockLcmEventsCmHandleStateHandler,
        mockModuleSyncStartedOnCmHandles, mockTrustLevelManager, mockAlternateIdChecker, mockCmHandleIdPerAlternateId))

    def setup() {
        // always accept all cm handles
        mockAlternateIdChecker.getIdsOfCmHandlesWithRejectedAlternateId(*_) >> []

        // always can find all cm handles in DB
        mockInventoryPersistence.getYangModelCmHandles(_) >> { args -> args[0].collect { new YangModelCmHandle(id:it) } }
    }

    def 'DMI Registration: Create, Update, Delete & Upgrade operations are processed in the right order'() {
        given: 'a registration with operations of all types'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setCreatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-1', publicProperties: ['publicProp1': 'value'], additionalProperties: [:])])
            dmiRegistration.setUpdatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-2', publicProperties: ['publicProp1': 'value'], additionalProperties: [:])])
            dmiRegistration.setRemovedCmHandles(['cmhandle-3'])
            dmiRegistration.setUpgradedCmHandles(new UpgradedCmHandles(cmHandles: ['cmhandle-4', 'cmhandle-5'], moduleSetTag: moduleSetTagForUpgrade))
        and: 'cm handles 2,3 and 4 already exist in the inventory'
            mockInventoryPersistence.getYangModelCmHandles(['cmhandle-2']) >> [new YangModelCmHandle()]
            mockInventoryPersistence.getYangModelCmHandles(['cmhandle-3']) >> [new YangModelCmHandle()]
            mockInventoryPersistence.getYangModelCmHandle('cmhandle-4') >> new YangModelCmHandle(id: 'cmhandle-4', moduleSetTag: '', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        and: 'cm handle 5 also exist but already has the new module set tag (upgrade to)'
            mockInventoryPersistence.getYangModelCmHandle('cmhandle-5') >> new YangModelCmHandle(id: 'cmhandle-5', moduleSetTag: moduleSetTagForUpgrade , compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        and: 'all cm handles are in READY state'
            mockCmHandleQueries.cmHandleHasState(_, CmHandleState.READY) >> true
        and: 'cm handle to be removed is in progress map'
            mockModuleSyncStartedOnCmHandles.containsKey('cmhandle-3') >> true
        when: 'registration is processed'
            def result = objectUnderTest.updateDmiRegistration(dmiRegistration)
        then: 'cm-handles are removed first'
            1 * objectUnderTest.processRemovedCmHandles(*_)
        and: 'de-registered cm handle entry is removed from in progress map'
            1 * mockModuleSyncStartedOnCmHandles.removeAsync('cmhandle-3')
        then: 'updated cm handles are processed by the property handler service'
            1 * objectUnderTest.processUpdatedCmHandles(*_)
            1 * mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(*_) >> [CmHandleRegistrationResponse.createSuccessResponse('cmhandle-2')]
        then: 'cm-handles are upgraded'
            1 * objectUnderTest.processUpgradedCmHandles(*_)
        and: 'result contains the correct cm handles for each operation'
            assert result.createdCmHandles.cmHandle == ['cmhandle-1']
            assert result.updatedCmHandles.cmHandle == ['cmhandle-2']
            assert result.removedCmHandles.cmHandle == ['cmhandle-3']
            assert result.upgradedCmHandles.cmHandle as Set == ['cmhandle-4', 'cmhandle-5'] as Set
        where: 'upgrade with and without module set tag'
            moduleSetTagForUpgrade << ['some tag', '']
    }

    def 'DMI Registration upgrade operation with upgrade node state #scenario'() {
        given: 'a registration with upgrade operation'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setUpgradedCmHandles(new UpgradedCmHandles(cmHandles: ['cmhandle-3'], moduleSetTag: 'some-module-set-tag'))
        and: 'cm handle has the state #cmHandleState'
            mockInventoryPersistence.getYangModelCmHandle('cmhandle-3') >> new YangModelCmHandle(id: 'cmhandle-3', moduleSetTag: '', compositeState: new CompositeState(cmHandleState: cmHandleState))
        when: 'registration is processed'
            def result = objectUnderTest.updateDmiRegistration(dmiRegistration)
        then: 'upgrade operation contains expected error code'
            assert result.upgradedCmHandles[0].status == expectedResponseStatus
        where: 'the following parameters are used'
            scenario    | cmHandleState        || expectedResponseStatus
            'READY'     | CmHandleState.READY  || Status.SUCCESS
            'Not READY' | CmHandleState.LOCKED || Status.FAILURE
    }

    def 'DMI Registration upgrade with exception #scenario'() {
        given: 'a registration with upgrade operation'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setUpgradedCmHandles(new UpgradedCmHandles(cmHandles: ['cmhandle-3'], moduleSetTag: 'some-module-set-tag'))
        and: 'exception while checking cm handle state'
            mockInventoryPersistence.getYangModelCmHandle('cmhandle-3') >> { throw exception }
        when: 'registration is processed'
            def result = objectUnderTest.updateDmiRegistration(dmiRegistration)
        then: 'upgrade operation contains expected error code'
            assert result.upgradedCmHandles.ncmpResponseStatus.code[0] == expectedErrorCode
        where: 'the following parameters are used'
            scenario               | exception                                                                || expectedErrorCode
            'data node not found'  | new DataNodeNotFoundException('some-dataspace-name', 'some-anchor-name') || '100'
            'cm handle is invalid' | new DataValidationException('some error message', 'some error details')  || '110'
    }

    def 'DMI Registration upgrade with exception while updating CM-handle state'() {
        given: 'a registration with upgrade operation'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setUpgradedCmHandles(new UpgradedCmHandles(cmHandles: ['cmhandle-3'], moduleSetTag: 'some-module-set-tag'))
        and: 'cm handle has the state READY'
            mockInventoryPersistence.getYangModelCmHandle('cmhandle-3') >> new YangModelCmHandle(id: 'cmhandle-3', moduleSetTag: '', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        and: 'exception will occur while updating cm handle state to LOCKED for upgrade'
            mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_) >> { throw new RuntimeException() }
        when: 'registration is processed'
            def result = objectUnderTest.updateDmiRegistration(dmiRegistration)
        then: 'upgrade operation contains expected error code'
            assert result.upgradedCmHandles[0].status == Status.FAILURE
            assert result.upgradedCmHandles[0].ncmpResponseStatus == UNKNOWN_ERROR
    }

    def 'Create CM-handle Validation: Registration with valid Service names: #scenario'() {
        given: 'a registration '
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: dmiPlugin, dmiModelPlugin: dmiModelPlugin,
                dmiDataPlugin: dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
        when: 'update registration and sync module is called with correct DMI plugin information'
            objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'create cm handles registration and sync modules is called with the correct plugin information'
            1 * objectUnderTest.processCreatedCmHandles(dmiPluginRegistration, _)
        where:
            scenario                          | dmiPlugin  | dmiModelPlugin | dmiDataPlugin || expectedDmiPluginRegisteredName
            'combined DMI plugin'             | 'service1' | ''             | ''            || 'service1'
            'data & model DMI plugins'        | ''         | 'service1'     | 'service2'    || 'service2'
            'data & model using same service' | ''         | 'service1'     | 'service1'    || 'service1'
    }

    def 'Create CM-handle Validation: Invalid DMI plugin service name with #scenario'() {
        given: 'a registration '
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: dmiPlugin, dmiModelPlugin: dmiModelPlugin,
                dmiDataPlugin: dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
        when: 'registration is called with incorrect DMI plugin information'
            objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'a DMI Request Exception is thrown with correct message details'
            def exceptionThrown = thrown(DmiRequestException.class)
            assert exceptionThrown.getMessage().contains(expectedMessageDetails)
        and: 'registration is not called'
            0 * objectUnderTest.processCreatedCmHandles(*_)
        where:
            scenario                         | dmiPlugin  | dmiModelPlugin | dmiDataPlugin || expectedMessageDetails
            'empty DMI plugins'              | ''         | ''             | ''            || 'No DMI plugin service names'
            'blank DMI plugins'              | ' '        | ' '            | ' '           || 'No DMI plugin service names'
            'null DMI plugins'               | null       | null           | null          || 'No DMI plugin service names'
            'all DMI plugins'                | 'service1' | 'service2'     | 'service3'    || 'Cannot register combined plugin service name and other service names'
            '(combined)DMI and Data Plugin'  | 'service1' | ''             | 'service2'    || 'Cannot register combined plugin service name and other service names'
            '(combined)DMI and model Plugin' | 'service1' | 'service2'     | ''            || 'Cannot register combined plugin service name and other service names'
            'only model DMI plugin'          | ''         | 'service1'     | ''            || 'Cannot register just a Data or Model plugin service name'
            'only data DMI plugin'           | ''         | ''             | 'service1'    || 'Cannot register just a Data or Model plugin service name'
    }

    def 'Create CM-Handle Successfully: #scenario.'() {
        given: 'a registration without cm-handle properties'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiPluginRegistration.createdCmHandles = [new NcmpServiceCmHandle(cmHandleId: 'cmhandle', additionalProperties: additionalProperties, publicProperties: publicProperties)]
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'a successful response is received'
            response.createdCmHandles.size() == 1
            with(response.createdCmHandles[0]) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle'
            }
        and: 'state handler is invoked with the expected parameters'
            1 * mockLcmEventsCmHandleStateHandler.initiateStateAdvised(_) >> {
                args ->  {
                        def yangModelCmHandles = args[0]
                        assert yangModelCmHandles.id == ['cmhandle']
                        assert yangModelCmHandles.dmiServiceName == ['my-server']
                    }
            }
        where:
            scenario                                 | additionalProperties     | publicProperties               || expectedAdditionalProperties               | expectedPublicProperties
            'with additional & public properties'    | ['dmi-key': 'dmi-value'] | ['public-key': 'public-value'] || '[{"name":"dmi-key","value":"dmi-value"}]' | '[{"name":"public-key","value":"public-value"}]'
            'with only public properties'            | [:]                      | ['public-key': 'public-value'] || [:]                                        | '[{"name":"public-key","value":"public-value"}]'
            'with only dmi properties'               | ['dmi-key': 'dmi-value'] | [:]                            || '[{"name":"dmi-key","value":"dmi-value"}]' | [:]
            'without additional & public properties' | [:]                      | [:]                            || [:]                                        | [:]
    }

    def 'Add CM-Handle #scenario.'() {
        given: ' registration details for one cm handles'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                createdCmHandles:[new NcmpServiceCmHandle(cmHandleId: 'ch-1', registrationTrustLevel: registrationTrustLevel)])
        when: 'registration is updated'
            objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'trustLevel is set for the created cm-handle'
            1 * mockTrustLevelManager.registerCmHandles(expectedMapping)
        where:
            scenario                 | registrationTrustLevel || expectedMapping
            'with trusted cm handle' | TrustLevel.COMPLETE    || [ 'ch-1' : TrustLevel.COMPLETE ]
            'without trust level'    | null                   || [ 'ch-1' : null ]
    }

    def 'Create CM-Handle Multiple Requests: All cm-handles creation requests are processed with some failures'() {
        given: 'a registration with three cm-handles to be created'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                    createdCmHandles: [new NcmpServiceCmHandle(cmHandleId: 'cmhandle1'),
                                       new NcmpServiceCmHandle(cmHandleId: 'cmhandle2'),
                                       new NcmpServiceCmHandle(cmHandleId: 'cmhandle3')])
        and: 'cm-handle creation is successful for 1st and 3rd; failed for 2nd'
            def xpath = "somePathWithId[@id='cmhandle2']"
            mockLcmEventsCmHandleStateHandler.initiateStateAdvised(*_) >> { throw AlreadyDefinedException.forDataNodes([xpath], 'some-context') }
        when: 'registration is updated to create cm-handles'
            def response = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'a response is received for all cm-handles'
            response.createdCmHandles.size() == 1
        and: 'all cm-handles creation fails'
            response.createdCmHandles.each {
                assert it.cmHandle == 'cmhandle2'
                assert it.status == Status.FAILURE
                assert it.ncmpResponseStatus == CM_HANDLE_ALREADY_EXIST
                assert it.errorText == 'cm-handle already exists'
            }
    }

    def 'Create CM-Handle Error Handling: Registration fails: #scenario'() {
        given: 'a registration without cm-handle properties'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiPluginRegistration.createdCmHandles = [new NcmpServiceCmHandle(cmHandleId: 'cmhandle')]
        and: 'cm-handler registration fails: #scenario'
            mockLcmEventsCmHandleStateHandler.initiateStateAdvised(*_) >> { throw exception }
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'a failure response is received'
            response.createdCmHandles.size() == 1
            with(response.createdCmHandles[0]) {
                assert it.status == Status.FAILURE
                assert it.cmHandle ==  'cmhandle'
                assert it.ncmpResponseStatus == expectedError
                assert it.errorText == expectedErrorText
            }
        where:
            scenario                                        | exception                                                                      || expectedError           | expectedErrorText
            'cm-handle already exist'                       | AlreadyDefinedException.forDataNodes(["path[@id='cmhandle']"], 'some-context') || CM_HANDLE_ALREADY_EXIST | 'cm-handle already exists'
            'unknown exception while registering cm-handle' | new RuntimeException('Failed')                                                 || UNKNOWN_ERROR           | 'Failed'
    }

    def 'Update CM-Handle: Update Operation Response is added to the response'() {
        given: 'a registration to update CmHandles'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server', updatedCmHandles: [{}])
        and: 'cm-handle updates can be processed successfully'
            def updateOperationResponse = [CmHandleRegistrationResponse.createSuccessResponse('cm-handle-1'),
                                           CmHandleRegistrationResponse.createFailureResponse('cm-handle-2', new Exception("Failed")),
                                           CmHandleRegistrationResponse.createFailureResponse('cm-handle-3', CM_HANDLES_NOT_FOUND),
                                           CmHandleRegistrationResponse.createFailureResponse('cm handle 4', CM_HANDLE_INVALID_ID)]
            mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(_) >> updateOperationResponse
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'the response contains updateOperationResponse'
            assert response.updatedCmHandles.size() == 4
            assert response.updatedCmHandles.containsAll(updateOperationResponse)
    }

    def 'Remove CmHandle Successfully'() {
        given: 'a registration update to delete a cm handle'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server', removedCmHandles: ['cmhandle'])
        when: 'the registration is updated'
            def response = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'the cmHandle state is set to "DELETING"'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_) >> { args -> args[0].values()[0] == CmHandleState.DELETING }
        then: 'method to delete anchors is called once'
            1 * mockInventoryPersistence.deleteAnchors(_)
        and: 'method to delete relevant list/list element is called once'
            1 * mockInventoryPersistence.deleteDataNodes(_)
        and: 'successful response is received'
            assert response.removedCmHandles.size() == 1
            with(response.removedCmHandles[0]) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle'
            }
        and: 'the cmHandle state is updated to "DELETED"'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_) >>  { args -> args[0].values()[0] == CmHandleState.DELETED }
    }

    def 'Remove CmHandle: Partial Success'() {
        given: 'a registration with three cm-handles to be deleted'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle1', 'cmhandle2', 'cmhandle3'])
        and: 'cm handles to be deleted in the progress map'
            mockModuleSyncStartedOnCmHandles.containsKey("cmhandle1") >> true
            mockModuleSyncStartedOnCmHandles.containsKey("cmhandle3") >> true
        and: 'delete fails for batch. Retry only fails for and cm handle 2'
            mockInventoryPersistence.deleteDataNodes(_) >> { throw new RuntimeException("Batch Failed") }
                                                        >> { /* cm handle 1 is OK */ }
                                                        >> { throw new RuntimeException("Cm handle 2 Failed")}
                                                        >> { /* cm handle 3 is OK */ }
        when: 'registration is updated to delete cmhandles'
            def response = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'the cmHandle states are all updated to "DELETING"'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch({ assert it.every { entry -> entry.value == CmHandleState.DELETING } })
        and: 'a response is received for all cm-handles'
            response.removedCmHandles.size() == 3
        and: 'successfully de-registered cm handle 1 is removed from in progress map'
            1 * mockModuleSyncStartedOnCmHandles.removeAsync('cmhandle1')
        and: 'successfully de-registered cm handle 3 is removed from in progress map even though it was already being removed'
            1 * mockModuleSyncStartedOnCmHandles.removeAsync('cmhandle3')
        and: 'failed de-registered cm handle entries should NOT be removed from in progress map'
            0 * mockModuleSyncStartedOnCmHandles.removeAsync('cmhandle2')
        and: '1st and 3rd cm-handle deletes successfully'
            with(response.removedCmHandles[0]) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle1'
            }
            with(response.removedCmHandles[2]) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle3'
            }
        and: '2nd cm-handle deletion fails'
            with(response.removedCmHandles[1]) {
                assert it.status == Status.FAILURE
                assert it.ncmpResponseStatus == UNKNOWN_ERROR
                assert it.errorText == 'Cm handle 2 Failed'
                assert it.cmHandle == 'cmhandle2'
            }
        and: 'the cmHandle state is updated to DELETED for 1st and 3rd'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch({
                assert it.size() == 2
                assert it.every { entry -> entry.value == CmHandleState.DELETED }
            })
    }

    def 'Remove CmHandle Error Handling: #scenario'() {
        given: 'a registration'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server', removedCmHandles: ['cmhandle'])
        and: 'cm-handle deletion fails on batch'
            mockInventoryPersistence.deleteDataNodes(_) >> { throw deleteListElementException }
        and: 'cm-handle deletion fails on individual delete'
            mockInventoryPersistence.deleteDataNode(_) >> { throw deleteListElementException }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'a failure response is received'
            assert response.removedCmHandles.size() == 1
            with(response.removedCmHandles[0]) {
                assert it.status == Status.FAILURE
                assert it.cmHandle == 'cmhandle'
                assert it.ncmpResponseStatus == expectedError
                assert it.errorText == expectedErrorText
            }
        and: 'the cm handle state is not updated to "DELETED"'
            0 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_, CmHandleState.DELETED)
        where:
        scenario                     | deleteListElementException                || expectedError        | expectedErrorText
        'cm-handle does not exist'   | new DataNodeNotFoundException('', '', '') || CM_HANDLES_NOT_FOUND | 'cm handle reference(s) not found'
        'cm-handle has invalid name' | new DataValidationException('', '')       || CM_HANDLE_INVALID_ID | 'cm handle reference has an invalid character(s) in id'
        'an unexpected exception'    | new RuntimeException('Failed')            || UNKNOWN_ERROR        | 'Failed'
    }

    def 'Set Cm Handle Data Sync Enabled Flag where data sync flag is #scenario'() {
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

    def 'Adding and removing id - alternate id mappings with #scenario.'() {
        given: 'a yang model cm handle with #scenario'
            def yangModelCmHandle = new YangModelCmHandle(id:'ch-1', alternateId: alternateId)
        when: 'it is added to the cache'
            objectUnderTest.addAlternateIdsToCache([yangModelCmHandle])
        then: 'it is added to the cache with the expected key'
            1 * mockCmHandleIdPerAlternateId.putAll([(expectedKey):'ch-1'])
        when: 'it is removed from the cache'
            objectUnderTest.removeAlternateIdsFromCache([yangModelCmHandle])
        then: 'the correct key is deleted from the cache'
            1 * mockCmHandleIdPerAlternateId.delete(expectedKey)
        where: 'the following alternate ids are used'
            scenario             | alternateId || expectedKey
            'with alternate id'  | 'alt-1'     || 'alt-1'
            'blank alternate id' | ''          || 'ch-1'
            'no alternate id'    | null        || 'ch-1'
    }

}
