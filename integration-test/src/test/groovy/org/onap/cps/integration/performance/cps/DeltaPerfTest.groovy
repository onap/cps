package org.onap.cps.integration.performance.cps

import groovy.util.logging.Slf4j
import org.onap.cps.api.CpsDeltaService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

@Slf4j
class DeltaPerfTest extends CpsPerfTestBase{
    CpsDeltaService objectUnderTest
    def setup() {
        objectUnderTest = cpsDeltaService
    }

    def 'Get delta between 2 anchors, #scenario'() {
        when: 'attempt to get delta between two 2 anchors'
            resourceMeter.start()
            objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', 'openroadm2', '/', fetchDescendantsOption, true)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
            log.info("Duration in seconds "+ durationInSeconds as String)
            println("limit * expected" + DEFAULT_TIME_LIMIT_FACTOR * expectedDuration)
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between 2 anchors', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 0.04
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.1
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 1.25
    }

    def 'Get delta between 2 anchors with grouping disabled'() {
        when: 'attempt to get delta between two 2 anchors with grouping disabled'
            resourceMeter.start()
            objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', 'openroadm2', '/openroadm-devices', fetchDescendantsOption, false)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
            log.info("Duration in seconds "+ durationInSeconds as String)
            println("limit * expected" + DEFAULT_TIME_LIMIT_FACTOR * expectedDuration)
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between 2 anchors with grouping disabled', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 0.05
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.07
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 1.6
    }

    def 'Get delta between an anchor and JSON payload with grouping enabled'() {
        when: 'attempt to get delta between an anchor and JSON payload with grouping enabled'
            resourceMeter.start()
            def jsonPayload = generateOpenRoadData(200)
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', '/openroadm-devices', Collections.emptyMap(), jsonPayload, fetchDescendantsOption, true)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
            log.info("Duration in seconds "+ durationInSeconds as String)
            print("limit * expected" + DEFAULT_TIME_LIMIT_FACTOR * expectedDuration)
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
            def jsonPayload = generateOpenRoadData(200)
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', '/openroadm-devices', Collections.emptyMap(), jsonPayload, INCLUDE_ALL_DESCENDANTS, false)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
            log.info("Duration in seconds "+ durationInSeconds as String)
            println("limit * expected" + DEFAULT_TIME_LIMIT_FACTOR * 3.5)
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Get delta between an anchor and JSON payload with grouping disabled', 3.1, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Apply delta report to an anchor'() {
        given: 'a delta report between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', 'openroadm2', '/openroadm-devices', INCLUDE_ALL_DESCENDANTS, true)
            def deltaReportAsJson = jsonObjectMapper.asJsonString(deltaReport)
            print("Delta: " + deltaReport.size())
        when: 'attempt to apply the delta report to a 3rd anchor'
            resourceMeter.start()
            objectUnderTest.applyChangesInDeltaReport(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm3', deltaReportAsJson)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
            log.info("Duration in seconds "+ durationInSeconds as String)
            println('limit * expected' + DEFAULT_TIME_LIMIT_FACTOR * 30.0)
        then: 'the delta is applied and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Apply delta report to an anchor', 0.007, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
    }
}
