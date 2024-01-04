/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation
 * Modifications Copyright (C) 2022 Bell Canada
 * Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ncmp.api.impl

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_FOUND
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_INVALID_ID
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR
import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.Status

import org.onap.cps.api.CpsDataService
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification

class NetworkCmProxyDataServicePropertyHandlerSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockCpsDataService = Mock(CpsDataService)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)

    def objectUnderTest = new NetworkCmProxyDataServicePropertyHandler(mockInventoryPersistence, mockCpsDataService, mockJsonObjectMapper)
    def static cmHandleId = 'myHandle1'
    def static cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}']"

    def static propertyDataNodes = [new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/additional-properties[@name='additionalProp1']").withLeaves(['name': 'additionalProp1', 'value': 'additionalValue1']).build(),
                                    new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/additional-properties[@name='additionalProp2']").withLeaves(['name': 'additionalProp2', 'value': 'additionalValue2']).build(),
                                    new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/public-properties[@name='publicProp3']").withLeaves(['name': 'publicProp3', 'value': 'publicValue3']).build(),
                                    new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/public-properties[@name='publicProp4']").withLeaves(['name': 'publicProp4', 'value': 'publicValue4']).build()]
    def static cmHandleDataNodeAsCollection = [new DataNode(xpath: cmHandleXpath, childDataNodes: propertyDataNodes, leaves: ['alternate-id': ''])]

    def 'Update CM Handle Public Properties: #scenario'() {
        given: 'the CPS service return a CM handle'
            mockInventoryPersistence.getCmHandleDataNode(cmHandleId) >> cmHandleDataNodeAsCollection
        and: 'an update cm handle request with public properties updates'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: updatedPublicProperties)]
        when: 'update data node leaves is called with the update request'
            objectUnderTest.updateCmHandleProperties(cmHandleUpdateRequest)
        then: 'the replace list method is called with correct params'
            1 * mockInventoryPersistence.replaceListContent(cmHandleXpath,_) >> { args ->
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

    def 'Update DMI Properties: #scenario'() {
        given: 'the CPS service return a CM handle'
            mockInventoryPersistence.getCmHandleDataNode(cmHandleId) >> cmHandleDataNodeAsCollection
        and: 'an update cm handle request with DMI properties updates'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, dmiProperties: updatedDmiProperties)]
        when: 'update data node leaves is called with the update request'
            objectUnderTest.updateCmHandleProperties(cmHandleUpdateRequest)
        then: 'replace list method should is called with correct params'
            expectedCallsToReplaceMethod * mockInventoryPersistence.replaceListContent(cmHandleXpath, _) >> { args ->
                {
                    assert args[1].leaves.size() == expectedPropertiesAfterUpdate.size()
                    assert args[1].leaves.containsAll(convertToProperties(expectedPropertiesAfterUpdate))
                }
            }
        where: 'following DMI properties updates are made'
            scenario                          | updatedDmiProperties                || expectedPropertiesAfterUpdate                                                                                           | expectedCallsToReplaceMethod
            'property added'                  | ['newAdditionalProp1': 'add-value'] || [['additionalProp1': 'additionalValue1'], ['additionalProp2': 'additionalValue2'], ['newAdditionalProp1': 'add-value']] | 1
            'property updated'                | ['additionalProp1': 'newValue']     || [['additionalProp2': 'additionalValue2'], ['additionalProp1': 'newValue']]                                              | 1
            'property removed'                | ['additionalProp1': null]           || [['additionalProp2': 'additionalValue2']]                                                                               | 1
            'property ignored(value is null)' | ['new-prop': null]                  || [['additionalProp1': 'additionalValue1'], ['additionalProp2': 'additionalValue2']]                                      | 1
            'no property changes'             | [:]                                 || [['additionalProp1': 'additionalValue1'], ['additionalProp2': 'additionalValue2']]                                      | 0
    }

    def 'Update CM Handle Properties, remove all properties: #scenario'() {
        given: 'the CPS service return a CM handle'
            def cmHandleDataNode = new DataNode(xpath: cmHandleXpath, childDataNodes: originalPropertyDataNodes)
            mockInventoryPersistence.getCmHandleDataNode(cmHandleId) >> [cmHandleDataNode]
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
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: [:], dmiProperties: [:])]
        and: 'data node cannot be found'
            mockInventoryPersistence.getCmHandleDataNode(*_) >> { throw exception }
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
        'Cm Handle does not exist' | 'cmHandleId'             | new DataNodeNotFoundException(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR)                        || CM_HANDLES_NOT_FOUND | 'cm handle id(s) not found'
        'Unknown'                  | 'cmHandleId'             | new RuntimeException('Failed')                                                                      || UNKNOWN_ERROR        | 'Failed'
        'Invalid cm handle id'     | 'cmHandleId with spaces' | new DataValidationException('Name Validation Error.', cmHandleId + 'contains an invalid character') || CM_HANDLE_INVALID_ID | 'cm-handle has an invalid character(s) in id'
    }

    def 'Multiple update operations in a single request'() {
        given: 'cm handles request'
            def cmHandleUpdateRequest = [new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProp1': "value"], dmiProperties: [:]),
                                         new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProp1': "value"], dmiProperties: [:]),
                                         new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProp1': "value"], dmiProperties: [:])]
        and: 'data node can be found for 1st and 3rd cm-handle but not for 2nd cm-handle'
            mockInventoryPersistence.getCmHandleDataNode(*_) >> cmHandleDataNodeAsCollection >> {
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
                assert it.errorText == 'cm handle id(s) not found'
            }
        then: 'the replace list method is called twice'
            2 * mockInventoryPersistence.replaceListContent(cmHandleXpath,_)
    }

    def convertToProperties(expectedPropertiesAfterUpdateAsMap) {
        def properties = [].withDefault { [:] }
        expectedPropertiesAfterUpdateAsMap.forEach(property ->
            property.forEach((key, val) -> {
                properties.add(['name': key, 'value': val])
            }))
        return properties
    }

}
