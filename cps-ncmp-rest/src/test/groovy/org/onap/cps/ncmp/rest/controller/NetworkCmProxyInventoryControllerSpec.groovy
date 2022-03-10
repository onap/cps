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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration
import org.onap.cps.utils.JsonObjectMapper
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

    @SpringBean
    NcmpRestInputMapper ncmpRestInputMapper = Mock()

    DmiPluginRegistration mockDmiPluginRegistration = Mock()

    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @Value('${rest.api.ncmp-inventory-base-path}/v1')
    def ncmpBasePathV1

    def 'Dmi plugin registration #scenario' () {
        given: 'a dmi plugin registration with #scenario'
            def jsonData = TestUtils.getResourceFileContent(dmiRegistrationJson)
        and: 'the expected rest input as an object'
            def expectedRestDmiPluginRegistration = jsonObjectMapper.convertJsonString(jsonData, RestDmiPluginRegistration)
        and: 'the converter returns a dmi registration (only for the expected input object)'
            ncmpRestInputMapper.toDmiPluginRegistration(expectedRestDmiPluginRegistration) >> mockDmiPluginRegistration
        when: 'post request is performed & registration is called with correct DMI plugin information'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonData)
            ).andReturn().response
        then: 'the converted object is forwarded to the registration service'
            1 * mockNetworkCmProxyDataService.updateDmiRegistrationAndSyncModule(mockDmiPluginRegistration)
        and: 'response status is no content'
            response.status ==  HttpStatus.NO_CONTENT.value()
        where: 'the following registration json is used'
            scenario                                                                       | dmiRegistrationJson
            'multiple services, added, updated and removed cm handles and many properties' | 'dmi_registration_all_singing_and_dancing.json'
            'updated cm handle with updated/new and removed properties'                    | 'dmi_registration_updates_only.json'
            'without any properties'                                                       | 'dmi_registration_without_properties.json'
    }

    def 'Dmi plugin registration with invalid json' () {
        given: 'a dmi plugin registration with #scenario'
            def jsonDataWithUndefinedDataLabel = '{"notAdmiPlugin":""}'
        when: 'post request is performed & registration is called with correct DMI plugin information'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonDataWithUndefinedDataLabel)
            ).andReturn().response
        then: 'response status is bad request'
            response.status == HttpStatus.BAD_REQUEST.value()
    }

}
