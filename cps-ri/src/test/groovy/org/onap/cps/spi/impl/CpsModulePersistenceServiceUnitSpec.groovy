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

import org.hibernate.exception.ConstraintViolationException
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.exceptions.DuplicatedYangResourceException
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.SchemaSetRepository
import org.onap.cps.spi.repository.YangResourceRepository
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared
import spock.lang.Specification

import java.sql.SQLException

/**
 * Specification unit test class for CPS module persistence service.
 */
class CpsModulePersistenceServiceUnitSpec extends Specification {

    // Instance to test
    CpsModulePersistenceService objectUnderTest

    // Mocks
    def dataspaceRepositoryMock = Mock(DataspaceRepository)
    def yangResourceRepositoryMock = Mock(YangResourceRepository)
    def schemaSetRepositoryMock = Mock(SchemaSetRepository)

    // Constants
    def yangResourceName = 'my-yang-resource-name'
    def yangResourceContent = 'my-yang-resource-content'

    // Scenario data
    @Shared
    yangResourceChecksum = 'ac2352cc20c10467528b2390bbf2d72d48b0319152ebaabcda207786b4a641c2'
    @Shared
    yangResourceChecksumDbConstraint = 'yang_resource_checksum_key'
    @Shared
    checksumIntegrityException =
            new DataIntegrityViolationException(
                    "checksum integrity exception",
                    new ConstraintViolationException('', new SQLException(yangResourceChecksum), yangResourceChecksumDbConstraint))
    @Shared
    anotherIntegrityException = new DataIntegrityViolationException("another integrity exception")

    def setup() {
        objectUnderTest = new CpsModulePersistenceServiceImpl()
        objectUnderTest.dataspaceRepository = dataspaceRepositoryMock
        objectUnderTest.yangResourceRepository = yangResourceRepositoryMock
        objectUnderTest.schemaSetRepository = schemaSetRepositoryMock
    }

    def 'Store schema set error scenario: #scenario.'() {
        given: 'no yang resource are currently saved'
            yangResourceRepositoryMock.findAllByChecksumIn(_) >> Collections.emptyList()
        and: 'persisting yang resource raises db constraint exception (in case of concurrent requests for example)'
            yangResourceRepositoryMock.saveAll(_) >> { throw dbException }
        when: 'attempt to store schema set '
            def newYangResourcesNameToContentMap = [(yangResourceName):yangResourceContent]
            objectUnderTest.storeSchemaSet('my-dataspace', 'my-schema-set', newYangResourcesNameToContentMap)
        then: 'an #expectedException is thrown'
            thrown(expectedThrowmException)
        where: 'the following data is used'
            scenario                | dbException                || expectedThrowmException
            'checksum data failure' | checksumIntegrityException || DuplicatedYangResourceException
            'other data failure'    | anotherIntegrityException  || DataIntegrityViolationException
    }

}
