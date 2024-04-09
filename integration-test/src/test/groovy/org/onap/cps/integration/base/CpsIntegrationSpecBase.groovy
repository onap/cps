/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

import org.onap.cps.utils.ContentType

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.DatabaseTestContainer
import org.onap.cps.integration.KafkaTestContainer
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.NetworkCmProxyQueryService
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.sync.ModuleSyncWatchdog
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.utils.SessionManager
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = [CpsDataspaceService])
@Testcontainers
@EnableAutoConfiguration
@AutoConfigureMockMvc
@EnableJpaRepositories(basePackageClasses = [DataspaceRepository])
@ComponentScan(basePackages = ['org.onap.cps'])
@EntityScan('org.onap.cps.spi.entities')
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
    NetworkCmProxyCmHandleQueryService networkCmProxyCmHandleQueryService

    @Autowired
    NetworkCmProxyDataService networkCmProxyDataService

    @Autowired
    NetworkCmProxyQueryService networkCmProxyQueryService

    @Autowired
    RestTemplate restTemplate

    @Autowired
    ModuleSyncWatchdog moduleSyncWatchdog

    @Autowired
    JsonObjectMapper jsonObjectMapper

    MockRestServiceServer mockDmiServer = null

    static DMI_URL = 'http://mock-dmi-server'
    static NO_MODULE_SET_TAG = ''
    static GENERAL_TEST_DATASPACE = 'generalTestDataspace'
    static BOOKSTORE_SCHEMA_SET = 'bookstoreSchemaSet'

    def static initialized = false
    def now = OffsetDateTime.now()

    def setup() {
        if (!initialized) {
            cpsDataspaceService.createDataspace(GENERAL_TEST_DATASPACE)
            createStandardBookStoreSchemaSet(GENERAL_TEST_DATASPACE)
            initialized = true
        }
        mockDmiServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build()
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

    def addAnchorsWithData(numberOfAnchors, dataspaceName, schemaSetName, anchorNamePrefix, data) {
        (1..numberOfAnchors).each {
            cpsAnchorService.createAnchor(dataspaceName, schemaSetName, anchorNamePrefix + it)
            cpsDataService.saveData(dataspaceName, anchorNamePrefix + it, data.replace("Easons", "Easons-"+it.toString()), OffsetDateTime.now())
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
        def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: cmHandleId, moduleSetTag: moduleSetTag)
        networkCmProxyDataService.updateDmiRegistrationAndSyncModule(new DmiPluginRegistration(dmiPlugin: dmiPlugin, createdCmHandles: [cmHandleToCreate]))
        moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        new PollingConditions().within(3, () -> {
            CmHandleState.READY == networkCmProxyDataService.getCmHandleCompositeState(cmHandleId).cmHandleState
        })
    }

    def deregisterCmHandle(dmiPlugin, cmHandleId) {
        deregisterCmHandles(dmiPlugin, [cmHandleId])
    }

    def deregisterCmHandles(dmiPlugin, cmHandleIds) {
        networkCmProxyDataService.updateDmiRegistrationAndSyncModule(new DmiPluginRegistration(dmiPlugin: dmiPlugin, removedCmHandles: cmHandleIds))
    }

    def mockDmiResponsesForModuleSync(dmiPlugin, cmHandleId, dmiModuleReferencesResponse, dmiModuleResourcesResponse) {
        mockDmiServer.expect(requestTo("${dmiPlugin}/dmi/v1/ch/${cmHandleId}/modules"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(dmiModuleReferencesResponse))
        mockDmiServer.expect(requestTo("${dmiPlugin}/dmi/v1/ch/${cmHandleId}/moduleResources"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(dmiModuleResourcesResponse))
    }

    def mockDmiIsNotAvailableForModuleSync(dmiPlugin, cmHandleId) {
        mockDmiServer.expect(requestTo("${dmiPlugin}/dmi/v1/ch/${cmHandleId}/modules"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))
    }

    def mockDmiWillRespondToHealthChecks(dmiPlugin) {
        mockDmiServer.expect(ExpectedCount.between(0, Integer.MAX_VALUE), requestTo("${dmiPlugin}/actuator/health"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body('{"status":"UP"}'))
    }

    def overrideCmHandleLastUpdateTime(cmHandleId, newUpdateTime) {
        String ISO_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        DateTimeFormatter ISO_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIMESTAMP_PATTERN);
        def jsonForUpdate = '{ "state": { "last-update-time": "%s" } }'.formatted(ISO_TIMESTAMP_FORMATTER.format(newUpdateTime))
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='${cmHandleId}']", jsonForUpdate, now, ContentType.JSON)
    }
}
