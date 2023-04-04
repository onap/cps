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

package org.onap.cps.integration.performance.base

import java.time.OffsetDateTime

import org.onap.cps.integration.base.CpsIntegrationSpecBase

class NcmpRegistryPerfTestBase extends PerfTestBase {

    def static REGISTRY_ANCHOR = 'ncmp-registry'
    def static REGISTRY_SCHEMA_SET = 'registrySchemaSet'
    def static NCMP_PERFORMANCE_TEST_DATASPACE = 'ncmpPerformacne'

    def printTitle() {
        println('##      N C M P   P E R F O R M A N C E   T E S T   R E S U L T S          ##')
    }

    def isInitialised() {
        return dataspaceExists(NCMP_PERFORMANCE_TEST_DATASPACE)
    }

    def setupPerformanceInfraStructure() {
        cpsAdminService.createDataspace(NCMP_PERFORMANCE_TEST_DATASPACE)
        def modelAsString = readResourceDataFile('ncmp-registry/dmi-registry@2022-05-10.yang')
        cpsModuleService.createSchemaSet(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_SCHEMA_SET, [registry: modelAsString])
    }

    def createInitialData() {
        def data = readResourceDataFile('ncmp-registry/1000-cmhandles.json')
        cpsAdminService.createAnchor(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_SCHEMA_SET, REGISTRY_ANCHOR)
        cpsDataService.saveData(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, data, OffsetDateTime.now())
    }


}
