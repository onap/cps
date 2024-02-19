/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2023-2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.models

import static org.onap.cps.ncmp.api.NcmpResponseStatus.ALTERNATE_ID_ALREADY_ASSOCIATED
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_ALREADY_EXIST
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR

import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.Status
import spock.lang.Specification
import java.util.stream.Collectors

class CmHandleRegistrationResponseSpec extends Specification {

    def 'Successful cm-handle Registration Response'() {
        when: 'cm-handle response is created'
            def cmHandleRegistrationResponse = CmHandleRegistrationResponse.createSuccessResponse('cmHandle')
        then: 'a success response is returned'
            with(cmHandleRegistrationResponse) {
                assert it.cmHandle == 'cmHandle'
                assert it.status == Status.SUCCESS
            }
        and: 'error details are null'
            cmHandleRegistrationResponse.ncmpResponseStatus == null
            cmHandleRegistrationResponse.errorText == null
    }

    def 'Failed cm-handle Registration Response: for unexpected exception'() {
        when: 'cm-handle response is created for an unexpected exception'
            def cmHandleRegistrationResponse =
                CmHandleRegistrationResponse.createFailureResponse('cmHandle', new Exception('unexpected error'))
        then: 'the response is created with expected value'
            with(cmHandleRegistrationResponse) {
                assert it.ncmpResponseStatus == UNKNOWN_ERROR
                assert it.cmHandle == 'cmHandle'
                assert errorText == 'unexpected error'
            }
    }

    def 'Failed cm-handle Registration Response'() {
        when: 'cm-handle failure response is created'
        def cmHandleRegistrationResponse =
                CmHandleRegistrationResponse.createFailureResponse('cmHandle', CM_HANDLE_ALREADY_EXIST)
        then: 'the response is created with expected value'
        with(cmHandleRegistrationResponse) {
            assert it.ncmpResponseStatus == CM_HANDLE_ALREADY_EXIST
            assert it.cmHandle == 'cmHandle'
            assert it.status == Status.FAILURE
            assert errorText == CM_HANDLE_ALREADY_EXIST.message
        }
    }

    def 'Failed cm-handle Registration with multiple responses.'() {
        when: 'cm-handle failure response is created for 2 xpaths'
            def cmHandleRegistrationResponses =
                CmHandleRegistrationResponse.createFailureResponsesFromXpaths(["somePathWithId[@id='123']", "somePathWithId[@id='456']"], CM_HANDLE_ALREADY_EXIST)
        then: 'the response has the correct cm handle ids'
            assert cmHandleRegistrationResponses.size() == 2
            assert cmHandleRegistrationResponses.stream().map(it -> it.cmHandle).collect(Collectors.toList())
                .containsAll(['123','456'])
    }

    def 'Failed cm-handle Registration with multiple responses with an unexpected xpath.'() {
        when: 'cm-handle failure response is created for one valid and one unexpected xpath'
            def cmHandleRegistrationResponses =
                CmHandleRegistrationResponse.createFailureResponsesFromXpaths(["somePathWithId[@id='123']", "valid/xpath/without-id[@key='123']"], CM_HANDLE_ALREADY_EXIST)
        then: 'the response has only one entry'
            assert cmHandleRegistrationResponses.size() == 1
    }

    def 'Failed cm-handle registration based on cm handle id and registration error'() {
        when: 'the failure response is created with alternate id already associated error code for 2 cm handle ids'
            def cmHandleRegistrationResponses =
                    CmHandleRegistrationResponse.createFailureResponsesFromCmHandleIds(['ch 1','ch 2'], ALTERNATE_ID_ALREADY_ASSOCIATED)
        then: 'the response has two entries with expected status and error'
            assert cmHandleRegistrationResponses.size() == 2
            for (cmHandleRegistrationResponse in cmHandleRegistrationResponses) {
                assert cmHandleRegistrationResponse.status == Status.FAILURE
                assert cmHandleRegistrationResponse.ncmpResponseStatus == ALTERNATE_ID_ALREADY_ASSOCIATED
                assert cmHandleRegistrationResponse.errorText == 'alternate id already associated'
            }
    }

}
