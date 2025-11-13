/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.init

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Subject

class InventoryDataMigrationSpec extends Specification{

    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockNetworkCmProxyInventoryFacade = Mock(NetworkCmProxyInventoryFacade)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    @Subject
    def objectUnderTest = new InventoryDataMigration(
            mockInventoryPersistence,
            mockCmHandleQueryService,
            mockNetworkCmProxyInventoryFacade

    )

    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender

    void setup() {
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
    }

    def "Should successfully migrate CM handles grouped by DMI service"() {
        given: 'a list of CM handle IDs'
            def cmHandleIds = ['handle1', 'handle2', 'handle3']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'CM handles with different DMI services'
            def handle1 = createCmHandle('handle1', 'dmi1', CmHandleState.READY)
            def handle2 = createCmHandle('handle2', 'dmi1', CmHandleState.ADVISED)
            def handle3 = createCmHandle('handle3', 'dmi2', CmHandleState.READY)
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle1') >> handle1
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle2') >> handle2
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle3') >> handle3
        when: 'migration is performed'
            objectUnderTest.migrateData()
        then: 'handles are retrieved and grouped correctly'
            1 * mockInventoryPersistence.setAndUpdateCmHandleField('handle1', 'cm-handle-state', 'READY')
            1 * mockInventoryPersistence.setAndUpdateCmHandleField('handle2', 'cm-handle-state', 'ADVISED')
            1 * mockInventoryPersistence.setAndUpdateCmHandleField('handle3', 'cm-handle-state', 'READY')
    }

    def "migrateData continues even when a batch throws an exception"() {
        given: 'a list of CM handle IDs that spans multiple batches'
            objectUnderTest.batchSize = 2
            def cmHandleIds = ['handle1', 'handle2', 'handle3', 'handle4', 'handle5', 'handle6']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds

        and: 'mock CM handles'
            def handle1 = createCmHandle('handle1', 'dmi1', CmHandleState.READY)
            def handle2 = createCmHandle('handle2', 'dmi1', CmHandleState.ADVISED)
            def handle3 = createCmHandle('handle3', 'dmi2', CmHandleState.READY)
            def handle4 = createCmHandle('handle4', 'dmi2', CmHandleState.READY)
            def handle5 = createCmHandle('handle5', 'dmi2', CmHandleState.READY)
            def handle6 = createCmHandle('handle6', 'dmi2', CmHandleState.READY)

        and: 'networkCmProxyInventoryFacade throws for one handle'
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle1') >> handle1
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle2') >> handle2
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle3') >> { throw new RuntimeException("Simulated failure in batch 2") }
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle4') >> handle4
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle5') >> handle5
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle6') >> handle6

        when: 'migration is performed'
            objectUnderTest.migrateData()

        then: 'migration continues even after one batch fails'
            1 * mockInventoryPersistence.setAndUpdateCmHandleField('handle1', 'cm-handle-state', 'READY')
            1 * mockInventoryPersistence.setAndUpdateCmHandleField('handle2', 'cm-handle-state', 'ADVISED')
            0 * mockInventoryPersistence.setAndUpdateCmHandleField('handle3', 'cm-handle-state', 'READY')
            1 * mockInventoryPersistence.setAndUpdateCmHandleField('handle4', 'cm-handle-state', 'READY')
            1 * mockInventoryPersistence.setAndUpdateCmHandleField('handle5', 'cm-handle-state', 'READY')
            1 * mockInventoryPersistence.setAndUpdateCmHandleField('handle6', 'cm-handle-state', 'READY')

    }

    def 'migrateData handles empty handle list gracefully'() {
        given:
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> []

        when:
            objectUnderTest.migrateData()

        then:
            0 * mockInventoryPersistence._
            noExceptionThrown()
    }

    private NcmpServiceCmHandle createCmHandle(String id, String dmiService, CmHandleState state) {
        def compositeState = Mock(CompositeState) {
            getCmHandleState() >> state
        }
        def handle = Mock(NcmpServiceCmHandle)
        handle.getCmHandleId() >> id
        handle.getDmiServiceName() >> dmiService
        handle.getCompositeState() >> compositeState
        return handle
    }


}
