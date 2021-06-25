/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation. All rights reserved.
 *  Modifications Copyright (C) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.api.impl

import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAdminService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.model.Anchor
import org.onap.cps.utils.YangUtils
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class E2ENetworkSliceSpec extends Specification {
    def mockModuleStoreService = Mock(CpsModulePersistenceService)
    def mockDataStoreService = Mock(CpsDataPersistenceService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def cpsModuleServiceImpl = new CpsModuleServiceImpl()
    def cpsDataServiceImple = new CpsDataServiceImpl()
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def dataspaceName = 'someDataspace'
    def anchorName = 'someAnchor'
    def schemaSetName = 'someSchemaSet'

    def setup() {
        cpsDataServiceImple.cpsDataPersistenceService = mockDataStoreService
        cpsDataServiceImple.cpsAdminService = mockCpsAdminService
        cpsDataServiceImple.yangTextSchemaSourceSetCache = mockYangTextSchemaSourceSetCache
        cpsModuleServiceImpl.yangTextSchemaSourceSetCache = mockYangTextSchemaSourceSetCache
        cpsModuleServiceImpl.cpsModulePersistenceService = mockModuleStoreService
    }

    def 'E2E model can be parsed by CPS.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap(
                    'ietf/ietf-inet-types@2013-07-15.yang',
                    'ietf/ietf-yang-types@2013-07-15.yang',
                    'e2e/basic/ran-network2020-08-06.yang'
            )
        when: 'Create schema set method is invoked'
            cpsModuleServiceImpl.createSchemaSet(dataspaceName, schemaSetName, yangResourcesNameToContentMap)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockModuleStoreService.storeSchemaSet(dataspaceName, schemaSetName, yangResourcesNameToContentMap)
    }

    def 'E2E Coverage Area-Tracking Area & TA-Cell mapping model can be parsed by CPS.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap(
                    'e2e/basic/cps-cavsta-onap-internal2021-01-28.yang')
        when: 'Create schema set method is invoked'
            cpsModuleServiceImpl.createSchemaSet(dataspaceName, schemaSetName, yangResourcesNameToContentMap)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockModuleStoreService.storeSchemaSet(dataspaceName, schemaSetName, yangResourcesNameToContentMap)
    }

    def 'E2E Coverage Area-Tracking Area & TA-Cell mapping data can be parsed by CPS.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap(
                    'e2e/basic/cps-cavsta-onap-internal2021-01-28.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap).getSchemaContext()
            def dataNodeStored
        and : 'a valid json is provided for the model'
            def jsonData = TestUtils.getResourceFileContent('e2e/basic/cps-Cavsta-Data.txt')
        and : 'all the further dependencies are mocked '
            mockCpsAdminService.getAnchor(dataspaceName, anchorName) >>
                    new Anchor().builder().name(anchorName).schemaSetName(schemaSetName).build()
            mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >>
                    YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap)
            mockModuleStoreService.getYangSchemaResources(dataspaceName, schemaSetName) >> schemaContext
        when: 'saveData method is invoked'
            cpsDataServiceImple.saveData(dataspaceName, anchorName, jsonData)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockDataStoreService.storeDataNode('someDataspace', 'someAnchor', _) >>
                    { args -> dataNodeStored = args[2]}
            def child = dataNodeStored.childDataNodes[0]
            assert child.childDataNodes.size() == 1
        and: 'list of Tracking Area for a Coverage Area are stored with correct xpath and child nodes '
            def listOfTAForCoverageArea = child.childDataNodes[0]
            listOfTAForCoverageArea.xpath == '/ran-coverage-area/pLMNIdList[@mcc=\'310\' and @mnc=\'410\']/' +
                    'coverage-area[@coverageArea=\'Washington\']'
            listOfTAForCoverageArea.childDataNodes[0].leaves.get('nRTAC') == 234
        and: 'list of cells in a tracking area are stored with correct xpath and child nodes '
            def listOfCellsInTrackingArea = listOfTAForCoverageArea.childDataNodes[0]
            listOfCellsInTrackingArea.xpath == '/ran-coverage-area/pLMNIdList[@mcc=\'310\' and @mnc=\'410\']/' +
                    'coverage-area[@coverageArea=\'Washington\']/coverageAreaTAList[@nRTAC=\'234\']'
            listOfCellsInTrackingArea.childDataNodes[0].leaves.get('cellLocalId') == 15709
    }

    def 'E2E Coverage Area-Tracking Area & TA-Cell mapping data can be parsed for RAN inventory.'() {
        def dataNodeStored
        given: 'valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap(
                    'e2e/basic/cps-ran-inventory@2021-01-28.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap).getSchemaContext()
        and : 'a valid json is provided for the model'
            def jsonData = TestUtils.getResourceFileContent('e2e/basic/cps-ran-inventory-data.json')
        and : 'all the further dependencies are mocked '
            mockCpsAdminService.getAnchor('someDataspace', 'someAnchor') >>
                    new Anchor().builder().name('someAnchor').schemaSetName('someSchemaSet').build()
            mockYangTextSchemaSourceSetCache.get('someDataspace', 'someSchemaSet') >> YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap)
            mockModuleStoreService.getYangSchemaResources('someDataspace', 'someSchemaSet') >> schemaContext
        when: 'saveData method is invoked'
            cpsDataServiceImple.saveData('someDataspace', 'someAnchor', jsonData)
        then: 'parameters are validated and processing is delegated to persistence service'
            1 * mockDataStoreService.storeDataNode('someDataspace', 'someAnchor', _) >>
                    { args -> dataNodeStored = args[2]}
        and: 'the size of the tree is correct'
            def cpsRanInventory = TestUtils.getFlattenMapByXpath(dataNodeStored)
            assert  cpsRanInventory.size() == 4
        and: 'ran-inventory contains the correct child node'
            def ranInventory = cpsRanInventory.get('/ran-inventory')
            def ranSlices = cpsRanInventory.get('/ran-inventory/ran-slices[@rannfnssiid=\'14559ead-f4fe-4c1c-a94c-8015fad3ea35\']')
            def sliceProfilesList = cpsRanInventory.get('/ran-inventory/ran-slices[@rannfnssiid=\'14559ead-f4fe-4c1c-a94c-8015fad3ea35\']/sliceProfilesList[@sliceProfileId=\'f33a9dd8-ae51-4acf-8073-c9390c25f6f1\']')
            def pLMNIdList = cpsRanInventory.get('/ran-inventory/ran-slices[@rannfnssiid=\'14559ead-f4fe-4c1c-a94c-8015fad3ea35\']/sliceProfilesList[@sliceProfileId=\'f33a9dd8-ae51-4acf-8073-c9390c25f6f1\']/pLMNIdList[@mcc=\'310\' and @mnc=\'410\']')
            ranInventory.getChildDataNodes().size() == 1
            ranInventory.getChildDataNodes().find( {it.xpath == ranSlices.xpath})
        and: 'ranSlices contains the correct child node'
            ranSlices.getChildDataNodes().size() == 1
            ranSlices.getChildDataNodes().find( {it.xpath == sliceProfilesList.xpath})
        and: 'sliceProfilesList contains the correct child node'
            sliceProfilesList.getChildDataNodes().size() == 1
            sliceProfilesList.getChildDataNodes().find( {it.xpath == pLMNIdList.xpath})
        and: 'pLMNIdList contains no children'
            pLMNIdList.getChildDataNodes().size() == 0

    }

    def 'E2E RAN Schema Model.'(){
        given: 'yang resources'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap(
                    'ietf/ietf-inet-types@2013-07-15.yang',
                    'ietf/ietf-yang-types@2013-07-15.yang',
                    'e2e/basic/cps-ran-schema-model@2021-05-19.yang'
            )
        and : 'json data'
            def jsonData = TestUtils.getResourceFileContent('e2e/basic/cps-ran-schema-model-data-v4.json')
        expect: 'schema context is built with no exception indicating the schema set being valid '
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap).getSchemaContext()
        and: 'data is parsed with no exception indicating the model match'
            YangUtils.parseJsonData(jsonData, schemaContext) != null
    }
}
