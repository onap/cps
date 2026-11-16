/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.instance.impl.HazelcastInstanceFactory
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters
import reactor.core.publisher.Mono
import spock.lang.Specification

class DmiPluginTrustLevelWatchDogSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockTrustLevelManager = Mock(TrustLevelManager)
    def trustLevelPerDmiPlugin = HazelcastInstanceFactory.getOrCreateHazelcastInstance(new Config('dmiPluginTrustLevelWatchDogSpecInstance')).getMap('trustLevelPerDmiPlugin')

    def objectUnderTest = new DmiPluginTrustLevelWatchDog(mockDmiRestClient, mockCmHandleQueryService, mockTrustLevelManager, trustLevelPerDmiPlugin)

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('dmiPluginTrustLevelWatchDogSpecInstance').shutdown()
    }

    def 'watch dmi plugin health status for #dmiHealhStatus'() {
        given: 'the cache has been initialised and "knows" about dmi-1'
            trustLevelPerDmiPlugin.put('dmi-1', dmiOldTrustLevel)
        and: 'dmi client returns health status #dmiHealhStatus'
            def urlTemplateParameters = new UrlTemplateParameters('dmi-1/actuator/health', [:])
            mockDmiRestClient.getDmiHealthStatus(urlTemplateParameters) >> Mono.just(dmiHealhStatus)
        and: 'the query returns the cm-handle references belonging to dmi-1'
            mockCmHandleQueryService.getCmHandleReferencesByDmiPluginIdentifier('dmi-1', false) >> ['ch-1', 'ch-2']
        when: 'dmi watch dog method runs'
            objectUnderTest.checkDmiAvailability()
        then: 'the update is delegated to the manager only when the trust level changes'
            numberOfCalls * mockTrustLevelManager.updateDmi('dmi-1', ['ch-1', 'ch-2'], TrustLevel.COMPLETE)
        and: 'no other update is delegated (a single failed health check is tolerated)'
            0 * mockTrustLevelManager.updateDmi(*_)
        where: 'the following parameters are used'
            dmiHealhStatus | dmiOldTrustLevel    || numberOfCalls
            'UP'           | TrustLevel.COMPLETE || 0
            'UP'           | TrustLevel.NONE     || 1
            'DOWN'         | TrustLevel.COMPLETE || 0
            'DOWN'         | TrustLevel.NONE     || 0
            ''             | TrustLevel.COMPLETE || 0
    }

    def 'watch dmi plugin health status with consecutive failures for #dmiHealthStatus'() {
        given: 'the cache "knows" about dmi-1 as trusted'
            trustLevelPerDmiPlugin.put('dmi-1', TrustLevel.COMPLETE)
        and: 'dmi client always returns health status #dmiHealthStatus'
            mockDmiRestClient.getDmiHealthStatus(_) >> Mono.just(dmiHealthStatus)
        and: 'the query returns the cm-handle references belonging to dmi-1'
            mockCmHandleQueryService.getCmHandleReferencesByDmiPluginIdentifier('dmi-1', false) >> ['ch-1']
        when: 'dmi watch dog method runs twice'
            objectUnderTest.checkDmiAvailability()
            objectUnderTest.checkDmiAvailability()
        then: 'no update is delegated to the manager yet'
            0 * mockTrustLevelManager.updateDmi(*_)
        when: 'dmi watch dog method runs for the third time'
            objectUnderTest.checkDmiAvailability()
        then: 'the trust level NONE is delegated to the manager exactly once'
            1 * mockTrustLevelManager.updateDmi('dmi-1', ['ch-1'], TrustLevel.NONE)
        where: 'the following health statuses are used'
            dmiHealthStatus << ['DOWN', '']
    }

    def 'watch dmi plugin health status with recovery between failures'() {
        given: 'the cache "knows" about dmi-1 as trusted'
            trustLevelPerDmiPlugin.put('dmi-1', TrustLevel.COMPLETE)
        and: 'dmi client returns two failures, one UP and two failures again'
            mockDmiRestClient.getDmiHealthStatus(_) >>> [Mono.just(''), Mono.just(''), Mono.just('UP'), Mono.just(''), Mono.just('')]
        when: 'dmi watch dog method runs once per health status'
            5.times { objectUnderTest.checkDmiAvailability() }
        then: 'no update is delegated to the manager because UP resets the failure count'
            0 * mockTrustLevelManager.updateDmi(*_)
    }
}
