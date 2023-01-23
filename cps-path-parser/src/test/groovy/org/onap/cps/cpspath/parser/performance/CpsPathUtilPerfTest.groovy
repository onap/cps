/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.cpspath.parser.performance

import org.onap.cps.cpspath.parser.CpsPathUtil
import org.springframework.util.StopWatch
import spock.lang.Specification

class CpsPathUtilPerfTest extends Specification {

    def 'CPS Path Processing Performance Test.'() {
        when: '20,000 paths are processed'
            def stopWatch = new StopWatch()
            stopWatch.start()
            (1..10000).each {
                CpsPathUtil.getNormalizedXpath('/long/path/to/see/if/it/adds/paring/time/significantly/parent/child[@common-leaf-name="123"]')
                CpsPathUtil.getNormalizedXpath('//child[@other-leaf=1]/leaf-name[text()="search"]/ancestor::parent')
            }
            stopWatch.stop()
        then: 'it takes less then 2100 milliseconds'
            assert stopWatch.getTotalTimeMillis() < 2100
    }

}
