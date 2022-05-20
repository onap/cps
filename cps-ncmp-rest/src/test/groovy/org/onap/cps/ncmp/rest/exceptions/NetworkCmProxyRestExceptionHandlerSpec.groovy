/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
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

package org.onap.cps.ncmp.rest.exceptions

import groovy.json.JsonSlurper
import org.mapstruct.factory.Mappers
import org.onap.cps.TestUtils
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.impl.exception.DmiRequestException
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException
import org.onap.cps.ncmp.api.impl.exception.ServerNcmpException
import org.onap.cps.ncmp.rest.controller.NcmpRestInputMapper
import org.onap.cps.spi.exceptions.CpsException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.ncmp.rest.exceptions.NetworkCmProxyRestExceptionHandlerSpec.ApiType.NCMP
import static org.onap.cps.ncmp.rest.exceptions.NetworkCmProxyRestExceptionHandlerSpec.ApiType.NCMPINVENTORY
import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest
class NetworkCmProxyRestExceptionHandlerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    NetworkCmProxyDataService mockNetworkCmProxyDataService = Mock()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = Stub()

    @SpringBean
    NcmpRestInputMapper ncmpRestInputMapper = Mappers.getMapper(NcmpRestInputMapper)

    @Value('${rest.api.ncmp-base-path}')
    def basePathNcmp

    @Value('${rest.api.ncmp-inventory-base-path}')
    def basePathNcmpInventory

    def dataNodeBaseEndpointNcmp
    def dataNodeBaseEndpointNcmpInventory

    @Shared
    def sampleErrorMessage = 'some error message'
    @Shared
    def sampleErrorDetails = 'some error details'

    def setup() {
        dataNodeBaseEndpointNcmp = "$basePathNcmp/v1"
        dataNodeBaseEndpointNcmpInventory = "$basePathNcmpInventory/v1"
    }

    def 'Get request with generic #scenario exception returns correct HTTP Status with #scenario'() {
        when: 'an exception is thrown by the service'
            setupTestException(exception, NCMP)
            def response = performTestRequest(NCMP)
        then: 'an HTTP response is returned with correct message and details'
            assertTestResponse(response, expectedErrorCode, expectedErrorMessage, expectedErrorDetails)
        where:
            scenario              | exception                                                        || expectedErrorDetails | expectedErrorMessage | expectedErrorCode
            'CPS'                 | new CpsException(sampleErrorMessage, sampleErrorDetails)         || sampleErrorDetails   | sampleErrorMessage   | INTERNAL_SERVER_ERROR
            'NCMP-server'         | new ServerNcmpException(sampleErrorMessage, sampleErrorDetails)  || null                 | sampleErrorMessage   | INTERNAL_SERVER_ERROR
            'NCMP-client'         | new DmiRequestException(sampleErrorMessage, sampleErrorDetails)  || null                 | sampleErrorMessage   | BAD_REQUEST
            'DataNode Validation' | new DataNodeNotFoundException('myDataspaceName', 'myAnchorName') || null                 | 'DataNode not found' | NOT_FOUND
            'other'               | new IllegalStateException(sampleErrorMessage)                    || null                 | sampleErrorMessage   | INTERNAL_SERVER_ERROR
            'Data Node Not Found' | new DataNodeNotFoundException('myDataspaceName', 'myAnchorName') || 'DataNode not found' | 'DataNode not found' | NOT_FOUND
    }

    def 'Post request with exception returns correct HTTP Status.'() {
        given: 'the service throws data validation exception'
            def exception = new DataValidationException(sampleErrorMessage, sampleErrorDetails)
            setupTestException(exception, NCMPINVENTORY)
        when: 'the HTTP request is made'
            def response = performTestRequest(NCMPINVENTORY)
        then: 'an HTTP response is returned with correct message and details'
            assertTestResponse(response, BAD_REQUEST, sampleErrorMessage, sampleErrorDetails)
    }

    def 'Failing DMI Request - passthrough scenario'() {
        given: 'failing DMI request'
            mockNetworkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle(*_) >> { throw new HttpClientRequestException('Error Message Details NCMP', 'Bad Request from DMI', 400) }
        when: 'the DMI request is executed'
            def response = mvc.perform(get("$dataNodeBaseEndpointNcmp/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore/categories=100"))
                .andReturn().response
        then: 'NCMP service responds with 502 Bad Gateway status'
            response.status == HttpStatus.BAD_GATEWAY.value()
        and: 'the NCMP response also contains the original DMI response details'
            response.contentAsString.contains('400')
            response.contentAsString.contains('Bad Request from DMI')
    }

    def setupTestException(exception, apiType) {
        if (NCMP == apiType) {
            mockNetworkCmProxyDataService.getYangResourcesModuleReferences(*_) >> { throw exception }
        }
        mockNetworkCmProxyDataService.updateDmiRegistrationAndSyncModule(*_) >> { throw exception }
    }

    def performTestRequest(apiType) {
        if (NCMP == apiType) {
            return mvc.perform(get("$dataNodeBaseEndpointNcmp/ch/testCmHandle/modules")).andReturn().response
        }
        def jsonData = TestUtils.getResourceFileContent('dmi_registration_all_singing_and_dancing.json')
        return mvc.perform(post("$dataNodeBaseEndpointNcmpInventory/ch").contentType(MediaType.APPLICATION_JSON).content(jsonData)).andReturn().response
    }

    static void assertTestResponse(response, expectedStatus , expectedErrorMessage , expectedErrorDetails) {
        assert response.status == expectedStatus.value()
        def content = new JsonSlurper().parseText(response.contentAsString)
        assert content['status'].toString().contains(expectedStatus.toString())
        assert content['message'].toString().contains(expectedErrorMessage)
        assert expectedErrorDetails == null || content['details'].toString().contains(expectedErrorDetails)
    }

    enum ApiType {
        NCMP,
        NCMPINVENTORY;
    }
}
