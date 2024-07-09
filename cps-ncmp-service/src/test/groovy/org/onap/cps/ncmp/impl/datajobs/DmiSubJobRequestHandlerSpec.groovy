package org.onap.cps.ncmp.impl.datajobs

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.data.models.OperationType
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DmiWriteOperation
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse
import org.onap.cps.ncmp.impl.dmi.DmiProperties
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.models.RequiredDmiService
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class DmiSubJobRequestHandlerSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockDmiProperties = Mock(DmiProperties)
    def static NO_AUTH = null
    def objectUnderTest = new DmiSubJobRequestHandler(mockDmiRestClient, mockDmiProperties, jsonObjectMapper)

    def 'Send a sub-job request to the DMI Plugin.'() {
        given: 'a data job id, metadata and a map of producer keys and write operations to create a request'
            def dataJobId = 'some-job-id'
            def dataJobMetadata = new DataJobMetadata('', '', '')
            def dmiWriteOperation = new DmiWriteOperation('', '', '', null, '', [:])
            def dmiWriteOperationsPerProducerKey = [new ProducerKey('', ''): [dmiWriteOperation]]
            def response = new ResponseEntity<>(new SubJobWriteResponse('my-sub-job-id', '', ''), HttpStatus.OK)
        when: 'sending request to DMI invoked'
            objectUnderTest.sendRequestsToDmi(dataJobId, dataJobMetadata, dmiWriteOperationsPerProducerKey)
        then: 'the dmi rest client is called'
            1 * mockDmiRestClient.synchronousPostOperationWithJsonData(RequiredDmiService.DATA, _, _, OperationType.CREATE, NO_AUTH) >> response
        and: 'the result contains the expected sub-job write responses'
            def result = response.body
            assert result.subJobId() == 'my-sub-job-id'
    }
}
