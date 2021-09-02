/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
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

import org.hibernate.StaleStateException
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.entities.FragmentEntity
import org.onap.cps.spi.exceptions.ConcurrencyException
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.FragmentRepository
import spock.lang.Specification


class CpsDataPersistenceServiceSpec extends Specification {

    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockAnchorRepository = Mock(AnchorRepository)
    def mockFragmentRepository = Mock(FragmentRepository)

    def objectUnderTest = new CpsDataPersistenceServiceImpl(
            mockDataspaceRepository, mockAnchorRepository, mockFragmentRepository)

    def 'Handling of StaleStateException (caused by concurrent updates) during data node tree update.'() {

        def parentXpath = 'parent-01'
        def myDataspaceName = 'my-dataspace'
        def myAnchorName = 'my-anchor'

        given: 'data node object'
            def submittedDataNode = new DataNodeBuilder()
                    .withXpath(parentXpath)
                    .withLeaves(['leaf-name': 'leaf-value'])
                    .build()
        and: 'fragment to be updated'
            mockFragmentRepository.getByDataspaceAndAnchorAndXpath(_, _, _) >> {
                def fragmentEntity = new FragmentEntity()
                fragmentEntity.setXpath(parentXpath)
                fragmentEntity.setChildFragments(Collections.emptySet())
                return fragmentEntity
            }
        and: 'data node is concurrently updated by another transaction'
            mockFragmentRepository.save(_) >> { throw new StaleStateException("concurrent updates") }

        when: 'attempt to update data node'
            objectUnderTest.replaceDataNodeTree(myDataspaceName, myAnchorName, submittedDataNode)

        then: 'concurrency exception is thrown'
            def concurrencyException = thrown(ConcurrencyException)
            assert concurrencyException.getDetails().contains(myDataspaceName)
            assert concurrencyException.getDetails().contains(myAnchorName)
            assert concurrencyException.getDetails().contains(parentXpath)
    }

    def 'Converting fragment with a property JSON value of #scenario'() {

        def xpath = 'parent-01'
        def myDataspaceName = 'my-dataspace'
        def myAnchorName = 'my-anchor'
        def fetchDescendants = FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

        given: 'fragment to be parsed'
        mockFragmentRepository.getByDataspaceAndAnchorAndXpath(_, _, _) >> {
            def fragmentEntity = new FragmentEntity()
            fragmentEntity.setXpath(xpath)
            fragmentEntity.setChildFragments(Collections.emptySet())
            fragmentEntity.setAttributes('{"cellLocalId": '+dataString+'}')
            return fragmentEntity
        }
        when: 'fragment converted to DataNode'
            def dataNode = objectUnderTest.getDataNode(myDataspaceName, myAnchorName, xpath, fetchDescendants)
        then: 'the data type is as expected and the value is retained'
            assert dataNode.leaves.get("cellLocalId").class == expectedDataClass
            assert dataNode.leaves.get("cellLocalId") == expectedDataObject
        where: 'the following Data Type is passed'
            scenario                                |dataString             ||expectedDataObject    |expectedDataClass
            'just numbers'                          |'15174'                ||15174                 |Integer
            'number with dot'                       |'15174.32'             ||15174.32              |Double
            'number with 0 value after dot'         |'15174.0'              ||15174.0               |Double
            'number with 0 value before dot'        |'0.32'                 ||0.32                  |Double
            'number higher than max int'            |'2147483648'           ||2147483648            |Long
            'just text'                             |'"Test"'               ||'Test'                |String
            'number with exponent'                  |'1.2345e5'             ||1.2345e5              |Double
            'number higher than max int with dot'   |'123456789101112.0'    ||123456789101112.0     |Double
            'text and numbers'                      |'"String = \'1234\'"'  ||'String = \'1234\''   |String
            'number as String'                      |'"12345"'              ||'12345'               |String
    }


}
