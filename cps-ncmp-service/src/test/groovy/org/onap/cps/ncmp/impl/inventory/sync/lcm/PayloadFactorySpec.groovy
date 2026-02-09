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

import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.events.lcm.Values
import spock.lang.Specification

class PayloadFactorySpec extends Specification {
    def 'Create payload for create operation.'() {
        given: 'a new cm handle'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch',
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: ['prop1': 'value1']
            )
        when: 'payload is created for create'
            def result = PayloadFactory.createPayloadV1(LcmEventType.CREATE, null, ncmpServiceCmHandle)
        then: 'new values are populated'
            assert result.cmHandleId == 'ch'
            assert result.newValues.dataSyncEnabled == true
            assert result.newValues.cmHandleState == Values.CmHandleState.READY
            assert result.newValues.cmHandleProperties == [['prop1': 'value1']]
        and: 'old values are not set'
            assert result.oldValues == null
    }

    def 'Create payload when no changes detected.'() {
        given: 'current and target cm handles with same properties'
            def currentCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: ['prop1': 'value1']
            )
            def targetCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch',
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: ['prop1': 'value1']
            )
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(LcmEventType.UPDATE, currentCmHandle, targetCmHandle)
        then: 'no updates are detected'
            assert result.cmHandleId == 'ch'
            assert result.oldValues == null
            assert result.newValues == null
    }

    def 'Create payload when data sync flag changes.'() {
        given: 'current and target cm handles with different data sync flags'
            def currentCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: CmHandleState.READY),
                publicProperties: [:]
            )
            def targetCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: [:]
            )
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(LcmEventType.UPDATE,currentCmHandle, targetCmHandle)
        then: 'data sync flag change is detected'
            assert result.oldValues.dataSyncEnabled == false
            assert result.newValues.dataSyncEnabled == true
    }

    def 'Create payload when cm handle state changes.'() {
        given: 'current and target cm handles with different states'
            def currentCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.ADVISED),
                publicProperties: [:]
            )
            def targetCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: [:]
            )
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(LcmEventType.UPDATE,currentCmHandle, targetCmHandle)
        then: 'state change is detected'
            assert result.oldValues.cmHandleState == Values.CmHandleState.ADVISED
            assert result.newValues.cmHandleState == Values.CmHandleState.READY
    }

    def 'Create payload when public properties change.'() {
        given: 'current and target cm handles with different properties'
            def currentCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: ['prop1': 'old value', 'prop2': 'to be deleted', 'prop4': 'unchanged']
            )
            def targetCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: ['prop1': 'new value', 'prop3': 'new', 'prop4': 'unchanged']
            )
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(LcmEventType.UPDATE,currentCmHandle, targetCmHandle)
        then: 'property changes are detected'
            assert result.oldValues.cmHandleProperties[0]['prop1'] == 'old value'
            assert result.oldValues.cmHandleProperties[0]['prop2'] == 'to be deleted'
            assert result.newValues.cmHandleProperties[0]['prop1'] == 'new value'
            assert result.newValues.cmHandleProperties[0]['prop3'] == 'new'
        and: 'unchanged property is not included in the result'
            assert !result.oldValues.cmHandleProperties[0].containsKey('prop4')
            assert !result.newValues.cmHandleProperties[0].containsKey('prop4')
    }

    def 'Create payload when multiple changes occur.'() {
        given: 'current and target cm handles with multiple differences'
            def currentCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: CmHandleState.ADVISED),
                publicProperties: ['prop1': 'value1']
            )
            def targetCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: ['prop1': 'newValue1']
            )
        when: 'payload is created'
            def result = PayloadFactory.createPayloadV1(LcmEventType.UPDATE,currentCmHandle, targetCmHandle)
        then: 'all changes are detected'
            assert result.oldValues.dataSyncEnabled == false
            assert result.newValues.dataSyncEnabled == true
            assert result.oldValues.cmHandleState == Values.CmHandleState.ADVISED
            assert result.newValues.cmHandleState == Values.CmHandleState.READY
            assert result.oldValues.cmHandleProperties[0]['prop1'] == 'value1'
            assert result.newValues.cmHandleProperties[0]['prop1'] == 'newValue1'
    }

    def 'Create payload for delete operation.'() {
        given: 'a cm handle being deleted'
            def currentCmHandle = new NcmpServiceCmHandle(
                compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: CmHandleState.READY),
                publicProperties: ['prop1': 'value1']
            )
            def targetCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch',
                compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: CmHandleState.DELETED),
                publicProperties: ['prop1': 'value1']
            )
        when: 'payload is created for delete'
            def result = PayloadFactory.createPayloadV1(LcmEventType.DELETE, currentCmHandle, targetCmHandle)
        then: 'cmHandleId is populated'
            assert result.cmHandleId == 'ch'
        and: 'no values are populated'
            assert result.oldValues == null
            assert result.newValues == null
    }
}
