/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Nordix Foundation
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
import okhttp3.mockwebserver.MockWebServer
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.api.exceptions.DataspaceNotFoundException
import org.onap.cps.api.model.DataNode
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
import org.onap.cps.ri.repository.DataspaceRepository
import org.onap.cps.ri.utils.SessionManager
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.OffsetDateTime
import java.util.concurrent.BlockingQueue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = [CpsDataspaceService])
@Testcontainers
@EnableAutoConfiguration
@AutoConfigureMockMvc
@EnableJpaRepositories(basePackageClasses = [DataspaceRepository])
@ComponentScan(basePackages = ['org.onap.cps'])
@EntityScan('org.onap.cps.ri.models')
@ActiveProfiles('module-sync-delayed')
abstract class CpsIntegrationSpecBase extends Specification {

    @Shared
    DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance()

    @Shared
    KafkaTestContainer kafkaTestContainer = KafkaTestContainer.getInstance()

    @Autowired
    MockMvc mvc

    @Autowired
    CpsDataspaceService cpsDataspaceService

    @Autowired
    CpsAnchorService cpsAnchorService

    @Autowired
    CpsDataService cpsDataService

    @Autowired
    CpsModuleService cpsModuleService

    @Autowired
    CpsQueryService cpsQueryService

    @Autowired
    SessionManager sessionManager

    @Autowired
    ParameterizedCmHandleQueryService networkCmProxyCmHandleQueryService

    @Autowired
    NetworkCmProxyFacade networkCmProxyFacade

    @Autowired
    NetworkCmProxyInventoryFacadeImpl NetworkCmProxyInventoryFacade

    @Autowired
    NetworkCmProxyQueryService networkCmProxyQueryService

    @Autowired
    ModuleSyncWatchdog moduleSyncWatchdog

    @Autowired
    ModuleSyncService moduleSyncService

    @Autowired
    BlockingQueue<String> moduleSyncWorkQueue

    @Autowired
    IMap<String, String> cpsAndNcmpLock

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    InventoryPersistence inventoryPersistence

    @Autowired
    AlternateIdMatcher alternateIdMatcher

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
    static MODULE_SYNC_WAIT_TIME_IN_SECONDS = 2

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

        DMI1_URL = String.format("http://%s:%s", mockDmiServer1.getHostName(), mockDmiServer1.getPort())
        DMI2_URL = String.format("http://%s:%s", mockDmiServer2.getHostName(), mockDmiServer2.getPort())
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

    def getBookstoreYangResourcesNameToContentMap() {
        def bookstoreModelFileContent = readResourceDataFile('bookstore/bookstore.yang')
        def bookstoreTypesFileContent = readResourceDataFile('bookstore/bookstore-types.yang')
        return [bookstore: bookstoreModelFileContent, bookstoreTypes: bookstoreTypesFileContent]
    }

    def createStandardBookStoreSchemaSet(targetDataspace) {
        cpsModuleService.createSchemaSet(targetDataspace, BOOKSTORE_SCHEMA_SET, getBookstoreYangResourcesNameToContentMap())
    }

    def createStandardBookStoreSchemaSet(targetDataspace, targetSchemaSet) {
        cpsModuleService.createSchemaSet(targetDataspace, targetSchemaSet, getBookstoreYangResourcesNameToContentMap())
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
        new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
            CmHandleState.READY == networkCmProxyInventoryFacade.getCmHandleCompositeState(cmHandleId).cmHandleState
        })
    }

    def registerCmHandleWithoutWaitForReady(dmiPlugin, cmHandleId, moduleSetTag, alternateId) {
        def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: cmHandleId, moduleSetTag: moduleSetTag, alternateId: alternateId)
        networkCmProxyInventoryFacade.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: dmiPlugin, createdCmHandles: [cmHandleToCreate]))
    }

    def registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(dmiPlugin, moduleSetTag, numberOfCmHandles, offset) {
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, ModuleNameStrategy.UNIQUE)
    }

    def registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(dmiPlugin, moduleSetTag, numberOfCmHandles, offset, ModuleNameStrategy moduleNameStrategy ) {
        def cmHandles = []
        def id = offset
        def modulePrefix = moduleNameStrategy.OVERLAPPING.equals(moduleNameStrategy) ? 'same' : moduleSetTag
        def moduleReferences = (1..200).collect {  "${modulePrefix}Module${it}" }
        (1..numberOfCmHandles).each {
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: "ch-${id}", moduleSetTag: moduleSetTag, alternateId: NO_ALTERNATE_ID)
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

}
