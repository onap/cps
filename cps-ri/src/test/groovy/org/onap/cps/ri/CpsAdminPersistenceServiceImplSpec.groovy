/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 Deutsche Telekom AG
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

package org.onap.cps.ri

import org.onap.cps.api.parameters.PaginationOption
import org.onap.cps.ri.models.AnchorEntity
import org.onap.cps.ri.models.DataspaceEntity
import org.onap.cps.ri.models.SchemaSetEntity
import org.onap.cps.ri.repository.AnchorRepository
import org.onap.cps.ri.repository.DataspaceRepository
import org.onap.cps.ri.repository.SchemaSetRepository
import org.onap.cps.spi.CpsAdminPersistenceService
import org.springframework.data.domain.PageRequest
import spock.lang.Specification

class CpsAdminPersistenceServiceImplSpec extends Specification {

    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockAnchorRepository = Mock(AnchorRepository)
    def mockSchemaSetRepository = Mock(SchemaSetRepository)

    CpsAdminPersistenceService objectUnderTest = new CpsAdminPersistenceServiceImpl(
            mockDataspaceRepository, mockAnchorRepository, mockSchemaSetRepository)

    def dataspaceEntity = new DataspaceEntity('some-dataspace')
    def schemaSetEntity = new SchemaSetEntity(id: 1, name: 'some-schema-set', dataspace: dataspaceEntity)
    def anchorEntity = AnchorEntity.builder()
            .name('some-anchor')
            .dataspace(dataspaceEntity)
            .schemaSet(schemaSetEntity)
            .build()
    def schemaSetNames = ["my-schema-set1","my-schema-set2"]

    def setup() {
        mockDataspaceRepository.getByName('some-dataspace') >> dataspaceEntity
        mockSchemaSetRepository.getByDataspaceAndName(dataspaceEntity, 'some-schema-set') >> schemaSetEntity
        mockAnchorRepository.getByDataspaceAndName(dataspaceEntity, 'some-anchor') >> anchorEntity
    }

    def 'Get all anchors for a dataspace.'() {
        given: 'two anchors exist in the dataspace'
            mockAnchorRepository.findAllByDataspace(dataspaceEntity) >> [anchorEntity,
                    AnchorEntity.builder().name('other-anchor').dataspace(dataspaceEntity).schemaSet(schemaSetEntity).build()]
        when: 'all anchors are retrieved'
            def result = objectUnderTest.getAnchors('some-dataspace')
        then: 'both anchors are returned'
            assert result.size() == 2
    }

    def 'Get paginated anchors for a dataspace.'() {
        given: 'two anchors exist in the dataspace'
            mockAnchorRepository.getByDataspace(dataspaceEntity, _ as PageRequest) >> [anchorEntity,
                    AnchorEntity.builder().name('other-anchor').dataspace(dataspaceEntity).schemaSet(schemaSetEntity).build()]
        when: 'anchors are retrieved with pagination (page 1, size 2)'
            def result = objectUnderTest.getAnchors('some-dataspace', new PaginationOption(1, 2))
        then: 'only one anchor is returned'
            assert result.size() == 2
    }

    def 'Get anchors for a dataspace by anchor names.'() {
        given: 'anchors are found by name'
            mockAnchorRepository.findAllByDataspaceAndNameIn(dataspaceEntity, ['some-anchor']) >> [anchorEntity]
        when: 'anchors are retrieved by name'
            def result = objectUnderTest.getAnchors('some-dataspace', ['some-anchor'])
        then: 'the correct anchor is returned'
            assert result.size() == 1
            assert result[0].name == 'some-anchor'
    }

    def 'Get all anchors for a schema set.'() {
        given: 'one anchor exists in the schema set'
            mockAnchorRepository.findAllBySchemaSet(schemaSetEntity) >> [anchorEntity]
        when: 'anchors are retrieved by schema set name'
            def result = objectUnderTest.getAnchorsBySchemaSetName('some-dataspace', 'some-schema-set')
        then: 'the correct anchor is returned'
            assert result.size() == 1
            assert result[0].name == 'some-anchor'
    }

    def 'Get paginated anchors for a schema sets.'() {
        given: 'the repository returns one anchor for the paginated query'
            mockAnchorRepository.getByDataspaceIdAndSchemaSetNameIn(dataspaceEntity, schemaSetNames, _ as PageRequest) >> [anchorEntity]
        when: 'anchors are retrieved with pagination (page 1, size 2)'
            def result = objectUnderTest.getAnchorsBySchemaSetNames('some-dataspace', schemaSetNames,
                    new PaginationOption(1, 2))
        then: 'the correct anchor is returned'
            assert result.size() == 1
            assert result[0].name == 'some-anchor'
    }
}




