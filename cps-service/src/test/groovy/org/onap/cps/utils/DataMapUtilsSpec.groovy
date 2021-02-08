/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2020 Nordix Foundation
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

import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification

class DataMapUtilsSpec extends Specification {

    def noChildren = []

    def dataNode = buildDataNode(
        "/parent",[parentLeaf:'parentLeafValue', parentLeafList:['parentLeafListEntry1','parentLeafListEntry2']],[
                buildDataNode('/parent/child-list[@id=1]',[listElementLeaf:'listElement1leafValue'],noChildren),
                buildDataNode('/parent/child-list[@id=2]',[listElementLeaf:'listElement2leafValue'],noChildren),
                buildDataNode('/parent/child-object',[childLeaf:'childLeafValue'],
                        [buildDataNode('/parent/child-object/grand-child-object',[grandChildLeaf:'grandChildLeafValue'],noChildren)]
                ),
            ])

    static def buildDataNode(xpath,  leaves,  children) {
        return new DataNodeBuilder().withXpath(xpath).withLeaves(leaves).withChildDataNodes(children).build()
    }

    def 'Data node structure conversion to map.'() {
        when: 'data node structure is converted to a map'
            Map result = DataMapUtils.toDataMap(dataNode)

        then: 'root node leaves are top level elements'
            result.parentLeaf == 'parentLeafValue'
            result.parentLeafList == ['parentLeafListEntry1','parentLeafListEntry2']

        and: 'leaves of child list element are listed as structures under common identifier'
            result.'child-list'.collect().containsAll(['listElementLeaf': 'listElement1leafValue'],
                                                      ['listElementLeaf': 'listElement2leafValue'])

        and: 'leaves for child element is populated under its node identifier'
            Map childObjectData = result.'child-object'
            childObjectData.childLeaf == 'childLeafValue'

        and: 'leaves for grandchild element is populated under its node identifier'
            Map grandChildObjectData = childObjectData.'grand-child-object'
            grandChildObjectData.grandChildLeaf == 'grandChildLeafValue'
    }

}
