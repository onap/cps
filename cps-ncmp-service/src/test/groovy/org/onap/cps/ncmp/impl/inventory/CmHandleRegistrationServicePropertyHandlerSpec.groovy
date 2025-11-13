/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
 * Modifications Copyright (C) 2022 Bell Canada
 * Modifications Copyright (C) 2024 TechMahindra Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.inventory

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.DataNode
import org.onap.cps.impl.DataNodeBuilder
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsHelper
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import spock.lang.Specification

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_FOUND
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_INVALID_ID
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR
import static org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse.Status
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR

class CmHandleRegistrationServicePropertyHandlerSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockCpsDataService = Mock(CpsDataService)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockAlternateIdChecker = Mock(AlternateIdChecker)
    def mockCmHandleIdPerAlternateId = Mock(IMap)
    def mockLcmEventsHelper = Mock(LcmEventsHelper)

    def objectUnderTest = new CmHandleRegistrationServicePropertyHandler(mockInventoryPersistence, mockCpsDataService, jsonObjectMapper, mockAlternateIdChecker, mockCmHandleIdPerAlternateId, mockLcmEventsHelper)
    def logger = Spy(ListAppender<ILoggingEvent>)

    void setup() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(CmHandleRegistrationServicePropertyHandler.class))
        setupLogger.addAppender(logger)
        setupLogger.setLevel(Level.DEBUG)
        logger.start()
        // Always accept all alternate IDs
        mockAlternateIdChecker.getIdsOfCmHandlesWithRejectedAlternateId(*_) >> []
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CmHandleRegistrationServicePropertyHandler.class)).detachAndStopAllAppenders()
    }

    def static cmHandleId = 'myHandle1'
    def static cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}']"

    def static propertyDataNodes = [new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/additional-properties[@name='additionalProp1']").withLeaves(['name': 'additionalProp1', 'value': 'additionalValue1']).build(),
                                    new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/additional-properties[@name='additionalProp2']").withLeaves(['name': 'additionalProp2', 'value': 'additionalValue2']).build(),
                                    new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/public-properties[@name='publicProp3']").withLeaves(['name': 'publicProp3', 'value': 'publicValue3']).build(),
                                    new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/public-properties[@name='publicProp4']").withLeaves(['name': 'publicProp4', 'value': 'publicValue4']).build()]
    def static cmHandleDataNodeAsCollection = [new DataNode(xpath: cmHandleXpath, childDataNodes: propertyDataNodes, leaves: ['id': cmHandleId])]

    def 'Update CM Handle Public Properties: #scenario'() {
        given: 'the CPS service return a CM handle'
            mockInventoryPersistence.getCmHandleDataNodeByCmHandleId(cmHandleId, INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNodeAsCollection
        and: 'an update cm handle request with public properties updates'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: updatedPublicProperties)]
        when: 'update data node leaves is called with the update request'
            objectUnderTest.updateCmHandleProperties(cmHandleUpdateRequest)
        then: 'the replace list method is called with correct params'
            1 * mockInventoryPersistence.replaceListContent(cmHandleXpath, _) >> { args ->
                {
                    assert args[1].leaves.size() == expectedPropertiesAfterUpdate.size()
                    assert args[1].leaves.containsAll(convertToProperties(expectedPropertiesAfterUpdate))
                }
            }
        where: 'following public properties updates are made'
            scenario                          | updatedPublicProperties      || expectedPropertiesAfterUpdate
            'property added'                  | ['newPubProp1': 'pub-val']   || [['publicProp3': 'publicValue3'], ['publicProp4': 'publicValue4'], ['newPubProp1': 'pub-val']]
            'property updated'                | ['publicProp4': 'newPubVal'] || [['publicProp3': 'publicValue3'], ['publicProp4': 'newPubVal']]
            'property removed'                | ['publicProp4': null]        || [['publicProp3': 'publicValue3']]
            'property ignored(value is null)' | ['pub-prop': null]           || [['publicProp3': 'publicValue3'], ['publicProp4': 'publicValue4']]
    }

    def 'Update Additional Properties: #scenario'() {
        given: 'the CPS service return a CM handle'
            mockInventoryPersistence.getCmHandleDataNodeByCmHandleId(cmHandleId, INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNodeAsCollection
        and: 'an update cm handle request with additional properties updates'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, additionalProperties: updatedAdditionalProperties)]
        when: 'update data node leaves is called with the update request'
            objectUnderTest.updateCmHandleProperties(cmHandleUpdateRequest)
        then: 'replace list method should is called with correct params'
            expectedCallsToReplaceMethod * mockInventoryPersistence.replaceListContent(cmHandleXpath, _) >> { args ->
                {
                    assert args[1].leaves.size() == expectedPropertiesAfterUpdate.size()
                    assert args[1].leaves.containsAll(convertToProperties(expectedPropertiesAfterUpdate))
                }
            }
        where: 'following additional properties updates are made'
            scenario                          | updatedAdditionalProperties          || expectedPropertiesAfterUpdate                                                                                           | expectedCallsToReplaceMethod
            'property added'                  | ['newAdditionalProp1': 'add-value'] || [['additionalProp1': 'additionalValue1'], ['additionalProp2': 'additionalValue2'], ['newAdditionalProp1': 'add-value']] | 1
            'property updated'                | ['additionalProp1': 'newValue']     || [['additionalProp2': 'additionalValue2'], ['additionalProp1': 'newValue']]                                              | 1
            'property removed'                | ['additionalProp1': null]           || [['additionalProp2': 'additionalValue2']]                                                                               | 1
            'property ignored(value is null)' | ['new-prop': null]                  || [['additionalProp1': 'additionalValue1'], ['additionalProp2': 'additionalValue2']]                                      | 1
            'no property changes'             | [:]                                 || [['additionalProp1': 'additionalValue1'], ['additionalProp2': 'additionalValue2']]                                      | 0
    }

    def 'Update CM Handle Properties, remove all public properties: #scenario'() {
        given: 'the CPS service return a CM handle'
            def cmHandleDataNode = new DataNode(xpath: cmHandleXpath, leaves: ['id': cmHandleId], childDataNodes: originalPropertyDataNodes)
            mockInventoryPersistence.getCmHandleDataNodeByCmHandleId(cmHandleId, INCLUDE_ALL_DESCENDANTS) >> [cmHandleDataNode]
        and: 'an update cm handle request that removes all public properties(existing and non-existing)'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProp3': null, 'publicProp4': null])]
        when: 'update data node leaves is called with the update request'
            objectUnderTest.updateCmHandleProperties(cmHandleUpdateRequest)
        then: 'the replace list method is not called'
            0 * mockInventoryPersistence.replaceListContent(*_)
        then: 'delete data node will be called for any existing property'
            expectedCallsToDeleteDataNode * mockInventoryPersistence.deleteDataNode(_) >> { arg ->
                {
                    assert arg[0].contains("@name='publicProp")
                }
            }
        where: 'following public properties updates are made'
            scenario                              | originalPropertyDataNodes || expectedCallsToDeleteDataNode
            '2 original properties, both removed' | propertyDataNodes         || 2
            'no original properties'              | []                        || 0
    }

    def '#scenario error leads to #exception when we try to update cmHandle'() {
        given: 'cm handles request'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: [:], additionalProperties: [:])]
        and: 'data node cannot be found'
            mockInventoryPersistence.getCmHandleDataNodeByCmHandleId(*_) >> { throw exception }
        when: 'update data node leaves is called using correct parameters'
            def response = objectUnderTest.updateCmHandleProperties(cmHandleUpdateRequest)
        then: 'one failed registration response'
            response.size() == 1
        and: 'it has expected error details'
            with(response.get(0)) {
                assert it.status == Status.FAILURE
                assert it.cmHandle == cmHandleId
                assert it.ncmpResponseStatus == expectedError
                assert it.errorText == expectedErrorText
            }
        where:
            scenario                   | cmHandleId               | exception                                                                                           || expectedError        | expectedErrorText
            'Cm Handle does not exist' | 'cmHandleId'             | new DataNodeNotFoundException(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR)                        || CM_HANDLES_NOT_FOUND | 'cm handle reference(s) not found'
            'Unknown'                  | 'cmHandleId'             | new RuntimeException('Failed')                                                                      || UNKNOWN_ERROR        | 'Failed'
            'Invalid cm handle id'     | 'cmHandleId with spaces' | new DataValidationException('Name Validation Error.', cmHandleId + 'contains an invalid character') || CM_HANDLE_INVALID_ID | 'cm handle reference has an invalid character(s) in id'
    }

    def 'Multiple update operations in a single request'() {
        given: 'cm handles request'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProp1': "value"], additionalProperties: [:]),
                                         new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProp1': "value"], additionalProperties: [:]),
                                         new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProp1': "value"], additionalProperties: [:])]
        and: 'data node can be found for 1st and 3rd cm-handle but not for 2nd cm-handle'
            mockInventoryPersistence.getCmHandleDataNodeByCmHandleId(*_) >> cmHandleDataNodeAsCollection >> {
                throw new DataNodeNotFoundException(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) } >> cmHandleDataNodeAsCollection
        when: 'update data node leaves is called using correct parameters'
            def cmHandleResponseList = objectUnderTest.updateCmHandleProperties(cmHandleUpdateRequest)
        then: 'response has 3 values'
            cmHandleResponseList.size() == 3
        and: 'the 1st and 3rd requests were processed successfully'
            with(cmHandleResponseList.get(0)) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == cmHandleId
            }
            with(cmHandleResponseList.get(2)) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == cmHandleId
            }
        and: 'the 2nd request failed with correct error code'
            with(cmHandleResponseList.get(1)) {
                assert it.status == Status.FAILURE
                assert it.cmHandle == cmHandleId
                assert it.ncmpResponseStatus == CM_HANDLES_NOT_FOUND
                assert it.errorText == 'cm handle reference(s) not found'
            }
        then: 'the replace list method is called twice'
            2 * mockInventoryPersistence.replaceListContent(cmHandleXpath, _)
    }

    def 'Update alternate id of existing CM Handle.'() {
        given: 'cm handles request'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, alternateId: 'alt-1')]
        and: 'the cm handle per alternate id cache returns a value'
            mockCmHandleIdPerAlternateId.get(_) >> cmHandleId
        and: 'a data node found'
            def dataNode = new DataNode(xpath: cmHandleXpath, leaves: ['id': cmHandleId, 'alternate-id': 'alt-1'])
            mockInventoryPersistence.getCmHandleDataNodeByCmHandleId(cmHandleId, INCLUDE_ALL_DESCENDANTS) >> [dataNode]
        when: 'cm handle properties is updated'
            def response = objectUnderTest.updateCmHandleProperties(cmHandleUpdateRequest)
        then: 'the update is delegated to inventory persistence with correct parameters'
            1 * mockInventoryPersistence.updateCmHandleField(cmHandleId, 'alternate-id', 'alt-1')
        and: 'one successful registration response'
            response.size() == 1
        and: 'the response shows success for the given cm handle id'
                assert response[0].status == Status.SUCCESS
                assert response[0].cmHandle == cmHandleId
    }

    def 'Update with rejected alternate id.'() {
        given: 'cm handles request'
            def updatedNcmpServiceCmHandles = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, alternateId: 'alt-1')]
        and: 'a data node found'
            def dataNode = new DataNode(xpath: cmHandleXpath, leaves: ['id': cmHandleId, 'alternate-id': 'alt-1'])
            mockInventoryPersistence.getCmHandleDataNodeByCmHandleId(cmHandleId, INCLUDE_ALL_DESCENDANTS) >> [dataNode]
        when: 'attempt to update the cm handle'
            def response = objectUnderTest.updateCmHandleProperties(updatedNcmpServiceCmHandles)
        then: 'the update is NOT delegated to cps data service'
            0 * mockCpsDataService.updateNodeLeaves(*_)
        and:  'the alternate id checker rejects the given cm handle (override default setup behavior)'
            mockAlternateIdChecker.getIdsOfCmHandlesWithRejectedAlternateId(*_) >> [cmHandleId]
        and: 'the response shows a failure for the given cm handle id'
            assert response[0].status == Status.FAILURE
            assert response[0].cmHandle == cmHandleId
    }

    def 'Update CM Handle data producer identifier from #scenario'() {
        given:  'an existing cm handle with old data producer identifier'
            DataNode existingCmHandleDataNode = new DataNode(xpath: cmHandleXpath, leaves: ['id': 'cmHandleId', 'data-producer-identifier': oldDataProducerIdentifier])
        and:    'an update request with a new data producer identifier'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'cmHandleId', dataProducerIdentifier: 'New Data Producer Identifier')
        and:    'the inventory persistence returns updated yang model'
            1 * mockInventoryPersistence.getYangModelCmHandle('cmHandleId') >> createYangModelCmHandle('cmHandleId', 'New Data Producer Identifier')
        when:   'data producer identifier is updated'
            objectUnderTest.updateDataProducerIdentifier(existingCmHandleDataNode, ncmpServiceCmHandle)
        then:   'the update node leaves method is invoked once with correct parameters'
            1 * mockInventoryPersistence.updateCmHandleField('cmHandleId', 'data-producer-identifier', 'New Data Producer Identifier')
        and:    'LCM event is sent'
            1 * mockLcmEventsHelper.sendLcmEventBatchAsynchronously({ cmHandleTransitionPairs ->
                assert cmHandleTransitionPairs[0].targetYangModelCmHandle.dataProducerIdentifier == 'New Data Producer Identifier'
            })
        where: 'the following scenarios are used'
            scenario             | oldDataProducerIdentifier
            'null to something'  | null
            'blank to something' | ''
    }

    def 'Update CM Handle data producer identifier with same value'() {
        given: 'an existing cm handle with existing data producer identifier'
            DataNode existingCmHandleDataNode = new DataNode(xpath: cmHandleXpath, leaves: ['id': 'cmHandleId', 'data-producer-identifier': 'same-data-producer-id'])
        and: 'an update request with the same data producer identifier'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'cmHandleId', dataProducerIdentifier: 'same-data-producer-id')
        when: 'data producer identifier is updated'
            objectUnderTest.updateDataProducerIdentifier(existingCmHandleDataNode, ncmpServiceCmHandle)
        then: 'the update node leaves method is not invoked'
            0 * mockCpsDataService.updateNodeLeaves(*_)
        and: 'No LCM events are sent'
            0 * mockLcmEventsHelper.sendLcmEventBatchAsynchronously(*_)
        and: 'debug information is logged'
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.DEBUG
            assert loggingEvent.formattedMessage.contains('dataProducerIdentifier for cmHandle cmHandleId is already set to same-data-producer-id')
    }

    def 'Update CM Handle data producer identifier from existing to new value'() {
        given: 'an existing cm handle with a data producer identifier'
            DataNode existingCmHandleDataNode = new DataNode(xpath: cmHandleXpath, leaves: ['id': 'cmHandleId', 'data-producer-identifier': 'oldDataProducerIdentifier'])
        and: 'an update request with a new data producer identifier'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'cmHandleId', dataProducerIdentifier: 'newDataProducerIdentifier')
        and: 'the inventory persistence returns updated yang model'
            mockInventoryPersistence.getYangModelCmHandle('cmHandleId') >> createYangModelCmHandle('cmHandleId', 'newDataProducerIdentifier')
        when: 'update data producer identifier is called'
            objectUnderTest.updateDataProducerIdentifier(existingCmHandleDataNode, ncmpServiceCmHandle)
        then: 'the update node leaves method is invoked once with correct parameters'
            1 * mockInventoryPersistence.updateCmHandleField('cmHandleId', 'data-producer-identifier', 'newDataProducerIdentifier')
        and: 'LCM event is sent'
            1 * mockLcmEventsHelper.sendLcmEventBatchAsynchronously( { cmHandleTransitionPairs ->
                assert cmHandleTransitionPairs[0].targetYangModelCmHandle.dataProducerIdentifier == 'newDataProducerIdentifier'
                assert cmHandleTransitionPairs[0].currentYangModelCmHandle.dataProducerIdentifier == 'oldDataProducerIdentifier'
            })
    }

    def 'Update CM Handle data producer identifier with null or blank target identifier'() {
        given: 'an existing cm handle'
            DataNode existingCmHandleDataNode = new DataNode(xpath: cmHandleXpath, leaves: ['id': 'cmHandleId', 'data-producer-identifier': 'some existing id'])
        and: 'an update request with null/blank data producer identifier'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'cmHandleId', dataProducerIdentifier: targetDataProducerIdentifier)
        when: 'data producer identifier update is attempted'
            objectUnderTest.updateDataProducerIdentifier(existingCmHandleDataNode, ncmpServiceCmHandle)
        then: 'the update node leaves method is not invoked'
            0 * mockCpsDataService.updateNodeLeaves(*_)
        and: 'No LCM events are sent'
            0 * mockLcmEventsHelper.sendLcmEventBatchAsynchronously(*_)
        and: 'warning is logged'
            def lastLoggingEvent = logger.list[0]
            assert lastLoggingEvent.level == Level.WARN
            assert lastLoggingEvent.formattedMessage.contains('Ignoring update for cmHandle cmHandleId: target dataProducerIdentifier is null or blank')
        where: 'the following invalid scenarios are used'
            scenario      | targetDataProducerIdentifier
            'null value'  | null
            'blank value' | ''
    }

    def convertToProperties(expectedPropertiesAfterUpdateAsMap) {
        def properties = [].withDefault { [:] }
        expectedPropertiesAfterUpdateAsMap.forEach(property ->
            property.forEach((key, val) -> {
                properties.add(['name': key, 'value': val])
            }))
        return properties
    }


    def createYangModelCmHandle(cmHandleId, dataProducerIdentifier) {
        new YangModelCmHandle(
            id: cmHandleId,
            dmiDataServiceName: 'some-dmi-plugin',
            dataProducerIdentifier: dataProducerIdentifier,
            additionalProperties: [],
            publicProperties: []
        )
    }
}
