/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.cps.api;

import lombok.Getter;
import org.onap.cps.spi.model.DataNode;

public class CpsDataNodeBuilder {

    private CpsDataNodeBuilder() {
        throw new IllegalStateException("Builder class");
    }

    /**
     * Create a parent Data Node.
     *
     * @param xpath  the xpath of root fragment
     */
    public static DataNode createParentDataNode(final String xpath) {
        return new DataNode(xpath);
    }

    /**
     * Create a Child dataNode where the parentDataNode is the parent.
     *
     * @param parentDataNode the parent data Node
     * @param childXPath The child xpath (relative to the parrent)
     * @return the child data node
     */
    public static DataNode createChildNode(final DataNode parentDataNode, final String childXPath) {
        final DataNode childDataNode = new DataNode(parentDataNode.getXpath() + childXPath);
        parentDataNode.addChildDataNode(childDataNode);
        return childDataNode;
    }


}
