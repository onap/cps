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

import io.netty.handler.timeout.TimeoutException
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

class ProvMnSExtensionsControllerSpec extends ProvMnSControllerBaseSpec {

    def 'Post data.'() {
        given: 'ProvMnSExtensions url'
            def provMnSExtensionsUrl = "$provMnSExtensionsBasePath/v1alpha1/actions/myClass=id1/someAction"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/myClass=id1')
        when: 'post request is performed'
            def response = mvc.perform(post(provMnSExtensionsUrl).contentType(MediaType.APPLICATION_JSON)
                    .content('{"customProperties":{}, "input":{}}')).andReturn().response
        then: 'response status is expected (OK)'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Post data with #scenario.'() {
        given: 'ProvMnS Extensions url'
            def provMnSExtensionsUrl = "$provMnSExtensionsBasePath/v1alpha1/actions/myClass=id1/someAction"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousGetOperation(*_) >> new ResponseEntity<>('Response from DMI service', OK)
        when: 'post request is performed'
            def response = mvc.perform(post(provMnSExtensionsUrl).contentType(MediaType.APPLICATION_JSON)
                    .content('{"customProperties":{}, "input":{}}')).andReturn().response
        then: 'response status as expected'
            assert response.status == expectedHttpStatus.value()
        and: 'the body contains the expected type'
            assert response.contentAsString.contains('"type":"' + expectedType)
        and: 'the body contains the expected title'
            assert response.contentAsString.contains('"title":"' + expectedTitle)
        where: 'following scenario'
            scenario              | yangModelCmHandle           || expectedHttpStatus   | expectedType              | expectedTitle
            'no data producer id' | cmHandleWithoutDataProducer || UNPROCESSABLE_ENTITY | 'SERVER_LIMITATION'       | 'Registered DMI does not support the ProvMnS interface.'
            'cm Handle NOT READY' | cmHandleNotReady            || NOT_ACCEPTABLE       | 'APPLICATION_LAYER_ERROR' | 'ch-1 is not in READY state. Current state: ADVISED'
    }

    def 'Post data attempt with exception: #exceptionDuringProcessing.'() {
        given: 'ProvMnS Extensions url'
            def provMnSExtensionsUrl = "$provMnSExtensionsBasePath/v1alpha1/actions/myClass=id1/someAction"
        and: 'an exception happens during the process (method not relevant)'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> {throw exceptionDuringProcessing}
        when: 'post request is performed'
            def response = mvc.perform(post(provMnSExtensionsUrl).contentType(MediaType.APPLICATION_JSON)
                    .content('{"customProperties":{}, "input":{}}')).andReturn().response
        then: 'response status is #expectedHttpStatus'
            assert response.status == expectedHttpStatus.value()
        and: 'the body contains the expected type'
            assert response.contentAsString.contains('"type":"' + expectedType)
        and: 'the body contains the expected title'
            assert response.contentAsString.contains('"title":"' + expectedTitle)
        where: 'following exceptions occur'
            exceptionDuringProcessing                           || expectedHttpStatus    | expectedType              | expectedTitle
            new NoAlternateIdMatchFoundException('myTarget')    || NOT_FOUND             | 'IE_NOT_FOUND'            | '/myClass=id1 not found'
            new Exception("my message", new TimeoutException()) || GATEWAY_TIMEOUT       | 'APPLICATION_LAYER_ERROR' | 'my message'
            new Exception("my message")                         || INTERNAL_SERVER_ERROR | 'APPLICATION_LAYER_ERROR' | 'my message'
    }

    def 'Post data attempt with invalid URL: #scenario.'() {
        given: 'ProvMnS Extensions url'
            def provMnSExtensionsUrl = "$provMnSExtensionsBasePath/$version/actions/$fdn/someAction"
        when: 'post request is performed'
            def response = mvc.perform(post(provMnSExtensionsUrl).contentType(MediaType.APPLICATION_JSON)
                    .content('{"customProperties":{}, "input":{}}')).andReturn().response
        then: 'response status as expected'
            assert response.status == expectedHttpStatus
        and: 'the body contains the expected 3GPP error data (when applicable)'
            assert response.contentAsString.contains(expectedType)
        where: 'following scenario'
            scenario          | version    | fdn            || expectedHttpStatus || expectedType
            'invalid version' | 'v0'       | 'not relevant' || 404                || NO_CONTENT
            'no fdn'          | 'v1alpha1' | ''             || 422                || '"type":"SERVER_LIMITATION"'
            'fdn without ='   | 'v1alpha1' | 'someClass'    || 422                || '"type":"SERVER_LIMITATION"'
    }
}
