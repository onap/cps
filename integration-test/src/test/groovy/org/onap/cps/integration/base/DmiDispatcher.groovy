/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.base

import static org.onap.cps.integration.base.CpsIntegrationSpecBase.readResourceDataFile

import org.springframework.http.HttpHeaders
import java.util.regex.Matcher
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

/**
 * This class simulates responses from the DMI server in NCMP integration tests.
 *
 * It is to be used with a MockWebServer, using mockWebServer.setDispatcher(new DmiDispatcher()).
 *
 * It currently implements the following endpoints:
 * - /actuator/health: healthcheck endpoint that responds with 200 OK / {"status":"UP"}
 * - /dmi/v1/ch/{cmHandleId}/modules: returns module references for a CM handle
 * - /dmi/v1/ch/{cmHandleId}/moduleResources: returns modules resources for a CM handle
 *
 * The module resource/reference responses are generated based on the module names in the map moduleNamesPerCmHandleId.
 * To configure the DMI so that CM handle 'ch-1' will have modules 'M1' and 'M2', you may use:
 *   dmiDispatcher.moduleNamesPerCmHandleId.put('ch-1', ['M1', 'M2']);
 *
 * To simulate the DMI not being available, the boolean isAvailable may be set to false, in which case the mock server
 * will always respond with 503 Service Unavailable.
 */
class DmiDispatcher extends Dispatcher {

    static final MODULE_REFERENCES_RESPONSE_TEMPLATE = readResourceDataFile('mock-dmi-responses/moduleReferencesTemplate.json')
    static final MODULE_RESOURCES_RESPONSE_TEMPLATE = readResourceDataFile('mock-dmi-responses/moduleResourcesTemplate.json')

    def isAvailable = true

    Map<String, List<String>> moduleNamesPerCmHandleId = [:]

    @Override
    MockResponse dispatch(RecordedRequest request) {
        if (!isAvailable) {
            return new MockResponse().setResponseCode(HttpStatus.SERVICE_UNAVAILABLE.value())
        }
        switch (request.path) {
            case ~/^\/dmi\/v1\/ch\/(.*)\/modules$/:
                def cmHandleId = Matcher.lastMatcher[0][1]
                return getModuleReferencesResponse(cmHandleId)

            case ~/^\/dmi\/v1\/ch\/(.*)\/moduleResources$/:
                def cmHandleId = Matcher.lastMatcher[0][1]
                return getModuleResourcesResponse(cmHandleId)

            default:
                throw new IllegalArgumentException('Mock DMI does not handle path ' + request.path)
        }
    }

    private getModuleReferencesResponse(cmHandleId) {
        def moduleReferences = '{"schemas":[' + getModuleNamesForCmHandle(cmHandleId).collect {
            MODULE_REFERENCES_RESPONSE_TEMPLATE.replaceAll("<MODULE_NAME>", it)
        }.join(',') + ']}'
        return mockOkResponseWithBody(moduleReferences)
    }

    private getModuleResourcesResponse(cmHandleId) {
        def moduleResources = '[' + getModuleNamesForCmHandle(cmHandleId).collect {
            MODULE_RESOURCES_RESPONSE_TEMPLATE.replaceAll("<MODULE_NAME>", it)
        }.join(',') + ']'
        return mockOkResponseWithBody(moduleResources)
    }

    private getModuleNamesForCmHandle(cmHandleId) {
        if (!moduleNamesPerCmHandleId.containsKey(cmHandleId)) {
            throw new IllegalArgumentException('Mock DMI has no modules configured for ' + cmHandleId)
        }
        return moduleNamesPerCmHandleId.get(cmHandleId)
    }

    private static mockOkResponseWithBody(responseBody) {
        return new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(responseBody)
    }
}
