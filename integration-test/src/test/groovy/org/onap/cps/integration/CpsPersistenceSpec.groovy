package org.onap.cps.integration

import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.ModuleReference
import org.springframework.test.context.jdbc.Sql

class CpsPersistenceSpec extends CpsIntegrationSpecBase{

    def 'Test creation of test data'() {
        when: 'A dataspace, schema set and anchor are persisted'
            createDataspaceSchemaSetAnchor(TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'src/test/resources/data/bookstore.yang', TEST_ANCHOR)
        and: 'data nodes are persisted under the created anchor'
            saveDataNodes(TEST_DATASPACE, TEST_ANCHOR, '/', 'src/test/resources/data/BookstoreDataNodes.json')
        then: 'The dataspace has been persisted successfully'
            cpsAdminService.getDataspace(TEST_DATASPACE).getName() == TEST_DATASPACE
        and: 'The schema set has been persisted successfully'
            cpsModuleService.getSchemaSet(TEST_DATASPACE, BOOKSTORE_SCHEMA_SET).getName() == BOOKSTORE_SCHEMA_SET
        and: 'The anchor has been persisted successfully'
            cpsAdminService.getAnchor(TEST_DATASPACE, TEST_ANCHOR).getName() == TEST_ANCHOR
        and: 'The data nodes have been persisted successfully'
            cpsDataService.getDataNode(TEST_DATASPACE, TEST_ANCHOR, '/bookstore', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS).xpath == '/bookstore'
    }

    def 'Test deletion of all test data'() {
        when: 'delete all from test dataspace method is called'
            deleteAllFromTestDataspace()
        and: 'the test dataspace is deleted'
            cpsAdminService.deleteDataspace(TEST_DATASPACE)
        then: 'there is no test dataspace'
            !cpsAdminService.getAllDataspaces().contains(TEST_DATASPACE)
    }

    def 'Read test for persisted data nodes'() {
        given:'There is a test dataspace created'
            cpsAdminService.createDataspace(TEST_DATASPACE)
        and: 'There is a schema set and anchor for the test dataspace'
            createSchemaSetAnchor(TEST_DATASPACE, 'bookstoreSchemaSet', 'src/test/resources/data/bookstore.yang', TEST_ANCHOR)
        when: 'data is persisted to the database'
            saveDataNodes(TEST_DATASPACE, TEST_ANCHOR, "/", "src/test/resources/data/BookstoreDataNodes.json")
        then: 'the correct data is saved'
            cpsDataService.getDataNode(TEST_DATASPACE, TEST_ANCHOR, '/bookstore', FetchDescendantsOption.OMIT_DESCENDANTS).leaves['bookstore-name'] == 'Easons'
    }
}
