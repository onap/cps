/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.models.CmHandle
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification

class NetworkCmProxyDataServicePropertyHandlerSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new NetworkCmProxyDataServicePropertyHandler(mockCpsDataService)
    def dataspaceName = 'NCMP-Admin'
    def anchorName = 'ncmp-dmi-registry'
    def cmHandleId = 'myHandle1'
    def cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}']"
    def noTimeStamp = null

    def propertyDataNodes = [new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/additional-properties[@name='additionalProp1']").withLeaves(['name': 'additionalProp1', 'value': 'additionalValue1']).build(),
                             new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/additional-properties[@name='additionalProp2']").withLeaves(['name': 'additionalProp2', 'value': 'additionalValue2']).build(),
                             new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/public-properties[@name='publicProp3']").withLeaves(['name': 'publicProp3', 'value': 'publicValue3']).build(),
                             new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/public-properties[@name='publicProp4']").withLeaves(['name': 'publicProp4', 'value': 'publicValue4']).build()]

    def cmHandleDataNode = new DataNode(xpath: cmHandleXpath, childDataNodes: propertyDataNodes)

    def 'Update CM Handle Public Properties: #scenario'() {
        given: 'the CPS service return a CM handle'
            mockCpsDataService.getDataNode(dataspaceName, anchorName, cmHandleXpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'an update cm handle request with public properties updates'
            def cmHandleUpdateRequest = [new CmHandle(cmHandleID: cmHandleId, publicProperties: updatedPublicProperties)]
        when: 'update data node leaves is called with the update request'
            objectUnderTest.updateDataNodeLeaves(cmHandleUpdateRequest)
        then: 'the replace list method is called with correct params'
            mockCpsDataService.replaceListContent(dataspaceName, anchorName, cmHandleXpath, _, noTimeStamp) >> { args ->
                {
                    assert args[3].leaves == expectedPropertiesAfterUpdate
                }
            }
        where: 'following public properties updates are made'
            scenario                          | updatedPublicProperties                    || expectedPropertiesAfterUpdate
            'property added'                  | ['newPubProp1': 'pub-val']                 || [['name': 'publicProp3', 'value': 'publicValue3'], ['name': 'publicProp4', 'value': 'publicValue4'], ['name': 'newPubProp1', 'value': 'pub-val']]
            'property updated'                | ['publicProp4': 'newPubVal']               || [['name': 'publicProp3', 'value': 'publicValue3'], ['name': 'publicProp4', 'value': 'newPubVal']]
            'property removed'                | ['publicProp4': null]                      || [['name': 'publicProp3', 'value': 'publicValue3']]
            'property ignored(value is null)' | ['pub-prop': null]                         || [['name': 'publicProp3', 'value': 'publicValue3'], ['name': 'publicProp4', 'value': 'publicValue4']]
            'all property removed'            | ['publicProp3': null, 'publicProp4': null] || [[]]
    }

    def 'Update DMI Properties: #scenario'() {
        given: 'the CPS service return a CM handle'
            mockCpsDataService.getDataNode(dataspaceName, anchorName, cmHandleXpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'an update cm handle request with public properties updates'
            def cmHandleUpdateRequest = [new CmHandle(cmHandleID: cmHandleId, dmiProperties: updatedDmiProperties)]
        when: 'update data node leaves is called with the update request'
            objectUnderTest.updateDataNodeLeaves(cmHandleUpdateRequest)
        then: 'replace list method should is called with correct params'
            mockCpsDataService.replaceListContent(dataspaceName, anchorName, cmHandleXpath, _, noTimeStamp) >> { args ->
                {
                    assert args[3].leaves == expectedPropertiesAfterUpdate
                }
            }
        where: 'following DMI properties updates are made'
            scenario                          | updatedDmiProperties                               || expectedPropertiesAfterUpdate
            'property added'                  | ['newAdditionalProp1': 'add-value']                || [['name': 'additionalProp1', 'value': 'additionalValue1'], ['name': 'additionalProp2', 'value': 'additionalValue2'], ['name': 'newAdditionalProp1', 'value': 'add-value']]
            'property updated'                | ['additionalProp1': 'newValue']                    || [['name': 'additionalProp2', 'value': 'additionalValue2'], ['name': 'additionalProp1', 'value': 'newValue']]
            'property removed'                | ['additionalProp1': null]                          || [['name': 'additionalProp2', 'value': 'additionalValue2']]
            'property ignored(value is null)' | ['new-prop': null]                                 || [['name': 'additionalProp1', 'value': 'additionalValue1'], ['name': 'additionalProp2', 'value': 'additionalValue2']]
            'all property removed'            | ['additionalProp1': null, 'additionalProp2': null] || [[]]
    }

    def 'Exception thrown when we try to update cmHandle'() {
        given: 'cm handles request'
            def cmHandleUpdateRequest = [new CmHandle(cmHandleID: cmHandleId, publicProperties: [:], dmiProperties: [:])]
        and: 'data node cannot be found'
            mockCpsDataService.getDataNode(*_) >> { throw new DataNodeNotFoundException(dataspaceName, anchorName, cmHandleXpath) }
        when: 'update data node leaves is called using correct parameters'
            objectUnderTest.updateDataNodeLeaves(cmHandleUpdateRequest)
        then: 'data validation exception is thrown'
            def exceptionThrown = thrown(DataValidationException.class)
            assert exceptionThrown.getMessage().contains('DataNode not found')
    }
}
