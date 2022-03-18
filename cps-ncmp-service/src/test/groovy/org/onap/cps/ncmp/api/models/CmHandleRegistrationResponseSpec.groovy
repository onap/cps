/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 2022 Bell Canada
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

import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.Status
import spock.lang.Specification

class CmHandleRegistrationResponseSpec extends Specification {

    def 'Successful CmHandle Registration Response'() {
        when: 'CMHandle response is created'
            def cmHandleRegistrationResponse = CmHandleRegistrationResponse.createSuccessResponse('cmHandle')
        then: 'the error text should have expected value'
            with(cmHandleRegistrationResponse) {
                assert it.cmHandle == 'cmHandle'
                assert it.status == Status.SUCCESS
            }
        and: 'error details are null'
            cmHandleRegistrationResponse.registrationError == null
            cmHandleRegistrationResponse.errorText == null
    }

    def 'Failed Cm Handle Registration Response: for unexpected exception'() {
        when: 'CMHandle response is created'
            def cmHandleRegistrationResponse =
                CmHandleRegistrationResponse.createFailureResponse('cmHandle', new Exception('unexpected error'))
        then: 'the response is created with expected value'
            with(cmHandleRegistrationResponse) {
                assert it.registrationError == RegistrationError.UNKNOWN_ERROR
                assert it.cmHandle == 'cmHandle'
                assert errorText == 'unexpected error'
            }
    }

    def 'Failed Cm Handle Registration Response: for known error'() {
        when: 'CMHandle response is created'
            def cmHandleRegistrationResponse =
                CmHandleRegistrationResponse.createFailureResponse('cmHandle', RegistrationError.CM_HANDLE_ALREADY_EXIST)
        then: 'the error text should have expected value'
            with(cmHandleRegistrationResponse) {
                assert it.registrationError == RegistrationError.CM_HANDLE_ALREADY_EXIST
                assert it.cmHandle == 'cmHandle'
                assert it.status == Status.FAILURE
                assert errorText == RegistrationError.CM_HANDLE_ALREADY_EXIST.errorText
            }

    }

}
