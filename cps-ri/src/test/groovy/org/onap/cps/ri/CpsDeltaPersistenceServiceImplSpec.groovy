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
import org.onap.cps.ri.repository.DeltaProjectionDto
import org.onap.cps.ri.repository.FragmentRepository
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

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

    def 'Get delta with OMIT_DESCENDANTS uses composite exact xpath query for #scenario'() {
        given: 'fragments returned by composite exact xpath repository method'
            mockFragmentRepository.findAllDeltaFragmentsExactXpath(10, 20, '/bookstore') >> compositeFragments
        when: 'delta is requested with OMIT_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', OMIT_DESCENDANTS)
        then: 'delta report contains expected number of entries'
            result.size() == expectedSize
        and: 'delta report contains expected action'
            result[0].action == expectedAction
        where: 'following data was used'
            scenario  | compositeFragments                                                                              || expectedSize | expectedAction
            'removed' | [createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)]                           || 1            | 'remove'
            'added'   | [createProjection('/bookstore', null, '{"bookstore-name":"New Store"}')]                         || 1            | 'create'
            'updated' | [createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"New Store"}')] || 1           | 'replace'
    }

    def 'Get delta with DIRECT_CHILDREN_ONLY uses composite direct children query for #scenario'() {
        given: 'fragments returned by composite direct children repository method'
            mockFragmentRepository.findAllDeltaFragmentsWithDirectChildren(10, 20, '/bookstore') >> compositeFragments
        when: 'delta is requested with DIRECT_CHILDREN_ONLY'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', DIRECT_CHILDREN_ONLY)
        then: 'delta report contains expected number of entries'
            result.size() == expectedSize
        and: 'delta report contains expected action'
            result[0].action == expectedAction
        where: 'following data was used'
            scenario  | compositeFragments                                                                                || expectedSize | expectedAction
            'removed' | [createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)]                             || 1            | 'remove'
            'added'   | [createProjection('/bookstore', null, '{"bookstore-name":"New Store"}')]                           || 1            | 'create'
            'updated' | [createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"New Store"}')] || 1            | 'replace'
    }

    def 'Get delta with INCLUDE_ALL_DESCENDANTS uses composite all-descendants query'() {
        given: 'fragments returned by composite all-descendants repository method'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)
            ]
        when: 'delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'delta report is returned correctly'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == '/bookstore'
    }

    def 'Get delta with OMIT_DESCENDANTS returns empty when no exact match'() {
        given: 'no fragments returned by composite exact xpath repository method'
            mockFragmentRepository.findAllDeltaFragmentsExactXpath(10, 20, '/bookstore') >> []
        when: 'delta is requested with OMIT_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', OMIT_DESCENDANTS)
        then: 'empty delta report is returned'
            result.isEmpty()
    }

    def 'Get delta with DIRECT_CHILDREN_ONLY includes parent and child changes'() {
        given: 'removed fragments at parent and child level returned by composite direct children repository method'
            mockFragmentRepository.findAllDeltaFragmentsWithDirectChildren(10, 20, '/bookstore') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', null),
                createProjection("/bookstore/categories[@code='01']", '{"code":"01","name":"Sci-fi"}', null)
            ]
        when: 'delta is requested with DIRECT_CHILDREN_ONLY'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', DIRECT_CHILDREN_ONLY)
        then: 'two delta reports are returned for parent and child'
            result.size() == 2
            result[0].action == 'remove'
            result[0].xpath == '/bookstore'
            result[1].action == 'remove'
            result[1].xpath == "/bookstore/categories[@code='01']"
    }

    def 'Get delta composite query classifies mixed results correctly'() {
        given: 'composite query returns a mix of removed, added, and updated fragments'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', null),
                createProjection('/library', null, '{"library-name":"City Library"}'),
                createProjection('/shop', '{"shop-name":"Old Shop"}', '{"shop-name":"New Shop"}')
            ]
        when: 'delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'three delta reports are returned with correct actions'
            result.size() == 3
            result[0].action == 'remove'
            result[0].xpath == '/bookstore'
            result[1].action == 'create'
            result[1].xpath == '/library'
            result[2].action == 'replace'
            result[2].xpath == '/shop'
    }

    def 'Get delta with updated fragment having identical leaf values is skipped'() {
        given: 'composite query returns fragment where source and target attributes are textually different but semantically equal'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"Easons"}')
            ]
        when: 'delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'no delta report is generated since leaves are identical'
            result.isEmpty()
    }

    def 'Get delta correctly classifies added fragment with null attributes'() {
        given: 'composite query returns a fragment that exists only in target with null attributes'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjectionWithIds('/bookstore', null, 2L, null, null)
            ]
        when: 'delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'a create delta report is generated'
            result.size() == 1
            result[0].action == 'create'
            result[0].xpath == '/bookstore'
    }

    def 'Get delta correctly classifies removed fragment with null attributes'() {
        given: 'composite query returns a fragment that exists only in source with null attributes'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjectionWithIds('/bookstore', 1L, null, null, null)
            ]
        when: 'delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'a remove delta report is generated'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == '/bookstore'
    }

    def 'Get grouped delta groups removed siblings under parent xpath'() {
        given: 'composite query returns multiple removed fragments under the same parent'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='02']/books[@title='Book A']", '{"lang":"en","price":699,"title":"Book A"}', null),
                createProjection("/bookstore/categories[@code='02']/books[@title='Book B']", '{"lang":"en","price":639,"title":"Book B"}', null)
            ]
        when: 'grouped delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getGroupedDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'a single grouped remove delta report is generated'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == "/bookstore/categories[@code='02']"
        and: 'source data contains both books merged under the node name'
            result[0].sourceData.containsKey('books')
            result[0].sourceData['books'].size() == 2
    }

    def 'Get grouped delta groups added siblings under parent xpath'() {
        given: 'composite query returns multiple added fragments under the same parent'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='02']/books[@title='New Book']", null, '{"lang":"en","price":699,"title":"New Book"}')
            ]
        when: 'grouped delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getGroupedDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'a single grouped create delta report is generated'
            result.size() == 1
            result[0].action == 'create'
            result[0].xpath == "/bookstore/categories[@code='02']"
        and: 'target data contains the added book under the node name'
            result[0].targetData.containsKey('books')
            result[0].targetData['books'].size() == 1
    }

    def 'Get grouped delta keeps replace entries individually'() {
        given: 'composite query returns an updated fragment'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='01']/books[@title='Far Horizons']", '{"title":"Far Horizons","authors":"Dan Simmons"}', '{"title":"Far Horizons","authors":"Melhua"}')
            ]
        when: 'grouped delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getGroupedDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'a single replace delta report is generated at the individual xpath'
            result.size() == 1
            result[0].action == 'replace'
            result[0].xpath == "/bookstore/categories[@code='01']/books[@title='Far Horizons']"
    }

    def 'Get grouped delta with mixed actions groups correctly'() {
        given: 'composite query returns a mix of removed, added, and updated fragments'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjection("/bookstore/categories[@code='02']/books[@title='Book A']", '{"lang":"en","title":"Book A"}', null),
                createProjection("/bookstore/categories[@code='02']/books[@title='Book B']", '{"lang":"en","title":"Book B"}', null),
                createProjection("/bookstore/categories[@code='02']/books[@title='New Book']", null, '{"lang":"en","title":"New Book"}'),
                createProjection("/bookstore/categories[@code='01']/books[@title='Far Horizons']", '{"title":"Far Horizons","authors":"Dan Simmons"}', '{"title":"Far Horizons","authors":"Melhua"}')
            ]
        when: 'grouped delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getGroupedDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'three delta reports are returned: replace, grouped remove, and grouped create'
            result.size() == 3
        and: 'the replace entry is individual'
            result[0].action == 'replace'
            result[0].xpath == "/bookstore/categories[@code='01']/books[@title='Far Horizons']"
        and: 'the removed entries are grouped under parent'
            result[1].action == 'remove'
            result[1].xpath == "/bookstore/categories[@code='02']"
            result[1].sourceData['books'].size() == 2
        and: 'the added entries are grouped under parent'
            result[2].action == 'create'
            result[2].xpath == "/bookstore/categories[@code='02']"
            result[2].targetData['books'].size() == 1
    }

    def 'Get grouped delta returns empty when no differences'() {
        given: 'composite query returns no fragments'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> []
        when: 'grouped delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getGroupedDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'an empty delta report is returned'
            result.isEmpty()
    }

    def 'Get grouped delta with OMIT_DESCENDANTS uses exact xpath query'() {
        given: 'fragments returned by composite exact xpath repository method'
            mockFragmentRepository.findAllDeltaFragmentsExactXpath(10, 20, '/bookstore') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)
            ]
        when: 'grouped delta is requested with OMIT_DESCENDANTS'
            def result = objectUnderTest.getGroupedDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', OMIT_DESCENDANTS)
        then: 'delta report is returned correctly'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == '/bookstore'
    }

    def 'Get grouped delta with DIRECT_CHILDREN_ONLY uses direct children query'() {
        given: 'fragments returned by composite direct children repository method'
            mockFragmentRepository.findAllDeltaFragmentsWithDirectChildren(10, 20, '/bookstore') >> [
                createProjection("/bookstore/categories[@code='02']/books[@title='Book A']", '{"lang":"en","title":"Book A"}', null),
                createProjection("/bookstore/categories[@code='02']/books[@title='Book B']", '{"lang":"en","title":"Book B"}', null)
            ]
        when: 'grouped delta is requested with DIRECT_CHILDREN_ONLY'
            def result = objectUnderTest.getGroupedDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', DIRECT_CHILDREN_ONLY)
        then: 'removed siblings are grouped under parent'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == "/bookstore/categories[@code='02']"
            result[0].sourceData['books'].size() == 2
    }

    def 'Get grouped delta skips update when source and target attributes are identical'() {
        given: 'composite query returns fragment where source and target attributes are semantically equal'
            mockFragmentRepository.findAllDeltaFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"Easons"}')
            ]
        when: 'grouped delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getGroupedDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'no delta report is generated since leaves are identical'
            result.isEmpty()
    }

    private static DeltaProjectionDto createProjection(String xpath, String sourceAttributes, String targetAttributes) {
        def sourceId = sourceAttributes != null ? 1L : null
        def targetId = targetAttributes != null ? 2L : null
        return new DeltaProjectionDto(xpath, sourceId, targetId, sourceAttributes, targetAttributes)
    }

    private static DeltaProjectionDto createProjectionWithIds(String xpath, Long sourceId, Long targetId, String sourceAttributes, String targetAttributes) {
        return new DeltaProjectionDto(xpath, sourceId, targetId, sourceAttributes, targetAttributes)
    }
}
