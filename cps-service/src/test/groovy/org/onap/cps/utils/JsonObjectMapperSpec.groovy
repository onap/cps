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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import groovy.json.JsonSlurper
import org.onap.cps.TestUtils
import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Specification

class JsonObjectMapperSpec extends Specification {

    def spiedObjectMapper = Spy(ObjectMapper)
    def jsonObjectMapper = new JsonObjectMapper(spiedObjectMapper)

    def 'Map a structured object to valid json #type.'() {
        given: 'an object model'
            def object = spiedObjectMapper.readValue(TestUtils.getResourceFileContent('bookstore.json'), Object)
        when: 'the object is mapped to string'
            def content = type == 'String' ? jsonObjectMapper.asJsonString(object) : jsonObjectMapper.asJsonBytes(object)
        then: 'the result is a valid json content (can be parsed)'
            def contentMap = new JsonSlurper().parseText(content instanceof String ? content : new String(content))
        and: 'the parsed content is as expected'
            assert contentMap.'test:bookstore'.'bookstore-name' == 'Chapters/Easons'
        where: 'the following expected json types are used'
            type << ['String', 'Bytes']
    }

    def 'Map a structured object to json String error.'() {
        given: 'some object'
            def object = new Object()
        and: 'the Object mapper throws an exception'
            spiedObjectMapper.writeValueAsString(object) >> { throw new JsonProcessingException('Sample problem'){} }
        when: 'attempting to convert the object to a string'
            jsonObjectMapper.asJsonString(object);
        then: 'a Data Validation Exception is thrown'
            def thrown = thrown(DataValidationException)
        and: 'the details containing the original error message'
            assert thrown.details == 'Sample problem'
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
            jsonObjectMapper.convertJsonString(content, Map);
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
        and: 'disable serialization failure on empty bean'
            spiedObjectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        when: 'the object is mapped to string'
            jsonObjectMapper.asJsonString(object);
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Map a structurally compatible json String to JsonNode.'() {
        given: 'Unstructured object'
            def content = '{ "nest": { "birds": "bird" } }'
        when: 'the object is mapped to string'
            def result = jsonObjectMapper.convertToJsonNode(content);
        then: 'the result is a valid JsonNode'
            result.fieldNames().next() == "nest"
    }

    def 'Map a unstructured json String to JsonNode.'() {
        given: 'Unstructured object'
            def content = '{ "nest": { "birds": "bird" }] }'
        when: 'the object is mapped to string'
            jsonObjectMapper.convertToJsonNode(content);
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }
}
