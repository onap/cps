/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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


import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import org.onap.cps.TestUtils
import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Specification

class JsonObjectMapperSpec extends Specification {

    def spiedObjectMapper = Spy(ObjectMapper)
    def jsonObjectMapper = new JsonObjectMapper(spiedObjectMapper)

    def 'Map a structured object to json String.'() {
        given: 'an object model'
            def object = spiedObjectMapper.readValue(TestUtils.getResourceFileContent('bookstore.json'), Object)
        when: 'the object is mapped to string'
            def content = jsonObjectMapper.asJsonString(object);
        then: 'the result is a valid json string (can be parsed)'
            def contentMap = new JsonSlurper().parseText(content)
        and: 'the parsed content is as expected'
            assert contentMap.'test:bookstore'.'bookstore-name' == 'Chapters'
    }

    def 'Map a structurally compatible object to class object of specific class type T.'() {
        given: 'a map object model'
            def contentMap = new JsonSlurper().parseText(TestUtils.getResourceFileContent('bookstore.json'))
        when: 'converted into a Map'
            def result = jsonObjectMapper.convertToValueType(contentMap, Map);
        then: 'the result is a mapped into class of type Map'
            assert result instanceof Map
        and: 'the map contains the expected key'
            assert result.containsKey('test:bookstore')
            assert result.'test:bookstore'.categories[0].name == 'SciFi'

    }

    def 'Mapping an unstructured json string to class object of specific class type T.'() {
        given: 'Unstructured json string'
            def content = '{ "nest": { "birds": "bird"] } }'
        when: 'mapping json string to given class type'
            def contentMap = jsonObjectMapper.convertJsonString(content, Map);
        then: 'an exception is thrown'
            thrown(DataValidationException)
    }

    def 'Map an incompatible object to class object of specific class type T.'() {
        given: 'a map object model'
            def contentMap = new JsonSlurper().parseText(TestUtils.getResourceFileContent('bookstore.json'))
        and: 'Object mapper throws an exception'
            spiedObjectMapper.convertValue(*_) >> { throw new IllegalArgumentException() }
        when: 'converted into specific class type'
            jsonObjectMapper.convertToValueType(contentMap, Object);
        then: 'an exception is thrown'
            thrown(DataValidationException)
    }

    def 'Map a unstructured object to json String.'() {
        given: 'Unstructured object'
            def object = new Object()
        when: 'the object is mapped to string'
            jsonObjectMapper.asJsonString(object);
        then: 'an exception is thrown'
            thrown(DataValidationException)
    }
}
