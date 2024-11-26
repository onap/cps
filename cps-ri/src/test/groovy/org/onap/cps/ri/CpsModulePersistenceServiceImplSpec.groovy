/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * Modifications Copyright (C) 2022-2023 Nordix Foundation
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

import org.hibernate.exception.ConstraintViolationException
import org.onap.cps.ri.models.SchemaSetEntity
import org.onap.cps.ri.repository.DataspaceRepository
import org.onap.cps.ri.repository.ModuleReferenceRepository
import org.onap.cps.ri.repository.SchemaSetRepository
import org.onap.cps.ri.repository.YangResourceRepository
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.api.exceptions.DuplicatedYangResourceException
import org.onap.cps.api.model.ModuleReference
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

import java.sql.SQLException

/**
 * Specification unit test class for CPS module persistence service.
 */
class CpsModulePersistenceServiceImplSpec extends Specification {

    CpsModulePersistenceService objectUnderTest

    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockYangResourceRepository = Mock(YangResourceRepository)
    def mockSchemaSetRepository = Mock(SchemaSetRepository)
    def mockModuleReferenceRepository = Mock(ModuleReferenceRepository)

    def yangResourceName = 'my-yang-resource-name'
    def yangResourceContent = 'module stores {\n' +
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

    static yangResourceChecksum = 'b13faef573ed1374139d02c40d8ce09c80ea1dc70e63e464c1ed61568d48d539'
    static yangResourceChecksumDbConstraint = 'yang_resource_checksum_key'
    static sqlExceptionMessage = String.format('(checksum)=(%s)', yangResourceChecksum)
    static checksumIntegrityException =  new DataIntegrityViolationException('checksum integrity exception',
                    new ConstraintViolationException('', new SQLException(sqlExceptionMessage), yangResourceChecksumDbConstraint))
    static checksumIntegrityExceptionWithoutChecksum =  new DataIntegrityViolationException('checksum integrity exception',
                    new ConstraintViolationException('', new SQLException('no checksum'), yangResourceChecksumDbConstraint))
    static otherIntegrityException = new DataIntegrityViolationException('another integrity exception')

    def setup() {
        objectUnderTest = new CpsModulePersistenceServiceImpl(mockYangResourceRepository, mockSchemaSetRepository,
            mockDataspaceRepository, mockModuleReferenceRepository)
    }

    def 'Store schema set error scenario: #scenario.'() {
        given: 'no yang resource are currently saved'
            mockYangResourceRepository.findAllByChecksumIn(_ as Collection<String>) >> Collections.emptyList()
        and: 'persisting yang resource raises db constraint exception (in case of concurrent requests for example)'
            mockYangResourceRepository.saveAll(_) >> { throw dbException }
        when: 'attempt to store schema set '
            def newYangResourcesNameToContentMap = [(yangResourceName):yangResourceContent]
            objectUnderTest.storeSchemaSet('my-dataspace', 'my-schema-set', newYangResourcesNameToContentMap)
        then: 'an #expectedThrownException is thrown'
            def e = thrown(expectedThrownException)
            assert e.getMessage().contains(expectedThrownExceptionMessage)
        where: 'the following data is used'
            scenario                            | dbException                               || expectedThrownException         | expectedThrownExceptionMessage
            'checksum data failure'             | checksumIntegrityException                || DuplicatedYangResourceException | yangResourceChecksum
            'checksum failure without checksum' | checksumIntegrityExceptionWithoutChecksum || DuplicatedYangResourceException | 'no checksum found'
            'other data failure'                | otherIntegrityException                   || DataIntegrityViolationException | 'another integrity exception'
    }

    def 'Upgrade existing schema set'() {
        given: 'old schema has empty yang resource'
            mockYangResourceRepository.findAllByChecksumIn(_ as Collection<String>) >> Collections.emptyList()
        def schemaSetEntity = new SchemaSetEntity(id: 1)
            mockSchemaSetRepository.getByDataspaceAndName(_, _) >> schemaSetEntity
        when: 'schema set update is requested'
            objectUnderTest.updateSchemaSetFromModules('my-dataspace', 'my-schemaset', [:], [new ModuleReference('some module name', 'some revision name')])
        then: 'no exception is thrown '
            noExceptionThrown()
    }

}
