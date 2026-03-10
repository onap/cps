/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory.models

import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.LockReasonCategory
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATAJOBS_READ
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATAJOBS_WRITE
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.MODEL

class YangModelCmHandleSpec extends Specification {

    def 'Creating yang model cm handle from a service api cm handle.'() {
        given: 'a cm handle with properties'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.cmHandleId = 'cm-handle-id01'
            ncmpServiceCmHandle.additionalProperties = [myAdditionalProperty:'value1']
            ncmpServiceCmHandle.publicProperties = [myPublicProperty:'value2']
            ncmpServiceCmHandle.dmiProperties = [myDmiProperty:'value3']
        and: 'with a composite state'
            def compositeState = new CompositeStateBuilder()
                .withCmHandleState(CmHandleState.LOCKED)
                .withLastUpdatedTime('some-update-time')
                .withLockReason(LockReasonCategory.MODULE_SYNC_FAILED, 'locked details')
                .withOperationalDataStores(DataStoreSyncState.SYNCHRONIZED, 'some-sync-time').build()
            ncmpServiceCmHandle.setCompositeState(compositeState)
        when: 'it is converted to a yang model cm handle'
            def objectUnderTest = YangModelCmHandle.toYangModelCmHandle(new DmiPluginRegistration(), ncmpServiceCmHandle,'my-module-set-tag', 'my-alternate-id', 'my-data-producer-identifier', 'ADVISED', 'my-dmi-property')
        then: 'the result has the right size'
            assert objectUnderTest.additionalProperties.size() == 1
        and: 'the result has the correct values for module set tag, alternate ID, and data producer identifier'
            assert objectUnderTest.moduleSetTag == 'my-module-set-tag'
            assert objectUnderTest.alternateId == 'my-alternate-id'
            assert objectUnderTest.dataProducerIdentifier == 'my-data-producer-identifier'
        and: 'the additional property in the result has the correct name and value'
            assert objectUnderTest.additionalProperties[0].name == 'myAdditionalProperty'
            assert objectUnderTest.additionalProperties[0].value == 'value1'
        and: 'the public property in the result has the correct name and value'
            assert objectUnderTest.publicProperties[0].name == 'myPublicProperty'
            assert objectUnderTest.publicProperties[0].value == 'value2'
        and: 'the composite state matches the composite state of the ncmpServiceCmHandle'
            objectUnderTest.getCompositeState().cmHandleState == CmHandleState.LOCKED
            objectUnderTest.getCompositeState() == ncmpServiceCmHandle.getCompositeState()
        and: 'the cm-handle-state is correct'
            assert objectUnderTest.cmHandleStatus == 'ADVISED'
        and: 'the dmi-properties are correct'
            assert objectUnderTest.dmiProperties == 'my-dmi-property'
    }

