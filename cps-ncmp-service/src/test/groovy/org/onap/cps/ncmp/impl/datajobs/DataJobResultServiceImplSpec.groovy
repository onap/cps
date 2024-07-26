package org.onap.cps.ncmp.impl.datajobs

import org.onap.cps.ncmp.impl.dmi.DmiProperties
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.dmi.UrlTemplateParameters
import reactor.core.publisher.Mono
import spock.lang.Specification

class DataJobResultServiceImplSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockDmiProperties = Mock(DmiProperties)
    def objectUnderTest = new DataJobResultServiceImpl(mockDmiRestClient, mockDmiProperties)

    def setup() {
        mockDmiProperties.dmiBasePath >> 'dmi'
    }

    def 'Retrieve data job result.'() {
        given: 'the required parameters for querying'
            def dmiServiceName = 'some-dmi-service'
            def requestId = 'some-request-id'
            def dataProducerJobId = 'some-data-producer-job-id'
            def dataProducerId = 'some-data-producer-id'
            def authorization = 'my authorization header'
            def destination = 'some-destination'
            def urlParams = new UrlTemplateParameters('some-dmi-service/dmi/v1/dataJob/{requestId}/dataProducerJob/{dataProducerJobId}/result?dataProducerId={dataProducerId}&destination={destination}', ['dataProducerJobId':'some-data-producer-job-id', 'dataProducerId':'some-data-producer-id', 'requestId':'some-request-id', 'destination': 'some-destination'])
        and: 'the rest client returns the result for the given parameters'
            mockDmiRestClient.getDataJobResult(urlParams, authorization) >> Mono.just(new Object())
        when: 'the job status is queried'
            def result = objectUnderTest.getDataJobResult(authorization, dmiServiceName, requestId, dataProducerJobId, dataProducerId, destination)
        then: 'the result from the rest client is returned'
            assert result != null
    }
}
