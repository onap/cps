/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.utils

import spock.lang.Specification

import java.time.Year

class EventDateTimeFormatterSpec extends Specification {

    def 'Get ISO formatted date and time.' () {
        expect: 'iso formatted date and time starts with current year'
            assert EventDateTimeFormatter.getCurrentIsoFormattedDateTime().startsWith(String.valueOf(Year.now()))
    }

    def 'Convert date time from string to OffsetDateTime type.'() {
        when: 'date time as a string is converted to OffsetDateTime type'
            def result = EventDateTimeFormatter.toIsoOffsetDateTime('2024-05-28T18:28:02.869+0100')
        then: 'the result convert back back to a string is the same as the original timestamp (except the format of timezone offset)'
            assert result.toString() == '2024-05-28T18:28:02.869+01:00'
    }

    def 'Convert blank string.' () {
        expect: 'converting a blank string result in null'
            assert EventDateTimeFormatter.toIsoOffsetDateTime(' ') == null
    }

}
