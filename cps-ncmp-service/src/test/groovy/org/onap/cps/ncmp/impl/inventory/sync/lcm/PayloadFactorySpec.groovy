/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory.sync.lcm

import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.DELETED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY
import static org.onap.cps.ncmp.events.lcm.Values.CmHandleState.ADVISED as LCM_ADVISED
import static org.onap.cps.ncmp.events.lcm.Values.CmHandleState.READY as LCM_READY
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.CREATE
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.DELETE
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.UPDATE

import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle

class PayloadFactorySpec extends Specification {

    def defaultState = new CompositeState(dataSyncEnabled: true, cmHandleState: READY)

    def 'Create payload for create operation.'() {
        given: 'a new cm handle'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch1', compositeState: defaultState, publicProperties: [prop1: 'value1'] )
        when: 'payload is created for create'
            def result = PayloadFactory.createPayloadV1(CREATE, null, ncmpServiceCmHandle)
        then: 'new values are populated'
            assert result.cmHandleId == 'ch1'
            assert result.newValues.dataSyncEnabled == true
            assert result.newValues.cmHandleState == LCM_READY
        and: 'the public properties are store as a list (fluke in old schema) of maps as cm handle properties (legacy name)'
            assert result.newValues.cmHandleProperties == [[prop1: 'value1']]
        and: 'old values are not set'
            assert result.oldValues == null
    }

    def 'Create payload when no changes detected.'() {
        given: 'current and target cm handles with same properties'
            def currentCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch1', compositeState: defaultState, publicProperties: [prop1: 'value1'] )
            def targetCmHandle  = new NcmpServiceCmHandle(cmHandleId: 'ch1', compositeState: defaultState, publicProperties: [prop1: 'value1'])
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(UPDATE, currentCmHandle, targetCmHandle)
        then: 'no updates are detected'
            assert result.cmHandleId == 'ch1'
            assert result.oldValues == null
            assert result.newValues == null
    }

