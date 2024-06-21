package org.onap.cps.integration.functional

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobService
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.springframework.beans.factory.annotation.Autowired

class NcmpWriteSubJobSpec extends CpsIntegrationSpecBase {

    @Autowired
    DataJobService dataJobService

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1']
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-2'] = ['M2']
        dmiDispatcher2.moduleNamesPerCmHandleId['ch-3'] = ['M3']
        registerCmHandle(DMI_URL, 'ch-1', NO_MODULE_SET_TAG, '/a/b')
        registerCmHandle(DMI_URL, 'ch-2', NO_MODULE_SET_TAG, '/a/b/c')
        registerCmHandle(DMI_URL2, 'ch-3', NO_MODULE_SET_TAG, '/a/b/c/d')
    }

    def cleanup() {
        deregisterCmHandle(DMI_URL, 'ch-1')
        deregisterCmHandle(DMI_URL, 'ch-2')
        deregisterCmHandle(DMI_URL2, 'ch-3')
    }

    def 'Create a sub-job write request.'() {
        given: 'the required input data for the write job'
            def dataJobWriteRequest = new DataJobWriteRequest([new WriteOperation('/a/b', '', '', null), new WriteOperation('/a/b/c', '', '', null), new WriteOperation('/a/b/c/d', '', '', null)])
            def myDataJobMetadata = new DataJobMetadata('', '', '')
            def dataJobId = 'my-data-job-id'
        when: 'I send a write job to NCMP with 2 sub-jobs for DMI 1 and 1 subjob for DMI 2'
            def response = dataJobService.writeDataJob(dataJobId, myDataJobMetadata, dataJobWriteRequest)
        then: 'each DMI received the expected sub-jobs'
            assert response.size() == 2
            assert response[0].class == SubJobWriteResponse.class
            assert dmiDispatcher1.receivedSubJobs['my-data-job-id']['data'].collect().size() == 2
            assert dmiDispatcher2.receivedSubJobs['my-data-job-id']['data'].collect().size() == 1
    }
}
