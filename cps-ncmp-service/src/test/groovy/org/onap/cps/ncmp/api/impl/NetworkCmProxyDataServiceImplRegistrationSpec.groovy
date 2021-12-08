/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.exception.NcmpException
import org.onap.cps.ncmp.api.models.CmHandle
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED

class NetworkCmProxyDataServiceImplRegistrationSpec extends Specification {

    @Shared
    def persistenceCmHandle = new CmHandle()

    @Shared
    def cmHandlesArray = ['cmHandle001']

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def spyObjectMapper = Spy(ObjectMapper)

    def noTimestamp = null

    def 'Register or re-register a DMI Plugin for the given cm-handle(s) with #scenario process.'() {
        given: 'a registration'
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'my-server')
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.cmHandleProperties = [name1: 'value1', name2: 'value2']
            dmiPluginRegistration.createdCmHandles = createdCmHandles
            dmiPluginRegistration.updatedCmHandles = updatedCmHandles
            dmiPluginRegistration.removedCmHandles = removedCmHandles
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","dmi-data-service-name":null,"dmi-model-service-name":null,"additional-properties":[{"name":"name1","value":"value1"},{"name":"name2","value":"value2"}]}]}'
        when: 'registration is updated and modules are synced'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'save list elements is invoked with the expected parameters'
            expectedCallsToSaveNode * mockCpsDataService.saveListElements('NCMP-Admin', 'ncmp-dmi-registry',
                    '/dmi-registry', expectedJsonData, noTimestamp)
        and: 'update node and child data nodes is invoked with correct parameters'
            expectedCallsToUpdateNode * mockCpsDataService.updateNodeLeavesAndExistingDescendantLeaves('NCMP-Admin',
                    'ncmp-dmi-registry', '/dmi-registry', expectedJsonData, noTimestamp)
        and: 'delete schema set is invoked with the correct parameters'
            expectedCallsToDeleteSchemaSetAndListElement * mockCpsModuleService.deleteSchemaSet('NFP-Operational', 'cmHandle001', CASCADE_DELETE_ALLOWED)
        and: 'delete list or list element is invoked with the correct parameters'
            expectedCallsToDeleteSchemaSetAndListElement * mockCpsDataService.deleteListOrListElement('NCMP-Admin',
                    'ncmp-dmi-registry', "/dmi-registry/cm-handles[@id='cmHandle001']", noTimestamp)
        where:
            scenario                    | createdCmHandles      | updatedCmHandles      | removedCmHandles || expectedCallsToSaveNode | expectedCallsToUpdateNode | expectedCallsToDeleteSchemaSetAndListElement
            'create'                    | [persistenceCmHandle] | []                    | []               || 1                       | 0                         | 0
            'update'                    | []                    | [persistenceCmHandle] | []               || 0                       | 1                         | 0
            'delete'                    | []                    | []                    | cmHandlesArray   || 0                       | 0                         | 1
            'create, update and delete' | [persistenceCmHandle] | [persistenceCmHandle] | cmHandlesArray   || 1                       | 1                         | 1
            'no valid data'             | null                  | null                  | null             || 0                       | 0                         | 0
    }

    def 'Register a DMI Plugin for the given cm-handle(s) without additional properties.'() {
        given: 'a registration without cm-handle properties'
            NetworkCmProxyDataServiceImpl objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'my-server')
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.cmHandleProperties = null
            dmiPluginRegistration.createdCmHandles = [persistenceCmHandle]
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","dmi-data-service-name":null,"dmi-model-service-name":null,"additional-properties":[]}]}'
        when: 'registration is updated'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'save list elements is invoked with the expected parameters'
            1 * mockCpsDataService.saveListElements('NCMP-Admin', 'ncmp-dmi-registry',
                    '/dmi-registry', expectedJsonData, noTimestamp)
    }

    def 'Register a DMI Plugin for a given cm-handle(s) with JSON processing errors during #scenario process.'() {
        given: 'a registration without cm-handle properties '
            NetworkCmProxyDataServiceImpl objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'some-plugin')
            dmiPluginRegistration.createdCmHandles = createdCmHandles
            dmiPluginRegistration.updatedCmHandles = updatedCmHandles
        and: 'an json processing exception occurs'
            spyObjectMapper.writeValueAsString(_) >> { throw (new JsonProcessingException('')) }
        when: 'registration is updated and modules are synced'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        where:
            scenario | createdCmHandles      | updatedCmHandles
            'create' | [persistenceCmHandle] | []
            'update' | []                    | [persistenceCmHandle]
    }

    def 'Register a DMI Plugin for the given cm-handle(s) with no data found during delete process.'() {
        given: 'a registration without cm-handle properties '
            NetworkCmProxyDataServiceImpl objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'some-plugin')
            dmiPluginRegistration.removedCmHandles = ['some cm handle']
        and: 'an json processing exception occurs during delete process'
            mockCpsDataService.deleteListOrListElement(*_) >>  { throw (new DataNodeNotFoundException('','')) }
        when: 'registration is updated and modules are synced'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Dmi plugin registration with #scenario'() {
        given: 'a registration '
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:dmiPlugin, dmiModelPlugin:dmiModelPlugin,
                    dmiDataPlugin:dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [persistenceCmHandle]
        when: 'update registration and sync module is called with correct DMI plugin information'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'create cm handles registration and sync modules is called with the correct plugin information'
            1 * objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(dmiPluginRegistration)
        where:
            scenario                          | dmiPlugin  | dmiModelPlugin | dmiDataPlugin
            'combined DMI plugin'             | 'service1' | ''             | ''
            'data & model DMI plugins'        | ''         | 'service1'     | 'service2'
            'data & model using same service' | ''         | 'service1'     | 'service1'
    }

    def 'Invalid dmi plugin registration with #scenario'() {
        given: 'a registration '
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:dmiPlugin, dmiModelPlugin:dmiModelPlugin,
                    dmiDataPlugin:dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [persistenceCmHandle]
        when: 'registration is called with incorrect DMI plugin information'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'an NcmpException is thrown with correct message details'
            def exceptionThrown = thrown(NcmpException)
            assert exceptionThrown.getMessage().contains(expectedMessageDetails)
        and: 'registration is not called'
            0 * objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(dmiPluginRegistration)
        where:
            scenario              | dmiPlugin  | dmiModelPlugin | dmiDataPlugin || expectedMessageDetails
            'no DMI plugin'       | ''         | ''             | ''            || 'No DMI plugin service names'
            'all DMI plugins'     | 'service1' | 'service2'     | 'service3'    || 'Invalid combination of plugin service names'
            'no model DMI plugin' | 'service1' | ''             | 'service2'    || 'Invalid combination of plugin service names'
            'no data DMI plugin'  | 'service1' | 'service2'     | ''            || 'Invalid combination of plugin service names'
    }

    def getObjectUnderTestWithModelSyncDisabled() {
        def objectUnderTest = Spy(new NetworkCmProxyDataServiceImpl(null, null, mockCpsModuleService,
                mockCpsDataService, null, null, spyObjectMapper))
        objectUnderTest.syncModulesAndCreateAnchor(*_) >> null
        return objectUnderTest
    }
}
