package org.onap.cps.integration

import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.ModuleReference
import org.springframework.test.context.jdbc.Sql

class CpsPersistenceSpec extends CpsIntegrationSpecBase{

    @Before
    def beforeEach() {
        cpsAdminService.createDataspace(TEST_DATASPACE)
    }

    @AfterEach
    def afterEach() {
        deleteAllFromTestDataspace()
    }

    def 'speed test for multi test feasibility #scenario'() {
        given:'There is a testDataspace created'
        and: 'There is a schema set and anchor for the test dataspace'
            createSchemaSetAnchor(TEST_DATASPACE, 'bookstoreSchemaSet', 'src/test/resources/data/bookstore.yang', TEST_ANCHOR)
        when: 'data is persisted to the database'
            saveDataNodes(TEST_DATASPACE, TEST_ANCHOR, "/", pathToDataNodesAsJson)
        then: 'the correct data is saved'
            cpsDataService.getDataNode(TEST_DATASPACE, TEST_ANCHOR, '/bookstore', FetchDescendantsOption.OMIT_DESCENDANTS).leaves['bookstore-name'] == expectedValue
        where:
            scenario     | pathToDataNodesAsJson                              || expectedValue
            'datanodes1' | 'src/test/resources/data/BookstoreDataNodes.json'  || 'Easons'
//            'datanodes2' | 'src/test/resources/data/BookstoreDataNodes2.json' || 'WHSmith'
//            'datanodes3' | 'src/test/resources/data/BookstoreDataNodes3.json' || 'Dubrays'

    }

}
