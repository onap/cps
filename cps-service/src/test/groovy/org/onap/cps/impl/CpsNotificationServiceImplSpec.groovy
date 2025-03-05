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

package org.onap.cps.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.onap.cps.api.model.Anchor
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.PrefixResolver
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [ObjectMapper, JsonObjectMapper])
class CpsNotificationServiceImplSpec extends Specification {

    def dataspaceName = 'CPS-Admin'
    def anchorName = 'cps-notification-subscriptions'
    def schemaSetName = 'cps-notification-subscriptions'
    def anchor = new Anchor(anchorName, dataspaceName, schemaSetName)

    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockPrefixResolver = Mock(PrefixResolver)
    def objectUnderTest = new CpsNotificationServiceImpl(mockCpsAnchorService, mockCpsDataService, mockCpsDataPersistenceService, mockPrefixResolver)

    def 'add notification subscription for list of dataspaces'() {
        given: 'details for notification subscription and subscription root node xpath'
            def notificationSubscriptionAsjson = 'some json'
            def xpath = '/dataspaces'
        and: 'anchor is provided'
            mockCpsAnchorService.getAnchor(dataspaceName, anchorName) >> anchor
        when: 'create notification subscription is called'
            objectUnderTest.createNotificationSubscription(notificationSubscriptionAsjson, xpath)
        then: 'the cps data service is called with the correct parameters'
            1 * mockCpsDataService.buildDataNodesWithParentNodeXpath(anchor, xpath, notificationSubscriptionAsjson, ContentType.JSON)
    }

    def 'delete notification subscription for given xpath'() {
        given: 'details for notification subscription'
            def xpath = '/some/path'
        when: 'delete notification subscription is called'
            objectUnderTest.deleteNotificationSubscription(xpath)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, xpath)
    }

    def 'get notification subscription for given xpath'() {
        given: 'details for notification subscription'
            def xpath = '/some/path'
        and: 'persistence service returns data nodes for subscribed data'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName,
                    xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    [new DataNodeBuilder().withXpath('/some/path').withLeaves([leaf: 'dataspace', leafList: ['ds01', 'ds02']]).build()]
        when: 'delete notification subscription is called'
            def result =  objectUnderTest.getNotificationSubscription(xpath)
        then: 'the result is a json representation of the data node(s) returned by the data persistence service'
            assert result.get(0).toString() == '{path={leaf=dataspace, leafList=[ds01, ds02]}}'
    }

    def 'is notification enabled for given anchor'() {
        given: 'data nodes available for given anchor'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    [new DataNodeBuilder().withXpath('/xpath-1').build()]
        when: 'is notification enabled is called'
            boolean isNotificationEnabled = objectUnderTest.isNotificationEnabled(dataspaceName, anchorName)
        then: 'the notification is enabled'
            assert isNotificationEnabled
    }

    def 'is notification disabled for given anchor'() {
        given: 'data nodes not available for given anchor'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']/anchors/anchor[@name='anchor-01']", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    {  throw new DataNodeNotFoundException(dataspaceName, anchorName) }
        when: 'is notification enabled is called'
            boolean isNotificationEnabled = objectUnderTest.isNotificationEnabled('ds01', 'anchor-01')
        then: 'the notification is disabled'
            assert !isNotificationEnabled
    }

    def 'is notification enabled for all anchors in a dataspace'() {
        given: 'data nodes available for given dataspace'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    [new DataNodeBuilder().withXpath('/xpath-1').build()]
        and: 'data nodes not available for any specific anchor'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']/anchors", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    {  throw new DataNodeNotFoundException(dataspaceName, anchorName) }
        when: 'is notification enabled is called'
            boolean isNotificationEnabled = objectUnderTest.notificationEnabledForAllAnchors('ds01')
        then: 'the notification is enabled'
            assert isNotificationEnabled
    }

    def 'is notification disabled for all anchors in a dataspace'() {
        given: 'data nodes available for given dataspace'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                [new DataNodeBuilder().withXpath('/xpath-1').build()]
        and: 'data nodes also available for any specific anchor'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']/anchors", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    [new DataNodeBuilder().withXpath('/xpath-1').build()]
        when: 'is notification enabled is called'
            boolean isNotificationEnabled = objectUnderTest.notificationEnabledForAllAnchors('ds01')
        then: 'the notification is disabled'
            assert !isNotificationEnabled
    }

}
