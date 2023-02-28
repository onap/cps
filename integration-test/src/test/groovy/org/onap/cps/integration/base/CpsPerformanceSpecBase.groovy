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

import java.time.OffsetDateTime
import org.springframework.web.multipart.MultipartFile
import static org.onap.cps.rest.utils.MultipartFileUtil.extractYangResourcesMap;

class CpsPerformanceSpecBase extends PerformanceSpecBase {

    def printTitle() {
        println('##        C P S   P E R F O R M A N C E   T E S T   R E S U L T S          ##')
    }

    def createInitialData() {
        def data = readResourceDataFile('bookstore/largeModelData.json')
        stopWatch.start()
        (1..10).each {
            addAnchorWithData("anchor${it}", data)
        }
        stopWatch.stop()
        def durationInMillis = stopWatch.getTotalTimeMillis()
        recordAndAssertPerformance('Creating anchors with large data tree', 2000, durationInMillis)
    }

    def setupPerformanceInfraStructure() {
        cpsAdminService.createDataspace(PERFORMANCE_TEST_DATASPACE)
        def modelAsString = readResourceDataFile('bookstore/bookstore.yang')
        cpsModuleService.createSchemaSet(PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, [largeModel: modelAsString])
    }

    def addOpenRoadModel() {
        def file = new File('src/test/resources/data/owb-msa221.zip')
        def multipartFile = Mock(MultipartFile)
        multipartFile.getOriginalFilename() >> file.getName()
        multipartFile.getInputStream() >> new FileInputStream(file)
        cpsModuleService.createSchemaSet(PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, MultipartFileUtil.extractYangResourcesMap(multipartFile))
    }

    def addAnchorWithData(anchorName, data) {
        cpsAdminService.createAnchor(PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, anchorName)
        cpsDataService.saveData(PERFORMANCE_TEST_DATASPACE, anchorName, data, OffsetDateTime.now())
    }

}
