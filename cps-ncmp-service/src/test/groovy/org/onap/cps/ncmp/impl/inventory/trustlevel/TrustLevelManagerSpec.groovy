/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.utils.events.InventoryEventProducer
import spock.lang.Specification

class TrustLevelManagerSpec extends Specification {

    TrustLevelManager objectUnderTest

    def hazelcastInstance
    IMap<String, TrustLevel> trustLevelPerCmHandleId
    IMap<String, TrustLevel>  trustLevelPerDmiPlugin

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockInventoryEventProducer = Mock(InventoryEventProducer)

    def setup() {
        hazelcastInstance = Hazelcast.newHazelcastInstance()
        trustLevelPerCmHandleId = hazelcastInstance.getMap("trustLevelPerCmHandle")
        trustLevelPerDmiPlugin = hazelcastInstance.getMap("trustLevelPerCmHandle")
        objectUnderTest = new TrustLevelManager(trustLevelPerCmHandleId, trustLevelPerDmiPlugin, mockInventoryPersistence, mockInventoryEventProducer)
    }

    def cleanup() {
        hazelcastInstance.shutdown()
    }

    def 'Initial dmi registration'() {
        given: 'a dmi plugin'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: dmiPlugin, dmiDataPlugin: dmiDataPlugin)
        when: 'method to register to the cache is called'
            objectUnderTest.registerDmiPlugin(dmiPluginRegistration)
        then: 'dmi plugin in the cache and trusted'
            assert trustLevelPerDmiPlugin.get(expectedDmiPlugin) == TrustLevel.COMPLETE
        where: 'the following parameters are used'
            dmiPlugin | dmiDataPlugin || expectedDmiPlugin
            'dmi-1'   | ''            || 'dmi-1'
            ''        | 'dmi-2'       || 'dmi-2'
    }

    def 'Initial cm handle registration'() {
        given: 'two cm handles: one with no trust level and one trusted'
            def cmHandleModelsToBeCreated = ['ch-1': null, 'ch-2': TrustLevel.COMPLETE]
        when: 'method to register to the cache is called'
            objectUnderTest.registerCmHandles(cmHandleModelsToBeCreated)
        then: 'no notification sent'
            0 * mockInventoryEventProducer.sendAvcEvent(*_)
        and: 'both cm handles are in the cache and are trusted'
            assert trustLevelPerCmHandleId.get('ch-1') == TrustLevel.COMPLETE
            assert trustLevelPerCmHandleId.get('ch-2') == TrustLevel.COMPLETE
    }

    def 'Initial cm handle registration with a cm handle that is not trusted'() {
        given: 'a not trusted cm handle'
            def cmHandleModelsToBeCreated = ['ch-2': TrustLevel.NONE]
        when: 'method to register to the cache is called'
            objectUnderTest.registerCmHandles(cmHandleModelsToBeCreated)
        then: 'notification is sent'
            1 * mockInventoryEventProducer.sendAvcEvent(*_)
    }

    def 'Dmi trust level updated'() {
        given: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'a trusted cm handle'
            trustLevelPerCmHandleId.put('ch-1', TrustLevel.COMPLETE)
        when: 'the update is handled'
            objectUnderTest.updateDmi('my-dmi', ['ch-1'], TrustLevel.NONE)
        then: 'notification is sent'
            1 * mockInventoryEventProducer.sendAvcEvent('ch-1', 'trustLevel', 'COMPLETE', 'NONE')
        and: 'the dmi in the cache is not trusted'
            assert trustLevelPerDmiPlugin.get('my-dmi') == TrustLevel.NONE
    }

    def 'Dmi trust level updated with same value'() {
        given: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'a trusted cm handle'
            trustLevelPerCmHandleId.put('ch-1', TrustLevel.COMPLETE)
        when: 'the update is handled'
            objectUnderTest.updateDmi('my-dmi', ['ch-1'], TrustLevel.COMPLETE)
        then: 'no notification is sent'
            0 * mockInventoryEventProducer.sendAvcEvent(*_)
        and: 'the dmi in the cache is trusted'
            assert trustLevelPerDmiPlugin.get('my-dmi') == TrustLevel.COMPLETE
    }

    def 'CmHandle trust level updated'() {
        given: 'a non trusted cm handle'
            trustLevelPerCmHandleId.put('ch-1', TrustLevel.NONE)
        and: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'inventory persistence service returns yang model cm handle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(id: 'ch-1', dmiDataServiceName: 'my-dmi')
        when: 'update of CmHandle to COMPLETE trust level handled'
            objectUnderTest.updateCmHandleTrustLevel('ch-1', TrustLevel.COMPLETE)
        then: 'the cm handle in the cache is trusted'
            assert trustLevelPerCmHandleId.get('ch-1', TrustLevel.COMPLETE)
        and: 'notification is sent'
            1 * mockInventoryEventProducer.sendAvcEvent('ch-1', 'trustLevel', 'NONE', 'COMPLETE')
    }

    def 'CmHandle trust level updated with same value'() {
        given: 'a non trusted cm handle'
            trustLevelPerCmHandleId.put('ch-1', TrustLevel.NONE)
        and: 'a trusted dmi plugin'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'inventory persistence service returns yang model cm handle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(id: 'ch-1', dmiDataServiceName: 'my-dmi')
        when: 'update of CmHandle trust to the same level (NONE)'
            objectUnderTest.updateCmHandleTrustLevel('ch-1', TrustLevel.NONE)
        then: 'the cm handle in the cache is not trusted'
            assert trustLevelPerCmHandleId.get('ch-1', TrustLevel.NONE)
        and: 'no notification is sent'
            0 * mockInventoryEventProducer.sendAvcEvent(*_)
    }

    def 'Dmi trust level restored to complete with non trusted CmHandle'() {
        given: 'a non trusted dmi'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.NONE)
        and: 'a non trusted CmHandle'
            trustLevelPerCmHandleId.put('ch-1', TrustLevel.NONE)
        when: 'restore the dmi trust level to COMPLETE'
            objectUnderTest.updateDmi('my-dmi', ['ch-1'], TrustLevel.COMPLETE)
        then: 'the cm handle in the cache is still NONE'
            assert trustLevelPerCmHandleId.get('ch-1') == TrustLevel.NONE
        and: 'no notification is sent'
            0 * mockInventoryEventProducer.sendAvcEvent(*_)
    }

    def 'Apply effective trust level among CmHandle and dmi plugin'() {
        given: 'a non trusted dmi'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.NONE)
        and: 'a trusted CmHandle'
            trustLevelPerCmHandleId.put('ch-1', TrustLevel.COMPLETE)
        and: 'a cm handle object'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
        when: 'effective trust level selected'
            objectUnderTest.applyEffectiveTrustLevel(ncmpServiceCmHandle)
        then: 'effective trust level is none'
            assert ncmpServiceCmHandle.currentTrustLevel == TrustLevel.NONE
    }

    def 'Apply effective trust levels from CmHandle batch'() {
        given: 'a trusted dmi'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'a trusted CmHandle'
            trustLevelPerCmHandleId.put('ch-1', TrustLevel.COMPLETE)
        and: 'a not trusted CmHandle'
            trustLevelPerCmHandleId.put('ch-2', TrustLevel.NONE)
        and: 'cm handle objects'
            def ncmpServiceCmHandle1 = new NcmpServiceCmHandle(cmHandleId: 'ch-1', dmiServiceName: 'my-dmi')
            def ncmpServiceCmHandle2 = new NcmpServiceCmHandle(cmHandleId: 'ch-2', dmiDataServiceName: 'my-dmi')
        when: 'effective trust level selected'
            objectUnderTest.applyEffectiveTrustLevels([ncmpServiceCmHandle1, ncmpServiceCmHandle2])
        then: 'effective trust levels are correctly applied'
            assert ncmpServiceCmHandle1.currentTrustLevel == TrustLevel.COMPLETE
            assert ncmpServiceCmHandle2.currentTrustLevel == TrustLevel.NONE
    }

    def 'Apply effective trust level  when the trust level caches are empty (restart case)'() {
        given: 'a cm-handle that is not in the cache'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
        when: 'effective trust level is applied'
            objectUnderTest.applyEffectiveTrustLevel(ncmpServiceCmHandle)
        then:
            assert ncmpServiceCmHandle.currentTrustLevel == TrustLevel.NONE
    }

    def 'Removing cm Handle ids from trust level cache.'() {
        given: 'a cm handle id with trustlevel in the cache'
            trustLevelPerCmHandleId.put('ch-1', TrustLevel.COMPLETE)
        when: 'remove existing and non-existing cm handle ids'
            objectUnderTest.removeCmHandles(['non-existing-id','ch-1'])
        then: 'both cm handle ids are not included in the cache'
            assert !trustLevelPerCmHandleId.contains('non-existing-id')
            assert !trustLevelPerCmHandleId.contains('ch-1')
        and: 'removing the non-existing id did not cause an exception'
            noExceptionThrown()
    }

}
