/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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

package org.onap.cps.ncmp.rest.util

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.Signature
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters
import org.onap.cps.ncmp.rest.model.ConditionProperties
import spock.lang.Specification

class CmHandleSearchExecutionCounterSpec extends Specification {

    def meterRegistry = new SimpleMeterRegistry()
    def mockJoinPoint = Mock(JoinPoint)
    def mockCountCmHandleSearchExecutionAnnotation = Mock(CountCmHandleSearchExecution)
    def mockSignature = Mock(Signature)

    def objectUnderTest = new CmHandleSearchExecutionCounter(meterRegistry)

    def setup() {
        mockCountCmHandleSearchExecutionAnnotation.methodName() >> 'testMethod'
        mockCountCmHandleSearchExecutionAnnotation.interfaceName() >> 'testInterface'
        mockSignature.toString() >> 'testSignature'
        mockJoinPoint.getSignature() >> mockSignature
    }

    def 'should track search with conditions'() {
        given: 'CmHandleQueryParameters with conditions'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition1 = new ConditionProperties(conditionName: 'condition1')
            def condition2 = new ConditionProperties(conditionName: 'condition2')
            cmHandleQueryParameters.addCmHandleQueryParametersItem(condition1).addCmHandleQueryParametersItem(condition2)
        and: 'joinPoint returns the parameters'
            mockJoinPoint.getArgs() >> [cmHandleQueryParameters]
        when: 'the annotated method is called'
            objectUnderTest.cmHandleSearchExecutionCounter(mockJoinPoint, mockCountCmHandleSearchExecutionAnnotation)
        then: 'the counter should be registered'
            def counter = findCounter('cm_handle_search_invocations', [
                'method'       : 'testMethod',
                'cps-interface': 'testInterface',
                'conditions'   : 'condition1_condition2'
            ])
        and: 'is incremented once'
            assert counter.count() == 1
    }

    def 'should track search with no conditions as NONE'() {
        given: 'empty CmHandleQueryParameters'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
        and: 'joinPoint returns the parameters'
            mockJoinPoint.getArgs() >> [cmHandleQueryParameters]
        when: 'the annotated method is called'
            objectUnderTest.cmHandleSearchExecutionCounter(mockJoinPoint, mockCountCmHandleSearchExecutionAnnotation)
        then: 'the counter should be registered with NONE tag'
            def counter = findCounter('cm_handle_search_invocations', [
                method         : 'testMethod',
                'cps-interface': 'testInterface',
                conditions     : 'NONE'
            ])
        and: 'is incremented once'
            assert counter.count() == 1
    }

    def 'should not create counter when args are empty'() {
        given: 'joinPoint with empty args'
            mockJoinPoint.getArgs() >> []
        when: 'the aspect method is called'
            objectUnderTest.cmHandleSearchExecutionCounter(mockJoinPoint, mockCountCmHandleSearchExecutionAnnotation)
        then: 'no counter should be registered'
            assert meterRegistry.find('cm_handle_search_invocations').counters().isEmpty()
    }

    def 'should not create counter when first arg is not CmHandleQueryParameters'() {
        given: 'joinPoint with non-CmHandleQueryParameters arg'
            mockJoinPoint.getArgs() >> ['not a CmHandleQueryParameters']
        when: 'the aspect method is called'
            objectUnderTest.cmHandleSearchExecutionCounter(mockJoinPoint, mockCountCmHandleSearchExecutionAnnotation)
        then: 'no counter should be registered'
            assert meterRegistry.find('cm_handle_search_invocations').counters().isEmpty()
    }

    def 'should sort condition names alphabetically'() {
        given: 'CmHandleQueryParameters with unsorted conditions'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition1 = new ConditionProperties(conditionName: 'zCondition')
            def condition2 = new ConditionProperties(conditionName: 'aCondition')
            cmHandleQueryParameters.addCmHandleQueryParametersItem(condition1).addCmHandleQueryParametersItem(condition2)
        and: 'joinPoint returns our parameters'
            mockJoinPoint.getArgs() >> [cmHandleQueryParameters]
        when: 'the aspect method is called'
            objectUnderTest.cmHandleSearchExecutionCounter(mockJoinPoint, mockCountCmHandleSearchExecutionAnnotation)
        then: 'the counter should be registered with alphabetically sorted tags'
            def counter = findCounter('cm_handle_search_invocations', [
                'method'       : 'testMethod',
                'cps-interface': 'testInterface',
                'conditions'   : 'aCondition_zCondition'
            ])
        and: 'counter is incremented once'
            assert counter.count() == 1
    }

    def findCounter(name, tags) {
        def counterSearch = meterRegistry.find(name)
        tags.each { key, value ->
            counterSearch = counterSearch.tag(key, value)
        }
        return counterSearch.counter()
    }
}