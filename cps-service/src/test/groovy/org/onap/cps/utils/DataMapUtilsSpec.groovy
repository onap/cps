/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2023 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada.
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.utils

import org.onap.cps.spi.api.model.DataNodeBuilder
import spock.lang.Specification

class DataMapUtilsSpec extends Specification {

    def noChildren = []

    def 'Data node structure conversion to map.'() {
        when: 'data node structure is converted to a map'
            def result = DataMapUtils.toDataMap(dataNode)
        then: 'root node identifier is null'
            result.parent == null
        then: 'root node leaves are top level elements'
            result.parentLeaf == 'parentLeafValue'
            result.parentLeafList == ['parentLeafListEntry1','parentLeafListEntry2']
        and: 'leaves of child list element are listed as structures under common identifier'
            result.'child-list'.collect().containsAll(['listElementLeaf': 'listElement1leafValue'],
                                                      ['listElementLeaf': 'listElement2leafValue'])
        and: 'leaves for child element is populated under its node identifier'
            result.'child-object'.childLeaf == 'childLeafValue'
        and: 'leaves for grandchild element is populated under its node identifier'
            result.'child-object'.'grand-child-object'.grandChildLeaf == 'grandChildLeafValue'
    }

    def 'Data node structure conversion to map with root node identifier.'() {
        when: 'data node structure is converted to a map with root node identifier'
            def result = DataMapUtils.toDataMapWithIdentifier(dataNode,dataNode.moduleNamePrefix)
        then: 'root node leaves are populated under its node identifier'
            def parentNode = result.parent
            parentNode.parentLeaf == 'parentLeafValue'
            parentNode.parentLeafList == ['parentLeafListEntry1','parentLeafListEntry2']
        and: 'leaves for child element is populated under its node identifier'
            parentNode.'child-object'.childLeaf == 'childLeafValue'
        and: 'leaves for grandchild element is populated under its node identifier'
            parentNode.'child-object'.'grand-child-object'.grandChildLeaf == 'grandChildLeafValue'
    }

    def 'Adding prefix to data node identifier.'() {
        when: 'a valid xPath is passed to the addPrefixToXpath method'
            def result = new DataMapUtils().getNodeIdentifierWithPrefix(xPath,'sampleModuleName')
        then: 'the correct modified node identifier is given'
            assert result == expectedNodeIdentifier
        where: 'the following parameters are used'
            scenario                                | xPath                                     | expectedNodeIdentifier
            'container xpath'                       | '/bookstore'                              | 'sampleModuleName:bookstore'
            'xpath contains list attribute'         | '/bookstore/categories[@code=1]'          | 'sampleModuleName:categories'
            'xpath contains list attributes with /' | '/bookstore/categories[@code="1/2"]'      | 'sampleModuleName:categories'
            'xpath contains list attributes with [' | '/bookstore/categories[@code="[1]"]'      | 'sampleModuleName:categories'
    }

    def 'Data node structure with anchor name conversion to map with root node identifier.'() {
        when: 'data node structure is converted to a map with root node identifier'
            def result = DataMapUtils.toDataMapWithIdentifierAndAnchor([dataNodeWithAnchor], dataNodeWithAnchor.anchorName, dataNodeWithAnchor.moduleNamePrefix)
        then: 'root node leaves are populated under its node identifier'
            def dataNodes = result.dataNodes as List
            assert dataNodes.size() == 1
            def parentNode = dataNodes[0].parent
            assert parentNode.parentLeaf == 'parentLeafValue'
            assert parentNode.parentLeafList == ['parentLeafListEntry1','parentLeafListEntry2']
        and: 'leaves for child element is populated under its node identifier'
            assert parentNode.'child-object'.childLeaf == 'childLeafValue'
        and: 'leaves for grandchild element is populated under its node identifier'
            assert parentNode.'child-object'.'grand-child-object'.grandChildLeaf == 'grandChildLeafValue'
        and: 'data node is associated with anchor name'
            assert result.anchorName == 'anchor01'
    }

    def 'Data node without leaves and without children.'() {
        given: 'a datanode with no leaves and no children'
            def dataNodeWithoutData = new DataNodeBuilder().withXpath('some xpath').build()
        when: 'it is converted to a map'
            def result = DataMapUtils.toDataMap(dataNodeWithoutData)
        then: 'an empty object map is returned'
            result.isEmpty()
    }

    def dataNode = buildDataNode(
        "/parent",[parentLeaf:'parentLeafValue', parentLeafList:['parentLeafListEntry1','parentLeafListEntry2']],[
        buildDataNode('/parent/child-list[@id="1/2"]',[listElementLeaf:'listElement1leafValue'],noChildren),
        buildDataNode('/parent/child-list[@id=2]',[listElementLeaf:'listElement2leafValue'],noChildren),
        buildDataNode('/parent/child-object',[childLeaf:'childLeafValue'],
            [buildDataNode('/parent/child-object/grand-child-object',[grandChildLeaf:'grandChildLeafValue'],noChildren)]
        ),
    ])

    def dataNodeWithAnchor = buildDataNodeWithAnchor(
        "/parent", 'anchor01',[parentLeaf:'parentLeafValue', parentLeafList:['parentLeafListEntry1','parentLeafListEntry2']],[
        buildDataNode('/parent/child-list[@id="1/2"]',[listElementLeaf:'listElement1leafValue'],noChildren),
        buildDataNode('/parent/child-list[@id=2]',[listElementLeaf:'listElement2leafValue'],noChildren),
        buildDataNode('/parent/child-object',[childLeaf:'childLeafValue'],
            [buildDataNode('/parent/child-object/grand-child-object',[grandChildLeaf:'grandChildLeafValue'],noChildren)]
        ),
    ])

    def buildDataNode(xpath,  leaves,  children) {
        return new DataNodeBuilder().withXpath(xpath).withLeaves(leaves).withChildDataNodes(children).build()
    }

    def buildDataNodeWithAnchor(xpath, anchorName, leaves,  children) {
        return new DataNodeBuilder().withXpath(xpath).withAnchor(anchorName).withLeaves(leaves).withChildDataNodes(children).build()
    }

}

