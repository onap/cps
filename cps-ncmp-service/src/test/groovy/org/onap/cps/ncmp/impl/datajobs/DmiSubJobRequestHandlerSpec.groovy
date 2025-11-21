package org.onap.cps.ncmp.impl.datajobs

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.data.models.OperationType
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DmiWriteOperation
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey
import org.onap.cps.ncmp.impl.dmi.DmiServiceAuthenticationProperties
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.models.RequiredDmiService
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class DmiSubJobRequestHandlerSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockDmiServiceAuthenticationProperties = Mock(DmiServiceAuthenticationProperties)
    def objectUnderTest = new DmiSubJobRequestHandler(mockDmiRestClient, mockDmiServiceAuthenticationProperties, jsonObjectMapper)

    def 'Send a sub-job request to the DMI Plugin.'() {
        given: 'a data job id, metadata and a map of producer keys and write operations to create a request'
            def dataJobId = 'my job id'
            def dataJobMetadata = new DataJobMetadata('my destination', 'my accept type', 'my content type','my policy')
            def dmiWriteOperation = new DmiWriteOperation('my path', 'operation', 'tag', null, 'o1')
            def dmiWriteOperationsPerProducerKey = [(new ProducerKey('dmi1', 'prod1')): [dmiWriteOperation]]
            def authorization = 'my authorization header'
        and: 'the dmi rest client will return a response (for the correct parameters)'
            def responseAsKeyValuePairs = [subJobId:'my-sub-job-id']
            def responseEntity = new ResponseEntity<>(responseAsKeyValuePairs, HttpStatus.OK)
            def expectedJson = '{"destination":"my destination","dataAcceptType":"my accept type","dataContentType":"my content type","jobExecutionPolicy":"my policy","dataProducerId":"prod1","dataJobId":"my job id","data":[{"path":"my path","op":"operation","moduleSetTag":"tag","value":null,"operationId":"o1"}]}'
            mockDmiRestClient.synchronousPostOperation(RequiredDmiService.DATA, _, expectedJson, OperationType.CREATE, authorization) >> responseEntity
        when: 'sending request to DMI invoked'
            objectUnderTest.sendRequestsToDmi(authorization, dataJobId, dataJobMetadata, dmiWriteOperationsPerProducerKey)
        then: 'the result contains the expected sub-job id'
            assert responseEntity.body.get('subJobId') == 'my-sub-job-id'
    }
}
