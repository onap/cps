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

    def 'Get delta with OMIT_DESCENDANTS uses exact xpath queries for #scenario'() {
        given: 'fragments returned by exact xpath repository methods'
            mockFragmentRepository.findRemovedFragmentsExactXpath(10, 20, '/bookstore') >> removedFragments
            mockFragmentRepository.findAddedFragmentsExactXpath(10, 20, '/bookstore') >> addedFragments
            mockFragmentRepository.findUpdatedFragmentsExactXpath(10, 20, '/bookstore') >> updatedFragments
        when: 'delta is requested with OMIT_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', OMIT_DESCENDANTS)
        then: 'delta report contains expected number of entries'
            result.size() == expectedSize
        and: 'delta report contains expected action'
            result[0].action == expectedAction
        where: 'following data was used'
            scenario  | removedFragments                                                         | addedFragments                                                           | updatedFragments                                                                                  || expectedSize | expectedAction
            'removed' | [createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)]    | []                                                                       | []                                                                                                || 1            | 'remove'
            'added'   | []                                                                       | [createProjection('/bookstore', null, '{"bookstore-name":"New Store"}')] | []                                                                                                || 1            | 'create'
            'updated' | []                                                                       | []                                                                       | [createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"New Store"}')] || 1            | 'replace'
    }

    def 'Get delta with DIRECT_CHILDREN_ONLY uses direct children queries for #scenario'() {
        given: 'fragments returned by direct children repository methods'
            mockFragmentRepository.findRemovedFragmentsWithDirectChildren(10, 20, '/bookstore', '/bookstore') >> removedFragments
            mockFragmentRepository.findAddedFragmentsWithDirectChildren(10, 20, '/bookstore', '/bookstore') >> addedFragments
            mockFragmentRepository.findUpdatedFragmentsWithDirectChildren(10, 20, '/bookstore', '/bookstore') >> updatedFragments
        when: 'delta is requested with DIRECT_CHILDREN_ONLY'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', DIRECT_CHILDREN_ONLY)
        then: 'delta report contains expected number of entries'
            result.size() == expectedSize
        and: 'delta report contains expected action'
            result[0].action == expectedAction
        where: 'following data was used'
            scenario  | removedFragments                                                         | addedFragments                                                           | updatedFragments                                                                                  || expectedSize | expectedAction
            'removed' | [createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)]    | []                                                                       | []                                                                                                || 1            | 'remove'
            'added'   | []                                                                       | [createProjection('/bookstore', null, '{"bookstore-name":"New Store"}')] | []                                                                                                || 1            | 'create'
            'updated' | []                                                                       | []                                                                       | [createProjection('/bookstore', '{"bookstore-name":"Easons"}', '{"bookstore-name":"New Store"}')] || 1            | 'replace'
    }

    def 'Get delta with INCLUDE_ALL_DESCENDANTS uses existing all-descendants queries'() {
        given: 'fragments returned by all-descendants repository methods'
            mockFragmentRepository.findDeltaRemovedFragments(10, 20, '/') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', null)
            ]
            mockFragmentRepository.findDeltaAddedFragments(10, 20, '/') >> []
            mockFragmentRepository.findDeltaUpdatedFragments(10, 20, '/') >> []
        when: 'delta is requested with INCLUDE_ALL_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/', INCLUDE_ALL_DESCENDANTS)
        then: 'delta report is returned correctly'
            result.size() == 1
            result[0].action == 'remove'
            result[0].xpath == '/bookstore'
    }

    def 'Get delta with OMIT_DESCENDANTS returns empty when no exact match'() {
        given: 'no fragments returned by exact xpath repository methods'
            mockFragmentRepository.findRemovedFragmentsExactXpath(10, 20, '/bookstore') >> []
            mockFragmentRepository.findAddedFragmentsExactXpath(10, 20, '/bookstore') >> []
            mockFragmentRepository.findUpdatedFragmentsExactXpath(10, 20, '/bookstore') >> []
        when: 'delta is requested with OMIT_DESCENDANTS'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', OMIT_DESCENDANTS)
        then: 'empty delta report is returned'
            result.isEmpty()
    }

    def 'Get delta with DIRECT_CHILDREN_ONLY includes parent and child changes'() {
        given: 'removed fragments at parent and child level returned by direct children repository methods'
            mockFragmentRepository.findRemovedFragmentsWithDirectChildren(10, 20, '/bookstore', '/bookstore') >> [
                createProjection('/bookstore', '{"bookstore-name":"Easons"}', null),
                createProjection("/bookstore/categories[@code='01']", '{"code":"01","name":"Sci-fi"}', null)
            ]
            mockFragmentRepository.findAddedFragmentsWithDirectChildren(10, 20, '/bookstore', '/bookstore') >> []
            mockFragmentRepository.findUpdatedFragmentsWithDirectChildren(10, 20, '/bookstore', '/bookstore') >> []
        when: 'delta is requested with DIRECT_CHILDREN_ONLY'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors('test-dataspace', 'source-anchor', 'target-anchor', '/bookstore', DIRECT_CHILDREN_ONLY)
        then: 'two delta reports are returned for parent and child'
            result.size() == 2
            result[0].action == 'remove'
            result[0].xpath == '/bookstore'
            result[1].action == 'remove'
            result[1].xpath == "/bookstore/categories[@code='01']"
    }

    def createProjection(String xpath, String sourceAttributes, String targetAttributes) {
        return [getXpath: { xpath }, getSourceAttributes: { sourceAttributes }, getTargetAttributes: { targetAttributes }] as DeltaProjection
    }
}
