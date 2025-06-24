/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import spock.lang.Specification

import java.util.concurrent.TimeUnit

class ResourceMeterAccuracyTest extends Specification {

    def objectUnderTest = new ResourceMeter()

    def 'Resource meter duration measurement.'() {
        when: 'we measure how long a known operation takes'
            objectUnderTest.start()
            TimeUnit.SECONDS.sleep(2)
            objectUnderTest.stop()
        then: 'the resource meter reports a duration within 40ms of the expected duration'
            assert objectUnderTest.getTotalTimeInSeconds() >= 1.96
            assert objectUnderTest.getTotalTimeInSeconds() <= 2.04
    }

}