    def 'Resolve DMI service name: #scenario and #requiredService service require.'() {
        given: 'a yang model cm handle'
            def yangModelCmHandle = new YangModelCmHandle(
                    dmiServiceName: dmiServiceName,
                    dmiDataServiceName: dmiDataServiceName,
                    dmiModelServiceName: dmiModelServiceName,
                    dmiDatajobsReadServiceName: dmiDatajobsReadServiceName,
                    dmiDatajobsWriteServiceName: dmiDatajobsWriteServiceName
            )
        expect:
            assert yangModelCmHandle.resolveDmiServiceName(requiredService) == expectedService
        where:
            scenario                                  | dmiServiceName     | dmiDataServiceName | dmiModelServiceName | dmiDatajobsReadServiceName | dmiDatajobsWriteServiceName | requiredService  || expectedService
            'specific data service registered'        | 'common service'   | 'data service'     | 'does not matter'   | null                       | null                        | DATA             || 'data service'
            'specific model service registered'       | 'common service'   | 'does not matter'  | 'model service'     | null                       | null                        | MODEL            || 'model service'
            'specific datajobs read service'          | 'common service'   | 'does not matter'  | 'does not matter'   | 'datajobs-read'            | null                        | DATAJOBS_READ    || 'datajobs-read'
            'specific datajobs write service'         | 'common service'   | 'does not matter'  | 'does not matter'   | null                       | 'datajobs-write'            | DATAJOBS_WRITE   || 'datajobs-write'
            'only common service for data'            | 'common service'   | null               | 'does not matter'   | null                       | null                        | DATA             || 'common service'
            'only common service for model'           | 'common service'   | 'does not matter'  | null                | null                       | null                        | MODEL            || 'common service'
            'only common for datajobs read'           | 'common service'   | 'does not matter'  | 'does not matter'   | null                       | null                        | DATAJOBS_READ    || 'common service'
            'only common for datajobs write'          | 'common service'   | 'does not matter'  | 'does not matter'   | null                       | null                        | DATAJOBS_WRITE   || 'common service'
            'specific data service, empty common'     | ''                 | 'data service'     | 'does not matter'   | null                       | null                        | DATA             || 'data service'
            'specific model service, empty common'    | ''                 | 'does not matter'  | 'model service'     | null                       | null                        | MODEL            || 'model service'
            'specific data service, blank common'     | '   '              | 'data service'     | 'does not matter'   | null                       | null                        | DATA             || 'data service'
            'specific model service, blank common'    | '   '              | 'does not matter'  | 'model service'     | null                       | null                        | MODEL            || 'model service'
            'specific data service, null common'      | null               | 'data service'     | 'does not matter'   | null                       | null                        | DATA             || 'data service'
            'specific model service, null common'     | null               | 'does not matter'  | 'model service'     | null                       | null                        | MODEL            || 'model service'
            'fallback to common for data'             | 'common service'   | null               | 'does not matter'   | null                       | null                        | DATA             || 'common service'
            'fallback to common for model'            | 'common service'   | 'does not matter'  | null                | null                       | null                        | MODEL            || 'common service'
            'only model service, data request'        | null               | null               | 'model service'     | null                       | null                        | DATA             || null
            'only data service, model request'        | null               | 'data service'     | null                | null                       | null                        | MODEL            || null
    }

    def 'Yang Model Cm Handle Deep Copy.'() {
        given: 'a yang model cm handle'
            def original = new YangModelCmHandle(id: 'original id',
                publicProperties: [new YangModelCmHandle.Property('publicProperty', 'value1')],
                additionalProperties: [new YangModelCmHandle.Property('additionalProperty', 'value2')],
                compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED, dataSyncEnabled: false))
        when: 'a deep copy is created'
            def copy = YangModelCmHandle.deepCopyOf(original)
        then: 'the objects are equal'
            assert original == copy
            assert original.equals(copy)
        and: 'have the same hash code'
            assert original.hashCode() == copy.hashCode()
        when: 'mutate the original yang model cm handle'
            original.id = 'changed id'
            original.publicProperties = [new YangModelCmHandle.Property('updatedPublicProperty', 'some new value')]
            original.additionalProperties = [new YangModelCmHandle.Property('updatedAdditionalProperty', 'some new value')]
            original.compositeState.cmHandleState = CmHandleState.READY
            original.compositeState.dataSyncEnabled = true
        then: 'there is no change in the copied object'
            assert copy.id == 'original id'
            assert copy.publicProperties == [new YangModelCmHandle.Property('publicProperty', 'value1')]
            assert copy.additionalProperties == [new YangModelCmHandle.Property('additionalProperty', 'value2')]
            assert copy.compositeState.cmHandleState == CmHandleState.ADVISED
            assert copy.compositeState.dataSyncEnabled == false
        and: 'the objects are not equal'
            assert original != copy
            assert original.equals(copy) == false
        and: 'have different hash codes'
            assert original.hashCode() != copy.hashCode()
    }

    def 'Yang Model Cm Handle Deep Copy for cm handled without optional properties.'() {
        given: 'a yang model cm handle'
            def original = new YangModelCmHandle(id: 'some id')
        when: 'a deep copy is created'
            def copy = YangModelCmHandle.deepCopyOf(original)
        then: 'the objects are equal'
            assert original == copy
            assert original.equals(copy)
    }

}
