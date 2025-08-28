/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
 * Modifications Copyright (C) 2021-2022 Bell Canada.
 * Modifications Copyright (C) 2021 Pantheon.tech
 * Modifications Copyright (C) 2022-2025 TechMahindra Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.impl

import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.model.Anchor
import org.onap.cps.events.CpsDataUpdateEventsProducer
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.CpsValidator
import org.onap.cps.utils.YangParser
import org.onap.cps.utils.YangParserHelper
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class E2ENetworkSliceSpec extends Specification {
    def mockCpsModulePersistenceService = Mock(CpsModulePersistenceService)
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockCpsValidator = Mock(CpsValidator)
    def timedYangTextSchemaSourceSetBuilder = new TimedYangTextSchemaSourceSetBuilder()
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache, timedYangTextSchemaSourceSetBuilder)
    def cpsModuleServiceImpl = new CpsModuleServiceImpl(mockCpsModulePersistenceService, mockYangTextSchemaSourceSetCache, mockCpsAnchorService, mockCpsValidator,timedYangTextSchemaSourceSetBuilder,yangParser)
    def mockCpsDataUpdateEventsProducer = Mock(CpsDataUpdateEventsProducer)
    def dataNodeFactory = new DataNodeFactoryImpl(yangParser)
    def cpsDataServiceImpl = new CpsDataServiceImpl(mockCpsDataPersistenceService, mockCpsDataUpdateEventsProducer, mockCpsAnchorService, dataNodeFactory, mockCpsValidator, yangParser)
    def dataspaceName = 'someDataspace'
    def anchorName = 'someAnchor'
    def schemaSetName = 'someSchemaSet'
    def noTimestamp = null

    def 'E2E model can be parsed by CPS.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap(
                    'ietf/ietf-inet-types@2013-07-15.yang',
                    'ietf/ietf-yang-types@2013-07-15.yang',
                    'e2e/basic/ran-network2020-08-06.yang'
            )
        when: 'Create schema set method is invoked'
            cpsModuleServiceImpl.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentPerName)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockCpsModulePersistenceService.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentPerName)
    }

    def 'E2E Coverage Area-Tracking Area & TA-Cell mapping model can be parsed by CPS.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap(
                    'e2e/basic/cps-cavsta-onap-internal2021-01-28.yang')
        when: 'Create schema set method is invoked'
            cpsModuleServiceImpl.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentPerName)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockCpsModulePersistenceService.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentPerName)
    }

    def 'E2E Coverage Area-Tracking Area & TA-Cell mapping data can be parsed by CPS.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap(
                    'e2e/basic/cps-cavsta-onap-internal2021-01-28.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceContentPerName).schemaContext()
            def dataNodeStored
        and : 'a valid json is provided for the model'
            def jsonData = TestUtils.getResourceFileContent('e2e/basic/cps-Cavsta-Data.txt')
        and : 'all the further dependencies are mocked '
            mockCpsAnchorService.getAnchor(dataspaceName, anchorName) >>
                    new Anchor().builder().name(anchorName).schemaSetName(schemaSetName).dataspaceName(dataspaceName).build()
            mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >>
                    YangTextSchemaSourceSetBuilder.of(yangResourceContentPerName)
            mockCpsModulePersistenceService.getYangSchemaResources(dataspaceName, schemaSetName) >> schemaContext
        when: 'saveData method is invoked'
            cpsDataServiceImpl.saveData(dataspaceName, anchorName, jsonData, noTimestamp)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockCpsDataPersistenceService.storeDataNodes('someDataspace', 'someAnchor', _) >>
                    { args -> dataNodeStored = args[2]}
            def child = dataNodeStored[0].childDataNodes[0]
            assert child.childDataNodes.size() == 1
        and: 'list of Tracking Area for a Coverage Area are stored with correct xpath and child nodes '
            def listOfTAForCoverageArea = child.childDataNodes[0]
            listOfTAForCoverageArea.xpath == '/ran-coverage-area/pLMNIdList[@mcc=\'310\' and @mnc=\'410\']/coverage-area[@coverageArea=\'Washington\']'
            assert  listOfTAForCoverageArea.childDataNodes[0].leaves.get('nRTAC') == 234
        and: 'list of cells in a tracking area are stored with correct xpath and child nodes '
            def listOfCellsInTrackingArea = listOfTAForCoverageArea.childDataNodes[0]
            listOfCellsInTrackingArea.xpath == '/ran-coverage-area/pLMNIdList[@mcc=\'310\' and @mnc=\'410\']/coverage-area[@coverageArea=\'Washington\']/coverageAreaTAList[@nRTAC=\'234\']'
            listOfCellsInTrackingArea.childDataNodes[0].leaves.get('cellLocalId') == 15709
    }

    def 'E2E Coverage Area-Tracking Area & TA-Cell mapping data can be parsed for RAN inventory.'() {
        def dataNodeStored
        given: 'valid yang resource as name-to-content map'
            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap('e2e/basic/cps-ran-inventory@2021-01-28.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceContentPerName).schemaContext()
        and : 'a valid json is provided for the model'
            def jsonData = TestUtils.getResourceFileContent('e2e/basic/cps-ran-inventory-data.json')
        and : 'all the further dependencies are mocked '
            mockCpsAnchorService.getAnchor('someDataspace', 'someAnchor') >>
                    new Anchor().builder().name('someAnchor').schemaSetName('someSchemaSet').dataspaceName(dataspaceName).build()
            mockYangTextSchemaSourceSetCache.get('someDataspace', 'someSchemaSet') >> YangTextSchemaSourceSetBuilder.of(yangResourceContentPerName)
            mockCpsModulePersistenceService.getYangSchemaResources('someDataspace', 'someSchemaSet') >> schemaContext
        when: 'saveData method is invoked'
            cpsDataServiceImpl.saveData('someDataspace', 'someAnchor', jsonData, noTimestamp)
        then: 'parameters are validated and processing is delegated to persistence service'
            1 * mockCpsDataPersistenceService.storeDataNodes('someDataspace', 'someAnchor', _) >> { args -> dataNodeStored = args[2]}
        and: 'the size of the tree is correct'
            def cpsRanInventory = TestUtils.getFlattenMapByXpath(dataNodeStored[0])
            assert  cpsRanInventory.size() == 4
        and: 'ran-inventory contains the correct child node'
            def ranInventory = cpsRanInventory.get('/ran-inventory')
            def ranSlices = cpsRanInventory.get('/ran-inventory/ran-slices[@rannfnssiid=\'14559ead-f4fe-4c1c-a94c-8015fad3ea35\']')
            def sliceProfilesList = cpsRanInventory.get('/ran-inventory/ran-slices[@rannfnssiid=\'14559ead-f4fe-4c1c-a94c-8015fad3ea35\']/sliceProfilesList[@sliceProfileId=\'f33a9dd8-ae51-4acf-8073-c9390c25f6f1\']')
            def pLMNIdList = cpsRanInventory.get('/ran-inventory/ran-slices[@rannfnssiid=\'14559ead-f4fe-4c1c-a94c-8015fad3ea35\']/sliceProfilesList[@sliceProfileId=\'f33a9dd8-ae51-4acf-8073-c9390c25f6f1\']/pLMNIdList[@mcc=\'310\' and @mnc=\'410\']')
            assert ranInventory.getChildDataNodes().size() == 1
            assert ranInventory.getChildDataNodes().find( {it.xpath == ranSlices.xpath})
        and: 'ranSlices contains the correct child node'
            assert ranSlices.getChildDataNodes().size() == 1
            assert ranSlices.getChildDataNodes().find( {it.xpath == sliceProfilesList.xpath})
        and: 'sliceProfilesList contains the correct child node'
            assert sliceProfilesList.getChildDataNodes().size() == 1
            assert sliceProfilesList.getChildDataNodes().find( {it.xpath == pLMNIdList.xpath})
        and: 'pLMNIdList contains no children'
            assert pLMNIdList.getChildDataNodes().size() == 0
    }

    def 'E2E RAN Schema Model.'(){
        given: 'yang resources'
            def yangResourceContentPerName = TestUtils.getYangResourcesAsMap(
                    'ietf/ietf-inet-types@2013-07-15.yang',
                    'ietf/ietf-yang-types@2013-07-15.yang',
                    'e2e/basic/cps-ran-schema-model@2021-05-19.yang'
            )
        and : 'json data'
            def jsonData = TestUtils.getResourceFileContent('e2e/basic/cps-ran-schema-model-data-v4.json')
        expect: 'schema context is built with no exception indicating the schema set being valid '
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceContentPerName).schemaContext()
        and: 'data is parsed with no exception indicating the model match'
            new YangParserHelper().parseData(ContentType.JSON, jsonData, schemaContext, '', false) != null
    }
}