    def 'Create payload when data sync flag changes.'() {
        given: 'current and target cm handles with different data sync flags'
            def currentCmHandle = new NcmpServiceCmHandle(compositeState: defaultState)
            def targetCmHandle  = new NcmpServiceCmHandle(compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: READY))
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(UPDATE,currentCmHandle, targetCmHandle)
        then: 'data sync flag change is detected'
            assert result.oldValues.dataSyncEnabled == true
            assert result.newValues.dataSyncEnabled == false
    }

    def 'Create payload when cm handle state changes.'() {
        given: 'current and target cm handles with different states'
            def currentCmHandle = new NcmpServiceCmHandle(compositeState: defaultState)
            def targetCmHandle  = new NcmpServiceCmHandle(compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: ADVISED))
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(UPDATE,currentCmHandle, targetCmHandle)
        then: 'state change is detected'
            assert result.oldValues.cmHandleState == LCM_READY
            assert result.newValues.cmHandleState == LCM_ADVISED
    }

    def 'Create payload when public properties change.'() {
        given: 'current and target cm handles with different properties'
            def currentCmHandle = new NcmpServiceCmHandle(publicProperties: [prop1: 'old value', prop2: 'to be deleted', prop4: 'unchanged'], compositeState: defaultState)
            def targetCmHandle  = new NcmpServiceCmHandle(publicProperties: [prop1: 'new value', prop3: 'new', prop4: 'unchanged'], compositeState: defaultState)
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(UPDATE,currentCmHandle, targetCmHandle)
        then: 'property changes are detected'
            assert result.oldValues.cmHandleProperties[0].prop1 == 'old value'
            assert result.oldValues.cmHandleProperties[0].prop2 == 'to be deleted'
            assert result.newValues.cmHandleProperties[0].prop1 == 'new value'
            assert result.newValues.cmHandleProperties[0].prop3 == 'new'
        and: 'unchanged property is still included in the result'
            assert result.oldValues.cmHandleProperties[0].prop4 == 'unchanged'
            assert result.newValues.cmHandleProperties[0].prop4 == 'unchanged'
    }

    def 'Create payload when multiple changes occur.'() {
        given: 'current and target cm handles with multiple differences'
            def currentCmHandle = new NcmpServiceCmHandle(compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: ADVISED), publicProperties: [prop1: 'value1'])
            def targetCmHandle  = new NcmpServiceCmHandle(compositeState: defaultState, publicProperties: [prop1: 'newValue1'])
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(UPDATE,currentCmHandle, targetCmHandle)
        then: 'all changes are detected'
            assert result.oldValues.dataSyncEnabled == false
            assert result.newValues.dataSyncEnabled == true
            assert result.oldValues.cmHandleState == LCM_ADVISED
            assert result.newValues.cmHandleState == LCM_READY
            assert result.oldValues.cmHandleProperties[0].prop1 == 'value1'
            assert result.newValues.cmHandleProperties[0].prop1 == 'newValue1'
    }

    def 'Create payload for delete operation.'() {
        given: 'a cm handle being deleted'
            def currentCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch1', compositeState: defaultState, publicProperties: [prop1: 'value1'])
            def targetCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch1', compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: DELETED), publicProperties: [prop1: 'value1'])
        when: 'payload is created for delete'
            def result = PayloadFactory.createPayloadV1(DELETE, currentCmHandle, targetCmHandle)
        then: 'cmHandleId is populated'
            assert result.cmHandleId == 'ch1'
        and: 'no values are populated'
            assert result.oldValues == null
            assert result.newValues == null
    }

    def 'Create V2 payload when #scenario changes.'() {
        given: 'current and target cm handles'
            def currentCmHandle = new NcmpServiceCmHandle(compositeState: defaultState, publicProperties: [:],(propertyName):currentValue)
            def targetCmHandle  = new NcmpServiceCmHandle(compositeState: defaultState, publicProperties: [:],(propertyName):targetValue)
        when: 'V2 payload is created'
            def result = PayloadFactory.createPayloadV2(UPDATE, currentCmHandle, targetCmHandle)
        then: 'changes are detected in V2 payload'
            assert result.oldValues[propertyName] == currentValue
            assert result.newValues[propertyName] == targetValue
        where:
            scenario                 | propertyName             | currentValue | targetValue
            'alternateId'            | 'alternateId'            | 'old-alt-id' | 'new-alt-id'
            'moduleSetTag'           | 'moduleSetTag'           | 'old-tag'    | 'new-tag'
            'dataProducerIdentifier' | 'dataProducerIdentifier' | 'old-dpi'    | 'new-dpi'
    }

    def 'Create V2 payload when public properties change.'() {
        given: 'current and target cm handles with different properties'
            def currentCmHandle = new NcmpServiceCmHandle(compositeState: defaultState, publicProperties: [oldProp:'oldValue'])
            def targetCmHandle  = new NcmpServiceCmHandle(compositeState: defaultState, publicProperties: [newProp:'newValue'])
        when: 'V2 payload is created'
            def result = PayloadFactory.createPayloadV2(UPDATE, currentCmHandle, targetCmHandle)
        then: 'property changes are detected in V2 payload as "chHandleProperties"'
            assert result.oldValues['cmHandleProperties'] == [oldProp:'oldValue']
            assert result.newValues['cmHandleProperties'] == [newProp:'newValue']
    }

    def 'Create V2 payload when cm handle state changes and dataSyncEnabled #dataSyncScenario.'() {
        given: 'current and target cm handles with different states'
            def currentCmHandle = new NcmpServiceCmHandle(compositeState: defaultState)
            def targetCmHandle  = new NcmpServiceCmHandle(compositeState: new CompositeState(dataSyncEnabled: targetDataSync, cmHandleState: ADVISED))
        when: 'V2 payload is created'
            def result = PayloadFactory.createPayloadV2(UPDATE, currentCmHandle, targetCmHandle)
        then: 'state change is detected in V2 payload'
            assert result.oldValues['cmHandleState'] == 'READY'
            assert result.newValues['cmHandleState'] == 'ADVISED'
        and: 'dataSyncEnabled is handled correctly'
            assert result.oldValues['dataSyncEnabled'] == expectedOldDataSync
            assert result.newValues['dataSyncEnabled'] == expectedNewDataSync
        where:
            dataSyncScenario | targetDataSync | expectedOldDataSync | expectedNewDataSync
            'changes'        | false          | true                | false
            'does not change'| true           | null                | null
    }
}
