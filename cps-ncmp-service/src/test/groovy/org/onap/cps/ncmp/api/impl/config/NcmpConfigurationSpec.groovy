/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
package org.onap.cps.ncmp.api.impl.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [NcmpConfiguration.DmiProperties])
class NcmpConfigurationSpec extends Specification{

    @Autowired
    NcmpConfiguration.DmiProperties dmiProperties

    def mockRestTemplateBuilder = new RestTemplateBuilder()

    def 'NcmpConfiguration Construction.'() {
        expect: 'the system can create an instance'
             new NcmpConfiguration() != null
    }

    def 'DMI Properties.'() {
        expect: 'properties are set to values in test configuration yaml file'
            dmiProperties.authUsername == 'some-user'
            dmiProperties.authPassword == 'some-password'
    }

    def 'Rest Template creation.'() {
        when: 'a rest template is created'
            def result = NcmpConfiguration.restTemplate(mockRestTemplateBuilder)
        then: 'the rest template is returned'
            assert result instanceof RestTemplate
        and: 'a jackson media converter has been added'
            def lastMessageConverter = result.getMessageConverters().get(result.getMessageConverters().size()-1)
            lastMessageConverter instanceof MappingJackson2HttpMessageConverter
        and: 'the jackson media converters supports the expected media types'
            lastMessageConverter.getSupportedMediaTypes() == [MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN];
    }
}
