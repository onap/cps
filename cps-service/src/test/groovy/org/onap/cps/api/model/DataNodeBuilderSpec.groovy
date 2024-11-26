/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

package org.onap.cps.api.model

import org.onap.cps.TestUtils
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.DataMapUtils
import org.onap.cps.utils.YangParserHelper
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.data.api.schema.ForeignDataNode
import spock.lang.Specification

class DataNodeBuilderSpec extends Specification {

    def objectUnderTest = new DataNodeBuilder()
    def yangParserHelper = new YangParserHelper()
    def validateAndParse = false

    def expectedLeavesByXpathMap = [
            '/test-tree'                                            : [],
            '/test-tree/branch[@name=\'Left\']'                     : [name: 'Left'],
            '/test-tree/branch[@name=\'Left\']/nest'                : [name: 'Small', birds: ['Sparrow', 'Robin', 'Finch']],
            '/test-tree/branch[@name=\'Right\']'                    : [name: 'Right'],
            '/test-tree/branch[@name=\'Right\']/nest'               : [name: 'Big', birds: ['Owl', 'Raven', 'Crow']],
            '/test-tree/fruit[@color=\'Green\' and @name=\'Apple\']': [color: 'Green', name: 'Apple']
    ]

    String[] networkTopologyModelRfc8345 = [
            'ietf/ietf-yang-types@2013-07-15.yang',
            'ietf/ietf-network-topology-state@2018-02-26.yang',
            'ietf/ietf-network-topology@2018-02-26.yang',
            'ietf/ietf-network-state@2018-02-26.yang',
            'ietf/ietf-network@2018-02-26.yang',
            'ietf/ietf-inet-types@2013-07-15.yang'
    ]

    def 'Converting ContainerNode (tree) to a DataNode (tree).'() {
        given: 'the schema context for expected model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'the json data parsed into container node object'
            def jsonData = TestUtils.getResourceFileContent('test-tree.json')
            def containerNode = yangParserHelper.parseData(ContentType.JSON, jsonData, schemaContext, '', validateAndParse)
        when: 'the container node is converted to a data node'
            def result = objectUnderTest.withContainerNode(containerNode).build()
            def mappedResult = TestUtils.getFlattenMapByXpath(result)
        then: '6 DataNode objects with unique xpath were created in total'
            mappedResult.size() == 6
        and: 'all expected xpaths were built'
            mappedResult.keySet().containsAll(expectedLeavesByXpathMap.keySet())
        and: 'each data node contains the expected attributes'
            mappedResult.each {
                xpath, dataNode -> assertLeavesMaps(dataNode.getLeaves(), expectedLeavesByXpathMap[xpath])
            }
    }

    def 'Converting ContainerNode (tree) to a DataNode (tree) for known parent node.'() {
        given: 'a schema context for expected model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'the json data parsed into container node object'
            def jsonData = '{ "branch": [{ "name": "Branch", "nest": { "name": "Nest", "birds": ["bird"] } }] }'
            def containerNode = yangParserHelper.parseData(ContentType.JSON, jsonData, schemaContext, '/test-tree', validateAndParse)
        when: 'the container node is converted to a data node with parent node xpath defined'
            def result = objectUnderTest.withContainerNode(containerNode).withParentNodeXpath('/test-tree').build()
            def mappedResult = TestUtils.getFlattenMapByXpath(result)
        then: '2 DataNode objects with unique xpath were created in total'
            mappedResult.size() == 2
        and: 'all expected xpaths were built'
            mappedResult.keySet().containsAll(['/test-tree/branch[@name=\'Branch\']', '/test-tree/branch[@name=\'Branch\']/nest'])
    }

