/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2024-2025 TechMahindra Ltd.
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

package org.onap.cps.integration.base

import com.hazelcast.map.IMap
import io.micrometer.core.instrument.MeterRegistry
import okhttp3.mockwebserver.MockWebServer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsDeltaService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.api.exceptions.DataspaceNotFoundException
import org.onap.cps.api.model.DataNode
import org.onap.cps.init.actuator.ModelLoaderRegistrationOnStartup
import org.onap.cps.init.actuator.ReadinessManager
import org.onap.cps.integration.DatabaseTestContainer
import org.onap.cps.integration.KafkaTestContainer
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.NetworkCmProxyInventoryFacadeImpl
import org.onap.cps.ncmp.impl.data.NetworkCmProxyFacade
import org.onap.cps.ncmp.impl.data.NetworkCmProxyQueryService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.ParameterizedCmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.sync.ModuleSyncService
import org.onap.cps.ncmp.impl.inventory.sync.ModuleSyncWatchdog
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.impl.utils.EventDateTimeFormatter
import org.onap.cps.ncmp.rest.controller.NetworkCmProxyInventoryController
import org.onap.cps.ri.repository.DataspaceRepository
import org.onap.cps.ri.repository.SchemaSetRepository
import org.onap.cps.ri.utils.SessionManager
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.concurrent.BlockingQueue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = [CpsDataspaceService])
@Testcontainers
@EnableAutoConfiguration
@AutoConfigureMockMvc
@EnableJpaRepositories(basePackageClasses = [DataspaceRepository])
@ComponentScan(basePackages = ['org.onap.cps'])
@EntityScan('org.onap.cps.ri.models')
abstract class CpsIntegrationSpecBase extends Specification {

    static KafkaConsumer kafkaConsumer

    @Shared
    DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance()

    @Shared
    KafkaTestContainer kafkaTestContainer = KafkaTestContainer.getInstance()

    @Autowired
    MockMvc mvc

    @Autowired
    NetworkCmProxyInventoryController networkCmProxyInventoryController

    @Autowired
    CpsDataspaceService cpsDataspaceService

    @Autowired
    CpsAnchorService cpsAnchorService

    @Autowired
    CpsDataService cpsDataService

    @Autowired
    CpsDeltaService cpsDeltaService

    @Autowired
    CpsModuleService cpsModuleService

    @Autowired
    CpsQueryService cpsQueryService

    @Autowired
    SessionManager sessionManager

    @Autowired
    CpsModulePersistenceService cpsModulePersistenceService

    @Autowired
    DataspaceRepository dataspaceRepository

    @Autowired
    SchemaSetRepository schemaSetRepository

    @Autowired
    ParameterizedCmHandleQueryService networkCmProxyCmHandleQueryService

    @Autowired
    NetworkCmProxyFacade networkCmProxyFacade

    @Autowired
    NetworkCmProxyInventoryFacadeImpl networkCmProxyInventoryFacade

    @Autowired
    NetworkCmProxyQueryService networkCmProxyQueryService

    @Autowired
    ModuleSyncWatchdog moduleSyncWatchdog

    @Autowired
    ModuleSyncService moduleSyncService

    @Autowired
    BlockingQueue<String> moduleSyncWorkQueue

    @Autowired
    @Qualifier("cpsCommonLocks")
    IMap<String, String> cpsCommonLocks

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    InventoryPersistence inventoryPersistence

    @Autowired
    AlternateIdMatcher alternateIdMatcher

    @Autowired
    ModelLoaderRegistrationOnStartup modelLoaderRegistrationOnStartup

    @Autowired
    ReadinessManager readinessManager

    @Autowired
    MeterRegistry meterRegistry

    @Value('${ncmp.policy-executor.server.port:8080}')
    private String policyServerPort;

    MockWebServer mockDmiServer1 = new MockWebServer()
    MockWebServer mockDmiServer2 = new MockWebServer()
    MockWebServer mockPolicyServer = new MockWebServer()

    DmiDispatcher dmiDispatcher1 = new DmiDispatcher()
    DmiDispatcher dmiDispatcher2 = new DmiDispatcher()

    PolicyDispatcher policyDispatcher = new PolicyDispatcher();

    def DMI1_URL = null
    def DMI2_URL = null

    static NO_MODULE_SET_TAG = ''
    static NO_ALTERNATE_ID = ''
    static GENERAL_TEST_DATASPACE = 'generalTestDataspace'
    static BOOKSTORE_SCHEMA_SET = 'bookstoreSchemaSet'

