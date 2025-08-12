/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.utils

import spock.lang.Specification

class JexResolverSpec extends Specification {

    def 'should resolve expected alternate IDs from complex jex basic expression with many paths input'() {
        given: 'a complex jex basic expression with many paths string with multiple absolute paths, attributes, and filters'
        String sampleBasicJexWithManyPaths = '''
            /SubNetwork[id="SN1"]/ManagedElement
            /SubNetwork[id="SN1"]/ManagedElement/attributes
            /SubNetwork[id="SN1"]/ManagedElement/attributes/vendorName
            /SubNetwork[id="SN1"]/ManagedElement[attributes/vendorName="Company XY"]
            /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]
            /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]/attributes
            /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]/attributes/opState
            /SubNetwork[id="SN1"]/ManagedElement[attributes/vendorName="Company XY"]
            /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]/attributes or
            /SubNetwork[id="SN2"]/ManagedElement/attributes/vendorName
            /SubNetwork[id="SN2"]/(ManagedElement|ThresholdMonitor)/attributes
        '''

        when: 'the resolver extracts alternate IDs'
        def result = JexResolver.resolveAlternateIds(sampleBasicJexWithManyPaths)

        then: 'only the expected top-level absolute paths with IDs are returned'
        result.size() == 3
        result.containsAll([
                '/SubNetwork=SN1',
                '/SubNetwork=SN1/ManagedElement=ME1',
                '/SubNetwork=SN2'
        ])
    }

    def 'should remove duplicate FDNs from results'() {
        given: 'jex expression with many paths containing duplicate paths differing only by sub-attributes'
        String jexMultiPathExpressions = '''
            /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]
            /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]/attributes
        '''

        when: 'the resolver is run'
        def result = JexResolver.resolveAlternateIds(jexMultiPathExpressions)

        then: 'only the shortest unique FDN is returned'
        result == ['/SubNetwork=SN1/ManagedElement=ME1']
    }

    def 'should only include absolute paths when mixed'() {
        given: 'jex expression with many paths containing absolute and non-absolute paths'
        String jexMultiPathExpressions = '''
            /SubNetwork[id="SN1"]/ManagedElement
            RelativeElement[id="R1"]/Child
        '''

        when: 'the resolver processes them'
        def result = JexResolver.resolveAlternateIds(jexMultiPathExpressions)

        then: 'only top-level absolute IDs are returned'
        result == ['/SubNetwork=SN1']
    }

    def 'should return empty list when no id present'() {
        given: 'a jex expression with path string with no id attribute'
        String jexPathExpression = "/SubNetwork/ManagedElement/attributes"

        expect: 'no alternate IDs are found'
        JexResolver.resolveAlternateIds(jexPathExpression) == []
    }

    def 'should return empty list when input is null or blank'() {
        expect: 'null or whitespace-only input returns an empty list'
        JexResolver.resolveAlternateIds(null) == []
        JexResolver.resolveAlternateIds('   ') == []
    }

    def 'should not include lines starting with &&'() {
        given: 'a jex expression with many paths string containing comment lines starting with &&'
        String jexMultiPathExpressions = '''
            /SubNetwork[id="SN1"]/ManagedElement
            && comment line
            /SubNetwork[id="SN1"]/ManagedElement/attributes
        '''

        when: 'the resolver is run'
        List<String> result = JexResolver.resolveAlternateIds(jexMultiPathExpressions)

        then: 'commented lines are ignored and only valid absolute IDs are returned'
        result == ['/SubNetwork=SN1']
    }

    def 'should handle id formats with no quotes, single quotes, or spaces'() {
        given: 'jex expression with many paths with varied id formatting styles'
        String jexMultiPathExpressions = '''
            /SubNetwork[id=SN1]/ManagedElement[id=ME1]
            /SubNetwork[id = 'SN2']/ManagedElement[id = 'ME2']
        '''

        when: 'the resolver processes the jex expression with many paths'
        def result = JexResolver.resolveAlternateIds(jexMultiPathExpressions)

        then: 'IDs are normalized to double-quoted format'
        result.containsAll([
                '/SubNetwork=SN1/ManagedElement=ME1',
                '/SubNetwork=SN2/ManagedElement=ME2'
        ])
    }

    def 'should ignore relative paths'() {
        given: 'jex expression with many paths contains both relative and absolute paths'
        String jexMultiPathExpressions = '''
            ManagedElement[id="ME1"]
            /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]
        '''

        when: 'the resolver processes the input'
        def result = JexResolver.resolveAlternateIds(jexMultiPathExpressions)

        then: 'only absolute paths are included in the result'
        result == ['/SubNetwork=SN1/ManagedElement=ME1']
    }

}