    def 'Converting ContainerNode (tree) to a DataNode (tree) -- augmentation case.'() {
        given: 'a schema context for expected model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(networkTopologyModelRfc8345)
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'the json data parsed into container node object'
            def jsonData = TestUtils.getResourceFileContent('ietf/data/ietf-network-topology-sample-rfc8345.json')
            def containerNode = yangParserHelper.parseData(ContentType.JSON, jsonData, schemaContext, '', validateAndParse)
        when: 'the container node is converted to a data node '
            def result = objectUnderTest.withContainerNode(containerNode).build()
            def mappedResult = TestUtils.getFlattenMapByXpath(result)
        then: 'all expected data nodes are populated'
            mappedResult.size() == 32
        and: 'xpaths for augmentation nodes (link and termination-point nodes) were built correctly'
            mappedResult.keySet().containsAll([
                    "/networks/network[@network-id='otn-hc']/link[@link-id='D1,1-2-1,D2,2-1-1']",
                    "/networks/network[@network-id='otn-hc']/link[@link-id='D1,1-3-1,D3,3-1-1']",
                    "/networks/network[@network-id='otn-hc']/link[@link-id='D2,2-1-1,D1,1-2-1']",
                    "/networks/network[@network-id='otn-hc']/link[@link-id='D2,2-3-1,D3,3-2-1']",
                    "/networks/network[@network-id='otn-hc']/link[@link-id='D3,3-1-1,D1,1-3-1']",
                    "/networks/network[@network-id='otn-hc']/link[@link-id='D3,3-2-1,D2,2-3-1']",
                    "/networks/network[@network-id='otn-hc']/node[@node-id='D1']/termination-point[@tp-id='1-0-1']",
                    "/networks/network[@network-id='otn-hc']/node[@node-id='D1']/termination-point[@tp-id='1-2-1']",
                    "/networks/network[@network-id='otn-hc']/node[@node-id='D1']/termination-point[@tp-id='1-3-1']",
                    "/networks/network[@network-id='otn-hc']/node[@node-id='D2']/termination-point[@tp-id='2-0-1']",
                    "/networks/network[@network-id='otn-hc']/node[@node-id='D2']/termination-point[@tp-id='2-1-1']",
                    "/networks/network[@network-id='otn-hc']/node[@node-id='D2']/termination-point[@tp-id='2-3-1']",
                    "/networks/network[@network-id='otn-hc']/node[@node-id='D3']/termination-point[@tp-id='3-1-1']",
                    "/networks/network[@network-id='otn-hc']/node[@node-id='D3']/termination-point[@tp-id='3-2-1']"
            ])
    }

    def 'Converting ContainerNode (tree) to a DataNode (tree) for known parent node -- augmentation case.'() {
        given: 'a schema context for expected model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(networkTopologyModelRfc8345)
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'parent node xpath referencing augmentation node within a model'
            def parentNodeXpath = "/networks/network[@network-id='otn-hc']/link[@link-id='D1,1-2-1,D2,2-1-1']"
        and: 'the json data fragment parsed into container node object for given parent node xpath'
            def jsonData = '{"source": {"source-node": "D1", "source-tp": "1-2-1"}}'
            def containerNode = yangParserHelper.parseData(ContentType.JSON, jsonData, schemaContext,parentNodeXpath, validateAndParse)
        when: 'the container node is converted to a data node with given parent node xpath'
            def result = objectUnderTest.withContainerNode(containerNode).withParentNodeXpath(parentNodeXpath).build()
        then: 'the resulting data node represents a child of augmentation node'
            assert result.xpath == "/networks/network[@network-id='otn-hc']/link[@link-id='D1,1-2-1,D2,2-1-1']/source"
            assert result.leaves['source-node'] == 'D1'
            assert result.leaves['source-tp'] == '1-2-1'
    }

    def 'Converting ContainerNode (tree) to a DataNode (tree) -- with ChoiceNode.'() {
        given: 'a schema context for expected model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('yang-with-choice-node.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'the json data fragment parsed into container node object'
            def jsonData = TestUtils.getResourceFileContent('data-with-choice-node.json')
            def containerNode = yangParserHelper.parseData(ContentType.JSON, jsonData, schemaContext, '', validateAndParse)
        when: 'the container node is converted to a data node'
            def result = objectUnderTest.withContainerNode(containerNode).build()
            def mappedResult = TestUtils.getFlattenMapByXpath(result)
        then: 'the resulting data node contains only one xpath with 3 leaves'
            mappedResult.keySet().containsAll([ '/container-with-choice-leaves' ])
            assert result.leaves['leaf-1'] == 'test'
            assert result.leaves['choice-case1-leaf-a'] == 'test'
            assert result.leaves['choice-case1-leaf-b'] == 'test'
    }