    static initialized = false
    def now = OffsetDateTime.now()

    enum ModuleNameStrategy { UNIQUE, OVERLAPPING }

    def setup() {
        if (!initialized) {
            cpsDataspaceService.createDataspace(GENERAL_TEST_DATASPACE)
            createStandardBookStoreSchemaSet(GENERAL_TEST_DATASPACE)
            initialized = true
        }
        mockDmiServer1.setDispatcher(dmiDispatcher1)
        mockDmiServer1.start()

        mockDmiServer2.setDispatcher(dmiDispatcher2)
        mockDmiServer2.start()

        mockPolicyServer.setDispatcher(policyDispatcher)
        mockPolicyServer.start(Integer.valueOf(policyServerPort))

        DMI1_URL = String.format('http://%s:%s', mockDmiServer1.getHostName(), mockDmiServer1.getPort())
        DMI2_URL = String.format('http://%s:%s', mockDmiServer2.getHostName(), mockDmiServer2.getPort())

        readinessManager.registerStartupProcess('Dummy process to prevent watchdogs processes starting during integration tests')
    }

    def cleanup() {
        mockDmiServer1.shutdown()
        mockDmiServer2.shutdown()
        mockPolicyServer.shutdown()
        cpsModuleService.deleteAllUnusedYangModuleData('NFP-Operational')
    }

    def static readResourceDataFile(filename) {
        return new File('src/test/resources/data/' + filename).text
    }

    // *** CPS Integration Test Utilities ***

    def static countDataNodesInTree(DataNode dataNode) {
        return 1 + countDataNodesInTree(dataNode.getChildDataNodes())
    }

    def static countDataNodesInTree(Collection<DataNode> dataNodes) {
        int nodeCount = 0
        for (DataNode parent : dataNodes) {
            nodeCount += countDataNodesInTree(parent)
        }
        return nodeCount
    }

    def getBookstoreyangResourceContentPerName() {
        def bookstoreModelFileContent = readResourceDataFile('bookstore/bookstore.yang')
        def bookstoreTypesFileContent = readResourceDataFile('bookstore/bookstore-types.yang')
        return [bookstore: bookstoreModelFileContent, bookstoreTypes: bookstoreTypesFileContent]
    }

    def createStandardBookStoreSchemaSet(targetDataspace) {
        cpsModuleService.createSchemaSet(targetDataspace, BOOKSTORE_SCHEMA_SET, getBookstoreyangResourceContentPerName())
    }

    def createStandardBookStoreSchemaSet(targetDataspace, targetSchemaSet) {
        cpsModuleService.createSchemaSet(targetDataspace, targetSchemaSet, getBookstoreyangResourceContentPerName())
    }

    def dataspaceExists(dataspaceName) {
        try {
            cpsDataspaceService.getDataspace(dataspaceName)
        } catch (DataspaceNotFoundException ignored) {
            return false
        }
        return true
    }

    def addAnchorsWithData(numberOfAnchors, dataspaceName, schemaSetName, anchorNamePrefix, data, contentType) {
        (1..numberOfAnchors).each {
            cpsAnchorService.createAnchor(dataspaceName, schemaSetName, anchorNamePrefix + it)
            cpsDataService.saveData(dataspaceName, anchorNamePrefix + it, data.replace("Easons", "Easons-"+it.toString()), OffsetDateTime.now(), contentType)
        }
    }

    def createJsonArray(name, numberOfElements, keyName, keyValuePrefix, dataPerKey) {
        def innerJson = (1..numberOfElements).collect {
            '{"' + keyName + '":"' + keyValuePrefix + '-' + it + '"' + (dataPerKey.empty? '': ',' + dataPerKey) + '}'
        }.join(',')
        return '{"' + name + '":[' + innerJson + ']}'
    }

    def createLeafList(name, numberOfElements, valuePrefix) {
        def innerJson = (1..numberOfElements).collect {'"' + valuePrefix + '-' + it + '"'}.join(',')
        return '"' + name + '":[' + innerJson + ']'
    }

    // *** NCMP Integration Test Utilities ***

    def registerCmHandle(dmiPlugin, cmHandleId, moduleSetTag) {
        registerCmHandle(dmiPlugin, cmHandleId, moduleSetTag, NO_ALTERNATE_ID)
    }

