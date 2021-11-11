package org.onap.cps.ncmp.api.impl.operations

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.api.models.PersistenceCmHandle
import spock.lang.Shared
import spock.lang.Specification

abstract class DmiOperationsBaseSpec extends Specification {

    @Shared
    def sampleAdditionalProperty = new PersistenceCmHandle.AdditionalProperty('prop1', 'val1')

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockCmHandlePropertiesRetriever = Mock(PersistenceCmHandleRetriever)
    def objectMapper = new ObjectMapper()
    def persistenceCmHandle = new PersistenceCmHandle()
    def static dmiServiceName = 'some service name'
    def static cmHandleId = 'some cm handle'

    def mockPersistenceCmHandleRetrieval(additionalProperties) {
        persistenceCmHandle.dmiDataServiceName = dmiServiceName
        persistenceCmHandle.dmiServiceName = dmiServiceName
        persistenceCmHandle.additionalProperties = additionalProperties
        persistenceCmHandle.id = cmHandleId
        mockCmHandlePropertiesRetriever.retrieveCmHandleDmiServiceNameAndProperties(cmHandleId) >> persistenceCmHandle
    }
}
