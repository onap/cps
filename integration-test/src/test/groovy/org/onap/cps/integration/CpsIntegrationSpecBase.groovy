/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada.
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

package org.onap.cps.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.impl.CpsAdminServiceImpl
import org.onap.cps.api.impl.CpsDataServiceImpl
import org.onap.cps.api.impl.CpsModuleServiceImpl
import org.onap.cps.spi.CascadeDeleteAllowed
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.impl.CpsAdminPersistenceServiceImpl
import org.onap.cps.spi.impl.CpsDataPersistenceServiceImpl
import org.onap.cps.spi.impl.CpsModulePersistenceServiceImpl
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.FragmentRepository
import org.onap.cps.spi.repository.ModuleReferenceRepository
import org.onap.cps.spi.repository.SchemaSetRepository
import org.onap.cps.spi.repository.YangResourceRepository
import org.onap.cps.spi.impl.utils.CpsValidatorImpl
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.EnableSharedInjection
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.stereotype.Repository
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime

@SpringBootTest(classes = [TestConfig, CpsAdminServiceImpl, CpsValidatorImpl])
@Testcontainers
@EnableAutoConfiguration
@EnableJpaRepositories(basePackageClasses = [DataspaceRepository])
@ComponentScan(basePackages = ["org.onap.cps.api", "org.onap.cps.spi.repository"])
@EntityScan("org.onap.cps.spi.entities")
class CpsIntegrationSpecBase extends Specification {

    @Shared
    DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance()

    @Autowired
    @Lazy
    CpsAdminServiceImpl cpsAdminService

    @Autowired
    @Lazy
    CpsDataServiceImpl cpsDataService

    @Autowired
    @Lazy
    CpsModuleServiceImpl cpsModuleService


    def static TEST_DATASPACE = 'testDataspace'
    def static BOOKSTORE_SCHEMA_SET = 'bookstoreSchemaSet'
    def static TEST_ANCHOR = 'testAnchor'

    @Configuration
    static class TestConfig {
        @Autowired
        @Lazy
        DataspaceRepository dataspaceRepository

        @Autowired
        @Lazy
        AnchorRepository anchorRepository

        @Autowired
        @Lazy
        SchemaSetRepository schemaSetRepository

        @Autowired
        @Lazy
        YangResourceRepository yangResourceRepository

        @Autowired
        @Lazy
        FragmentRepository fragmentRepository

        @Autowired
        @Lazy
        ModuleReferenceRepository moduleReferenceRepository

        @Autowired
        @Lazy
        JsonObjectMapper jsonObjectMapper

        @Bean
        CpsAdminPersistenceServiceImpl cpsAdminPersistenceService() {
            new CpsAdminPersistenceServiceImpl(dataspaceRepository, anchorRepository, schemaSetRepository, yangResourceRepository)
        }

        @Bean
        CpsDataPersistenceService cpsDataPersistenceService() {
            return (CpsDataPersistenceService) new CpsDataPersistenceServiceImpl(dataspaceRepository, anchorRepository, fragmentRepository, jsonObjectMapper)
        } // try remove private constructor

        @Bean
        CpsModulePersistenceService cpsModulePersistenceService() {
            return (CpsModulePersistenceService) new CpsModulePersistenceServiceImpl(yangResourceRepository, schemaSetRepository, dataspaceRepository, cpsAdminPersistenceService(), moduleReferenceRepository)
        }

        @Bean
        JsonObjectMapper jsonObjectMapper() {
            return new JsonObjectMapper(new ObjectMapper())
        }
    }

    def 'Test creation of test data'() {
        when: 'A dataspace, schema set and anchor are persisted'
            createDataspaceSchemaSetAnchor(TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'src/test/resources/data/bookstore.yang', TEST_ANCHOR)
        and: 'data nodes are persisted under the created anchor'
            saveDataNodes(TEST_DATASPACE, TEST_ANCHOR, '/', 'src/test/resources/data/BookstoreDataNodes.json')
        then: 'The dataspace has been persisted successfully'
            cpsAdminService.getDataspace(TEST_DATASPACE).getName() == TEST_DATASPACE
        and: 'The schema set has been persisted successfully'
            cpsModuleService.getSchemaSet(TEST_DATASPACE, BOOKSTORE_SCHEMA_SET).getName() == BOOKSTORE_SCHEMA_SET
        and: 'The anchor has been persisted successfully'
            cpsAdminService.getAnchor(TEST_DATASPACE, TEST_ANCHOR).getName() == TEST_ANCHOR
        and: 'The data nodes have been persisted successfully'
            cpsDataService.getDataNode(TEST_DATASPACE, TEST_ANCHOR, '/bookstore', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS).xpath == '/bookstore'
    }

    def 'Test deletion of all test data'() {
        when: 'delete all from test dataspace method is called'
            deleteAllFromTestDataspace()
        and: 'the test dataspace is deleted'
            cpsAdminService.deleteDataspace(TEST_DATASPACE)
        then: 'there is no test dataspace'
            !cpsAdminService.getAllDataspaces().contains(TEST_DATASPACE)
    }


    def createDataspaceSchemaSetAnchor(String dataspaceName, String schemaSetName, String pathToYangModel, String anchorName) {
        cpsAdminService.createDataspace(dataspaceName)
        def bookstoreFileContent = new File(pathToYangModel).text
        cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, [(pathToYangModel) : bookstoreFileContent])
        cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName)
    }

    def createSchemaSetAnchor(String dataspaceName, String schemaSetName, String pathToYangModel, String anchorName) {
        cpsAdminService.createDataspace(dataspaceName)
        def bookstoreFileContent = new File(pathToYangModel).text
        cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, [(pathToYangModel) : bookstoreFileContent])
        cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName)
    }

    def saveDataNodes(String dataspaceName, String anchorName, String parentNodeXpath, String pathToDataNodesAsJson) {
        def dataNodesAsJSON = new File(pathToDataNodesAsJson).text
        if (isRootXpath(parentNodeXpath)) {
            cpsDataService.saveData(dataspaceName, anchorName, dataNodesAsJSON,
                OffsetDateTime.now(), ContentType.JSON);
        } else {
            cpsDataService.saveData(dataspaceName, anchorName, parentNodeXpath,
                dataNodesAsJSON, OffsetDateTime.now(), ContentType.JSON);
        }
    }

    def deleteAllFromTestDataspace() {
        def anchors = cpsAdminService.getAnchors(TEST_DATASPACE)
        for(anchor in anchors) {
            cpsDataService.deleteDataNodes(TEST_DATASPACE, anchor.getName(), OffsetDateTime.now())
            cpsAdminService.deleteAnchor(TEST_DATASPACE, anchor.getName())
        }
        def schemaSets = cpsModuleService.getSchemaSets(TEST_DATASPACE)
        for(schemaSet in schemaSets) {
            cpsModuleService.deleteSchemaSet(TEST_DATASPACE, schemaSet.getName(), CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED)
        }
    }

    private static boolean isRootXpath(final String xpath) {
        return "/".equals(xpath);
    }
}
