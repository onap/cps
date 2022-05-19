/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class InventoryPersistenceSpec extends Specification {

    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    def mockCpsDataService = Mock(CpsDataService)

    def mockCompositeStateBuilder = Mock(CompositeStateBuilder)

    def objectUnderTest = new InventoryPersistence(spiedJsonObjectMapper, mockCpsDataService)

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    def 'Get a Cm Handle Composite State'() {
        given: 'a valid cm handle id'
            def cmHandleId = 'Some-Cm-Handle'
            def dataNode = new DataNode(leaves: ['cm-handle-state': 'ADVISED'])
            def compositeState = new CompositeState(cmHandleState: 'ADVISED')
        when: 'get cm handle state is invoked'
            objectUnderTest.getCmHandleState(cmHandleId)
        then: 'cps data service get data node is invoked and returns a valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']/state', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'a composite state is returned from the data node'
            mockCompositeStateBuilder.fromDataNode(dataNode) >> compositeState
        and: 'the composite state contains the correct values'
            compositeState.cmHandleState == CmHandleState.ADVISED
    }

    def 'Update Cm Handle with #scenario State'() {
        given: 'a cm handle and a composite state'
            def cmHandleId = 'Some-Cm-Handle'
            def compositeState = new CompositeState(cmHandleState: cmHandleState)
        when: 'update cm handle state is invoked with the #scenario state'
            objectUnderTest.updateCmHandleState(cmHandleId, compositeState, formattedDateAndTime)
        then: 'update node leaves is invoked with the correct params'
            1 * mockCpsDataService.replaceNodeTree('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']', expectedJsonData, _ as OffsetDateTime)
        where: 'the following states are used'
             scenario | cmHandleState        || expectedJsonData
            'READY'   | CmHandleState.READY  || '{"state":{"cm-handle-state":"READY","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
            'LOCKED'  | CmHandleState.LOCKED || '{"state":{"cm-handle-state":"LOCKED","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
    }

}
