package org.onap.cps.integration.functional.ncmp

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobResultService
import org.springframework.beans.factory.annotation.Autowired

class DataJobResultServiceSpec extends CpsIntegrationSpecBase {

    @Autowired
    DataJobResultService dataJobResultService;

    def 'Get the status of a data job from DMI.'() {
        given: 'the required data about the data job'
            def authorization = 'my authorization header'
            def dmiServiceName = DMI1_URL
            def dataProducerId = 'some-data-producer-id'
            def dataProducerJobId = 'some-data-producer-job-id'
            def destination = 'some-destination'
        when: 'the data job status checked'
            def result = dataJobResultService.getDataJobResult(authorization, dmiServiceName, dataProducerJobId, dataProducerId, destination)
        then: 'the status is that defined in the mock service.'
            assert result != null
    }
}