    def 'Converting ContainerNode into DataNode collection: #scenario.'() {
        given: 'a schema context for expected model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'parent node xpath referencing parent of list element'
            def parentNodeXpath = '/test-tree'
        and: 'the json data fragment (list element) parsed into container node object'
            def containerNode = yangParserHelper.parseData(ContentType.JSON, jsonData, schemaContext, parentNodeXpath, validateAndParse)
        when: 'the container node is converted to a data node collection'
            def result = objectUnderTest.withContainerNode(containerNode).withParentNodeXpath(parentNodeXpath).buildCollection()
            def resultXpaths = result.collect { it.getXpath() }
        then: 'the resulting collection contains data nodes for expected list elements'
            assert resultXpaths.size() == expectedSize
            assert resultXpaths.containsAll(expectedXpaths)
        where: 'following parameters are used'
            scenario           | jsonData                                         | expectedSize | expectedXpaths
            'single entry'     | '{"branch": [{"name": "One"}]}'                  | 1            | ['/test-tree/branch[@name=\'One\']']
            'multiple entries' | '{"branch": [{"name": "One"}, {"name": "Two"}]}' | 2            | ['/test-tree/branch[@name=\'One\']', '/test-tree/branch[@name=\'Two\']']
    }

    def 'Converting ContainerNode to a Collection with #scenario.'() {
        expect: 'converting null to a collection returns an empty collection'
            assert objectUnderTest.withContainerNode(containerNode).buildCollection().isEmpty()
        where: 'the following container node is used'
            scenario              | containerNode
            'null object'         | null
            'object without body' | Mock(ContainerNode)
    }

    def 'Converting ContainerNode to a DataNode with unsupported Normalized Node.'() {
        given: 'a container node of an unsupported type'
            def mockContainerNode = Mock(ContainerNode)
            mockContainerNode.body() >> [ Mock(ForeignDataNode) ]
        when: 'attempt to convert it'
            objectUnderTest.withContainerNode(mockContainerNode).build()
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'Build datanode from attributes.'() {
        when: 'data node is built'
            def result = new DataNodeBuilder()
                .withDataspace('my dataspace')
                .withAnchor('my anchor')
                .withModuleNamePrefix('my prefix')
                .withXpath('some xpath')
                .withLeaves([leaf1: 'value1'])
                .withChildDataNodes([Mock(DataNode)])
                .build()
        then: 'the datanode has all the defined attributes'
            assert result.dataspace == 'my dataspace'
            assert result.anchorName == 'my anchor'
            assert result.moduleNamePrefix == 'my prefix'
            assert result.moduleNamePrefix == 'my prefix'
            assert result.xpath == 'some xpath'
            assert result.leaves == [leaf1: 'value1']
            assert result.childDataNodes.size() == 1
    }

    def 'Use of adding the module name prefix attribute of data node.'() {
        when: 'data node is built with a prefix'
            def testDataNode = new DataNodeBuilder()
                    .withXpath(xPath)
                    .withLeaves(sampleLeaves)
                    .build()
        then: 'the result when node request is a #scenario includes the correct prefix'
            def result = new DataMapUtils().toDataMapWithIdentifier(testDataNode, 'sampleModuleNamePrefix')
            result.toString() == expectedResult
        where: 'the following parameters are used'
            scenario          | xPath                                       | sampleLeaves                   | expectedResult
            'list attribute'  | '/test-tree/branch[@name=\'Right\']/nest'   | [name: 'Big', birds: ['Owl']]  | '{sampleModuleNamePrefix:nest={name=Big, birds=[Owl]}}'
            'container xpath' | '/test-tree/branch[@name=\'Left\']'         | [name: 'Left']                 | '{sampleModuleNamePrefix:branch={name=Left}}'
    }

    def static assertLeavesMaps(actualLeavesMap, expectedLeavesMap) {
        expectedLeavesMap.each { key, value ->
            {
                def actualValue = actualLeavesMap[key]
                if (value instanceof Collection<?> && actualValue instanceof Collection<?>) {
                    assert value.size() == actualValue.size()
                    assert value.containsAll(actualValue)
                } else {
                    assert value == actualValue
                }
            }
        }
    }
}
