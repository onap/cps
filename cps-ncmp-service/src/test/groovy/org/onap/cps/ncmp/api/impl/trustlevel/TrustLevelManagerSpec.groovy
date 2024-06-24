/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.trustlevel

import org.onap.cps.ncmp.api.impl.events.avc.ncmptoclient.AvcEventPublisher
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import spock.lang.Specification

class TrustLevelManagerSpec extends Specification {

    def trustLevelPerCmHandle = [:]
    def trustLevelPerDmiPlugin = [:]

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockAttributeValueChangeEventPublisher = Mock(AvcEventPublisher)
    def objectUnderTest = new TrustLevelManager(trustLevelPerCmHandle, trustLevelPerDmiPlugin, mockInventoryPersistence, mockAttributeValueChangeEventPublisher)

    def 'Initial cm handle registration'() {
        given: 'two cm handles: one with no trust level and one trusted'
            def cmHandleModelsToBeCreated = ['ch-1': null, 'ch-2': TrustLevel.COMPLETE]
        when: 'the initial registration handled'
            objectUnderTest.handleInitialRegistrationOfTrustLevels(cmHandleModelsToBeCreated)
        then: 'no notification sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
        and: 'both cm handles are in the cache and are trusted'
            assert trustLevelPerCmHandle.get('ch-1') == TrustLevel.COMPLETE
            assert trustLevelPerCmHandle.get('ch-2') == TrustLevel.COMPLETE
    }

    def 'Initial cm handle registration with a cm handle that is not trusted'() {
        given: 'a not trusted cm handle'
            def cmHandleModelsToBeCreated = ['ch-2': TrustLevel.NONE]
        when: 'the initial registration handled'
            objectUnderTest.handleInitialRegistrationOfTrustLevels(cmHandleModelsToBeCreated)
        then: 'notification is sent'
            1 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

    def 'Dmi trust level updated'() {
        given: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'a trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        when: 'the update is handled'
            objectUnderTest.handleUpdateOfDmiTrustLevel('my-dmi', ['ch-1'], TrustLevel.NONE)
        then: 'notification is sent'
            1 * mockAttributeValueChangeEventPublisher.publishAvcEvent('ch-1', 'trustLevel', 'COMPLETE', 'NONE')
        and: 'the dmi in the cache is not trusted'
            assert trustLevelPerDmiPlugin.get('my-dmi') == TrustLevel.NONE
    }

    def 'Dmi trust level updated with same value'() {
        given: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'a trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        when: 'the update is handled'
            objectUnderTest.handleUpdateOfDmiTrustLevel('my-dmi', ['ch-1'], TrustLevel.COMPLETE)
        then: 'no notification is sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
        and: 'the dmi in the cache is trusted'
            assert trustLevelPerDmiPlugin.get('my-dmi') == TrustLevel.COMPLETE
    }

    def 'Device trust level updated'() {
        given: 'a non trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        and: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'inventory persistence service returns yang model cm handle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(id: 'ch-1', dmiDataServiceName: 'my-dmi')
        when: 'update of device to COMPLETE trust level handled'
            objectUnderTest.handleUpdateOfDeviceTrustLevel('ch-1', TrustLevel.COMPLETE)
        then: 'the cm handle in the cache is trusted'
            assert trustLevelPerCmHandle.get('ch-1', TrustLevel.COMPLETE)
        and: 'notification is sent'
            1 * mockAttributeValueChangeEventPublisher.publishAvcEvent('ch-1', 'trustLevel', 'NONE', 'COMPLETE')
    }

    def 'Device trust level updated with same value'() {
        given: 'a non trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        and: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'inventory persistence service returns yang model cm handle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(id: 'ch-1', dmiDataServiceName: 'my-dmi')
        when: 'update of device trust to the same level (NONE)'
            objectUnderTest.handleUpdateOfDeviceTrustLevel('ch-1', TrustLevel.NONE)
        then: 'the cm handle in the cache is not trusted'
            assert trustLevelPerCmHandle.get('ch-1', TrustLevel.NONE)
        and: 'no notification is sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

    def 'Dmi trust level restored to complete with non trusted device'() {
        given: 'a non trusted dmi'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.NONE)
        and: 'a non trusted device'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        when: 'restore the dmi trust level to COMPLETE'
            objectUnderTest.handleUpdateOfDmiTrustLevel('my-dmi', ['ch-1'], TrustLevel.COMPLETE)
        then: 'the cm handle in the cache is still NONE'
            assert trustLevelPerCmHandle.get('ch-1') == TrustLevel.NONE
        and: 'no notification is sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

}
