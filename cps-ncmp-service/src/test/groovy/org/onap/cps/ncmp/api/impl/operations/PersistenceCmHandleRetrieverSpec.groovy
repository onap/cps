package org.onap.cps.ncmp.api.impl.operations

import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.models.PersistenceCmHandle
import spock.lang.Shared

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class PersistenceCmHandleRetrieverSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new PersistenceCmHandleRetriever(mockCpsDataService)

    def cmHandleId = 'some cm handle'
    def leaves = ["dmi-service-name":"common service name","dmi-data-service-name":"data service name","dmi-model-service-name":"model service name"]
    def xpath = "/dmi-registry/cm-handles[@id='some cm handle']"

    @Shared
    def childDataNodesForCmHandleProperties = [new DataNode(leaves: ["name":"name1","value":"value1"]),
                                               new DataNode(leaves: ["name":"name2","value":"value2"])]

    def "Retrieve CmHandle using datanode #scenario."() {
        given: 'the cps data service returns a data node from the dmi registry'
            def dataNode = new DataNode(childDataNodes:childDataNodes, leaves: leaves)
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', xpath, INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'retrieving the persisted cm handle'
            def result = objectUnderTest.retrieveCmHandleDmiServiceNameAndProperties(cmHandleId)
        then: 'the result has the correct id and service names'
            result.id == cmHandleId
            result.dmiServiceName == 'common service name'
            result.dmiDataServiceName == 'data service name'
            result.dmiModelServiceName == 'model service name'
        and: 'the expected additional properties'
            result.additionalProperties == expectedCmHandleProperties
        where: 'the following parameters are used'
            scenario                        | childDataNodes                      || expectedCmHandleProperties
            'without additional properties' | []                                  || []
            'with additional properties'    | childDataNodesForCmHandleProperties || [new PersistenceCmHandle.AdditionalOrPublicProperty("name1", "value1"),
                                                                                      new PersistenceCmHandle.AdditionalOrPublicProperty("name2", "value2")]
    }
}
