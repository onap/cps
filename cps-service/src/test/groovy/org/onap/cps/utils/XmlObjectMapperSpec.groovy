/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.onap.cps.api.exceptions.DataValidationException
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectWriter
import spock.lang.Specification

class XmlObjectMapperSpec extends Specification {

    def spiedXmlMapper = Spy(XmlMapper)
    def objectUnderTest = new XmlObjectMapper(spiedXmlMapper)
    def xmlObjectMapperdefault = new XmlObjectMapper()

    def 'Map a structured object to Xml'() {
        given: 'an object model'
            def object = [bookstore: [['bookstore-name': 'Chapters',categories: [code: '1', name: 'SciFi']]]]
            def expectedResult = '<stores><bookstore><bookstore-name>Chapters</bookstore-name><categories><code>1</code><name>SciFi</name></categories></bookstore></stores>'
        when: 'the object is mapped to string'
            def result = objectUnderTest.asXmlString(object, "stores")
        then: 'the result is a valid xml string'
            assert result == expectedResult
    }

    def 'Map a structured object with exception during mapping.'() {
        given: 'some object'
            def object = new Object();
        and: 'the XmlMapper chain throws an exception'
            def writer = Mock(ObjectWriter)
            spiedXmlMapper.writer() >> writer
            writer.withRootName('stores') >> writer
            writer.writeValueAsString(_) >> { throw new Exception() }
        when: 'attempt to build XML'
            objectUnderTest.asXmlString(object, 'stores')
        then: 'the exception has correct message'
            def thrownException = thrown(DataValidationException)
            thrownException.message == 'Data Validation Failed'
    }
}

