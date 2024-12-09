/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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
    def trustLevelPerDmiPlugin = HazelcastInstanceFactory
        .getOrCreateHazelcastInstance(new Config('hazelcastInstanceName'))
        .getMap('trustLevelPerDmiPlugin')

    def objectUnderTest = new DmiPluginTrustLevelWatchDog(mockDmiRestClient, mockCmHandleQueryService, mockTrustLevelManager, trustLevelPerDmiPlugin)

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('hazelcastInstanceName').shutdown()
    }

    def 'watch dmi plugin health status for #dmiHealhStatus'() {
        given: 'the cache has been initialised and "knows" about dmi-1'
            trustLevelPerDmiPlugin.put('dmi-1', dmiOldTrustLevel)
        and: 'dmi client returns health status #dmiHealhStatus'
            def urlTemplateParameters = new UrlTemplateParameters('dmi-1/actuator/health', [:])
            mockDmiRestClient.getDmiHealthStatus(urlTemplateParameters) >> Mono.just(dmiHealhStatus)
        when: 'dmi watch dog method runs'
            objectUnderTest.checkDmiAvailability()
        then: 'the update delegated to manager'
            numberOfCalls * mockTrustLevelManager.updateDmi('dmi-1', _, newDmiTrustLevel)
        where: 'the following parameters are used'
            dmiHealhStatus | dmiOldTrustLevel    | newDmiTrustLevel    || numberOfCalls
            'UP'           | TrustLevel.COMPLETE | TrustLevel.COMPLETE || 0
            'DOWN'         | TrustLevel.COMPLETE | TrustLevel.NONE     || 1
            'DOWN'         | TrustLevel.NONE     | TrustLevel.NONE     || 0
            'UP'           | TrustLevel.NONE     | TrustLevel.COMPLETE || 1
            ''             | TrustLevel.COMPLETE | TrustLevel.NONE     || 1
    }
}
