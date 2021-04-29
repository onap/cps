/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class CpsDataServiceImplSpec extends Specification {
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def objectUnderTest = new CpsDataServiceImpl()

    def setup() {
        objectUnderTest.cpsDataPersistenceService = mockCpsDataPersistenceService
        objectUnderTest.cpsAdminService = mockCpsAdminService
        objectUnderTest.cpsModuleService = mockCpsModuleService
        objectUnderTest.yangTextSchemaSourceSetCache = mockYangTextSchemaSourceSetCache
    }

    def dataspaceName = 'some dataspace'
    def anchorName = 'some anchor'
    def schemaSetName = 'some schema set'

    def 'Saving json data.'() {
        given: 'that the admin service will return an anchor'
            def anchor = Anchor.builder().name(anchorName).schemaSetName(schemaSetName).build()
            mockCpsAdminService.getAnchor(dataspaceName, anchorName) >> anchor
        and: 'the schema source set cache returns a schema source set'
            def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
            mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        and: 'the schema source sets returns the test-tree schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
            mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
        when: 'save data method is invoked with test-tree json data'
            def jsonData = TestUtils.getResourceFileContent('test-tree.json')
            objectUnderTest.saveData(dataspaceName, anchorName, jsonData)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.storeDataNode(dataspaceName, anchorName,
                    { dataNode -> dataNode.xpath == '/test-tree' })
    }

    def 'Saving child data fragment under existing node.'() {
        given: 'that the admin service will return an anchor'
            def anchor = Anchor.builder().name(anchorName).schemaSetName(schemaSetName).build()
            mockCpsAdminService.getAnchor(dataspaceName, anchorName) >> anchor
        and: 'the schema source set cache returns a schema source set'
            def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
            mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        and: 'the schema source sets returns the test-tree schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
            mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
        when: 'save data method is invoked with test-tree json data'
            def jsonData = '{"branch": [{"name": "New"}]}'
            objectUnderTest.saveData(dataspaceName, anchorName, '/test-tree',jsonData)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.addChildDataNode(dataspaceName, anchorName,'/test-tree',
                    { dataNode -> dataNode.xpath == '/test-tree/branch[@name=\'New\']' })
    }

    def 'Get data node with option #fetchDescendantsOption.'() {
        def xpath = '/xpath'
        def dataNode = new DataNodeBuilder().withXpath(xpath).build()
        given: 'persistence service returns data for get data request'
            mockCpsDataPersistenceService.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption) >> dataNode
        expect: 'service returns same data if uses same parameters'
            objectUnderTest.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption) == dataNode
        where: 'all fetch options are supported'
            fetchDescendantsOption << FetchDescendantsOption.values()
    }

    def 'Update data node leaves: #scenario.'() {
        given: 'that the admin service will return an anchor'
            def anchor = Anchor.builder().name(anchorName).schemaSetName(schemaSetName).build()
            mockCpsAdminService.getAnchor(dataspaceName, anchorName) >> anchor
        and: 'the schema source set cache returns a schema source set'
            def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
            mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        and: 'the schema source sets returns the test-tree schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
            mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
        when: 'update data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath, jsonData)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.updateDataLeaves(dataspaceName, anchorName, nodeXpath, leaves)
        where: 'following parameters were used'
            scenario         | parentNodeXpath | jsonData                         | nodeXpath                           | leaves
            'top level node' | '/'             | '{ "test-tree": {"branch": []}}' | '/test-tree'                        | Collections.emptyMap()
            'level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}'  | '/test-tree/branch[@name=\'Name\']' | ['name': 'Name']
    }

    def 'Replace data node: #scenario.'() {
        given: 'that the admin service will return an anchor'
            def anchor = Anchor.builder().name(anchorName).schemaSetName(schemaSetName).build()
            mockCpsAdminService.getAnchor(dataspaceName, anchorName) >> anchor
        and: 'the schema source set cache returns a schema source set'
            def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
            mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        and: 'the schema source sets returns the test-tree schema context'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
            mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
        when: 'replace data method is invoked with json data #jsonData and parent node xpath #parentNodeXpath'
            objectUnderTest.replaceNodeTree(dataspaceName, anchorName, parentNodeXpath, jsonData)
        then: 'the persistence service method is invoked with correct parameters'
            1 * mockCpsDataPersistenceService.replaceDataNodeTree(dataspaceName, anchorName,
                    { dataNode -> dataNode.xpath == nodeXpath })
        where: 'following parameters were used'
            scenario         | parentNodeXpath | jsonData                         | nodeXpath
            'top level node' | '/'             | '{ "test-tree": {"branch": []}}' | '/test-tree'
            'level 2 node'   | '/test-tree'    | '{"branch": [{"name":"Name"}]}'  | '/test-tree/branch[@name=\'Name\']'
    }
}