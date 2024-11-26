/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada.
 *  Modifications Copyright (C) 2021-2023 Nordix Foundation.
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
package org.onap.cps.ri

import org.hibernate.exception.ConstraintViolationException
import org.onap.cps.ri.models.DataspaceEntity
import org.onap.cps.ri.models.SchemaSetEntity
import org.onap.cps.ri.repository.DataspaceRepository
import org.onap.cps.ri.repository.ModuleReferenceRepository
import org.onap.cps.ri.repository.SchemaSetRepository
import org.onap.cps.ri.repository.YangResourceRepository
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.api.exceptions.DuplicatedYangResourceException
import org.onap.cps.api.model.ModuleReference
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.EnableRetry
import spock.lang.Specification

import java.sql.SQLException

@SpringBootTest(classes=[CpsModulePersistenceServiceImpl])
@EnableRetry
class CpsModulePersistenceServiceConcurrencySpec extends Specification {

    @Autowired
    CpsModulePersistenceService objectUnderTest

    @SpringBean
    DataspaceRepository dataspaceRepository = Mock()

    @SpringBean
    YangResourceRepository yangResourceRepository = Mock()

    @SpringBean
    SchemaSetRepository schemaSetRepository = Mock()

    @SpringBean
    CpsAdminPersistenceService cpsAdminPersistenceService = Mock()

    @SpringBean
    ModuleReferenceRepository moduleReferenceRepository = Mock()

    def NEW_RESOURCE_NAME = 'some new resource'
    def NEW_RESOURCE_CONTENT = 'module stores {\n' +
            '    yang-version 1.1;\n' +
            '    namespace "org:onap:ccsdk:sample";\n' +
            '}'

    def newYangResourcesNameToContentMap = [(NEW_RESOURCE_NAME):NEW_RESOURCE_CONTENT]

    def yangResourceChecksum = 'b13faef573ed1374139d02c40d8ce09c80ea1dc70e63e464c1ed61568d48d539'

    def yangResourceChecksumDbConstraint = 'yang_resource_checksum_key'

    def sqlExceptionMessage = String.format('(checksum)=(%s)', yangResourceChecksum)

    def checksumIntegrityException = new DataIntegrityViolationException("checksum integrity exception",
                 new ConstraintViolationException('', new SQLException(sqlExceptionMessage), yangResourceChecksumDbConstraint))

    def 'Store new schema set, maximum retries.'() {
        given: 'no pre-existing schemaset in database'
            dataspaceRepository.getByName(_) >> new DataspaceEntity()
            yangResourceRepository.findAllByChecksumIn(_) >> Collections.emptyList()
        when: 'a new schemaset is stored'
            objectUnderTest.storeSchemaSet('some dataspace', 'some new schema set', newYangResourcesNameToContentMap)
        then: 'a duplicated yang resource exception is thrown '
            thrown(DuplicatedYangResourceException)
        and: 'the system will attempt to save the data 5 times (because checksum integrity exception is thrown each time)'
            5 * yangResourceRepository.saveAll(_) >> { throw checksumIntegrityException }
    }

    def 'Store new schema set, succeed on third attempt.'() {
        given: 'no pre-existing schemaset in database'
            dataspaceRepository.getByName(_) >> new DataspaceEntity()
            yangResourceRepository.findAllByChecksumIn(_) >> Collections.emptyList()
        when: 'a new schemaset is stored'
            objectUnderTest.storeSchemaSet('some dataspace', 'some new schema set', newYangResourcesNameToContentMap)
        then: 'no exception is thrown '
            noExceptionThrown()
        and: 'the system will attempt to save the data 2 times with checksum integrity exception but then succeed'
            2 * yangResourceRepository.saveAll(_) >> { throw checksumIntegrityException }
            1 * yangResourceRepository.saveAll(_) >> []
    }

    def 'Store schema set using modules, maximum retries.'() {
        given: 'map of new modules, a list of existing modules, module reference'
            def mapOfNewModules = [newModule1: 'module newmodule { yang-version 1.1; revision "2021-10-12" { } }']
            def moduleReferenceForExistingModule = new ModuleReference("test","2021-10-12")
            def listOfExistingModulesModuleReference = [moduleReferenceForExistingModule]
        and: 'no pre-existing schemaset in database'
            dataspaceRepository.getByName(_) >> new DataspaceEntity()
            yangResourceRepository.findAllByChecksumIn(_) >> Collections.emptyList()
        when: 'a new schemaset is stored from a module'
            objectUnderTest.storeSchemaSetFromModules('some dataspace', 'some new schema set' , mapOfNewModules, listOfExistingModulesModuleReference)
        then: 'a duplicated yang resource exception is thrown '
            thrown(DuplicatedYangResourceException)
        and: 'the system will attempt to save the data 5 times (because checksum integrity exception is thrown each time)'
            5 * yangResourceRepository.saveAll(_) >> { throw checksumIntegrityException }
    }

    def 'Store schema set using modules, succeed on third attempt.'() {
        given: 'map of new modules, a list of existing modules, module reference'
            def mapOfNewModules = [newModule1: 'module newmodule { yang-version 1.1; revision "2021-10-12" { } }']
            def moduleReferenceForExistingModule = new ModuleReference("test","2021-10-12")
            def listOfExistingModulesModuleReference = [moduleReferenceForExistingModule]
        and: 'no pre-existing schemaset in database'
            def dataspaceEntity = new DataspaceEntity()
            dataspaceRepository.getByName(_) >> new DataspaceEntity()
            yangResourceRepository.findAllByChecksumIn(_) >> Collections.emptyList()
            yangResourceRepository.getResourceIdsByModuleReferences(_) >> []
        and: 'can retrieve schemaset details after storing it'
            def schemaSetEntity = new SchemaSetEntity()
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, 'new schema set') >> schemaSetEntity
        when: 'a new schemaset is stored from a module'
            objectUnderTest.storeSchemaSetFromModules('some dataspace', 'new schema set' , mapOfNewModules, listOfExistingModulesModuleReference)
        then: 'no exception is thrown '
            noExceptionThrown()
        and: 'the system will attempt to save the data 2 times with checksum integrity exception but then succeed'
            2 * yangResourceRepository.saveAll(_) >> { throw checksumIntegrityException }
            1 * yangResourceRepository.saveAll(_) >> []
    }

}
