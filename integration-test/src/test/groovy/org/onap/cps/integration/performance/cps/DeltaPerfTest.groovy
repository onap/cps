/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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

package org.onap.cps.integration.performance.cps


import org.onap.cps.api.CpsDeltaService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class DeltaPerfTest extends CpsPerfTestBase{
    CpsDeltaService objectUnderTest
    def setup() {
        objectUnderTest = cpsDeltaService
    }

    def jsonPayload = generateModifiedOpenRoadData(200, 100, 100, 50)

    def 'Get delta between 2 anchors, #scenario'() {
        when: 'attempt to get delta between two 2 anchors'
            resourceMeter.start()
            10.times {
                objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', 'openroadm-modified1', xpath, fetchDescendantsOption, true)
            }
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between 2 anchors', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | xpath                                                             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | '/openroadm-devices/openroadm-device[@device-id=\'C201-7-1A-1\']' | OMIT_DESCENDANTS        || 0.5
            'direct descendants' | '/'                                                               | DIRECT_CHILDREN_ONLY    || 0.9
            'all descendants'    | '/'                                                               | INCLUDE_ALL_DESCENDANTS || 14.0
    }

    def 'Get delta between 2 anchors with grouping disabled'() {
        when: 'attempt to get delta between two 2 anchors with grouping disabled'
            resourceMeter.start()
            10.times {
                objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', 'openroadm-modified1', xpath, fetchDescendantsOption, false)
            }
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between 2 anchors with grouping disabled', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | xpath                                                             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | '/openroadm-devices/openroadm-device[@device-id=\'C201-7-1A-1\']' | OMIT_DESCENDANTS        || 0.3
            'direct descendants' | '/openroadm-devices'                                              | DIRECT_CHILDREN_ONLY    || 0.5
            'all descendants'    | '/openroadm-devices'                                              | INCLUDE_ALL_DESCENDANTS || 10.0
    }

    def 'Get delta between an anchor and JSON payload with grouping enabled'() {
        when: 'attempt to get delta between an anchor and JSON payload with grouping enabled'
            resourceMeter.start()
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', '/openroadm-devices', Collections.emptyMap(), jsonPayload, fetchDescendantsOption, true)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between an anchor and JSON payload with grouping enabled', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 0.7
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.8
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 3.5
    }

    def 'Get delta between an anchor and JSON payload with grouping disabled'() {
        when: 'attempt to get delta between an anchor and JSON payload with grouping disabled'
            resourceMeter.start()
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', '/openroadm-devices', Collections.emptyMap(), jsonPayload, INCLUDE_ALL_DESCENDANTS, false)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between an anchor and JSON payload with grouping disabled', 3.0, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Apply delta report to an anchor'() {
        given: 'a delta report between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', 'openroadm-modified1', '/openroadm-devices', INCLUDE_ALL_DESCENDANTS, true)
            def deltaReportAsJson = jsonObjectMapper.asJsonString(deltaReport)
        when: 'attempt to apply the delta report to an anchor'
            resourceMeter.start()
            objectUnderTest.applyChangesInDeltaReport(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', deltaReportAsJson)
            resourceMeter.stop()
            resetTestAnchorData()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is applied and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Apply delta report to an anchor', 2.5, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
    }
}
