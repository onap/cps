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

package org.onap.cps.integration.base

import org.onap.cps.api.CpsQueryService
import org.onap.cps.api.impl.CpsAdminServiceImpl
import org.onap.cps.api.impl.CpsDataServiceImpl
import org.onap.cps.api.impl.CpsModuleServiceImpl
import org.onap.cps.integration.DatabaseTestContainer
import org.onap.cps.spi.config.CpsSessionFactory
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.impl.utils.CpsValidatorImpl
import org.onap.cps.spi.utils.SessionManager
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

@SpringBootTest(classes = [TestConfig, CpsAdminServiceImpl, CpsValidatorImpl, SessionManager, CpsSessionFactory])
@Testcontainers
@EnableAutoConfiguration
@EnableJpaRepositories(basePackageClasses = [DataspaceRepository])
@ComponentScan(basePackages = ['org.onap.cps.api', 'org.onap.cps.spi.repository'])
@EntityScan('org.onap.cps.spi.entities')
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

    @Autowired
    @Lazy
    CpsQueryService cpsQueryService

    @Autowired
    @Lazy
    SessionManager sessionManager

    def static GENERAL_TEST_DATASPACE = 'generalTestDataspace'
    def static BOOKSTORE_SCHEMA_SET = 'bookstoreSchemaSet'

    def static initialized = false
    def now = OffsetDateTime.now()

    def setup() {
        if (!initialized) {
            cpsAdminService.createDataspace(GENERAL_TEST_DATASPACE)
            def bookstoreModelFileContent = readResourceDataFile('bookstore/bookstore.yang')
            cpsModuleService.createSchemaSet(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, [bookstore : bookstoreModelFileContent])
            initialized = true;
        }
    }

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

    def static readResourceDataFile(filename) {
        return new File('src/test/resources/data/' + filename).text
    }

    def dataspaceExists(dataspaceName) {
        try {
            cpsAdminService.getDataspace(dataspaceName)
        } catch (DataspaceNotFoundException dataspaceNotFoundException) {
            return false
        }
        return true
    }

    def addAnchorsWithData(numberOfAnchors, dataspaceName, schemaSetName, anchorNamePrefix, data) {
        (1..numberOfAnchors).each {
            cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorNamePrefix + it)
            cpsDataService.saveData(dataspaceName, anchorNamePrefix + it, data.replace("Easons", "Easons-"+it.toString()), OffsetDateTime.now())
        }
    }

    def createJsonArray(name, numberOfElements, keyName, keyValuePrefix, dataPerKey) {
        def json = '{"' + name + '":['
        (1..numberOfElements).each {
            json += '{"' + keyName + '":"' + keyValuePrefix + '-' + it + '"'
            if (!dataPerKey.isEmpty()) {
                json += ',' + dataPerKey
            }
            json += '}'
            if (it < numberOfElements) {
                json += ','
            }
        }
        json += ']}'
    }

    def createLeafList(name, numberOfElements, valuePrefix) {
        def json = '"' + name + '":['
        (1..numberOfElements).each {
            json += '"' + valuePrefix + '-' + it + '"'
            if (it < numberOfElements) {
                json += ','
            }
        }
        json += ']'
    }

}
