package org.onap.cps.api.impl

import org.onap.cps.spi.DataPersistencyService
import spock.lang.Specification;


class CpServiceImplSpec extends Specification {

    def dataPersistencyService = Mock(DataPersistencyService)
    def objectUnderTest = new CpServiceImpl()

    def setup() {
        // Insert mocked dependencies
        objectUnderTest.dataPersistencyService = dataPersistencyService;
    }

    def 'Storing a json object'() {
        given: 'that the data persistency service returns an id of 123'
            dataPersistencyService.storeJsonStructure(_) >> 123

        when: 'a json structure is stored using the data persistency service'
            def result = objectUnderTest.storeJsonStructure('')

        then: ' the same id is returned'
            result == 123
    }
}
