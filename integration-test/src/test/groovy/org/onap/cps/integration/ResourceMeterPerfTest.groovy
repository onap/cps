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

class ResourceMeterPerfTest extends Specification {

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

}
