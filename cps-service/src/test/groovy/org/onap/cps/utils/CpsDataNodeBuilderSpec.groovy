/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
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

import spock.lang.Specification

class CpsDataNodeBuilderSpec extends Specification {
    def parentDataNode
    def parentXpath = '/bookstore'

    def 'Create DataNode.'() {
        when: 'a data node is created for a given xPath'
            parentDataNode = CpsDataNodeBuilder.createDataNode(parentXpath)
        then: 'the created data node contains xPath as set above'
            parentDataNode.xpath == parentXpath
        and: 'no child nodes are mapped to the parent'
            parentDataNode.childDataNodes == null
    }

    def 'Create child data node.'() {
        given: 'a xpathId for the parent Node'
            def childXpath = '/categories[@code=\'02\']'
        when: 'a child node is created for a given xPath'
            parentDataNode = CpsDataNodeBuilder.createDataNode(parentXpath)
            def childDataNode = CpsDataNodeBuilder.createChildNode(parentDataNode, childXpath)
        then: 'the xpathId of child includes the parent xpath as well'
            childDataNode.xpath == parentXpath + childXpath
        and: 'child nodes are now mapped to the parent'
            parentDataNode.childDataNodes != null
            parentDataNode.getChildDataNodes()[0] == childDataNode
    }
}
