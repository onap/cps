/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * Modifications Copyright (C) 2022 Nordix Foundation
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

import org.hibernate.exception.ConstraintViolationException
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.exceptions.DuplicatedYangResourceException
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.ModuleReferenceRepository
import org.onap.cps.spi.repository.SchemaSetRepository
import org.onap.cps.spi.repository.YangResourceRepository
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared
import spock.lang.Specification

import java.sql.SQLException

/**
 * Specification unit test class for CPS module persistence service.
 */
class CpsModulePersistenceServiceSpec extends Specification {

    // Instance to test
    CpsModulePersistenceService objectUnderTest

    // Mocks
    def dataspaceRepositoryMock = Mock(DataspaceRepository)
    def yangResourceRepositoryMock = Mock(YangResourceRepository)
    def schemaSetRepositoryMock = Mock(SchemaSetRepository)
    def cpsAdminPersistenceServiceMock = Mock(CpsAdminPersistenceService)
    def moduleReferenceRepositoryMock = Mock(ModuleReferenceRepository)

    // Constants
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

    // Scenario data
    @Shared
    yangResourceChecksum = 'b13faef573ed1374139d02c40d8ce09c80ea1dc70e63e464c1ed61568d48d539'
    @Shared
    yangResourceChecksumDbConstraint = 'yang_resource_checksum_key'
    @Shared
    sqlExceptionMessage = String.format('(checksum)=(%s)', yangResourceChecksum)
    @Shared
    checksumIntegrityException =
            new DataIntegrityViolationException(
                    "checksum integrity exception",
                    new ConstraintViolationException('', new SQLException(sqlExceptionMessage), yangResourceChecksumDbConstraint))
    @Shared
    anotherIntegrityException = new DataIntegrityViolationException("another integrity exception")

    def setup() {
        objectUnderTest = new CpsModulePersistenceServiceImpl(yangResourceRepositoryMock, schemaSetRepositoryMock,
            dataspaceRepositoryMock, cpsAdminPersistenceServiceMock, moduleReferenceRepositoryMock)
    }

    def 'Store schema set error scenario: #scenario.'() {
        given: 'no yang resource are currently saved'
            yangResourceRepositoryMock.findAllByChecksumIn(_) >> Collections.emptyList()
        and: 'persisting yang resource raises db constraint exception (in case of concurrent requests for example)'
            yangResourceRepositoryMock.saveAll(_) >> { throw dbException }
        when: 'attempt to store schema set '
            def newYangResourcesNameToContentMap = [(yangResourceName):yangResourceContent]
            objectUnderTest.storeSchemaSet('my-dataspace', 'my-schema-set', newYangResourcesNameToContentMap)
        then: 'an #expectedThrownException is thrown'
            def e = thrown(expectedThrownException)
            e.getMessage().contains(expectedThrownExceptionMessage)
        where: 'the following data is used'
            scenario                | dbException                || expectedThrownException | expectedThrownExceptionMessage
            'checksum data failure' | checksumIntegrityException || DuplicatedYangResourceException | yangResourceChecksum
            'other data failure'    | anotherIntegrityException  || DataIntegrityViolationException | 'another integrity exception'
    }

}
