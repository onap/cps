/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.event.lcm

import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.ncmp.cmhandle.event.lcm.Values
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.DELETING
import static org.onap.cps.ncmp.api.inventory.CmHandleState.READY

class LcmEventsCreatorSpec extends Specification {

    def objectUnderTest = new LcmEventsCreator()
    def cmHandleId = 'test-cm-handle'

    def 'Map the LcmEvent for #operation'() {
        given: 'NCMP cm handle details with current and old details'
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: existingCmHandleState),
                    publicProperties: existingPublicProperties)
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: targetCmHandleState),
                publicProperties: targetPublicProperties)
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'event header is mapped correctly'
            assert result.eventSource == 'org.onap.ncmp'
            assert result.eventCorrelationId == cmHandleId
            assert result.eventType == LcmEventType.UPDATE.eventType
        and: 'event payload is mapped correctly with correct cmhandle id'
            assert result.event.cmHandleId == cmHandleId
        and: 'it should have correct old values'
            assert result.event.oldValues.cmHandleState == expectedExistingCmHandleState
            assert result.event.oldValues.cmHandleProperties == [expectedExistingPublicProperties]
            assert result.event.oldValues.dataSyncEnabled == null
        and: 'the correct new values'
            assert result.event.newValues.cmHandleProperties == [expectedTargetPublicProperties]
            assert result.event.newValues.cmHandleState == expectedTargetCmHandleState
            assert result.event.newValues.dataSyncEnabled == null
        where: 'following parameters are provided'
            operation   | existingCmHandleState | targetCmHandleState | existingPublicProperties                                    | targetPublicProperties         || expectedExistingPublicProperties                            | expectedTargetPublicProperties  | expectedExistingCmHandleState | expectedTargetCmHandleState
            'UPDATE'    | ADVISED               | READY               | ['publicProperty1': 'value1', 'publicProperty2': 'value2']  | ['publicProperty1': 'value11'] || ['publicProperty1': 'value1', 'publicProperty2': 'value2']  | ['publicProperty1': 'value11']  | Values.CmHandleState.ADVISED  | Values.CmHandleState.READY
            'DELETING'  | READY                 | DELETING            | ['publicProperty1': 'value3', 'publicProperty2': 'value4']  | ['publicProperty1': 'value33'] || ['publicProperty1': 'value3', 'publicProperty2': 'value4']  | ['publicProperty1': 'value33']  | Values.CmHandleState.READY    | Values.CmHandleState.DELETING
            'CHANGE'    | READY                 | READY               | ['publicProperty1': 'value3', 'publicProperty2': 'value4']  | ['publicProperty1': 'value33'] || ['publicProperty1': 'value3', 'publicProperty2': 'value4']  | ['publicProperty1': 'value33']  | null                          | null
    }

    def 'Map the LcmEvent for all properties NO CHANGE'() {
        given: 'NCMP cm handle details with current and old details'
            def publicProperties = ['publicProperty1': 'value3', 'publicProperty2': 'value4']
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: READY),
                    publicProperties: publicProperties)
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: READY),
                    publicProperties: publicProperties)
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'Properties are just the one which are same'
            assert result.event.oldValues == null
            assert result.event.newValues == null
    }

    def 'Map the LcmEvent for operation CREATE'() {
        given: 'NCMP cm handle details'
            def targetNcmpServiceCmhandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: READY),
                publicProperties: ['publicProperty1': 'value11', 'publicProperty2': 'value22'])
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProperty1': 'value1', 'publicProperty2': 'value2'])
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmhandle, existingNcmpServiceCmHandle)
        then: 'event header is mapped correctly'
            assert result.eventSource == 'org.onap.ncmp'
            assert result.eventCorrelationId == cmHandleId
            assert result.eventType == LcmEventType.CREATE.eventType
        and: 'event payload is mapped correctly'
            assert result.event.cmHandleId == cmHandleId
            assert result.event.newValues.cmHandleState == Values.CmHandleState.READY
            assert result.event.newValues.dataSyncEnabled == false
            assert result.event.newValues.cmHandleProperties == [['publicProperty1': 'value11', 'publicProperty2': 'value22']]
        and: 'it should not have any old values'
            assert result.event.oldValues == null
    }

    def 'Map the LcmEvent for DELETE operation'() {
        given: 'NCMP cm handle details'
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: CmHandleState.DELETED),
                publicProperties: ['publicProperty1': 'value11', 'publicProperty2': 'value22'])
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: DELETING),
                publicProperties: ['publicProperty1': 'value1'])
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'event header is mapped correctly'
            assert result.eventSource == 'org.onap.ncmp'
            assert result.eventCorrelationId == cmHandleId
            assert result.eventType == LcmEventType.DELETE.eventType
        and: 'event payload is mapped correctly '
            assert result.event.cmHandleId == cmHandleId
            assert result.event.oldValues == null
            assert result.event.newValues == null
    }

    def 'Map the LcmEvent for datasync flag transition from #operation'() {
        given: 'NCMP cm handle details with current and old details'
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: existingDataSyncEnableFlag, cmHandleState: ADVISED))
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: targetDataSyncEnableFlag, cmHandleState: READY))
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'event header is mapped correctly'
            assert result.eventSource == 'org.onap.ncmp'
            assert result.eventCorrelationId == cmHandleId
            assert result.eventType == LcmEventType.UPDATE.eventType
        and: 'event payload is mapped correctly with correct cmhandle id'
            assert result.event.cmHandleId == cmHandleId
        and: 'it should have correct old values'
            assert result.event.oldValues.cmHandleState == Values.CmHandleState.ADVISED
            assert result.event.oldValues.dataSyncEnabled == existingDataSyncEnableFlag
        and: 'the correct new values'
            assert result.event.newValues.cmHandleState == Values.CmHandleState.READY
            assert result.event.newValues.dataSyncEnabled == targetDataSyncEnableFlag
        where: 'following parameters are provided'
            operation       | existingDataSyncEnableFlag | targetDataSyncEnableFlag
            'false to true' | false                      | true
            'false to null' | false                      | null
            'true to false' | true                       | false
            'true to null'  | true                       | null
            'null to true'  | null                       | true
            'null to false' | null                       | false

    }

    def 'Map the LcmEvent for datasync flag for same transition from #operation'() {
        given: 'NCMP cm handle details with current and old details'
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: existingDataSyncEnableFlag, cmHandleState: ADVISED))
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: targetDataSyncEnableFlag, cmHandleState: READY))
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'the data sync flag is not present in the event'
            assert result.event.oldValues.dataSyncEnabled == null
            assert result.event.newValues.dataSyncEnabled == null
        where: 'following parameters are provided'
            operation        | existingDataSyncEnableFlag | targetDataSyncEnableFlag
            'false to false' | false                      | false
            'true to true'   | true                       | true
            'null to null'   | null                       | null

    }
}