/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

import org.onap.cps.api.impl.CpsAdminServiceImpl
import org.onap.cps.api.impl.CpsDataServiceImpl
import org.onap.cps.api.impl.CpsModuleServiceImpl
import org.onap.cps.spi.CascadeDeleteAllowed
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.impl.utils.CpsValidatorImpl
import org.onap.cps.utils.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
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

    def createDataspaceSchemaSetAnchor(String dataspaceName, String schemaSetName, String schemaSetFileName, String anchorName) {
        cpsAdminService.createDataspace(dataspaceName)
        createSchemaSetAnchor(dataspaceName, schemaSetName, schemaSetFileName, anchorName)
    }

    def createSchemaSetAnchor(String dataspaceName, String schemaSetName, String schemaSetFileName, String anchorName) {
        def bookstoreFileContent = readResourceFile(schemaSetFileName)
        cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, [(schemaSetFileName) : bookstoreFileContent])
        cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName)
    }

    def saveDataNodes(String dataspaceName, String anchorName, String parentNodeXpath, String dataNodesFileName) {
        def dataNodesAsJSON = readResourceFile(dataNodesFileName)
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

    def static readResourceFile(String filename) {
        return new File('src/test/resources/data/' + filename).text
    }

    def static isRootXpath(final String xpath) {
        return "/".equals(xpath);
    }
}
