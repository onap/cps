/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
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

package org.onap.cps.integration

import java.util.concurrent.TimeUnit
import spock.lang.Specification

class ResourceMeterPerfTest extends Specification {

    final int MEGABYTE = 1_000_000

    def resourceMeter = new ResourceMeter()

    def 'ResourceMeter accurately measures duration'() {
        when: 'we measure how long a known operation takes'
            resourceMeter.start()
            TimeUnit.SECONDS.sleep(2)
            resourceMeter.stop()
        then: 'ResourceMeter reports a duration within 10ms of the expected duration'
            assert resourceMeter.getTotalTimeInSeconds() >= 2
            assert resourceMeter.getTotalTimeInSeconds() <= 2.01
    }

    def 'ResourceMeter reports memory usage when allocating a large byte array'() {
        when: 'the resource meter is started'
            resourceMeter.start()
        and: 'some memory is allocated'
            byte[] array = new byte[50 * MEGABYTE]
        and: 'the resource meter is stopped'
            resourceMeter.stop()
        then: 'the reported memory usage is close to the amount of memory allocated'
            assert resourceMeter.getTotalMemoryUsageInMB() >= 50
            assert resourceMeter.getTotalMemoryUsageInMB() <= 55
    }

    def 'ResourceMeter measures PEAK memory usage when garbage collector runs'() {
        when: 'the resource meter is started'
            resourceMeter.start()
        and: 'some memory is allocated'
            byte[] array = new byte[50 * MEGABYTE]
        and: 'the memory is garbage collected'
            array = null
            ResourceMeter.performGcAndWait()
        and: 'the resource meter is stopped'
            resourceMeter.stop()
        then: 'the reported memory usage is close to the peak amount of memory allocated'
            assert resourceMeter.getTotalMemoryUsageInMB() >= 50
            assert resourceMeter.getTotalMemoryUsageInMB() <= 55
    }

    def 'ResourceMeter measures memory increase only during measurement'() {
        given: '50 megabytes is allocated before measurement'
            byte[] arrayBefore = new byte[50 * MEGABYTE]
        when: 'memory is allocated during measurement'
            resourceMeter.start()
            byte[] arrayDuring = new byte[40 * MEGABYTE]
            resourceMeter.stop()
        and: '50 megabytes is allocated after measurement'
            byte[] arrayAfter = new byte[50 * MEGABYTE]
        then: 'the reported memory usage is close to the amount allocated DURING measurement'
            assert resourceMeter.getTotalMemoryUsageInMB() >= 40
            assert resourceMeter.getTotalMemoryUsageInMB() <= 45
    }

}
