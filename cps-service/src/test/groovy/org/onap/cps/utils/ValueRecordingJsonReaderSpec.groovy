/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

package org.onap.cps.utils

import spock.lang.Specification

class ValueRecordingJsonReaderSpec extends Specification {

    def 'Field name tracking within a JSON object.'() {
        given: 'a JSON object with two string fields'
            def objectUnderTest = new ValueRecordingJsonReader(new StringReader('{"alpha": "one", "beta": "two"}'))
        when: 'the object is entered'
            objectUnderTest.beginObject()
        then: 'no field names have been seen yet'
            assert objectUnderTest.getSeenFieldNames() == [] as Set
        when: 'both field names and their values are read'
            objectUnderTest.nextName()
            objectUnderTest.nextString()
            objectUnderTest.nextName()
            objectUnderTest.nextString()
        then: 'both field names are recorded'
            assert objectUnderTest.getSeenFieldNames() == ['alpha', 'beta'] as Set
        when: 'the object is exited'
            objectUnderTest.endObject()
        then: 'the recorded field names are cleared'
            assert objectUnderTest.getSeenFieldNames() == [] as Set
    }

    def 'Reading a string value from a JSON object.'() {
        given: 'a JSON object with a string field'
            def objectUnderTest = new ValueRecordingJsonReader(new StringReader('{"key": "hello"}'))
        when: 'the object is entered and the string value is consumed'
            objectUnderTest.beginObject()
            objectUnderTest.nextName()
            objectUnderTest.nextString()
        then: 'the value is stored as the last string value'
            assert objectUnderTest.getLastStringValue() == 'hello'
        and: 'the last-value-null flag is false'
            assert !objectUnderTest.isLastValueNull()
    }

    def 'Reading a null value from a JSON object.'() {
        given: 'a JSON object with a null field'
            def objectUnderTest = new ValueRecordingJsonReader(new StringReader('{"key": null}'))
        when: 'the object is entered and the null value is consumed'
            objectUnderTest.beginObject()
            objectUnderTest.nextName()
            objectUnderTest.nextNull()
        then: 'the last-value-null flag is true'
            assert objectUnderTest.isLastValueNull()
        and: 'lastStringValue is null'
            assert objectUnderTest.getLastStringValue() == null
    }

    def 'Field name tracking with nested JSON objects.'() {
        given: 'a JSON object containing a nested object'
            def objectUnderTest = new ValueRecordingJsonReader(new StringReader('{"outer": {"inner": "value"}}'))
        when: 'the outer object, its field name, and the inner object are entered'
            objectUnderTest.beginObject()
            objectUnderTest.nextName()
            objectUnderTest.beginObject()
            objectUnderTest.nextName()
        then: 'only the inner scope field name is visible'
            assert objectUnderTest.getSeenFieldNames() == ['inner'] as Set
        when: 'the inner object is exited'
            objectUnderTest.nextString()
            objectUnderTest.endObject()
        then: 'the outer scope field name is visible again'
            assert objectUnderTest.getSeenFieldNames() == ['outer'] as Set
    }

    def 'Path tracking after reading a field name.'() {
        given: 'a simple JSON object'
            def objectUnderTest = new ValueRecordingJsonReader(new StringReader('{"name": "Alice"}'))
        expect: 'path is at root before entering the object'
            assert objectUnderTest.getPath() == '$'
        when: 'the object is entered and the field name is read'
            objectUnderTest.beginObject()
            objectUnderTest.nextName()
        then: 'the path reflects the current field name'
            assert objectUnderTest.getPath() == '$.name'
    }
}
