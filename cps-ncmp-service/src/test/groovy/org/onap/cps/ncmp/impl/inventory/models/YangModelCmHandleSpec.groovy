/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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
import org.onap.cps.ncmp.api.inventory.models.LockReasonCategory
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.MODEL

class YangModelCmHandleSpec extends Specification {

    def 'Creating yang model cm handle from a service api cm handle.'() {
        given: 'a cm handle with properties'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.cmHandleId = 'cm-handle-id01'
            ncmpServiceCmHandle.dmiProperties = [myDmiProperty:'value1']
            ncmpServiceCmHandle.publicProperties = [myPublicProperty:'value2']
        and: 'with a composite state'
            def compositeState = new CompositeStateBuilder()
                .withCmHandleState(CmHandleState.LOCKED)
                .withLastUpdatedTime('some-update-time')
                .withLockReason(LockReasonCategory.MODULE_SYNC_FAILED, 'locked details')
                .withOperationalDataStores(DataStoreSyncState.SYNCHRONIZED, 'some-sync-time').build()
            ncmpServiceCmHandle.setCompositeState(compositeState)
        when: 'it is converted to a yang model cm handle'
            def objectUnderTest = YangModelCmHandle.toYangModelCmHandle('', '', '', ncmpServiceCmHandle,'my-module-set-tag', 'my-alternate-id', 'my-data-producer-identifier')
        then: 'the result has the right size'
            assert objectUnderTest.dmiProperties.size() == 1
        and: 'the result has the correct values for module set tag, alternate ID, and data producer identifier'
            assert objectUnderTest.moduleSetTag == 'my-module-set-tag'
            assert objectUnderTest.alternateId == 'my-alternate-id'
            assert objectUnderTest.dataProducerIdentifier == 'my-data-producer-identifier'
        and: 'the DMI property in the result has the correct name and value'
            assert objectUnderTest.dmiProperties[0].name == 'myDmiProperty'
            assert objectUnderTest.dmiProperties[0].value == 'value1'
        and: 'the public property in the result has the correct name and value'
            assert objectUnderTest.publicProperties[0].name == 'myPublicProperty'
            assert objectUnderTest.publicProperties[0].value == 'value2'
        and: 'the composite state matches the composite state of the ncmpServiceCmHandle'
            objectUnderTest.getCompositeState().cmHandleState == CmHandleState.LOCKED
            objectUnderTest.getCompositeState() == ncmpServiceCmHandle.getCompositeState()
    }

    def 'Resolve DMI service name: #scenario and #requiredService service require.'() {
        given: 'a yang model cm handle'
            def objectUnderTest = YangModelCmHandle.toYangModelCmHandle(dmiServiceName, dmiDataServiceName,
                    dmiModelServiceName, new NcmpServiceCmHandle(cmHandleId: 'cm-handle-id-1'),'', '', '')
        expect:
            assert objectUnderTest.resolveDmiServiceName(requiredService) == expectedService
        where:
            scenario                        | dmiServiceName     | dmiDataServiceName | dmiModelServiceName | requiredService || expectedService
            'common service registered'     | 'common service'   | 'does not matter'  | 'does not matter'   | DATA            || 'common service'
            'common service registered'     | 'common service'   | 'does not matter'  | 'does not matter'   | MODEL           || 'common service'
            'common service empty'          | ''                 | 'data service'     | 'does not matter'   | DATA            || 'data service'
            'common service empty'          | ''                 | 'does not matter'  | 'model service'     | MODEL           || 'model service'
            'common service blank'          | '   '              | 'data service'     | 'does not matter'   | DATA            || 'data service'
            'common service blank'          | '   '              | 'does not matter'  | 'model service'     | MODEL           || 'model service'
            'common service null '          | null               | 'data service'     | 'does not matter'   | DATA            || 'data service'
            'common service null'           | null               | 'does not matter'  | 'model service'     | MODEL           || 'model service'
            'only model service registered' | null               | null               | 'does not matter'   | DATA            || null
            'only data service registered'  | null               | 'does not matter'  | null                | MODEL           || null
    }

    def 'Yang Model Cm Handle Deep Copy'() {
        given: 'a yang model cm handle'
            def currentYangModelCmHandle = new YangModelCmHandle(id: 'cmhandle',
                publicProperties: [new YangModelCmHandle.Property('publicProperty1', 'value1')],
                dmiProperties: [new YangModelCmHandle.Property('dmiProperty1', 'value1')],
                compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED, dataSyncEnabled: false))
        when: 'a deep copy is created'
            def yangModelCmhandleDeepCopy = YangModelCmHandle.deepCopyOf(currentYangModelCmHandle)
        and: 'we try to mutate current yang model cm handle'
            currentYangModelCmHandle.id = 'cmhandle-changed'
            currentYangModelCmHandle.dmiProperties = [new YangModelCmHandle.Property('updatedPublicProperty1', 'value1')]
            currentYangModelCmHandle.publicProperties = [new YangModelCmHandle.Property('updatedDmiProperty1', 'value1')]
            currentYangModelCmHandle.compositeState.cmHandleState = CmHandleState.READY
            currentYangModelCmHandle.compositeState.dataSyncEnabled = true
        then: 'there is no change in the deep copied object'
            assert yangModelCmhandleDeepCopy.id == 'cmhandle'
            assert yangModelCmhandleDeepCopy.dmiProperties == [new YangModelCmHandle.Property('dmiProperty1', 'value1')]
            assert yangModelCmhandleDeepCopy.publicProperties == [new YangModelCmHandle.Property('publicProperty1', 'value1')]
            assert yangModelCmhandleDeepCopy.compositeState.cmHandleState == CmHandleState.ADVISED
            assert yangModelCmhandleDeepCopy.compositeState.dataSyncEnabled == false
        and: 'equality on reference and hashcode behave as expected'
            assert currentYangModelCmHandle.hashCode() != yangModelCmhandleDeepCopy.hashCode()
            assert currentYangModelCmHandle != yangModelCmhandleDeepCopy

    }


}
