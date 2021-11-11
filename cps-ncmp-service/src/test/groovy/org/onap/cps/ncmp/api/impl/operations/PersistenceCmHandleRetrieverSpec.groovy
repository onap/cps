package org.onap.cps.ncmp.api.impl.operations

import org.onap.cps.api.CpsDataService
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class PersistenceCmHandleRetrieverSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new PersistenceCmHandleRetriever(mockCpsDataService)

    def "Retrieve CmHandle"() {
        given: ''
            def dataNode = new DataNode(childDataNodes:[])
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', "/dmi-registry/cm-handles[@id='some cm handle']", INCLUDE_ALL_DESCENDANTS) >> dataNode
        when:
            def result = objectUnderTest.retrieveCmHandleDmiServiceNameAndProperties('some cm handle')
        then:
            result !=null
    }
}
