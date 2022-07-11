package org.onap.cps.ncmp.api.inventory

import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class InventoryQuerySpec extends Specification {
    def cpsDataPersistenceService = Mock(CpsDataPersistenceService)

    InventoryQuery objectUnderTest = new InventoryQuery(cpsDataPersistenceService)

    def static pNFDemo1 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo1\']', leaves: ['id':'PNFDemo1'])
    def static pNFDemo2 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo2\']', leaves: ['id':'PNFDemo2'])
    def static pNFDemo3 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo3\']', leaves: ['id':'PNFDemo3'])
    def static pNFDemo4 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo4\']', leaves: ['id':'PNFDemo4'])

    def 'Query CmHandles with public properties query pair.'() {
        given: 'the DataNodes queried for a given cpsPath are returned from the persistence service layer.'
            mockResponses()
        when: 'a query is execute (with and without Data)'
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandlePublicProperties(publicPropertyPairs)
        then: 'the correct cm handle data objects are returned'
            returnedCmHandlesWithData.keySet().containsAll(expectedCmHandleIds)
            returnedCmHandlesWithData.keySet().size() == expectedCmHandleIds.size()
        where: 'the following data is used'
            scenario                         | publicPropertyPairs                                                                           || expectedCmHandleIds
            'single property matches'        | ['Contact' : 'newemailforstore@bookstore.com']                                                || ['PNFDemo1', 'PNFDemo2', 'PNFDemo4']
            'public property does not match' | ['wont_match' : 'wont_match']                                                                 || []
            '2 properties, only one match'   | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': 'newemailforstore2@bookstore.com'] || ['PNFDemo4']
            '2 properties, no matches'       | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': '']                                || []
    }

    def 'Query CmHandles using empty public properties query pair.'() {
        when: 'a query on CmHandle public properties is executed using an empty map'
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandlePublicProperties([:])
        then: 'no cm handle data objects are returned'
            returnedCmHandlesWithData.keySet().size() == 0
    }

    def 'Combine two query results where #scenario.'() {
        given: 'two query results in the form of a map of NcmpServiceCmHandles'
        when: 'the query results are combined into a single query result'
            def result = objectUnderTest.combineCmHandleQueries(firstQuery, secondQuery)
        then: 'the returned map consists of the expectedResult'
            result == expectedResult
        where:
            scenario                                                     | firstQuery                                   | secondQuery                                  || expectedResult
            'two queries with unique and non unique entries exist'       | ['PNFDemo1': pNFDemo1, 'PNFDemo2': pNFDemo2] | ['PNFDemo1': pNFDemo1, 'PNFDemo3': pNFDemo3] || ['PNFDemo1': pNFDemo1]
            'the first query contains entries and second query is empty' | ['PNFDemo1': pNFDemo1, 'PNFDemo2': pNFDemo2] | [:]                                          || [:]
            'the second query contains entries and first query is empty' | [:]                                          | ['PNFDemo1': pNFDemo1, 'PNFDemo3': pNFDemo3] || [:]
            'the first query contains entries and second query is null'  | ['PNFDemo1': pNFDemo1, 'PNFDemo2': pNFDemo2] | null                                         || ['PNFDemo1': pNFDemo1, 'PNFDemo2': pNFDemo2]
            'the second query contains entries and first query is null'  | null                                         | ['PNFDemo1': pNFDemo1, 'PNFDemo3': pNFDemo3] || ['PNFDemo1': pNFDemo1, 'PNFDemo3': pNFDemo3]
            'both queries are empty'                                     | [:]                                          | [:]                                          || [:]
            'both queries are null'                                      | null                                         | null                                         || [:]
    }

    void mockResponses() {
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact\' and @value=\'newemailforstore@bookstore.com\']/ancestor::cm-handles', _) >> [pNFDemo1, pNFDemo2, pNFDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'wont_match\' and @value=\'wont_match\']/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact2\' and @value=\'newemailforstore2@bookstore.com\']/ancestor::cm-handles', _) >> [pNFDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact2\' and @value=\'\']/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//state[@cm-handle-state=\'READY\']/ancestor::cm-handles', _) >> [pNFDemo1, pNFDemo3]
        cpsDataPersistenceService.queryDataNodes(_, _, '//state[@cm-handle-state=\'LOCKED\']/ancestor::cm-handles', _) >> [pNFDemo2, pNFDemo4]
    }

}
