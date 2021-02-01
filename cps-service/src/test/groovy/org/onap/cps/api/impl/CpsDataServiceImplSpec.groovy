/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.impl

import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchChildrenOption
import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification
import spock.lang.Unroll

public class CpsDataServiceImplSpec extends Specification {

    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def objectUnderTest = new CpsDataServiceImpl()

    def setup() {
        objectUnderTest.cpsDataPersistenceService = mockCpsDataPersistenceService;
        objectUnderTest.cpsAdminService = mockCpsAdminService;
        objectUnderTest.cpsModuleService = mockCpsModuleService;
        objectUnderTest.yangTextSchemaSourceSetCache = mockYangTextSchemaSourceSetCache;
    }

    def dataspaceName = 'some dataspace'
    def anchorName = 'some anchor'

    @Unroll
    def 'Get data node with option #fetchChildrenOption'() {
        def xpath = "/xpath"
        def dataNode = new DataNodeBuilder().withXpath(xpath).build()
        given:
            mockCpsDataPersistenceService.getDataNode(dataspaceName, anchorName, xpath, fetchChildrenOption) >> dataNode
        expect:
            objectUnderTest.getDataNode(dataspaceName, anchorName, xpath, fetchChildrenOption) == dataNode
        where:
            fetchChildrenOption << FetchChildrenOption.values()
    }

}
