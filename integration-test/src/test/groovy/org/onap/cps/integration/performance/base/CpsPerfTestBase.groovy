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

import java.util.concurrent.TimeUnit

class CpsPerfTestBase extends PerfTestBase {

    static final def CPS_PERFORMANCE_TEST_DATASPACE = 'cpsPerformanceDataspace'
    static final def OPENROADM_ANCHORS = 3
    static final def OPENROADM_DEVICES_PER_ANCHOR = 1000
    static final def OPENROADM_DATANODES_PER_DEVICE = 86

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
        addOpenRoadModel()
        addOpenRoadData()
    }

    def addOpenRoadModel() {
        def file = new File('src/test/resources/data/openroadm/correctedModel.zip')
        def multipartFile = Mock(MultipartFile)
        multipartFile.getOriginalFilename() >> file.getName()
        multipartFile.getInputStream() >> new FileInputStream(file)
        cpsModuleService.createSchemaSet(CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, MultipartFileUtil.extractYangResourcesMap(multipartFile))
    }

    def addOpenRoadData() {
        def data = generateOpenRoadData(OPENROADM_DEVICES_PER_ANCHOR)
        stopWatch.start()
        addAnchorsWithData(OPENROADM_ANCHORS, CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'openroadm', data)
        stopWatch.stop()
        def durationInMillis = stopWatch.getTotalTimeMillis()
        recordAndAssertPerformance('Creating openroadm anchors with large data tree', TimeUnit.SECONDS.toMillis(200), durationInMillis)
    }

    def generateOpenRoadData(numberOfNodes) {
        def innerNode = readResourceDataFile('openroadm/innerNode.json')
        return '{ "openroadm-devices": { "openroadm-device": [' +
            (1..numberOfNodes).collect { innerNode.replace('NODE_ID_HERE', it.toString()) }.join(',') +
            ']}}'
    }

    def 'Warm the database'() {
        when: 'dummy get data nodes runs so that populating the DB does not get included in other test timings'
            stopWatch.start()
            def result = cpsDataService.getDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', '/', FetchDescendantsOption.OMIT_DESCENDANTS)
            assert countDataNodesInTree(result) == 1
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'all data is read within expected time'
            recordAndAssertPerformance("Warming database", 100, durationInMillis)
    }

}
