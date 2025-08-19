package org.onap.cps.integration.performance.cps

import org.onap.cps.api.CpsDeltaService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDeltaServicePerfTest extends CpsPerfTestBase{
    CpsDeltaService objectUnderTest
    def setup() {
        objectUnderTest = cpsDeltaService
    }

    def 'Get delta between 2 anchors'() {
        when: 'attempt to get delta between two 2 anchors'
            resourceMeter.start()
            def delta = objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', 'openroadm2', '/openroadm-devices', fetchDescendantsOption, true)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between 2 anchors', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 30.0
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.125
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 1.5
    }

    def 'Get delta between 2 anchors with grouping disabled'() {
        when: 'attempt to get delta between two 2 anchors with grouping disabled'
            resourceMeter.start()
            def delta = objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', 'openroadm2', '/openroadm-devices', fetchDescendantsOption, false)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between 2 anchors with grouping disabled', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 30.0
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.125
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 1.6
    }

    def 'Get delta between an anchor and JSON payload with grouping enabled'() {
        when: 'attempt to get delta between an anchor and JSON payload with grouping enabled'
            resourceMeter.start()
            def jsonPayload = generateOpenRoadData(200)
            def delta = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', '/openroadm-devices', Collections.emptyMap(), jsonPayload, fetchDescendantsOption, true)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between an anchor and JSON payload with grouping enabled', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 30.0
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.8
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 3.5
    }

    def 'Get delta between an anchor and JSON payload with grouping disabled'() {
        when: 'attempt to get delta between an anchor and JSON payload with grouping disabled'
            resourceMeter.start()
            def jsonPayload = generateOpenRoadData(200)
            def delta = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', '/openroadm-devices', Collections.emptyMap(), jsonPayload, fetchDescendantsOption, false)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between an anchor and JSON payload with grouping disabled', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 35.0
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.8
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 3.5
    }
}
