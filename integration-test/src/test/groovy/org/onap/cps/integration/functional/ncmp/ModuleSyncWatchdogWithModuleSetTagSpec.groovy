/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.functional.ncmp

import io.micrometer.core.instrument.MeterRegistry
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.impl.inventory.sync.ModuleSyncWatchdog
import org.springframework.beans.factory.annotation.Autowired
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

class ModuleSyncWatchdogWithModuleSetTagSpec extends CpsIntegrationSpecBase {

    ModuleSyncWatchdog objectUnderTest

    @Autowired
    MeterRegistry meterRegistry

    def NUMBER_OF_TAGS = 2
    def CM_HANDLES_PER_TAG = 250
    def TOTAL_CM_HANDLES = NUMBER_OF_TAGS * CM_HANDLES_PER_TAG

    def setup() {
        objectUnderTest = moduleSyncWatchdog
        def offset = 1
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'tagA', CM_HANDLES_PER_TAG, offset)
        offset += CM_HANDLES_PER_TAG
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'tagB', CM_HANDLES_PER_TAG, offset)
        meterRegistry.clear()
    }

    def cleanup() {
        deregisterSequenceOfCmHandles(DMI1_URL, TOTAL_CM_HANDLES)
    }

    def 'CPS-2478 Highlight module sync inefficiencies.'() {
        when: 'sync all advised cm handles'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        and: 'wait a little (to give all threads time to get started, and the instrumentation timer to be created)'
            Thread.sleep(100)
        and: 'wait until all cm handles have been queried (for their tag)'
            def dbModuleQueriesTimer = meterRegistry.get('cps.module.service.module.reference.query.by.attribute').timer()
            new PollingConditions().within(10, () -> {
                assert dbModuleQueriesTimer.count() >= TOTAL_CM_HANDLES
            })
        and: 'this takes about 1 second'
            //assert  dbModuleQueriesTimer.totalTime(TimeUnit.MILLISECONDS) > 800
            //assert  dbModuleQueriesTimer.totalTime(TimeUnit.MILLISECONDS) < 1200
            System.out.println('*** CPS-2478, query module references (ms) : ' + dbModuleQueriesTimer.totalTime(TimeUnit.MILLISECONDS))
        then: 'exactly 100 (internal batch size) calls to DMI to get module references'
            def dmiModuleRetrievalTimer = meterRegistry.get('cps.ncmp.inventory.module.references.from.dmi').timer()
            assert dmiModuleRetrievalTimer.count() == 200
        and: 'this takes about 1.2 second'
            //assert dmiModuleRetrievalTimer.totalTime(TimeUnit.MILLISECONDS) > 1000
            //assert dmiModuleRetrievalTimer.totalTime(TimeUnit.MILLISECONDS) < 1600
            System.out.println('*** CPS-2478, get modules from DMI    (ms) : ' + dmiModuleRetrievalTimer.totalTime(TimeUnit.MILLISECONDS))
        then: ' wait a little again to ensure the last batch is saved'
            Thread.sleep(1000)
        and: '1 schema set stored in db created for each cm handle'
            def dbSchemaSetStorageTimer = meterRegistry.get('cps.module.persistence.schemaset.store').timer()
            //TODO Toine, check why this is almost always 1 short of 500
            assert dbSchemaSetStorageTimer.count() >= TOTAL_CM_HANDLES - 1
        and: 'this takes over 8 seconds'
            //assert dbSchemaSetStorageTimer.totalTime(TimeUnit.MILLISECONDS) > 8000
            System.out.println('*** CPS-2478, store schema sets in DB (ms) : ' + dbSchemaSetStorageTimer.totalTime(TimeUnit.MILLISECONDS))
    }

}
