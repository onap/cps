/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class E2ENetworkSliceSpec extends Specification {
    def mockModuleStoreService = Mock(CpsModulePersistenceService)
    def mockDataStoreService = Mock(CpsDataPersistenceService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockCpsAdminService = Mock(CpsAdminService)
    def objectUnderTest = new CpsModuleServiceImpl()
    def cpsDataServiceImple = new CpsDataServiceImpl()

    def setup() {
        objectUnderTest.cpsModulePersistenceService = mockModuleStoreService
        objectUnderTest.yangTextSchemaSourceSetCache = mockYangTextSchemaSourceSetCache
        cpsDataServiceImple.cpsDataPersistenceService = mockDataStoreService
        cpsDataServiceImple.yangTextSchemaSourceSetCache = mockYangTextSchemaSourceSetCache
        cpsDataServiceImple.cpsAdminService = mockCpsAdminService
    }

    def 'E2E model can be parsed by CPS.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('e2e/basic/ietf-inet-types.yang','e2e/basic/ietf-yang-types.yang','e2e/basic/ran-network2020-08-06.yang')
        when: 'Create schema set method is invoked'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockModuleStoreService.storeSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
    }

    def 'E2E Coverage Area-Tracking Area & TA-Cell mapping model can be parsed by CPS.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap(
                    'e2e/basic/ietf-inet-types.yang','e2e/basic/ietf-yang-types.yang',
                    'e2e/basic/cps-cavsta-onap-internal2021-01-28.yang')
        when: 'Create schema set method is invoked'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockModuleStoreService.storeSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
    }

    def 'E2E Coverage Area-Tracking Area & TA-Cell mapping data can be parsed.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap(
                    'e2e/basic/ietf-inet-types.yang','e2e/basic/ietf-yang-types.yang',
                    'e2e/basic/cps-cavsta-onap-internal2021-01-28.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap).getSchemaContext()
        and : 'a valid json is provided for the model'
            def jsonData = TestUtils.getResourceFileContent('e2e/basic/Data.txt')
        and : 'all the further dependencies are mocked '
            mockCpsAdminService.getAnchor('someDataspace', 'someAnchor') >>
                    new Anchor().builder().name('someAnchor').schemaSetName('someSchemaSet').build()
            mockYangTextSchemaSourceSetCache.get('someDataspace', 'someSchemaSet') >> YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap)
            mockModuleStoreService.getYangSchemaResources('someDataspace', 'someSchemaSet') >> schemaContext
        when: 'saveData method is invoked'
            cpsDataServiceImple.saveData('someDataspace', 'someAnchor', jsonData)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockDataStoreService.storeDataNode('someDataspace', 'someAnchor', _)
    }
}
