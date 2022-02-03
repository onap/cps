/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */
package org.onap.cps.spi.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.exception.ConstraintViolationException
import org.mockito.InjectMocks
import org.mockito.Mock
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.entities.DataspaceEntity
import org.onap.cps.spi.entities.YangResourceEntity
import org.onap.cps.spi.exceptions.DuplicatedYangResourceException
import org.onap.cps.spi.model.ExtendedModuleReference
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.FragmentRepository
import org.onap.cps.spi.repository.SchemaSetRepository
import org.onap.cps.spi.repository.YangResourceRepository
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.jdbc.Sql
import spock.lang.Shared
import spock.lang.Specification

import java.sql.SQLException

@SpringBootTest
class CpsModulePersistenceServiceConcurrencySpec extends Specification {

    @Autowired
    CpsModulePersistenceService objectUnderTest

    @Autowired
    AnchorRepository anchorRepository

    @Autowired
    SchemaSetRepository schemaSetRepository

    @Autowired
    CpsAdminPersistenceService cpsAdminPersistenceService

    @Autowired
    DataspaceRepository dataspaceRepository

    @Autowired
    FragmentRepository fragmentRepository

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())


    static final String DATASPACE_NAME = 'DATASPACE-001'

    static final String SCHEMA_SET_NAME_NEW = 'SCHEMA-SET-NEW'

    static final String NEW_RESOURCE_NAME = 'some new resource'
    static final String NEW_RESOURCE_CONTENT = 'module stores {\n' +
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

    def newYangResourcesNameToContentMap = [(NEW_RESOURCE_NAME):NEW_RESOURCE_CONTENT]

    @Shared
    yangResourceChecksum = 'b13faef573ed1374139d02c40d8ce09c80ea1dc70e63e464c1ed61568d48d539'

    @Shared
    yangResourceChecksumDbConstraint = 'yang_resource_checksum_key'

    @Shared
    sqlExceptionMessage = String.format('(checksum)=(%s)', yangResourceChecksum)

    @Shared
    checksumIntegrityException =
           new DataIntegrityViolationException("checksum integrity exception",
                 new ConstraintViolationException('', new SQLException(sqlExceptionMessage), yangResourceChecksumDbConstraint))

    @SpringBean
    YangResourceRepository yangResourceRepositoryMock = Mock()

    @SpringBean
    DataspaceRepository dataspaceRepositoryMock = Mock()


    def 'Store new schema set error.'() {
        given:
            dataspaceRepositoryMock.getByName(_) >> new DataspaceEntity()
            yangResourceRepositoryMock.findAllByChecksumIn(_) >> Collections.emptyList()
        when: 'a new schemaset is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, newYangResourcesNameToContentMap)
        then: 'the schema set is persisted correctly'
            5 * yangResourceRepositoryMock.saveAll(_) >> { throw checksumIntegrityException }
            def e = thrown(DuplicatedYangResourceException)
    }
}
