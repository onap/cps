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

package org.onap.cps.ncmp.impl.inventory.sync.lcm

import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.events.lcm.v1.Values
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.DELETING
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY

class LcmEventsCreatorSpec extends Specification {

    LcmEventHeaderMapper lcmEventsHeaderMapper = Mappers.getMapper(LcmEventHeaderMapper)

    def objectUnderTest = new LcmEventsCreator(lcmEventsHeaderMapper)
    def cmHandleId = 'test-cm-handle'

    def 'Map the LcmEvent for #operation'() {
        given: 'NCMP cm handle details with current and old properties'
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
        and: 'it should have correct old state and properties'
            assert result.event.oldValues.cmHandleState == expectedExistingCmHandleState
            assert result.event.oldValues.cmHandleProperties == [expectedExistingPublicProperties]
        and: 'the correct new state and properties'
            assert result.event.newValues.cmHandleProperties == [expectedTargetPublicProperties]
            assert result.event.newValues.cmHandleState == expectedTargetCmHandleState
        where: 'following parameters are provided'
            operation   | existingCmHandleState | targetCmHandleState | existingPublicProperties                                    | targetPublicProperties         || expectedExistingPublicProperties                            | expectedTargetPublicProperties  | expectedExistingCmHandleState | expectedTargetCmHandleState
            'UPDATE'    | ADVISED               | READY               | ['publicProperty1': 'value1', 'publicProperty2': 'value2']  | ['publicProperty1': 'value11'] || ['publicProperty1': 'value1', 'publicProperty2': 'value2']  | ['publicProperty1': 'value11']  | Values.CmHandleState.ADVISED  | Values.CmHandleState.READY
            'DELETING'  | READY                 | DELETING            | ['publicProperty1': 'value3', 'publicProperty2': 'value4']  | ['publicProperty1': 'value33'] || ['publicProperty1': 'value3', 'publicProperty2': 'value4']  | ['publicProperty1': 'value33']  | Values.CmHandleState.READY    | Values.CmHandleState.DELETING
            'CHANGE'    | READY                 | READY               | ['publicProperty1': 'value3', 'publicProperty2': 'value4']  | ['publicProperty1': 'value33'] || ['publicProperty1': 'value3', 'publicProperty2': 'value4']  | ['publicProperty1': 'value33']  | null                          | null
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

    def 'Map the LcmEventHeader'() {
        given: 'NCMP cm handle details with current and old details'
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(cmHandleState: ADVISED))
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeState(cmHandleState: READY))
        when: 'the event header is populated'
            def result = objectUnderTest.populateLcmEventHeader(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'the header has fields populated'
            assert result.eventCorrelationId == cmHandleId
            assert result.eventId != null
    }

    def 'Map the LcmEvent for alternate ID, data producer identifier, and module set tag when they contain #scenario'() {
        given: 'NCMP cm handle details with current and old values for alternate ID, module set tag, and data producer identifier'
            def existingNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, alternateId: existingAlternateId, moduleSetTag: existingModuleSetTag, dataProducerIdentifier: existingDataProducerIdentifier, compositeState: new CompositeState(dataSyncEnabled: false))
            def targetNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, alternateId: targetAlternateId, moduleSetTag: targetModuleSetTag, dataProducerIdentifier: targetDataProducerIdentifier, compositeState: new CompositeState(dataSyncEnabled: false))
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle)
        then: 'the alternate ID, module set tag, and data producer identifier are present or are an empty string in the payload'
            assert result.event.alternateId == targetAlternateId
            assert result.event.moduleSetTag == targetModuleSetTag
            assert result.event.dataProducerIdentifier == targetDataProducerIdentifier
        where: 'the following values are provided for the alternate ID, module set tag, and data producer identifier'
            scenario                                     | existingAlternateId | targetAlternateId | existingModuleSetTag | targetModuleSetTag | existingDataProducerIdentifier | targetDataProducerIdentifier
            'same target and existing values'            | 'someAlternateId'   | 'someAlternateId' | 'someModuleSetTag'   | 'someModuleSetTag' | 'someDataProducerIdentifier'   | 'someDataProducerIdentifier'
            'blank target and existing values'           | ''                  | ''                | ''                   | ''                 | ''                             | ''
            'new target value and blank existing values' | ''                  | 'someAlternateId' | ''                   | 'someAlternateId'  | ''                             | 'someDataProducerIdentifier'
    }
}
