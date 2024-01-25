/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events.lcm

import static org.onap.cps.ncmp.api.impl.inventory.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.impl.inventory.CmHandleState.DELETING
import static org.onap.cps.ncmp.api.impl.inventory.CmHandleState.READY

import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.CompositeState
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.events.lcm.v2.Values
import spock.lang.Specification

class LcmEventsCreatorSpec extends Specification {

    def objectUnderTest = new LcmEventsCreator()
    def cmHandleId = 'test-cm-handle'

    def 'Map the LcmEvent for #operation'() {
        given: 'NCMP cm handle details with current and old properties'
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: existingCmHandleState),
                    publicProperties: existingPublicProperties)
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: targetCmHandleState),
                publicProperties: targetPublicProperties)
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'event payload is mapped correctly with correct cmhandle id'
            assert result.data.cmHandleId == cmHandleId
        and: 'it should have correct old state and properties'
            assert result.data.oldValues.cmHandleState == expectedExistingCmHandleState
            assert result.data.oldValues.cmHandleProperties == [expectedExistingPublicProperties]
        and: 'the correct new state and properties'
            assert result.data.newValues.cmHandleProperties == [expectedTargetPublicProperties]
            assert result.data.newValues.cmHandleState == expectedTargetCmHandleState
        where: 'following parameters are provided'
            operation  | existingCmHandleState | targetCmHandleState | existingPublicProperties                                   | targetPublicProperties         || expectedExistingPublicProperties                           | expectedTargetPublicProperties | expectedExistingCmHandleState | expectedTargetCmHandleState
            'UPDATE'   | ADVISED               | READY               | ['publicProperty1': 'value1', 'publicProperty2': 'value2'] | ['publicProperty1': 'value11'] || ['publicProperty1': 'value1', 'publicProperty2': 'value2'] | ['publicProperty1': 'value11'] | Values.CmHandleState.ADVISED  | Values.CmHandleState.READY
            'DELETING' | READY                 | DELETING            | ['publicProperty1': 'value3', 'publicProperty2': 'value4'] | ['publicProperty1': 'value33'] || ['publicProperty1': 'value3', 'publicProperty2': 'value4'] | ['publicProperty1': 'value33'] | Values.CmHandleState.READY    | Values.CmHandleState.DELETING
            'CHANGE'   | READY                 | READY               | ['publicProperty1': 'value3', 'publicProperty2': 'value4'] | ['publicProperty1': 'value33'] || ['publicProperty1': 'value3', 'publicProperty2': 'value4'] | ['publicProperty1': 'value33'] | null                          | null
    }

    def 'Map the LcmEvent for all properties NO CHANGE'() {
        given: 'NCMP cm handle details without any changes'
            def publicProperties = ['publicProperty1': 'value3', 'publicProperty2': 'value4']
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: READY),
                    publicProperties: publicProperties)
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: READY),
                    publicProperties: publicProperties)
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'Properties are just the one which are same'
            assert result.data.oldValues == null
            assert result.data.newValues == null
    }

    def 'Map the LcmEvent for operation CREATE'() {
        given: 'NCMP cm handle details'
            def targetNcmpServiceCmhandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: READY),
                publicProperties: ['publicProperty1': 'value11', 'publicProperty2': 'value22'])
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProperty1': 'value1', 'publicProperty2': 'value2'])
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmhandle, existingNcmpServiceCmHandle)
        then: 'event payload is mapped correctly'
            assert result.data.cmHandleId == cmHandleId
            assert result.data.newValues.cmHandleState == Values.CmHandleState.READY
            assert result.data.newValues.dataSyncEnabled == false
            assert result.data.newValues.cmHandleProperties == [['publicProperty1': 'value11', 'publicProperty2': 'value22']]
        and: 'it should not have any old values'
            assert result.data.oldValues == null
    }

    def 'Map the LcmEvent for DELETE operation'() {
        given: 'NCMP cm handle details'
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: false, cmHandleState: CmHandleState.DELETED),
                publicProperties: ['publicProperty1': 'value11', 'publicProperty2': 'value22'])
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: true, cmHandleState: DELETING),
                publicProperties: ['publicProperty1': 'value1'])
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'event payload is mapped correctly '
            assert result.data.cmHandleId == cmHandleId
            assert result.data.oldValues == null
            assert result.data.newValues == null
    }

    def 'Map the LcmEvent for datasync flag transition from #operation'() {
        given: 'NCMP cm handle details with current and old details'
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: existingDataSyncEnableFlag, cmHandleState: ADVISED))
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(dataSyncEnabled: targetDataSyncEnableFlag, cmHandleState: READY))
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'event payload is mapped correctly with correct cmhandle id'
            assert result.data.cmHandleId == cmHandleId
        and: 'it should have correct old values'
            assert result.data.oldValues.cmHandleState == Values.CmHandleState.ADVISED
            assert result.data.oldValues.dataSyncEnabled == existingDataSyncEnableFlag
        and: 'the correct new values'
            assert result.data.newValues.cmHandleState == Values.CmHandleState.READY
            assert result.data.newValues.dataSyncEnabled == targetDataSyncEnableFlag
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
            assert result.data.oldValues.dataSyncEnabled == null
            assert result.data.newValues.dataSyncEnabled == null
        where: 'following parameters are provided'
            operation        | existingDataSyncEnableFlag | targetDataSyncEnableFlag
            'false to false' | false                      | false
            'true to true'   | true                       | true
            'null to null'   | null                       | null

    }

    def 'Map the LcmEvent for alternateIds when #scenario'() {
        given: 'NCMP cm handle details with current and old alternate IDs'
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, alternateId: existingAlternateId, compositeState: new CompositeState(dataSyncEnabled: false))
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, alternateId: targetAlternateId, compositeState: new CompositeState(dataSyncEnabled: false))
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'the alternate ID is present or is an empty string in the payload'
            assert result.data.alternateId == expectedPayloadAlternateId
        and: 'the new and old alternate IDs are present or null'
            if (result.data.newValues == null) {
                assert result.data.oldValues == expectedExistingAlternateId
                assert result.data.newValues == expectedTargetAlternateId
            } else {
                assert result.data.oldValues.alternateId == expectedExistingAlternateId
                assert result.data.newValues.alternateId == expectedTargetAlternateId
            }
        where: 'the following alternate IDs are provided'
            scenario                               | existingAlternateId | targetAlternateId || expectedExistingAlternateId | expectedTargetAlternateId | expectedPayloadAlternateId
            'no new or old alternate ID'           | null                | null              || null                        | null                      | ''
            'same new and old alternate ID'        | 'someAlternateId'   | 'someAlternateId' || null                        | null                      | 'someAlternateId'
            'new alternate id no old alternate ID' | null                | 'someAlternateId' || ''                          | 'someAlternateId'         | 'someAlternateId'
    }
}