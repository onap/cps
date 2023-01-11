package org.onap.cps.integration

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Configuration
class TestConfig {
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

    @Bean
    CpsAdminPersistenceServiceImpl cpsAdminPersistenceService() {
        new CpsAdminPersistenceServiceImpl(dataspaceRepository, anchorRepository, schemaSetRepository, yangResourceRepository)
    }

    @Bean
    CpsDataPersistenceService cpsDataPersistenceService() {
        return (CpsDataPersistenceService) new CpsDataPersistenceServiceImpl(dataspaceRepository, anchorRepository, fragmentRepository, jsonObjectMapper, null)
    } // try remove private constructor

    @Bean
    CpsModulePersistenceService cpsModulePersistenceService() {
        return (CpsModulePersistenceService) new CpsModulePersistenceServiceImpl(yangResourceRepository, schemaSetRepository, dataspaceRepository, cpsAdminPersistenceService(), moduleReferenceRepository)
    }

    @Bean
    JsonObjectMapper jsonObjectMapper() {
        return new JsonObjectMapper(new ObjectMapper())
    }
}