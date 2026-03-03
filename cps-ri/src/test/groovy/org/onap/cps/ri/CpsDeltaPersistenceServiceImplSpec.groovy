/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG
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

package org.onap.cps.ri

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ri.models.AnchorEntity
import org.onap.cps.ri.models.DataspaceEntity
import org.onap.cps.ri.repository.AnchorRepository
import org.onap.cps.ri.repository.DataspaceRepository
import org.onap.cps.ri.repository.DeltaProjection
import org.onap.cps.ri.repository.FragmentRepository
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CpsDeltaPersistenceServiceImplSpec extends Specification {

    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockAnchorRepository = Mock(AnchorRepository)
    def mockFragmentRepository = Mock(FragmentRepository)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def objectUnderTest = new CpsDeltaPersistenceServiceImpl(mockDataspaceRepository, mockAnchorRepository,
            mockFragmentRepository, jsonObjectMapper)

    static def dataspaceEntity = new DataspaceEntity(id: 1, name: 'test-dataspace')
    static def sourceAnchorEntity = new AnchorEntity(id: 10, dataspace: dataspaceEntity)
    static def targetAnchorEntity = new AnchorEntity(id: 20, dataspace: dataspaceEntity)

    def setup() {
        mockDataspaceRepository.getByName('test-dataspace') >> dataspaceEntity
        mockAnchorRepository.getByDataspaceAndName(dataspaceEntity, 'source-anchor') >> sourceAnchorEntity
        mockAnchorRepository.getByDataspaceAndName(dataspaceEntity, 'target-anchor') >> targetAnchorEntity
    }

    def 'Get delta with removed container node'() {
        given: 'a removed container node fragment returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'one delta report is returned with remove action'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == '/bookstore'
        and: 'source data is wrapped under container node name'
            result[0].sourceData == ['bookstore': ['bookstore-name': 'Easons']]
            result[0].targetData == null
    }

    def 'Get delta with removed list element node'() {
        given: 'a removed list element fragment returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='02']", '{"code":"02","name":"Kids"}', null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'source data is wrapped as array under list node name'
            result.size() == 1
            result[0].action == 'remove'
            result[0].sourceData == ['categories': [['code': '02', 'name': 'Kids']]]
    }

    def 'Get delta with added container node'() {
        given: 'an added container node fragment returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> [
                createProjection('/bookstore', null, '{"bookstore-name":"New Store"}')
            ]
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'one delta report is returned with create action'
            result.size() == 1
            result[0].action == 'create'
            result[0].xpath == '/bookstore'
        and: 'target data is wrapped under container node name'
            result[0].sourceData == null
            result[0].targetData == ['bookstore': ['bookstore-name': 'New Store']]
    }

    def 'Get delta with added list element node'() {
        given: 'an added list element fragment returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='03']", null, '{"code":"03","name":"Fiction"}')
            ]
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'target data is wrapped as array under list node name'
            result.size() == 1
            result[0].action == 'create'
            result[0].targetData == ['categories': [['code': '03', 'name': 'Fiction']]]
    }

    def 'Get delta with updated container node where leaf value changed'() {
        given: 'an updated container node fragment returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"New Store"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'one delta report is returned with replace action'
            result.size() == 1
            result[0].action == 'replace'
            result[0].xpath == '/bookstore'
        and: 'only the changed leaf is present in source and target data'
            result[0].sourceData == ['bookstore': ['bookstore-name': 'Easons']]
            result[0].targetData == ['bookstore': ['bookstore-name': 'New Store']]
    }

    def 'Get delta with updated node where leaf is added in target'() {
        given: 'a node where target has an additional leaf'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"Easons","location":"Dublin"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'only the new leaf appears in target data, source data is null (no changed leaves in source)'
            result.size() == 1
            result[0].action == 'replace'
            result[0].sourceData == null
            result[0].targetData == ['bookstore': ['location': 'Dublin']]
    }

    def 'Get delta with updated node where leaf is removed in target'() {
        given: 'a node where target has a leaf removed'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons","location":"Dublin"}', '{"bookstore-name":"Easons"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'only the removed leaf appears in source data, target data is null'
            result.size() == 1
            result[0].action == 'replace'
            result[0].sourceData == ['bookstore': ['location': 'Dublin']]
            result[0].targetData == null
    }

    def 'Get delta with updated list element includes key leaves'() {
        given: 'an updated list element fragment returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='01']", '{"code":"01","name":"Children"}', '{"code":"01","name":"Kids"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'replace action with key leaves included in both source and target data'
            result.size() == 1
            result[0].action == 'replace'
        and: 'source data contains the changed leaf and the key leaf, wrapped as array'
            result[0].sourceData == ['categories': [['name': 'Children', 'code': '01']]]
        and: 'target data contains the changed leaf and the key leaf, wrapped as array'
            result[0].targetData == ['categories': [['name': 'Kids', 'code': '01']]]
    }

    def 'Get delta with no differences returns empty list'() {
        given: 'no fragments returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'empty delta report is returned'
            result.isEmpty()
    }

    def 'Get delta with updated node where attributes are identical after leaf comparison'() {
        given: 'a fragment where source and target attributes are effectively the same'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"Easons"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'no delta report is returned since leaves are identical'
            result.isEmpty()
    }

    def 'Get delta with removed node having null attributes'() {
        given: 'a removed fragment with null attributes'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> [
                createProjection('/bookstore/empty-container', null, null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'delta report is returned with remove action and no source data'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == '/bookstore/empty-container'
            result[0].sourceData == null
    }

    def 'Get delta with multiple removed, added, and updated nodes'() {
        given: 'multiple fragments returned by repository for all three types'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> [
                createProjection('/bookstore/old-node', '{"key":"old-value"}', null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> [
                createProjection('/bookstore/new-node', null, '{"key":"new-value"}')
            ]
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"Crossword"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'three delta reports are returned: one removed, one updated, one added'
            result.size() == 3
        and: 'the first entry is a remove action'
            result[0].action == 'remove'
            result[0].xpath == '/bookstore/old-node'
        and: 'the second entry is a replace action'
            result[1].action == 'replace'
            result[1].xpath == '/bookstore'
        and: 'the third entry is a create action'
            result[2].action == 'create'
            result[2].xpath == '/bookstore/new-node'
    }

    def 'Get delta with updated node having multiple leaf changes'() {
        given: 'a fragment with multiple leaves where some changed, some unchanged'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"name":"Old","location":"Dublin","rating":5}', '{"name":"New","location":"Dublin","rating":9}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'only changed leaves are in the delta report, unchanged "location" is excluded'
            result.size() == 1
            result[0].action == 'replace'
            result[0].sourceData == ['bookstore': ['name': 'Old', 'rating': 5]]
            result[0].targetData == ['bookstore': ['name': 'New', 'rating': 9]]
    }

    def 'Get delta with added node having null attributes'() {
        given: 'an added fragment with null attributes'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> [
                createProjection('/bookstore/empty-container', null, null)
            ]
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'delta report is returned with create action and no target data'
            result.size() == 1
            result[0].action == 'create'
            result[0].xpath == '/bookstore/empty-container'
            result[0].targetData == null
    }

    def 'Get delta with added node having empty string attributes'() {
        given: 'an added fragment with empty string attributes'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> [
                createProjection('/bookstore/empty-leaf-container', '', null)
            ]
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'delta report is returned with create action and no target data'
            result.size() == 1
            result[0].action == 'create'
            result[0].xpath == '/bookstore/empty-leaf-container'
            result[0].targetData == null
    }

    def 'Get delta with non-root xpath parameter'() {
        given: 'a removed fragment returned for a non-root xpath'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/bookstore') >> [
                createProjection("/bookstore/categories[@code='01']", '{"code":"01","name":"Sci-fi"}', null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/bookstore') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/bookstore') >> []
        when: 'delta is requested with a non-root xpath'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore')
        then: 'delta report is returned correctly'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == "/bookstore/categories[@code='01']"
            result[0].sourceData == ['categories': [['code': '01', 'name': 'Sci-fi']]]
    }

    def 'Get delta with multiple removed fragments'() {
        given: 'multiple removed fragments returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> [
                createProjection('/bookstore/node-a', '{"key":"value-a"}', null),
                createProjection('/bookstore/node-b', '{"key":"value-b"}', null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'two remove delta reports are returned'
            result.size() == 2
            result.every { it.action == 'remove' }
            result[0].xpath == '/bookstore/node-a'
            result[1].xpath == '/bookstore/node-b'
    }

    def 'Get delta with multiple added fragments'() {
        given: 'multiple added fragments returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> [
                createProjection('/bookstore/node-x', null, '{"key":"value-x"}'),
                createProjection('/bookstore/node-y', null, '{"key":"value-y"}')
            ]
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'two create delta reports are returned'
            result.size() == 2
            result.every { it.action == 'create' }
            result[0].xpath == '/bookstore/node-x'
            result[1].xpath == '/bookstore/node-y'
    }

    def 'Get delta with multiple updated fragments where some are filtered out'() {
        given: 'two updated fragments where one has identical attributes'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"Easons"}'),
                createProjection('/bookstore/info', '{"version":"1.0"}', '{"version":"2.0"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'only the actually changed fragment appears in the delta report'
            result.size() == 1
            result[0].action == 'replace'
            result[0].xpath == '/bookstore/info'
            result[0].sourceData == ['info': ['version': '1.0']]
            result[0].targetData == ['info': ['version': '2.0']]
    }

    def 'Get delta with deeper nested xpath wraps under correct node name'() {
        given: 'a removed fragment with a deeper nested xpath'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> [
                createProjection('/bookstore/shelves/books', '{"title":"CPS Guide"}', null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'source data is wrapped under the leaf node name only'
            result.size() == 1
            result[0].action == 'remove'
            result[0].sourceData == ['books': ['title': 'CPS Guide']]
    }

    def 'Get delta with updated list element where only a leaf is added in target'() {
        given: 'an updated list element where target has an additional leaf'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='01']", '{"code":"01","name":"Kids"}', '{"code":"01","name":"Kids","description":"Children books"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'only the new leaf appears in target data with key leaves, source data is null (no changed source leaves)'
            result.size() == 1
            result[0].action == 'replace'
            result[0].sourceData == null
            result[0].targetData == ['categories': [['description': 'Children books', 'code': '01']]]
    }

    def 'Get delta with updated list element where only a leaf is removed in target'() {
        given: 'an updated list element where target has a leaf removed'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='01']", '{"code":"01","name":"Kids","description":"Children books"}', '{"code":"01","name":"Kids"}')
            ]
        when: 'delta is requested'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
        then: 'only the removed leaf appears in source data with key leaves, target data is null'
            result.size() == 1
            result[0].action == 'replace'
            result[0].sourceData == ['categories': [['description': 'Children books', 'code': '01']]]
            result[0].targetData == null
    }

    def 'Get delta result is unmodifiable'() {
        given: 'fragments returned by repository'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested and modification attempted'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/')
            result.add(null)
        then: 'an exception is thrown because the list is unmodifiable'
            thrown(UnsupportedOperationException)
    }

    def createProjection(String xpath, String sourceAttributes, String targetAttributes) {
        return [getXpath: { xpath }, getSourceAttributes: { sourceAttributes }, getTargetAttributes: { targetAttributes }] as DeltaProjection
    }
}
