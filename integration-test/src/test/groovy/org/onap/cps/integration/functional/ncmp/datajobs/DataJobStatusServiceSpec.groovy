package org.onap.cps.integration.functional.ncmp.datajobs

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobStatusService
import org.springframework.beans.factory.annotation.Autowired

class DataJobStatusServiceSpec extends CpsIntegrationSpecBase {

    @Autowired
    DataJobStatusService dataJobStatusService

    def 'Get the status of a data job from DMI.'() {
        given: 'the required data about the data job'
            def dmiServiceName = DMI1_URL
            def dataProducerId = 'some-data-producer-id'
            def dataProducerJobId = 'some-data-producer-job-id'
            def authorization = 'my authorization header'
        when: 'the data job status checked'
            def result = dataJobStatusService.getDataJobStatus(authorization, dmiServiceName, dataProducerId, dataProducerJobId)
        then: 'the status is that defined in the mock service.'
            assert result == '{"status":"status details from mock service"}'
    }
}