    def registerCmHandle(dmiPlugin, cmHandleId, moduleSetTag, alternateId) {
        registerCmHandleWithoutWaitForReady(dmiPlugin, cmHandleId, moduleSetTag, alternateId)
        moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        CmHandleState.READY == networkCmProxyInventoryFacade.getCmHandleCompositeState(cmHandleId).cmHandleState
    }

    def registerCmHandleWithoutWaitForReady(dmiPlugin, cmHandleId, moduleSetTag, alternateId) {
        def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: cmHandleId, moduleSetTag: moduleSetTag, alternateId: alternateId, dataProducerIdentifier: 'some data producer id')
        networkCmProxyInventoryFacade.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: dmiPlugin, createdCmHandles: [cmHandleToCreate]))
    }

    def registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(dmiPlugin, moduleSetTag, numberOfCmHandles, offset) {
        registerSequenceOfCmHandles(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, ModuleNameStrategy.UNIQUE, { id -> "alt=${id}" })
    }

    def registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, altIdPrefix) {
        registerSequenceOfCmHandles(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, ModuleNameStrategy.UNIQUE, { id -> "${altIdPrefix}alt=${id}" })
    }


    def registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, ModuleNameStrategy moduleNameStrategy) {
        registerSequenceOfCmHandles(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, moduleNameStrategy, { id -> "alt=${id}" })
    }

    def registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, ModuleNameStrategy moduleNameStrategy, Closure<String> alternateIdGenerator) {
        registerSequenceOfCmHandles(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, moduleNameStrategy, alternateIdGenerator)
    }

    def registerSequenceOfCmHandles(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, ModuleNameStrategy moduleNameStrategy, Closure<String> alternateIdGenerator) {
        def cmHandles = []
        def id = offset
        def modulePrefix = moduleNameStrategy.OVERLAPPING.equals(moduleNameStrategy) ? 'same' : moduleSetTag
        def moduleReferences = (1..200).collect { "${modulePrefix}Module${it}" }

        (1..numberOfCmHandles).each {
            def alternateId = alternateIdGenerator(id)
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: "ch-${id}", moduleSetTag: moduleSetTag, alternateId: alternateId)
            cmHandles.add(ncmpServiceCmHandle)
            dmiDispatcher1.moduleNamesPerCmHandleId[ncmpServiceCmHandle.cmHandleId] = moduleReferences
            dmiDispatcher2.moduleNamesPerCmHandleId[ncmpServiceCmHandle.cmHandleId] = moduleReferences
            id++
        }
        networkCmProxyInventoryFacade.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: dmiPlugin, createdCmHandles: cmHandles))
    }

    def deregisterCmHandle(dmiPlugin, cmHandleId) {
        deregisterCmHandles(dmiPlugin, [cmHandleId])
    }

    def deregisterCmHandles(dmiPlugin, cmHandleIds) {
        networkCmProxyInventoryFacade.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: dmiPlugin, removedCmHandles: cmHandleIds))
    }

    def deregisterSequenceOfCmHandles(dmiPlugin, numberOfCmHandles, offset) {
        def cmHandleIds = []
        def id = offset
        (1..numberOfCmHandles).each { cmHandleIds.add('ch-' + id++) }
        networkCmProxyInventoryFacade.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: dmiPlugin, removedCmHandles: cmHandleIds))
    }

    def subscribeAndClearPreviousMessages(consumerGroupId, topicName) {
        kafkaConsumer = KafkaTestContainer.getConsumer(consumerGroupId, StringDeserializer.class)
        kafkaConsumer.subscribe([topicName])
        kafkaConsumer.poll(Duration.ofMillis(500))
    }

    def getLatestConsumerRecordsWithMaxPollOf1Second(numberOfRecordsToRead) {
        def consumerRecords = []
        def retryAttempts = 10
        while (consumerRecords.size() < numberOfRecordsToRead && retryAttempts-- > 0) {
            consumerRecords.addAll(kafkaConsumer.poll(Duration.ofMillis(100)))
        }
        return consumerRecords
    }

    def timestampIsVeryRecent(eventTime) {
        def eventTimeAsOffsetDateTime = EventDateTimeFormatter.toIsoOffsetDateTime(eventTime)
        Duration.between(eventTimeAsOffsetDateTime, ZonedDateTime.now()).seconds < 3
    }

    def clearPreviousInstrumentation() {
        meterRegistry.clear()
    }

}
