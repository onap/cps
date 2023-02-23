/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

    def 'Has next descendant for fetch descendant option: #scenario'() {
        when: 'fetch descendant option with #depth depth'
            def fetchDescendantsOption = new FetchDescendantsOption(depth)
        then: 'next level descendants available: #expectedHasNext'
            assert fetchDescendantsOption.hasNext() == expectedHasNext
        where: 'following parameters are used'
            scenario                  | depth || expectedHasNext
            'omit descendants'        | 0     || false
            'first child'             | 1     || true
            'second child'            | 2     || true
            'include all descendants' | -1    || true
    }

    def 'Has next descendant for fetch descendant option: invalid depth'() {
        given: 'fetch descendant option with -2 depth'
            def fetchDescendantsOption = new FetchDescendantsOption(-2)
        when: 'next level descendants not available'
            fetchDescendantsOption.hasNext()
        then: 'exception thrown'
            thrown IllegalArgumentException
    }

    def 'Next descendant for fetch descendant option: #scenario.'() {
        when: 'fetch descendant option with #depth depth'
            def fetchDescendantsOption = new FetchDescendantsOption(depth)
        then: 'the next level of depth is as expected'
            fetchDescendantsOption.next().depth == depth - 1
        where: 'following parameters are used'
            scenario                  | depth
            'first child'             | 1
            'second child'            | 2
    }

    def 'Next descendant for fetch descendant option: include all descendants.'() {
        when: 'fetch descendant option with -1 depth'
            def fetchDescendantsOption = new FetchDescendantsOption(-1)
        then: 'the next level of depth is as expected'
            fetchDescendantsOption.next().depth == -1
    }

    def 'Next descendant for fetch descendant option: omit descendants.'() {
        given: 'fetch descendant option with 0 depth'
            def fetchDescendantsOption = new FetchDescendantsOption(0)
        when: 'the next level of depth is not allowed'
            fetchDescendantsOption.next()
        then: 'exception thrown'
            thrown IllegalArgumentException
    }

    def 'Create fetch descendant option with  descendant using #scenario.'() {
        when: 'the next level of depth is not allowed'
           def FetchDescendantsOption fetchDescendantsOption = FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString)
        then: 'fetch descendant object created'
            assert fetchDescendantsOption.depth == expectedDepth
        where: 'following parameters are used'
            scenario                            | fetchDescendantsOptionAsString || expectedDepth
            'all descendants using number'      | '-1'                           || -1
            'all descendants using all'         | 'all'                          || -1
            'No descendants by default'         | ''                             || 0
            'No descendants using none'         | 'none'                         || 0
            'til 10th descendants using number' | '10'                           || 10
    }

    def 'String values.'() {
        expect: 'fetch descendant option with #depth depth'
            assert fetchDescendantsOption.toString() == expectedStringValue
        where: 'the following option is used'
            fetchDescendantsOption                            || expectedStringValue
            FetchDescendantsOption.OMIT_DESCENDANTS           || 'OmitDescendants'
            FetchDescendantsOption.FETCH_DIRECT_CHILDREN_ONLY || 'FetchDirectChildrenOnly'
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS    || 'IncludeAllDescendants'
            new FetchDescendantsOption(2)                     || 'Depth=2'
    }

}
