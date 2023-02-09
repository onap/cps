/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.init

import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.spi.model.Dataspace
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import spock.lang.Specification

class ModelLoaderSpec extends Specification {

    def mockCpsAdminService = Mock(CpsAdminService)
    def mockCpsModuleService = Mock(CpsModuleService)

    def objectUnderTest = new ModelLoader(mockCpsAdminService, mockCpsModuleService)

    def SUBSCRIPTION_DATASPACE_NAME = objectUnderTest.SUBSCRIPTION_DATASPACE_NAME;
    def SUBSCRIPTION_ANCHOR_NAME = objectUnderTest.SUBSCRIPTION_ANCHOR_NAME;
    def SUBSCRIPTION_SCHEMASET_NAME = objectUnderTest.SUBSCRIPTION_SCHEMASET_NAME;

    def sampleYangContentMap = ['subscription.yang':'module subscription { *sample content* }']
    def event = new ApplicationReadyEvent(new SpringApplication(), null, null, null)

    def 'Onboard subscription model successfully via application ready event'() {
        given: 'the admin service returns the correct dataspace'
            mockCpsAdminService.getDataspace(SUBSCRIPTION_DATASPACE_NAME) >> new Dataspace(SUBSCRIPTION_DATASPACE_NAME)
        when: 'the application is ready'
            objectUnderTest.onApplicationEvent(event)
        then: 'the module service to create schema set is called once'
            1 * mockCpsModuleService.createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME,sampleYangContentMap)
        and: 'the admin service to create an anchor set is called once'
            1 * mockCpsAdminService.createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME, SUBSCRIPTION_SCHEMASET_NAME)
    }

    def 'Onboard subscription model via application ready event fails'() {
        given: 'the admin service returns null for the expected dataspace'
            mockCpsAdminService.getDataspace(SUBSCRIPTION_DATASPACE_NAME) >> null
        when: 'the application is ready'
            objectUnderTest.onApplicationEvent(event)
        then: 'the admin service retries to verify that dataspace exists'
            11 * mockCpsAdminService.getDataspace(SUBSCRIPTION_DATASPACE_NAME)
    }

    def 'Create schema set from model file'() {
        given: 'the method to create yang resource to content map returns the correct map'
            def yangResourceToContentMap = objectUnderTest.createYangResourceToContentMap("subscription.yang", "model/subscription.yang")
        when: 'the method to create schema set is called with the following parameters'
            objectUnderTest.createSchemaSetFromModelFile("myDataspace", "mySchemaSet", "subscription.yang", "model/subscription.yang")
        then: 'yang resource to content map is as expected'
            assert sampleYangContentMap == yangResourceToContentMap
        and: 'the module service is called once with the correct map'
            1 * mockCpsModuleService.createSchemaSet(_, _, yangResourceToContentMap)
    }

    def 'Get file content as string'() {
        when: 'the method to get yang content as string has NULL for path parameter'
            def response = objectUnderTest.getFileContentAsString(pathToModel)
        then: 'the response is as expected'
            assert response == expectedResponse
        where: 'the following are used'
            scenario        | pathToModel                 || expectedResponse
            'valid path'    | 'model/subscription.yang'   || 'module subscription { *sample content* }'
            'invalid path'  | 'model/invalid.yang'        || ''
            'null'          | null                        || ''

    }

    def 'Create Anchor with model loader'() {
        when: 'create anchor is called'
            objectUnderTest.createAnchor("myDataspace", "mySchemaSet", "myAnchor")
        then: 'the admin service method to create anchor is invoked once with the correct parameters'
            1 * mockCpsAdminService.createAnchor("myDataspace", "mySchemaSet", "myAnchor")
    }
}