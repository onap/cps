package org.onap.cps.spi.impl

import org.onap.cps.spi.entities.DataspaceEntity
import org.onap.cps.spi.entities.SchemaSetEntity
import org.onap.cps.spi.entities.YangResourceEntity
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.SchemaSetRepository
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet
import spock.lang.Specification

class CpsModulePersistenceServiceImplTest extends Specification {
    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockSchemaSetRepository= Mock(SchemaSetRepository)
    def schemaSetEntity = Mock(SchemaSetEntity)
    def mockYangResourceEntity = Mock(YangResourceEntity)
    def mockDataspaceEntity = Mock(DataspaceEntity)
    def objectUnderTest = new CpsModulePersistenceServiceImpl()

    def setup() {
        objectUnderTest.dataspaceRepository = mockDataspaceRepository
        objectUnderTest.schemaSetRepository = mockSchemaSetRepository
    }

    def 'Get all yang resources for someDataspace and someSchemaSet.'() {
        when: 'Get yang schema resources twice'
            mockSchemaSetRepository.getByDataspaceAndName(mockDataspaceEntity, 'someSchemaSetName') >> schemaSetEntity
            schemaSetEntity.getYangResources() >> ImmutableSet.of(mockYangResourceEntity)
            mockYangResourceEntity.getName() >> "someYangResourceName"
            mockYangResourceEntity.getContent() >> "someYangResourceContent"
            def result = objectUnderTest.getYangSchemaResources('someDataspace','someSchemaSetName')
            def result1 =objectUnderTest.getYangSchemaResources('someDataspace','someSchemaSetName')
        then: 'The persistence service method is invoked with same parameters only once'
            !result.isEmpty()
            !result1.isEmpty()
            1 * mockDataspaceRepository.getByName('someDataspace') >> mockDataspaceEntity
    }
}
