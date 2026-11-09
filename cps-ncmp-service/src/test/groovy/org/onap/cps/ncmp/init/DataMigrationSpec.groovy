/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Subject

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY

class DataMigrationSpec extends Specification{

    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def cmHandleWithAdditionalProperty = new YangModelCmHandle(id: 'ch-1', dmiServiceName: 'dmi1', compositeState: new CompositeState(cmHandleState: READY), additionalProperties: [new YangModelCmHandle.Property('id', '4')], publicProperties: [])
    def cmHandleWithAdditionalPropertyEmpty = new YangModelCmHandle(id: 'ch-2', dmiServiceName: 'dmi1', compositeState: new CompositeState(cmHandleState: ADVISED), additionalProperties: [], publicProperties: [])
    def cmHandleWithAdditionalPropertyWithTwoEntries = new YangModelCmHandle(id: 'ch-3', dmiServiceName: 'dmi2', compositeState: new CompositeState(cmHandleState: READY), additionalProperties: [new YangModelCmHandle.Property('k1', 'v1'), new YangModelCmHandle.Property('k2', 'v2')], publicProperties: [])
    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        mockInventoryPersistence.getYangModelCmHandle('ch-1') >> cmHandleWithAdditionalProperty
        mockInventoryPersistence.getYangModelCmHandle('ch-2') >> cmHandleWithAdditionalPropertyEmpty
        mockInventoryPersistence.getYangModelCmHandle('ch-3') >> cmHandleWithAdditionalPropertyWithTwoEntries
        setupLogger(Level.ERROR)
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(DataMigration.class)).detachAndStopAllAppenders()
    }

    @Subject
    def objectUnderTest = Spy(new DataMigration(mockInventoryPersistence, mockCmHandleQueryService, jsonObjectMapper))

    def 'CM Handle migration.'() {
        given:  'a list of CM handle IDs'
            def cmHandleIds = ['ch-1', 'ch-2', 'ch-3']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        when:   'migration is performed'
            objectUnderTest.migrateInventoryToModelRelease20260423(3)
        then:   'handles are processed in bulk with state and dmi-properties read directly from persistence'
            1 * mockInventoryPersistence.cmHandleBulkMigrate({ cmHandleStateUpdates ->
                def actualData = cmHandleStateUpdates.collect { [id: it.cmHandleId, state: it.state, dmiProperties: it.dmiProperties] }
                assert actualData.size() == 3
                assert actualData[0].id == 'ch-1'
                assert actualData[0].state == 'READY'
                assert actualData[0].dmiProperties == '{"id":"4"}'
                assert actualData[1].id == 'ch-2'
                assert actualData[1].state == 'ADVISED'
                assert actualData[1].dmiProperties == '{}'
                assert actualData[2].id == 'ch-3'
                assert actualData[2].state == 'READY'
                assert actualData[2].dmiProperties == '{"k1":"v1","k2":"v2"}'
            })
    }

    def 'CM Handle migration with exception for a cm handle in batch.'() {
        given: 'a faulty CM handle ID'
            def cmHandleIds = ['faultyCmHandle']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'an exception is thrown when getting cm handle'
            mockInventoryPersistence.getYangModelCmHandle('faultyCmHandle') >> { throw new RuntimeException('Simulated failure') }
        when: 'migration is performed'
            objectUnderTest.migrateInventoryToModelRelease20260423(2)
        then: 'migration processes no batches'
            1 * mockInventoryPersistence.cmHandleBulkMigrate([])
    }

    def 'Migrate batch with error.'() {
        given: 'a cm handle'
            def cmHandleIds = ['ch-1']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'an exception happens updating cm handle states'
            mockInventoryPersistence.cmHandleBulkMigrate(*_) >> {
                throw new RuntimeException('Simulated failure')
            }
        when: 'migration is performed'
            objectUnderTest.migrateInventoryToModelRelease20260423(2)
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
