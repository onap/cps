/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.api.parameters


import org.onap.cps.api.exceptions.DataValidationException
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

    def 'Create fetch descendant option from string scenario: #scenario.'() {
        when: 'create fetch descendant option from string'
           def fetchDescendantsOption = FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString)
        then: 'fetch descendant object created with correct depth'
            assert fetchDescendantsOption.depth == expectedDepth
        where: 'following parameters are used'
            scenario                            | fetchDescendantsOptionAsString || expectedDepth
            'all descendants using number'      | '-1'                           || -1
            'all descendants using all'         | 'all'                          || -1
            'No descendants by default'         | ''                             || 0
            'No descendants using none'         | 'none'                         || 0
            'No descendants using number'       | '0'                            || 0
            'direct child using number'         | '1'                            || 1
            'direct child using direct'         | 'direct'                       || 1
            'til 10th descendants using number' | '10'                           || 10
    }

    def 'Create fetch descendant option from string with invalid string.'() {
        when: 'attempt to create fetch descendant option from invalid string'
            FetchDescendantsOption.getFetchDescendantsOption('invalid-string')
        then: 'a validation exception is thrown with the invalid string in the details'
            def thrown = thrown(DataValidationException)
            thrown.details.contains('invalid-string')
    }

    def 'Convert to string.'() {
        expect: 'each fetch descendant option has the correct String value'
            assert fetchDescendantsOption.toString() == expectedStringValue
        where: 'the following option is used'
            fetchDescendantsOption                         || expectedStringValue
            FetchDescendantsOption.OMIT_DESCENDANTS        || 'OmitDescendants'
            FetchDescendantsOption.DIRECT_CHILDREN_ONLY    || 'DirectChildrenOnly'
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS || 'IncludeAllDescendants'
            new FetchDescendantsOption(2)                  || 'Depth=2'
    }

}
