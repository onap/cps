/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * Modifications Copyright (C) 2021-2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
*/

package org.onap.cps.spi.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.StaleStateException
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.entities.AnchorEntity
import org.onap.cps.spi.entities.FragmentEntity
import org.onap.cps.spi.entities.SchemaSetEntity
import org.onap.cps.spi.entities.YangResourceEntity
import org.onap.cps.spi.exceptions.ConcurrencyException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.FragmentRepository
import org.onap.cps.spi.utils.SessionManager
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

class CpsDataPersistenceServiceSpec extends Specification {

    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockAnchorRepository = Mock(AnchorRepository)
    def mockFragmentRepository = Mock(FragmentRepository)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockSessionManager = Mock(SessionManager)

    def objectUnderTest = new CpsDataPersistenceServiceImpl(
            mockDataspaceRepository, mockAnchorRepository, mockFragmentRepository, jsonObjectMapper,mockSessionManager)

    @Shared
    def NEW_RESOURCE_CONTENT = 'module stores {\n' +
            '    yang-version 1.1;\n' +
            '    namespace "org:onap:ccsdk:sample";\n' +
            '\n' +
            '    prefix book-store;\n' +
            '\n' +
            '    revision "2020-09-15" {\n' +
            '        description\n' +
            '        "Sample Model";\n' +
            '    }' +
            '}'

    @Shared
    def yangResourceSet = [new YangResourceEntity(moduleName: 'moduleName', content: NEW_RESOURCE_CONTENT,
            fileName: 'sampleYangResource'
    )] as Set


    def 'Handling of StaleStateException (caused by concurrent updates) during data node tree update using a singular data node.'() {
        given: 'the fragment repository returns a fragment entity'
            mockFragmentRepository.getByDataspaceAndAnchorAndXpath(_, _, _) >> {
                def fragmentEntity = new FragmentEntity()
                fragmentEntity.setChildFragments([new FragmentEntity()] as Set<FragmentEntity>)
                return fragmentEntity
            }
        and: 'a data node is concurrently updated by another transaction'
            mockFragmentRepository.save(_) >> { throw new StaleStateException("concurrent updates") }
        when: 'attempt to update data node with submitted data nodes'
            objectUnderTest.replaceDataNodeTree('some-dataspace', 'some-anchor', new DataNodeBuilder().withXpath('/some/xpath').build())
        then: 'concurrency exception is thrown'
            def concurrencyException = thrown(ConcurrencyException)
            assert concurrencyException.getDetails().contains('some-dataspace')
            assert concurrencyException.getDetails().contains('some-anchor')
            assert concurrencyException.getDetails().contains('/some/xpath')
    }

    def 'Handling of StaleStateException (caused by concurrent updates) during data node tree update using multiple data nodes.'() {
        given: 'the fragment repository returns a list of fragment entities'
            mockFragmentRepository.getByDataspaceAndAnchorAndXpath(_, _, _) >> new FragmentEntity()
        and: 'a data node is concurrently updated by another transaction'
            mockFragmentRepository.saveAll(_) >> { throw new StaleStateException("concurrent updates") }
        when: 'attempt to update data node with submitted data nodes'
            objectUnderTest.replaceDataNodeTree('some-dataspace', 'some-anchor', [])
        then: 'concurrency exception is thrown'
            def concurrencyException = thrown(ConcurrencyException)
            assert concurrencyException.getDetails().contains('some-dataspace')
            assert concurrencyException.getDetails().contains('some-anchor')
    }

    def 'Retrieving a data node with a property JSON value of #scenario'() {
        given: 'a fragment with a property JSON value of #scenario'
        mockFragmentRepository.getByDataspaceAndAnchorAndXpath(_, _, _) >> {
            new FragmentEntity(childFragments: Collections.emptySet(),
                    attributes: "{\"some attribute\": ${dataString}}",
                    anchor: new AnchorEntity(schemaSet: new SchemaSetEntity(yangResources: yangResourceSet )))
        }
        when: 'getting the data node represented by this fragment'
            def dataNode = objectUnderTest.getDataNode('my-dataspace', 'my-anchor',
                    '/parent-01', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the leaf is of the correct value and data type'
            def attributeValue = dataNode.leaves.get('some attribute')
            assert attributeValue == expectedValue
            assert attributeValue.class == expectedDataClass
        where: 'the following Data Type is passed'
            scenario                              | dataString            || expectedValue     | expectedDataClass
            'just numbers'                        | '15174'               || 15174             | Integer
            'number with dot'                     | '15174.32'            || 15174.32          | Double
            'number with 0 value after dot'       | '15174.0'             || 15174.0           | Double
            'number with 0 value before dot'      | '0.32'                || 0.32              | Double
            'number higher than max int'          | '2147483648'          || 2147483648        | Long
            'just text'                           | '"Test"'              || 'Test'            | String
            'number with exponent'                | '1.2345e5'            || 1.2345e5          | Double
            'number higher than max int with dot' | '123456789101112.0'   || 123456789101112.0 | Double
            'text and numbers'                    | '"String = \'1234\'"' || "String = '1234'" | String
            'number as String'                    | '"12345"'             || '12345'           | String
    }

    def 'Retrieving a data node with invalid JSON'() {
        given: 'a fragment with invalid JSON'
            mockFragmentRepository.getByDataspaceAndAnchorAndXpath(_, _, _) >> {
                new FragmentEntity(childFragments: Collections.emptySet(), attributes: '{invalid json')
            }
        when: 'getting the data node represented by this fragment'
            def dataNode = objectUnderTest.getDataNode('my-dataspace', 'my-anchor',
                    '/parent-01', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'start session'() {
        when: 'start session'
            objectUnderTest.startSession()
        then: 'the session manager method to start session is invoked'
            1 * mockSessionManager.startSession()
    }

    def 'close session'() {
        given: 'session ID'
            def someSessionId = 'someSessionId'
        when: 'close session method is called with session ID as parameter'
            objectUnderTest.closeSession(someSessionId)
        then: 'the session manager method to close session is invoked with parameter'
            1 * mockSessionManager.closeSession(someSessionId, mockSessionManager.WITH_COMMIT)
    }

    def 'Lock anchor.'(){
        when: 'lock anchor method is called with anchor entity details'
            objectUnderTest.lockAnchor('mySessionId', 'myDataspaceName', 'myAnchorName', 123L)
        then: 'the session manager method to lock anchor is invoked with same parameters'
            1 * mockSessionManager.lockAnchor('mySessionId', 'myDataspaceName', 'myAnchorName', 123L)
    }
}