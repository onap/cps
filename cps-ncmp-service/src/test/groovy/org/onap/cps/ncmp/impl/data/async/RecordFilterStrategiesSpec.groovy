/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved
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

package org.onap.cps.ncmp.impl.data.async

import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.Headers
import spock.lang.Specification

import java.nio.charset.Charset

class RecordFilterStrategiesSpec extends Specification {

    def objectUnderTest = new RecordFilterStrategies()

    def headers = Mock(Headers)
    def header = Mock(Header)

    def 'Determining cloud event using ce_type header for a #scenario.'() {
        given: 'headers contain a header for key: #key'
            headers.lastHeader(key) >> header
        expect: 'the check for cloud events returns #expectedResult'
            assert objectUnderTest.isCloudEvent(headers) == expectedResult
        where: 'the following headers (keys) are defined'
            scenario          | key       || expectedResult
            'cloud event'     | 'ce_type' || true
            'non-cloud event' | 'other'   || false
    }

    def 'Excluding cloud event with header #scenario.'() {
        given: 'headers contain a header for key: #key and value: #value'
            header.value() >> value.getBytes(Charset.defaultCharset())
            headers.lastHeader(key) >> header
        expect: 'the event would (not) be excluded: #expectedToBeExcluded'
            assert objectUnderTest.isNotCloudDataOperationEvent(headers) == expectedToBeExcluded
        where: 'the following headers are defined'
            scenario                      | key       | value                                  || expectedToBeExcluded
            'DataOperationEvent'          | 'ce_type' | 'DataOperationEvent'                   || false
            'contains DataOperationEvent' | 'ce_type' | 'Contains DataOperationEvent and more' || false
            'other type'                  | 'ce_type' | 'other'                                || true
            'no ce_type header'           | 'other'   | 'irrelevant'                           || true
    }

}
