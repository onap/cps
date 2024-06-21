package org.onap.cps.integration.functional

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobService
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.springframework.beans.factory.annotation.Autowired

class NcmpWriteSubJobSpec extends CpsIntegrationSpecBase {

    def DMI_STUB_URL = 'http://localhost:8784'

    @Autowired
    DataJobService dataJobService

    def setup() {
        dmiDispatcher.moduleNamesPerCmHandleId['ch-1'] = ['M1']
        registerCmHandle(DMI_STUB_URL, 'ch-1', NO_MODULE_SET_TAG, '/a/b')
    }

    def cleanup() {
        deregisterCmHandle(DMI_STUB_URL, 'ch-1')
    }

    def 'Create a sub-job write request.'() {
        given: 'the required input data for the write job'
            def dataJobWriteRequest = new DataJobWriteRequest([new WriteOperation('/a/b', '', '', null)])
            def myDataJobMetadata = new DataJobMetadata('', '', '')
            def dataJobId = 'my-data-job-id'
        when: 'the data job service is called'
            def response = dataJobService.writeDataJob(dataJobId, myDataJobMetadata, dataJobWriteRequest)
        then: 'the result has the expected size and return type'
            assert response.size() == 1
            assert response[0].class == SubJobWriteResponse.class
            assert response[0].subJobId() == '1'
            assert response[0].dataProducerId() == 'my-data-producer-id'
            assert response[0].dmiServiceName() == 'some-dmi-service-name'
    }
}
