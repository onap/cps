package org.onap.cps.integration.functional.ncmp

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobStatusService
import org.springframework.beans.factory.annotation.Autowired

class DataJobStatusServiceSpec extends CpsIntegrationSpecBase {

    @Autowired
    DataJobStatusService dataJobStatusService

    def 'Get the status of a data job from DMI.'() {
        given: 'the required data about the data job'
            def dmiServiceName = DMI_URL
            def requestId = 'some-request-id'
            def dataProducerJobId = 'some-data-producer-job-id'
            def dataJobId = 'some-da-job-id'
            def authorization = null
        when: 'the data job status checked'
            def result = dataJobStatusService.getDataJobStatus(dmiServiceName, requestId, dataProducerJobId, dataJobId, authorization)
        then: 'the status is that defined in the mock service.'
            assert result == 'status details from mock service'
    }
}
