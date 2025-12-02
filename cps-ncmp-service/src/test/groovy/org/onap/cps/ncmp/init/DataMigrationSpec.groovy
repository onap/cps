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
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Subject

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY

class DataMigrationSpec extends Specification{

    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockNetworkCmProxyInventoryFacade = Mock(NetworkCmProxyInventoryFacade)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def cmHandle1 = new NcmpServiceCmHandle(cmHandleId: 'ch-1', dmiServiceName: 'dmi1', compositeState: new CompositeState(cmHandleState: READY))
    def cmHandle2 = new NcmpServiceCmHandle(cmHandleId: 'ch-2', dmiServiceName: 'dmi1', compositeState: new CompositeState(cmHandleState: ADVISED))
    def cmHandle3 = new NcmpServiceCmHandle(cmHandleId: 'ch-3', dmiServiceName: 'dmi2', compositeState: new CompositeState(cmHandleState: READY))

    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('ch-1') >> cmHandle1
        mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('ch-2') >> cmHandle2
        mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('ch-3') >> cmHandle3
        setupLogger(Level.ERROR)
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(DataMigration.class)).detachAndStopAllAppenders()
    }


    @Subject
    def objectUnderTest = Spy(new DataMigration(mockInventoryPersistence, mockCmHandleQueryService, mockNetworkCmProxyInventoryFacade))

    def 'CM Handle migration.'() {
        given:  'a list of CM handle IDs'
            def cmHandleIds = ['ch-1', 'ch-2', 'ch-3']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        when:   'migration is performed'
            objectUnderTest.migrateInventoryToModelRelease20250722(3)
        then:   'handles are processed in bulk'
            1 * mockInventoryPersistence.bulkUpdateCmHandleStates({ cmHandleStateUpdates ->
                def actualData = cmHandleStateUpdates.collect { [id: it.cmHandleId, state: it.state] }
                assert actualData.size() == 3
                assert actualData.containsAll([
                    [id: 'ch-1', state: 'READY'],
                    [id: 'ch-2', state: 'ADVISED'],
                    [id: 'ch-3', state: 'READY']
                ])
            })
    }

    def 'CM Handle migration with exception for a cm handle in batch.'() {
        given: 'a faulty CM handle ID'
            def cmHandleIds = ['faultyCmHandle']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'an exception is thrown when getting cm handle'
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('faultyCmHandle') >> { throw new RuntimeException('Simulated failure') }
        when: 'migration is performed'
            objectUnderTest.migrateInventoryToModelRelease20250722(2)
        then: 'migration processes no batches'
            1 * mockInventoryPersistence.bulkUpdateCmHandleStates([])
    }

    def 'Migrate batch with error.'() {
        given: 'a cm handle'
            def cmHandleIds = ['ch-1']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'an exception happens updating cm handle states'
            mockInventoryPersistence.bulkUpdateCmHandleStates(*_) >> {
                throw new RuntimeException('Simulated failure')
            }
        when: 'migration is performed'
            objectUnderTest.migrateInventoryToModelRelease20250722(2)
        then: 'exception is caught and logged'
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.ERROR
            assert loggingEvent.formattedMessage.contains('Failed to perform bulk update for batch')
    }

    def setupLogger(level) {
        def setupLogger = ((Logger) LoggerFactory.getLogger(DataMigration))
        setupLogger.setLevel(level)
        setupLogger.addAppender(logger)
        logger.start()
    }
}
