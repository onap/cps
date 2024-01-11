/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl

import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevelManager
import org.onap.cps.ncmp.api.models.UpgradedCmHandles

import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_FOUND
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_ALREADY_EXIST
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_INVALID_ID
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR
import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.Status

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService
import org.onap.cps.ncmp.api.impl.events.lcm.LcmEventsCmHandleStateHandler
import org.onap.cps.ncmp.api.impl.exception.DmiRequestException
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

class NetworkCmProxyDataServiceImplRegistrationSpec extends Specification {

    @Shared
    def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'some-cm-handle-id')

    def mockCpsModuleService = Mock(CpsModuleService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def mockNetworkCmProxyDataServicePropertyHandler = Mock(NetworkCmProxyDataServicePropertyHandler)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockCmHandleQueries = Mock(CmHandleQueries)
    def stubbedNetworkCmProxyCmHandlerQueryService = Stub(NetworkCmProxyCmHandleQueryService)
    def mockLcmEventsCmHandleStateHandler = Mock(LcmEventsCmHandleStateHandler)
    def mockCpsDataService = Mock(CpsDataService)
    def mockModuleSyncStartedOnCmHandles = Mock(IMap<String, Object>)
    def trustLevelPerDmiPlugin = [:]
    def mockTrustLevelManager = Mock(TrustLevelManager)
    def alternateIdPerCmHandle = new HashMap<String, String>()
    def cmHandlePerAlternateId = new HashMap<String, String>()
    def objectUnderTest = getObjectUnderTest()

    def static cmHandlesWithAllPopulatedAlternateIds = [new NcmpServiceCmHandle(cmHandleId: 'cmhandle1', alternateId: 'my-alternate-id-1'),
             new NcmpServiceCmHandle(cmHandleId: 'cmhandle2', alternateId: 'my-alternate-id-2')]

    def static cmHandlesWithSomePopulatedAlternateIds = [new NcmpServiceCmHandle(cmHandleId: 'cmhandle1', alternateId: 'my-alternate-id-1'),
             new NcmpServiceCmHandle(cmHandleId: 'cmhandle2')]

    def 'DMI Registration: Create, Update, Delete & Upgrade operations are processed in the right order'() {
        given: 'a registration with operations of all types'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setCreatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-1', publicProperties: ['publicProp1': 'value'], dmiProperties: [:])])
            dmiRegistration.setUpdatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-2', publicProperties: ['publicProp1': 'value'], dmiProperties: [:])])
            dmiRegistration.setRemovedCmHandles(['cmhandle-2'])
            dmiRegistration.setUpgradedCmHandles(new UpgradedCmHandles(cmHandles: ['cmhandle-3'], moduleSetTag: 'some-module-set-tag'))
        and: 'cm handles are persisted'
            mockInventoryPersistence.getYangModelCmHandles(['cmhandle-2']) >> [new YangModelCmHandle()]
        and: 'cm handle is in READY state'
            mockCmHandleQueries.cmHandleHasState('cmhandle-3', CmHandleState.READY) >> true
        when: 'registration is processed'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiRegistration)
        then: 'cm-handles are removed first'
            1 * objectUnderTest.parseAndProcessDeletedCmHandlesInRegistration(*_)
        and: 'de-registered cm handle entry is removed from in progress map'
            1 * mockModuleSyncStartedOnCmHandles.remove('cmhandle-2')
        then: 'cm-handles are created'
            1 * objectUnderTest.parseAndProcessCreatedCmHandlesInRegistration(*_)
        then: 'cm-handles are updated'
            1 * mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(*_)
        then: 'cm-handles are upgraded'
            1 * objectUnderTest.parseAndProcessUpgradedCmHandlesInRegistration(*_)
    }

    def 'DMI Registration upgrade operation with upgrade node state #scenario'() {
        given: 'a registration with upgrade operation'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setUpgradedCmHandles(new UpgradedCmHandles(cmHandles: ['cmhandle-3'], moduleSetTag: 'some-module-set-tag'))
        and: 'exception while checking cm handle state'
            mockCmHandleQueries.cmHandleHasState('cmhandle-3', CmHandleState.READY) >> isReady
        when: 'registration is processed'
            def result = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiRegistration)
        then: 'upgrade operation contains expected error code'
            assert result.upgradedCmHandles.status[0] == expectedResponseStatus
        where: 'the following parameters are used'
            scenario    | isReady || expectedResponseStatus
            'READY'     | true    || Status.SUCCESS
            'Not READY' | false   || Status.FAILURE
    }

    def 'DMI Registration upgrade with exception #scenario'() {
        given: 'a registration with upgrade operation'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setUpgradedCmHandles(new UpgradedCmHandles(cmHandles: ['cmhandle-3'], moduleSetTag: 'some-module-set-tag'))
        and: 'exception while checking cm handle state'
            mockCmHandleQueries.cmHandleHasState('cmhandle-3', CmHandleState.READY) >> { throw exception }
        when: 'registration is processed'
            def result = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiRegistration)
        then: 'upgrade operation contains expected error code'
            assert result.upgradedCmHandles.ncmpResponseStatus.code[0] == expectedErrorCode
        where: 'the following parameters are used'
            scenario               | exception                                                                || expectedErrorCode
            'data node not found'  | new DataNodeNotFoundException('some-dataspace-name', 'some-anchor-name') || '100'
            'cm handle is invalid' | new DataValidationException('some error message', 'some error details')  || '110'
    }

    def 'DMI Registration: Response from all operations types are in response'() {
        given: 'a registration with operations of all three types'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setCreatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-1', publicProperties: ['publicProp1': 'value'], dmiProperties: [:])])
            dmiRegistration.setUpdatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-2', publicProperties: ['publicProp1': 'value'], dmiProperties: [:])])
            dmiRegistration.setRemovedCmHandles(['cmhandle-2'])
        and: 'update cm-handles can be processed successfully'
            def updateResponses = [CmHandleRegistrationResponse.createSuccessResponse('cmhandle-2')]
            mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(*_) >> updateResponses
        and: 'create cm-handles can be processed successfully'
            def createdResponses = [CmHandleRegistrationResponse.createSuccessResponse('cmhandle-1')]
            objectUnderTest.parseAndProcessCreatedCmHandlesInRegistration(*_) >> createdResponses
        and: 'delete cm-handles can be processed successfully'
            def removeResponses = [CmHandleRegistrationResponse.createSuccessResponse('cmhandle-3')]
            objectUnderTest.parseAndProcessDeletedCmHandlesInRegistration(*_) >> removeResponses
        when: 'registration is processed'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiRegistration)
        then: 'response has values from all operations'
            response.removedCmHandles == removeResponses
            response.createdCmHandles == createdResponses
            response.updatedCmHandles == updateResponses
    }

    def 'Create CM-handle Validation: Registration with valid Service names: #scenario'() {
        given: 'a registration '
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: dmiPlugin, dmiModelPlugin: dmiModelPlugin,
                dmiDataPlugin: dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
        when: 'update registration and sync module is called with correct DMI plugin information'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'create cm handles registration and sync modules is called with the correct plugin information'
            1 * objectUnderTest.parseAndProcessCreatedCmHandlesInRegistration(dmiPluginRegistration)
        and: 'dmi is added to the dmi trustLevel map'
            assert trustLevelPerDmiPlugin.size() == 1
            assert trustLevelPerDmiPlugin.containsKey(expectedDmiPluginRegisteredName)
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
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a DMI Request Exception is thrown with correct message details'
            def exceptionThrown = thrown(DmiRequestException.class)
            assert exceptionThrown.getMessage().contains(expectedMessageDetails)
        and: 'registration is not called'
            0 * objectUnderTest.parseAndProcessCreatedCmHandlesInRegistration(dmiPluginRegistration)
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
            dmiPluginRegistration.createdCmHandles = [new NcmpServiceCmHandle(cmHandleId: 'cmhandle', dmiProperties: dmiProperties, publicProperties: publicProperties)]
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
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
            scenario                          | dmiProperties            | publicProperties               || expectedDmiProperties                      | expectedPublicProperties
            'with dmi & public properties'    | ['dmi-key': 'dmi-value'] | ['public-key': 'public-value'] || '[{"name":"dmi-key","value":"dmi-value"}]' | '[{"name":"public-key","value":"public-value"}]'
            'with only public properties'     | [:]                      | ['public-key': 'public-value'] || [:]                                        | '[{"name":"public-key","value":"public-value"}]'
            'with only dmi properties'        | ['dmi-key': 'dmi-value'] | [:]                            || '[{"name":"dmi-key","value":"dmi-value"}]' | [:]
            'without dmi & public properties' | [:]                      | [:]                            || [:]                                        | [:]
    }

    def 'Add CM-Handle to trustLevelPerCmHandle Successfully with: #scenario.'() {
        given: 'a registration with trustLevel and populated cache'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiPluginRegistration.createdCmHandles = [new NcmpServiceCmHandle(cmHandleId: 'ch-1', registrationTrustLevel: TrustLevel.NONE),
                                                      new NcmpServiceCmHandle(cmHandleId: cmHandleId, registrationTrustLevel: registrationTrustLevel)]
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a successful response is received'
            assert response.createdCmHandles.size() == expectedNumberOfCreatedCmHandles
        and: 'trustLevel is set for the created cm-handle'
            1 * mockTrustLevelManager.handleInitialRegistrationOfTrustLevels(_)
        where:
            scenario                                 | cmHandleId | registrationTrustLevel || expectedNumberOfCreatedCmHandles
            'new trusted cm handle'                  | 'ch-new'   | TrustLevel.COMPLETE    || 2
            'existing cm handle without trust level' | 'ch-1'     | null                   || 1
            'new cm handle without trust level'      | 'ch-new'   | null                   || 2
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
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
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
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
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
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the response contains updateOperationResponse'
            assert response.updatedCmHandles.size() == 4
            assert response.updatedCmHandles.containsAll(updateOperationResponse)
    }

    def 'Remove CmHandle Successfully: #scenario'() {
        given: 'a registration'
            addPersistedYangModelCmHandles(['cmhandle'])
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle'])
        and: '#scenario'
            mockCpsModuleService.deleteSchemaSetsWithCascade(_, ['cmhandle']) >>
                { if (!schemaSetExist) { throw new SchemaSetNotFoundException("", "") } }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the cmHandle state is updated to "DELETING"'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_)
        and: 'method to delete relevant schema set is called once'
            1 * mockInventoryPersistence.deleteSchemaSetsWithCascade(_)
        and: 'method to delete relevant list/list element is called once'
            1 * mockInventoryPersistence.deleteDataNodes(_)
        and: 'successful response is received'
            assert response.removedCmHandles.size() == 1
            with(response.removedCmHandles[0]) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle'
            }
        and: 'the cmHandle state is updated to "DELETED"'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_)
        where:
            scenario                                            | schemaSetExist
            'schema-set exists and can be deleted successfully' | true
            'schema-set does not exist'                         | false
    }

    def 'Remove CmHandle: Partial Success'() {
        given: 'some unique yang model cm handles'
            addPersistedYangModelCmHandles(['cmhandle1', 'cmhandle2', 'cmhandle3'])
        and: 'a registration with three cm-handles to be deleted'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle1', 'cmhandle2', 'cmhandle3'])
        and: 'cm-handle deletion fails on batch'
            mockInventoryPersistence.deleteDataNodes(_) >> { throw new RuntimeException("Failed") }
        and: 'cm-handle deletion is successful for 1st and 3rd; failed for 2nd'
            mockInventoryPersistence.deleteDataNode("/dmi-registry/cm-handles[@id='cmhandle2']") >> { throw new RuntimeException("Failed") }
        when: 'registration is updated to delete cmhandles'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the cmHandle states are all updated to "DELETING"'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch({ assert it.every { entry -> entry.value == CmHandleState.DELETING } })
        and: 'a response is received for all cm-handles'
            response.removedCmHandles.size() == 3
        and: 'successfully de-registered cm handle 1 is removed from in progress map'
            1 * mockModuleSyncStartedOnCmHandles.remove('cmhandle1')
        and: 'successfully de-registered cm handle 3 is removed from in progress map even though it was already being removed'
            1 * mockModuleSyncStartedOnCmHandles.remove('cmhandle3') >> 'already in progress'
        and: 'failed de-registered cm handle entries should not be removed from in progress map'
            0 * mockModuleSyncStartedOnCmHandles.remove('cmhandle2')
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
                assert it.errorText == 'Failed'
                assert it.cmHandle == 'cmhandle2'
            }
        and: 'the cmHandle state is updated to DELETED for 1st and 3rd'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch({
                assert it.size() == 2
                assert it.every { entry -> entry.value == CmHandleState.DELETED }
            })
    }

    def 'Remove CmHandle Error Handling: Schema Set Deletion failed'() {
        given: 'a registration'
            addPersistedYangModelCmHandles(['cmhandle'])
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle'])
        and: 'schema set batch deletion failed with unknown error'
            mockInventoryPersistence.deleteSchemaSetsWithCascade(_) >> { throw new RuntimeException('Failed') }
        and: 'schema set single deletion failed with unknown error'
            mockInventoryPersistence.deleteSchemaSetWithCascade(_) >> { throw new RuntimeException('Failed') }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'cm-handle is not deleted'
            0 * mockInventoryPersistence.deleteDataNodes(_)
        and: 'the cmHandle state is not updated to "DELETED"'
            0 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch([yangModelCmHandle: CmHandleState.DELETED])
        and: 'a failure response is received'
            assert response.removedCmHandles.size() == 1
            with(response.removedCmHandles[0]) {
                assert it.status == Status.FAILURE
                assert it.cmHandle == 'cmhandle'
                assert it.errorText == 'Failed'
                assert it.ncmpResponseStatus == UNKNOWN_ERROR
            }
    }

    def 'Remove CmHandle Error Handling: #scenario'() {
        given: 'a registration'
            addPersistedYangModelCmHandles(['cmhandle'])
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle'])
        and: 'cm-handle deletion fails on batch'
            mockInventoryPersistence.deleteDataNodes(_) >> { throw deleteListElementException }
        and: 'cm-handle deletion fails on individual delete'
            mockInventoryPersistence.deleteDataNode(_) >> { throw deleteListElementException }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
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
        scenario                     | cmHandleId             | deleteListElementException                || expectedError        | expectedErrorText
        'cm-handle does not exist'   | 'cmhandle'             | new DataNodeNotFoundException('', '', '') || CM_HANDLES_NOT_FOUND | 'cm handle id(s) not found'
        'cm-handle has invalid name' | 'cm handle with space' | new DataValidationException('', '')       || CM_HANDLE_INVALID_ID | 'cm-handle has an invalid character(s) in id'
        'an unexpected exception'    | 'cmhandle'             | new RuntimeException('Failed')            || UNKNOWN_ERROR        | 'Failed'
    }

    def 'Adding data to alternate id caches: #scenario'() {
        given: 'a registration with three CM Handles to be created'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                    createdCmHandles: dmiPluginRegistrations)
        and: 'the caches are empty initially'
            assert alternateIdPerCmHandle.size() == 0
            assert cmHandlePerAlternateId.size() == 0
        when: 'the DMI plugin registration happens'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the caches have the correct number of entries'
            assert alternateIdPerCmHandle.size() == cacheSize
            assert cmHandlePerAlternateId.size() == cacheSize
        where:
            scenario                   | dmiPluginRegistrations                 || cacheSize
            'an alternate id for each' | cmHandlesWithAllPopulatedAlternateIds  || 2
            'an alternate id for one'  | cmHandlesWithSomePopulatedAlternateIds || 1
    }

    def getObjectUnderTest() {
        return Spy(new NetworkCmProxyDataServiceImpl(spiedJsonObjectMapper, mockDmiDataOperations,
                mockNetworkCmProxyDataServicePropertyHandler, mockInventoryPersistence, mockCmHandleQueries,
                stubbedNetworkCmProxyCmHandlerQueryService, mockLcmEventsCmHandleStateHandler, mockCpsDataService,
                mockModuleSyncStartedOnCmHandles, trustLevelPerDmiPlugin as Map<String, TrustLevel>, mockTrustLevelManager, alternateIdPerCmHandle, cmHandlePerAlternateId))
    }

    def addPersistedYangModelCmHandles(ids) {
        def yangModelCmHandles = ids.collect { new YangModelCmHandle(id:it) }
        mockInventoryPersistence.getYangModelCmHandles(ids) >> yangModelCmHandles
    }
}
