/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 TechMahindra Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.api.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@ContextConfiguration(classes = [ObjectMapper, JsonObjectMapper])
class CpsNotificationServiceImplSpec extends Specification {
    //def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    //def mockYangParser = Mock(YangParser)
    //def mockCpsDataspaceService = Mock(CpsDataspaceService)
    //def mockCpsModuleService = Mock(CpsModuleService)
    def objectUnderTest = new CpsNotificationServiceImpl(mockCpsDataService)

    /*def setup() {
        mockCpsDataspaceService.createDataspace('CPS-Admin')
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('cps-notification-subscriptions@2024-07-03.yang')
        mockCpsModuleService.createSchemaSet('CPS-Admin', 'notification-subscription', yangResourceNameToContent)
        mockCpsAnchorService.createAnchor('CPS-Admin', 'notification-subscription', 'cps-notification-subscriptions')
    }

    void cleanup() {
        mockCpsAnchorService.deleteAnchor('CPS-Admin', 'notification-subscription')
        mockCpsModuleService.deleteSchemaSet('CPS-Admin', 'notification-subscription', CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED)
        mockCpsDataspaceService.deleteDataspace('CPS-Admin')
    }*/

    def 'add notification subscription for list of dataspaces'() {
        given: 'details for notification subscription'
            def dataspaceName = 'some-dataspace'
            def jsonData = '{"dataspace":[{"name":"ds01"},{"name":"ds02"}]}'
            def xpath = '/'
        //mockYangParser.parseData(ContentType.JSON, jsonData, 'cps-notification-subscriptions', '/dataspaces') >>
        when: 'create notification subscription is called'
            objectUnderTest.createNotificationSubscription(jsonData, xpath)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataService.saveListElements('CPS-Admin', 'cps-notification-subscriptions', xpath, jsonData, _, _)
    }

    def 'delete notification subscription for dataspace'() {
        given: 'details for notification subscription'
            def xpath = '/dataspaces/dataspace[@name="ds01]'
        when: 'delete notification subscription is called'
            objectUnderTest.deleteNotificationSubscription(xpath)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataService.deleteDataNode('CPS-Admin', 'cps-notification-subscriptions', xpath, _)
    }

}
