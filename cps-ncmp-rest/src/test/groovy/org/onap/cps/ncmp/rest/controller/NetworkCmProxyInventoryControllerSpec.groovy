/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(NetworkCmProxyInventoryController)
@Import(ObjectMapper)
class NetworkCmProxyInventoryControllerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    NetworkCmProxyDataService mockNetworkCmProxyDataService = Mock()

    @Value('${rest.api.ncmp-inventory-base-path}/v1')
    def ncmpBasePathV1

    def 'Register CM Handle Event' () {
        given: 'jsonData'
            def jsonData = TestUtils.getResourceFileContent('dmi-registration.json')
        when: 'post request is performed'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
            ).andReturn().response
        then: 'the cm handles are registered with the service'
            1 * mockNetworkCmProxyDataService.updateDmiRegistrationAndSyncModule(_)
        and: 'response status is created'
            response.status == HttpStatus.CREATED.value()
    }

    def 'Dmi plugin registration' () {
        given: 'a dmi registration for one cm handle'
            def jsonData = TestUtils.getResourceFileContent('dmi_registration_combined_valid.json' )
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleID : 'example-name')
            def expectedDmiPluginRegistration = new DmiPluginRegistration(
                dmiPlugin: 'service1',
                dmiDataPlugin: '',
                dmiModelPlugin: '',
                createdCmHandles: [ncmpServiceCmHandle])
        when: 'post request is performed & registration is called with correct DMI plugin information'
            mvc.perform(
                post("$ncmpBasePathV1/ch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonData)
            ).andReturn().response
        then: 'no NcmpException is thrown & updateDmiRegistrationAndSyncModule is called with correct parameters'
            1 * mockNetworkCmProxyDataService.updateDmiRegistrationAndSyncModule({
                it.getDmiPlugin() == expectedDmiPluginRegistration.getDmiPlugin()
                it.getDmiDataPlugin() == expectedDmiPluginRegistration.getDmiDataPlugin()
                it.getDmiModelPlugin() == expectedDmiPluginRegistration.getDmiModelPlugin()
                it.getCreatedCmHandles().get(0).getCmHandleID() == expectedDmiPluginRegistration.getCreatedCmHandles().get(0).getCmHandleID()
            })
    }
}

