/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2023 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.utils

import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME

class DataNodeBaseSpec extends Specification {

    def leaves1 = [status:'PENDING', cmHandleId:'CMHandle3', details:'Subscription forwarded to dmi plugin'] as Map
    def dataNode1 = createDataNodeWithLeaves(leaves1)

    def leaves2 = [status:'ACCEPTED', cmHandleId:'CMHandle2', details:''] as Map
    def dataNode2 = createDataNodeWithLeaves(leaves2)

    def leaves3 = [status:'REJECTED', cmHandleId:'CMHandle1', details:'Cm handle does not exist'] as Map
    def dataNode3 = createDataNodeWithLeaves(leaves3)

    def leaves4 = [datastore:'passthrough-running'] as Map
    def dataNode4 = createDataNodeWithLeavesAndChildDataNodes(leaves4, [dataNode1, dataNode2, dataNode3])

    static def createDataNodeWithLeaves(leaves) {
        return new DataNodeBuilder().withDataspace(NCMP_DATASPACE_NAME)
            .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry/subscription')
            .withLeaves(leaves).build()
    }

    static def createDataNodeWithLeavesAndChildDataNodes(leaves, dataNodes) {
        return new DataNodeBuilder().withDataspace(NCMP_DATASPACE_NAME)
            .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry/subscription')
            .withLeaves(leaves).withChildDataNodes(dataNodes)
            .build()
    }
}
