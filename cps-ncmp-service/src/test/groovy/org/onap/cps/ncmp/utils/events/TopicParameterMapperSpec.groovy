/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2026 Nordix Foundation
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

package org.onap.cps.ncmp.utils.events

import org.onap.cps.ncmp.api.exceptions.InvalidTopicException
import spock.lang.Specification

class TopicParameterMapperSpec extends Specification {

    def 'Valid topic name validation.'() {
        when: 'a valid topic name is validated'
            TopicValidator.validateTopicName('my-valid-topic')
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Validating invalid topic names.'() {
        when: 'the invalid topic name is validated'
            TopicValidator.validateTopicName(topicName)
        then: 'boolean response will be returned for #scenario'
            thrown(InvalidTopicException)
        where: 'the following names are used'
            scenario                  | topicName
            'empty topic'             | ''
            'blank topic'             | ' '
            'invalid non empty topic' | '1_5_*_#'
    }

    def 'Validating topic is not reserved.'() {
        when: 'a topic different from the async topic is validated'
            TopicValidator.validateTopicNotReserved('some-client-topic', 'ncmp-async-topic')
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Validating topic that equals the reserved async topic.'() {
        when: 'a topic equal to the async topic is validated'
            TopicValidator.validateTopicNotReserved('ncmp-async-topic', 'ncmp-async-topic')
        then: 'an invalid topic exception is thrown'
            thrown(InvalidTopicException)
    }
}
