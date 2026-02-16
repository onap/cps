/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.rest.controller

import org.springframework.http.ResponseEntity

import static org.springframework.http.HttpStatus.OK
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.http.MediaType

@org.springframework.test.context.TestPropertySource(properties = ["ncmp.policy-executor.enabled=false"])
class ProvMnSControllerPolicyExecutorDisabledSpec extends ProvMnSControllerBaseSpec {

    def 'Patch request bypasses policy executor when disabled.'() {
        given: 'provMnS url'
            def provmnsUrl = "$provMnSBasePath/v1/managedElement=1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/managedElement=1/myClass=id1')
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousPatchOperation(*_) >> new ResponseEntity<>('content from DMI', OK)
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                    .contentType(patchMediaType)
                    .content(patchJsonBody))
                    .andReturn().response
        then: 'the response is successful'
            assert response.status == OK.value()
        and: 'policy executor was never invoked'
            0 * mockPolicyExecutor._
    }

    def 'Put request bypasses policy executor when disabled.'() {
        given: 'provMnS url'
            def provMnsUrl = "$provMnSBasePath/v1/ManagedElement=1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/ManagedElement=1/myClass=id1')
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousPutOperation(*_) >> new ResponseEntity<>('content from DMI', OK)
        when: 'put data resource request is performed'
            def response = mvc.perform(put(provMnsUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(resourceAsJson))
                    .andReturn().response
        then: 'the response is successful'
            assert response.status == OK.value()
        and: 'policy executor was never invoked'
            0 * mockPolicyExecutor._
    }

    def 'Delete request bypasses policy executor when disabled.'() {
        given: 'provMnS url'
            def provMnsUrl = "$provMnSBasePath/v1/ManagedElement=1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/ManagedElement=1/myClass=id1')
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousDeleteOperation(*_) >> new ResponseEntity<>('content from DMI', OK)
        when: 'Delete data resource request is performed'
            def response = mvc.perform(delete(provMnsUrl)).andReturn().response
        then: 'the response is successful'
            assert response.status == OK.value()
        and: 'policy executor was never invoked'
            0 * mockPolicyExecutor._
    }

}
