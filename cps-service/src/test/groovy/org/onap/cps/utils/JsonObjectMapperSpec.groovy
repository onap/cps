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
            def content = jsonObjectMapper.mapObjectAsJsonString(object);
            def contentMap = new JsonSlurper().parseText(content)
        then: 'the result is a json string'
            assert contentMap.'test:bookstore'.'bookstore-name' == 'Chapters'
    }

    def 'Map a structurally compatible object to class object of specific class type T.'() {
        given: 'a map object model'
            def contentMap = new JsonSlurper().parseText(TestUtils.getResourceFileContent('bookstore.json'))
        when: 'converted into specific class type'
            def object = jsonObjectMapper.convertFromValueToValueType(contentMap, Object);
        then: 'the result is a mapped into class type T'
            assert object != null
    }

    def 'Mapping a structured json string to class object of specific class type T.'() {
        given: 'a json string'
            def content = TestUtils.getResourceFileContent('bookstore.json')
        when: 'mapping json string to given class type'
            def contentMap = jsonObjectMapper.convertStringContentToValueType(content, Map);
        then: 'the result is specified class type object'
            assert contentMap.'test:bookstore'.categories[0].name == 'SciFi'
    }

    def 'Mapping an unstructured json string to class object of specific class type T.'() {
        given: 'a json string'
            def content = '{ "branch": { "nest": { "name": "N", "birds": "bird"] } } }'
        when: 'mapping json string to given class type'
            def contentMap = jsonObjectMapper.convertStringContentToValueType(content, Map);
        then: 'an exception is thrown'
            thrown(DataValidationException)
    }

    def 'Map an incompatible object to class object of specific class type T.'() {
        given: 'a map object model'
            def contentMap = new JsonSlurper().parseText(TestUtils.getResourceFileContent('bookstore.json'))
        and: 'Object mapper throws an exception'
            spiedObjectMapper.convertValue(*_) >> { throw new IllegalArgumentException() }
        when: 'converted into specific class type'
            jsonObjectMapper.convertFromValueToValueType(contentMap, Object);
        then: 'an exception is thrown'
            thrown(DataValidationException)
    }

    def 'Map a unstructured object to json String.'() {
        given: 'an object model'
            def object = new Object()
        when: 'the object is mapped to string'
            jsonObjectMapper.mapObjectAsJsonString(object);
        then: 'an exception is thrown'
            thrown(DataValidationException)
    }
}
