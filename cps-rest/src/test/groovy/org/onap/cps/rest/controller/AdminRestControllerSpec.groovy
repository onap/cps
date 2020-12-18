/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.cps.rest.controller


import org.modelmapper.ModelMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.spi.model.Anchor
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@AutoConfigureMockMvc
@WebMvcTest
class AdminRestControllerSpec extends Specification {

    @SpringBean
    CpsAdminService mockCpsAdminService = Mock()

    @SpringBean
    ModelMapper modelMapper = Mock()

    @Autowired
    MockMvc mvc

    def anchorsEndpoint = '/v1/dataspaces/dataspace-name/anchors'
    def anchor = new Anchor()
    def anchorList = []

    def setup() {
        anchor.name = 'my_anchor'
        anchorList.add(anchor)
    }

    def 'when createAnchor API is called, the response status is 201. '() {
        given:
            def requestParams = new LinkedMultiValueMap<>()
            requestParams.add('dataspace-name', 'my_dataspace')
            requestParams.add('schema-set-name', 'my_schema')
            requestParams.add('anchor-name', 'my_anchor')
            mockCpsAdminService.createAnchor(_) >> 'my_anchor'
        expect: 'Status is 201 and the response is the name of the created anchor -> my_anchor'
            mvc.perform(post(anchorsEndpoint).contentType(MediaType.APPLICATION_JSON).params(requestParams as MultiValueMap))
                    .andExpect(status().isCreated())
                    .andReturn().response.getContentAsString().contains('my_anchor')
    }

    def 'when get all anchors for a dataspace API is called, the response status is 200 '() {
        given:
            mockCpsAdminService.getAnchors(_) >> anchorList
        expect: 'Status is 200 and the response is Collection of Anchors containing anchor name -> my_anchor'
            mvc.perform(get(anchorsEndpoint))
                    .andExpect(status().isOk())
                    .andReturn().response.getContentAsString().contains('my_anchor')
    }
}
