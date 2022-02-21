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
import org.onap.cps.ncmp.api.models.CmHandle
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

    def 'Dmi plugin registration with #scenario' () {
        given: 'a dmi plugin registration with created'
            def jsonData = TestUtils.getResourceFileContent(dmiRegistrationJson)
            def createdCmHandle = new CmHandle(cmHandleID : 'example-name', dmiProperties: expectedCreatedDmiProperties, publicProperties: expectedCreatedPublicProperties)
            def updatedCmHandle = new CmHandle(cmHandleID : 'updated-example', dmiProperties: expectedUpdatedDmiProperties, publicProperties: expectedUpdatedPublicProperties)
            def expectedDmiPluginRegistration = new DmiPluginRegistration(
                dmiPlugin: 'service1',
                dmiDataPlugin: '',
                dmiModelPlugin: '',
                createdCmHandles: [createdCmHandle],
                updatedCmHandles: [updatedCmHandle],
                removedCmHandles: ['removed-cm-handle'])
        when: 'post request is performed & registration is called with correct DMI plugin information'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonData)
            ).andReturn().response
        then: 'no NcmpException is thrown & updateDmiRegistrationAndSyncModule is called with the expected parameters'
            1 * mockNetworkCmProxyDataService.updateDmiRegistrationAndSyncModule({
                it instanceof DmiPluginRegistration
                it.dmiPlugin == expectedDmiPluginRegistration.dmiPlugin
                it.dmiDataPlugin == expectedDmiPluginRegistration.dmiDataPlugin
                it.dmiModelPlugin == expectedDmiPluginRegistration.dmiModelPlugin
                it.createdCmHandles[0].cmHandleID == expectedDmiPluginRegistration.createdCmHandles[0].cmHandleID
                it.updatedCmHandles[0].cmHandleID == expectedDmiPluginRegistration.updatedCmHandles[0].cmHandleID
                it.removedCmHandles[0] == expectedDmiPluginRegistration.removedCmHandles[0]
                it.createdCmHandles[0].dmiProperties == expectedDmiPluginRegistration.createdCmHandles[0].dmiProperties
                it.updatedCmHandles[0].publicProperties == expectedDmiPluginRegistration.updatedCmHandles[0].publicProperties
            })
        and: 'response status is created'
            response.status == HttpStatus.CREATED.value()
        where: 'there following parameters are used'
            scenario                    | dmiRegistrationJson                                       | expectedCreatedDmiProperties             | expectedCreatedPublicProperties                        | expectedUpdatedDmiProperties                             | expectedUpdatedPublicProperties
            'additional properties'     | 'dmi_registration_combined_valid_with_properties.json'    | ['Property-Example': 'example property'] | ['Public-Property-Example': 'public example property'] | ['Updated-Property-Example': 'updated example property'] | ['Public-Updated-Property-Example': 'updated public example property']
            'no additional properties'  | 'dmi_registration_combined_valid_without_properties.json' | null                                     | null                                                   |  null                                                    | null

    }
}

