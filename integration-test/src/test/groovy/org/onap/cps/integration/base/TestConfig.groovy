/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.integration.base

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.notification.NotificationService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.impl.CpsAdminPersistenceServiceImpl
import org.onap.cps.spi.impl.CpsDataPersistenceServiceImpl
import org.onap.cps.spi.impl.CpsModulePersistenceServiceImpl
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.FragmentRepository
import org.onap.cps.spi.repository.ModuleReferenceRepository
import org.onap.cps.spi.repository.SchemaSetRepository
import org.onap.cps.spi.repository.YangResourceRepository
import org.onap.cps.spi.utils.SessionManager
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.TimedYangParser
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import spock.lang.Specification

@Configuration
class TestConfig extends Specification{
    @Autowired
    @Lazy
    DataspaceRepository dataspaceRepository

    @Autowired
    @Lazy
    AnchorRepository anchorRepository

    @Autowired
    @Lazy
    SchemaSetRepository schemaSetRepository

    @Autowired
    @Lazy
    YangResourceRepository yangResourceRepository

    @Autowired
    @Lazy
    FragmentRepository fragmentRepository

    @Autowired
    @Lazy
    ModuleReferenceRepository moduleReferenceRepository

    @Autowired
    @Lazy
    JsonObjectMapper jsonObjectMapper

    @Autowired
    @Lazy
    SessionManager stubbedSessionManager

    @Bean
    CpsAdminPersistenceServiceImpl cpsAdminPersistenceService() {
        new CpsAdminPersistenceServiceImpl(dataspaceRepository, anchorRepository, schemaSetRepository, yangResourceRepository)
    }

    @Bean
    CpsDataPersistenceService cpsDataPersistenceService() {
        return (CpsDataPersistenceService) new CpsDataPersistenceServiceImpl(dataspaceRepository, anchorRepository, fragmentRepository, jsonObjectMapper, stubbedSessionManager)
    }

    @Bean
    CpsModulePersistenceService cpsModulePersistenceService() {
        return (CpsModulePersistenceService) new CpsModulePersistenceServiceImpl(yangResourceRepository, schemaSetRepository, dataspaceRepository, cpsAdminPersistenceService(), moduleReferenceRepository)
    }

    @Bean
    JsonObjectMapper jsonObjectMapper() {
        return new JsonObjectMapper(new ObjectMapper())
    }

    @Bean
    NotificationService notificationService() {
        return Stub(NotificationService)
    }

    @Bean
    SessionManager sessionManager() {
        return Stub(SessionManager)
    }

    @Bean
    TimedYangParser timedYangParser() {
        return new TimedYangParser()
    }

    @Bean
    TimedYangTextSchemaSourceSetBuilder textSchemaSourceSetBuilder() {
        return new TimedYangTextSchemaSourceSetBuilder()
    }

}
