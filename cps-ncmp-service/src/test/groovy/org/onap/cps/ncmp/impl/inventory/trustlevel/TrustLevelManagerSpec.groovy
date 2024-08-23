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

package org.onap.cps.ncmp.impl.inventory.trustlevel

import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.utils.events.CmAvcEventPublisher
import spock.lang.Ignore
import spock.lang.Specification

class TrustLevelManagerSpec extends Specification {

    def trustLevelPerCmHandle = [:]
    def trustLevelPerDmiPlugin = [:]

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockAttributeValueChangeEventPublisher = Mock(CmAvcEventPublisher)
    def objectUnderTest = new TrustLevelManager(trustLevelPerCmHandle, trustLevelPerDmiPlugin, mockInventoryPersistence, mockAttributeValueChangeEventPublisher)

    def 'Initial dmi registration'() {
        given: 'a dmi plugin'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'dmi-1')
        when: 'method to register to the cache is called'
            objectUnderTest.registerDmiPlugin(dmiPluginRegistration)
        then: 'dmi plugin in the cache and trusted'
            assert trustLevelPerDmiPlugin.get('dmi-1') == TrustLevel.COMPLETE
    }

    def 'Initial cm handle registration'() {
        given: 'two cm handles: one with no trust level and one trusted'
            def cmHandleModelsToBeCreated = ['ch-1': null, 'ch-2': TrustLevel.COMPLETE]
        when: 'method to register to the cache is called'
            objectUnderTest.registerCmHandles(cmHandleModelsToBeCreated)
        then: 'no notification sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
        and: 'both cm handles are in the cache and are trusted'
            assert trustLevelPerCmHandle.get('ch-1') == TrustLevel.COMPLETE
            assert trustLevelPerCmHandle.get('ch-2') == TrustLevel.COMPLETE
    }

    def 'Initial cm handle registration with a cm handle that is not trusted'() {
        given: 'a not trusted cm handle'
            def cmHandleModelsToBeCreated = ['ch-2': TrustLevel.NONE]
        when: 'method to register to the cache is called'
            objectUnderTest.registerCmHandles(cmHandleModelsToBeCreated)
        then: 'notification is sent'
            1 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

    def 'Dmi trust level updated'() {
        given: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'a trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        when: 'the update is handled'
            objectUnderTest.updateDmi('my-dmi', ['ch-1'], TrustLevel.NONE)
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
            objectUnderTest.updateDmi('my-dmi', ['ch-1'], TrustLevel.COMPLETE)
        then: 'no notification is sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
        and: 'the dmi in the cache is trusted'
            assert trustLevelPerDmiPlugin.get('my-dmi') == TrustLevel.COMPLETE
    }

    def 'CmHandle trust level updated'() {
        given: 'a non trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        and: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'inventory persistence service returns yang model cm handle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(id: 'ch-1', dmiDataServiceName: 'my-dmi')
        when: 'update of CmHandle to COMPLETE trust level handled'
            objectUnderTest.updateCmHandleTrustLevel('ch-1', TrustLevel.COMPLETE)
        then: 'the cm handle in the cache is trusted'
            assert trustLevelPerCmHandle.get('ch-1', TrustLevel.COMPLETE)
        and: 'notification is sent'
            1 * mockAttributeValueChangeEventPublisher.publishAvcEvent('ch-1', 'trustLevel', 'NONE', 'COMPLETE')
    }

    def 'CmHandle trust level updated with same value'() {
        given: 'a non trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        and: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'inventory persistence service returns yang model cm handle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(id: 'ch-1', dmiDataServiceName: 'my-dmi')
        when: 'update of CmHandle trust to the same level (NONE)'
            objectUnderTest.updateCmHandleTrustLevel('ch-1', TrustLevel.NONE)
        then: 'the cm handle in the cache is not trusted'
            assert trustLevelPerCmHandle.get('ch-1', TrustLevel.NONE)
        and: 'no notification is sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

    def 'Dmi trust level restored to complete with non trusted CmHandle'() {
        given: 'a non trusted dmi'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.NONE)
        and: 'a non trusted CmHandle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        when: 'restore the dmi trust level to COMPLETE'
            objectUnderTest.updateDmi('my-dmi', ['ch-1'], TrustLevel.COMPLETE)
        then: 'the cm handle in the cache is still NONE'
            assert trustLevelPerCmHandle.get('ch-1') == TrustLevel.NONE
        and: 'no notification is sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

    @Ignore
    // TODO: CPS-2375
    def 'Select effective trust level among CmHandle and dmi plugin'() {
        given: 'a non trusted dmi'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.NONE)
        and: 'a trusted CmHandle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        when: 'effective trust level selected'
            def effectiveTrustLevel = objectUnderTest.getEffectiveTrustLevel('ch-1')
        then: 'effective trust level is trusted'
            assert effectiveTrustLevel == TrustLevel.NONE
    }

    def 'CmHandle trust level (COMPLETE) removed'() {
        given: 'a trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        when: 'the remove is handled'
            objectUnderTest.removeCmHandles(['ch-1'])
        then: 'cm handle removed from the cache'
            assert trustLevelPerCmHandle.get('ch-1') == null
        and: 'notification is sent'
            1 * mockAttributeValueChangeEventPublisher.publishAvcEvent(_,'trustLevel','COMPLETE','NONE')
    }

    def 'CmHandle trust level (NONE) removed'() {
        given: 'a non-trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        when: 'the remove is handled'
            objectUnderTest.removeCmHandles(['ch-1'])
        then: 'cm handle removed from the cache'
            assert trustLevelPerCmHandle.get('ch-1') == null
        and: 'no notification is sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

}
