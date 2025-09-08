/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.datajobs.subscription.dmi

import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.JexParser
import spock.lang.Specification

class DmiInEventMapperSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def objectUnderTest = new DmiInEventMapper(mockInventoryPersistence)

    def setup() {
        def yangModelCmHandles = [new YangModelCmHandle(id: 'ch-1', additionalProperties: [new YangModelCmHandle.Property('k1', 'v1')], publicProperties: []),
                                  new YangModelCmHandle(id: 'ch-2', additionalProperties: [new YangModelCmHandle.Property('k2', 'v2')], publicProperties: [])]
        mockInventoryPersistence.getYangModelCmHandles(['ch-1', 'ch-2'] as Set) >> yangModelCmHandles
    }

    def 'Check for Cm Notification Subscription DMI In Event mapping'() {
        given: 'data job subscription details'
            def cmHandleIds = ['ch-1', 'ch-2'].asList()
            def dataNodeSelectors = ['/dataNodeSelector1'].asList()
            def notificationTypes = []
            def notificationFilter = ''
            def dataNodeSelectorAsJsonExpression = JexParser.toJsonExpressionsAsString(dataNodeSelectors)
        when: 'we try to map the values'
            def result = objectUnderTest.toDmiInEvent(cmHandleIds, dataNodeSelectors, notificationTypes, notificationFilter)
        then: 'it contains correct cm handles'
            assert result.data.cmHandles.cmhandleId.containsAll(cmHandleIds)
        and: 'correct data node selector'
            assert result.data.productionJobDefinition.targetSelector.dataNodeSelector == dataNodeSelectorAsJsonExpression

    }
}
