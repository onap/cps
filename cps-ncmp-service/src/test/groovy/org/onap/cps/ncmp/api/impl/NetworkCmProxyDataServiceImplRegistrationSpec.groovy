/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.exception.NcmpException
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations
import org.onap.cps.ncmp.api.models.CmHandle
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
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
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockDmiModelOperations = Mock(DmiModelOperations)
    def mockDmiDataOperations = Mock(DmiDataOperations)

    def noTimestamp = null

    def 'Register or re-register a DMI Plugin for the given cm-handle(s) with #scenario process.'() {
        given: 'a registration'
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'my-server')
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.dmiProperties = [dmiProp1: 'dmiValue1', dmiProp2: 'dmiValue2']
            persistenceCmHandle.publicProperties = [publicProp1: 'publicValue1', publicProp2: 'publicValue2' ]
            dmiPluginRegistration.createdCmHandles = createdCmHandles
            dmiPluginRegistration.updatedCmHandles = updatedCmHandles
            dmiPluginRegistration.removedCmHandles = removedCmHandles
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","dmi-data-service-name":null,"dmi-model-service-name":null,' +
                '"additional-properties":[{"name":"dmiProp1","value":"dmiValue1"},{"name":"dmiProp2","value":"dmiValue2"}],' +
                '"public-properties":[{"name":"publicProp1","value":"publicValue1"},{"name":"publicProp2","value":"publicValue2"}]' +
                '}]}'
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
            'update'                    | []                    | [persistenceCmHandle] | []               || 0                       | 0                         | 0
            'delete'                    | []                    | []                    | cmHandlesArray   || 0                       | 0                         | 1
            'create, update and delete' | [persistenceCmHandle] | [persistenceCmHandle] | cmHandlesArray   || 1                       | 0                         | 1
            'no valid data'             | null                  | null                  | null             || 0                       | 0                         | 0
    }

    def 'Register a DMI Plugin for the given cm-handle(s) without DMI properties.'() {
        given: 'a registration without cm-handle properties'
            NetworkCmProxyDataServiceImpl objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'my-server')
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.dmiProperties = Collections.emptyMap()
            persistenceCmHandle.publicProperties = Collections.emptyMap()
            dmiPluginRegistration.createdCmHandles = [persistenceCmHandle]
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","dmi-data-service-name":null,"dmi-model-service-name":null,"additional-properties":[],"public-properties":[]}]}'
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
            spiedJsonObjectMapper.asJsonString(_) >> { throw (new JsonProcessingException('')) }
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

    def 'Register a DMI Plugin for the given cm-handle(s) with no schema set found during delete process.'() {
        given: 'a registration'
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'my-server')
            dmiPluginRegistration.removedCmHandles = cmHandlesArray
        and: 'an exception occurs during delete schema set process'
            mockCpsModuleService.deleteSchemaSet(_,_,_) >>  { throw (new Exception('')) }
        when: 'registration is updated and modules are synced'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'delete list or list element is still called'
            1 * mockCpsDataService.deleteListOrListElement(_,_,_,_)
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

    def 'Invalid DMI plugin registration with #scenario'() {
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
            scenario                        | dmiPlugin  | dmiModelPlugin | dmiDataPlugin || expectedMessageDetails
            'empty DMI plugins'             | ''         | ''             | ''            || 'No DMI plugin service names'
            'blank DMI plugins'             | ' '        | ' '            | ' '           || 'No DMI plugin service names'
            'null DMI plugins'              | null       | null           | null          || 'No DMI plugin service names'
            'all DMI plugins'               | 'service1' | 'service2'     | 'service3'    || 'Cannot register combined plugin service name and other service names'
            '(combined)DMI and Data Plugin' | 'service1' | ''             | 'service2'    || 'Cannot register combined plugin service name and other service names'
            '(combined)DMI and model Plugin'| 'service1' | 'service2'     | ''            || 'Cannot register combined plugin service name and other service names'
            'only model DMI plugin'         | ''         | 'service1'     | ''            || 'Cannot register just a Data or Model plugin service name'
            'only data DMI plugin'          | ''         | ''             | 'service1'    || 'Cannot register just a Data or Model plugin service name'
    }

    def 'Add/Remove/Update properties(DMI and Public) as part of CM-Handle registration update #scenario'() {
        given: 'a CM-handle registration'
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def cmHandleId = "myHandle1"
            def cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}']"
        and: 'a data node with some attributes(leaves)'
            def dataNode = new DataNode()
            dataNode.xpath = cmHandleXpath
            dataNode.leaves = existingProperties
            dataNode.childDataNodes = [childCmHandles]
        and: 'dmiPluginRegistration input update request'
            def dmiPluginReg = new DmiPluginRegistration();
            dmiPluginReg.dmiPlugin = 'onap.dmap.plugin';
            dmiPluginReg.updatedCmHandles = [new CmHandle(cmHandleID: cmHandleId, dmiProperties: updatedDmiProperties, publicProperties: updatedPublicProperties)]
        and: 'get the data node which needs to be updated'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'updateDmiRegistrationAndSyncModule is called'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginReg)
        then: 'check for leaves or immediate attributes with updated values'
            assert dataNode.leaves.size() == expectedCmHandlesAfterUpdate.size()
            assert dataNode.leaves == expectedCmHandlesAfterUpdate
        then: 'check for child-nodes attributes'
            assert dataNode.childDataNodes == [expectedChildCmhandles]
        where:
            scenario                                    | existingProperties                       | updatedDmiProperties  | updatedPublicProperties     | childCmHandles                 || expectedCmHandlesAfterUpdate                                      | expectedChildCmhandles
            'property removed'                          | ['prop': 'value', 'pub-prop': 'pub-val'] | ['prop': null]        | ['pub-prop': null]          | ['child-key': 'child-value']   || [:]                                                               | ['child-key': 'child-value']
            'property updated'                          | ['prop': 'value', 'pub-prop': 'pub-val'] | ['prop': 'newValue']  | ['pub-prop': 'newPubVal']   | ['ch1': 'val1', 'ch2': 'val2'] || ['prop': 'newValue', 'pub-prop': 'newPubVal']                     | ['ch1': 'val1', 'ch2': 'val2']
            'property added'                            | ['prop': 'value']                        | ['new-prop': 'value'] | ['new-pub-prop': 'pub-val'] | [:]                            || ['prop': 'value', 'new-prop': 'value', 'new-pub-prop': 'pub-val'] | [:]
            'property ignored(value is null)'           | ['prop': 'value', 'pub-prop': 'pub-val'] | ['propx': null]       | ['pub-propx': null]         | [:]                            || ['prop': 'value', 'pub-prop': 'pub-val']                          | [:]
            'no existing property and we try to add'    | [:]                                      | ['prop4': 'value4']   | ['pub-prop4': 'val4']       | [:]                            || ['prop4': 'value4', 'pub-prop4': 'val4']                          | [:]
            'no existing property and we try to remove' | [:]                                      | ['prop4': null]       | ['pub-prop4': null]         | [:]                            || [:]                                                               | [:]
    }

    def 'Exception logged when Add/Remove/Update properties(DMI and Public) called as part of CM-Handle registration update'() {
        given: 'a CM-handle registration'
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def cmHandleId = "unknownHandle"
            def cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}']"
        and: 'a data node'
            def dataNode = new DataNode()
            dataNode.xpath = cmHandleXpath
        and: 'dmiPluginRegistration input update request'
            def dmiPluginReg = new DmiPluginRegistration();
            dmiPluginReg.dmiPlugin = 'onap.dmap.plugin';
            dmiPluginReg.updatedCmHandles = [new CmHandle(cmHandleID: cmHandleId)]
        and: 'get the data node which needs to be updated'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> {
                throw new DataNodeNotFoundException('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXpath)
            }
        when: 'updateDmiRegistrationAndSyncModule is called for unknown dataNode'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginReg)
        then: 'an exception is thrown(logged) and replaceListContent is not called'
            0 * mockCpsDataService.replaceListContent(*_)
    }

    def getObjectUnderTestWithModelSyncDisabled() {
        def objectUnderTest = Spy(new NetworkCmProxyDataServiceImpl(mockCpsDataService, spiedJsonObjectMapper, mockDmiDataOperations, mockDmiModelOperations,
                mockCpsModuleService, mockCpsAdminService))
        objectUnderTest.syncModulesAndCreateAnchor(*_) >> null
        return objectUnderTest
    }
}
