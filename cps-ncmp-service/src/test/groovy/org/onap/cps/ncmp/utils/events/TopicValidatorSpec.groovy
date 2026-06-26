/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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

class TopicValidatorSpec extends Specification {

    def objectUnderTest = new TopicValidator(reservedTopicName: 'my-async-topic')

    def 'Valid topic name validation.'() {
        when: 'a valid topic name is validated'
            objectUnderTest.validateTopicName(topicName)
        then: 'no exception is thrown'
            noExceptionThrown()
        where: 'the following valid names are used'
            scenario                    | topicName
            'alphanumeric only'         | 'myTopic123'
            'with hyphen'               | 'my-topic'
            'with underscore'           | 'my_topic'
            'with dot'                  | 'my.topic'
            'mixed valid special chars' | 'my-topic.name_1'
            'single character'          | 'a'
            'starts with dot'           | '.exampleTopic'
            'ends with dot'             | 'exampleTopic.'
            'starts with hyphen'        | '-topic'
            'ends with hyphen'          | 'topic-'
            'starts with underscore'    | '_topic'
            'ends with underscore'      | 'topic_'
            'consecutive dots'          | 'topic..name'
            'consecutive hyphens'       | 'topic--name'
            'consecutive mixed chars'   | 'topic.-name'
            'max length (249 chars)'    | 'a' * 249
    }

    def 'Validating invalid topic names.'() {
        when: 'the invalid topic name is validated'
            objectUnderTest.validateTopicName(topicName)
        then: 'an invalid topic exception is thrown for #scenario'
            thrown(InvalidTopicException)
        where: 'the following names are used'
            scenario                          | topicName
            'empty topic'                     | ''
            'blank topic'                     | ' '
            'special characters'              | '1_5_*_#'
            'single dot'                      | '.'
            'double dot'                      | '..'
            'exceeds max length (250 chars)'  | 'a' * 250
            'contains space'                  | 'topic name'
            'contains hash'                   | 'topic#name'
            'contains asterisk'               | 'topic*name'
            'reserved async topic'            | 'my-async-topic'
    }

}
