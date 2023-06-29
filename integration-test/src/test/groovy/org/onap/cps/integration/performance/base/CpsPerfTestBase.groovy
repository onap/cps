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

import org.onap.cps.rest.utils.MultipartFileUtil
import org.onap.cps.spi.FetchDescendantsOption
import org.springframework.web.multipart.MultipartFile

class CpsPerfTestBase extends PerfTestBase {

    static def CPS_PERFORMANCE_TEST_DATASPACE = 'cpsPerformanceDataspace'

    def printTitle() {
        println('##        C P S   P E R F O R M A N C E   T E S T   R E S U L T S          ##')
    }

    def isInitialised() {
        return dataspaceExists(CPS_PERFORMANCE_TEST_DATASPACE)
    }

    def setupPerformanceInfraStructure() {
        cpsAdminService.createDataspace(CPS_PERFORMANCE_TEST_DATASPACE)
        def modelAsString = readResourceDataFile('bookstore/bookstore.yang')
        cpsModuleService.createSchemaSet(CPS_PERFORMANCE_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, [bookstore: modelAsString])
    }

    def createInitialData() {
        createWarmupData()
        createLargeBookstoresData()
        addOpenRoadModel()
        addOpenRoadData()
    }

    def createWarmupData() {
        def data = "{\"bookstore\":{}}"
        stopWatch.start()
        addAnchorsWithData(1,  CPS_PERFORMANCE_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'warmup', data)
        stopWatch.stop()
        def durationInMillis = stopWatch.getTotalTimeMillis()
        recordAndAssertPerformance('Creating warmup anchor with tiny data tree', 300, durationInMillis)
    }

    def createLargeBookstoresData() {
        def data = readResourceDataFile('bookstore/largeModelData.json')
        stopWatch.start()
        addAnchorsWithData(5, CPS_PERFORMANCE_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'bookstore', data)
        stopWatch.stop()
        def durationInMillis = stopWatch.getTotalTimeMillis()
        recordAndAssertPerformance('Creating bookstore anchors with large data tree', 1_500, durationInMillis)
    }

    def addOpenRoadModel() {
        def file = new File('src/test/resources/data/openroadm/correctedModel.zip')
        def multipartFile = Mock(MultipartFile)
        multipartFile.getOriginalFilename() >> file.getName()
        multipartFile.getInputStream() >> new FileInputStream(file)
        cpsModuleService.createSchemaSet(CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, MultipartFileUtil.extractYangResourcesMap(multipartFile))
    }

    def addOpenRoadData() {
        def data = generateOpenRoadData(50)
        stopWatch.start()
        addAnchorsWithData(5, CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'openroadm', data)
        stopWatch.stop()
        def durationInMillis = stopWatch.getTotalTimeMillis()
        recordAndAssertPerformance('Creating openroadm anchors with large data tree', 20_000, durationInMillis)
    }

    def generateOpenRoadData(numberOfNodes) {
        def innerNode = readResourceDataFile('openroadm/innerNode.json')
        return '{ "openroadm-devices": { "openroadm-device": [' +
            (1..numberOfNodes).collect { innerNode.replace('NODE_ID_HERE', it.toString()) }.join(',') +
            ']}}'
    }

    def 'Warm the database'() {
        when: 'get data nodes for warmup anchor'
            stopWatch.start()
            def result = cpsDataService.getDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'warmup1', '/', FetchDescendantsOption.OMIT_DESCENDANTS)
            assert countDataNodesInTree(result) == 1
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'all data is read within 20 seconds'
            recordAndAssertPerformance("Warming database", 20_000, durationInMillis)
    }

}
