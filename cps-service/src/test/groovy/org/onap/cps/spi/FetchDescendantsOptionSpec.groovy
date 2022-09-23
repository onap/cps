/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.spi


import spock.lang.Specification

class FetchDescendantsOptionSpec extends Specification {
    def 'Check has next descendant for fetch descendant option: #scenario'() {
        when: 'a fetch descendant option'
            def descendantOption = new FetchDescendantsOption(depth)
        then: 'fetch descendant option has next method send the correct response'
            descendantOption.hasNext() == hasNext
        where: 'following parameters are used'
            scenario                  | depth || hasNext
            'omit descendants'        | 0     || false
            'include all descendants' | -1    || true
            'too low depth'           | -2    || false
            'first child'             | 1     || true
            'second child'            | 2     || true
    }

    def 'Get next descendant for fetch descendant option: #scenario'() {
        when: 'a fetch descendant option'
            def descendantOption = new FetchDescendantsOption(depth)
        then: 'fetch descendant option next method send the correct response'
            descendantOption.next().depth == next
        where: 'following parameters are used'
            scenario                  | depth || next
            'omit descendants'        | 0     || -1
            'include all descendants' | -1    || -1
            'too low depth'           | -2    || -3
            'first child'             | 1     || 0
            'second child'            | 2     || 1
    }
}
