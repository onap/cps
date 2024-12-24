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
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.Anchor
import org.onap.cps.impl.DataNodeBuilder
import org.onap.cps.impl.YangTextSchemaSourceSetCache
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.YangParser
import org.onap.cps.TestUtils
import org.onap.cps.utils.YangParserHelper
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification


@ContextConfiguration(classes = [ObjectMapper, JsonObjectMapper])
class CpsNotificationServiceImplSpec extends Specification {

    def dataspaceName = 'CPS-Admin'
    def anchorName = 'cps-notification-subscriptions'
    def schemaSetName = 'cps-notification-subscriptions'
    def anchor = Anchor.builder().name(anchorName).dataspaceName(dataspaceName).schemaSetName(schemaSetName).build()

    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)
    def yangParser = new YangParser(new YangParserHelper(), mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)
    def objectUnderTest = new CpsNotificationServiceImpl(mockCpsAnchorService, mockCpsDataPersistenceService, yangParser)

    def 'add notification subscription for list of dataspaces'() {
        given: 'details for notification subscription'
            def jsonData = '{"dataspace":[{"name":"ds01"},{"name":"ds02"}]}'
            def xpath = '/dataspaces'
        and: 'schema set for given anchor and dataspace references notification subscription model'
            setupSchemaSetMocks('cps-notification-subscriptions@2024-07-03.yang')
        and: 'anchor is provided'
            mockCpsAnchorService.getAnchor(dataspaceName, anchorName) >> anchor
        when: 'create notification subscription is called'
            objectUnderTest.createNotificationSubscription(jsonData, xpath)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataPersistenceService.addListElements('CPS-Admin', 'cps-notification-subscriptions', xpath, { dataNodeCollection ->
                {
                    assert dataNodeCollection.size() == 2
                    assert dataNodeCollection.collect { it.getXpath() }
                            .containsAll(['/dataspaces/dataspace[@name=\'ds01\']', '/dataspaces/dataspace[@name=\'ds02\']'])
                }
            })
    }

    def 'add notification subscription throws exception'() {
        given: 'details for notification subscription'
            def jsonData = '{"dataspace":[{"name":"ds01"},{"name":"ds02"}]}'
            def xpath = '/dataspaces'
        and: 'schema set for given anchor and dataspace references invalid data model'
            setupSchemaSetMocks('test-tree.yang')
        and: 'anchor is provided'
            mockCpsAnchorService.getAnchor(dataspaceName, anchorName) >> anchor
        when: 'create notification subscription is called'
            objectUnderTest.createNotificationSubscription(jsonData, xpath)
        then: 'data validation exception is thrown '
            thrown(DataValidationException)
    }

    def 'delete notification subscription for given xpath'() {
        given: 'details for notification subscription'
            def xpath = '/dataspaces/dataspace[@name="ds01]'
        when: 'delete notification subscription is called'
            objectUnderTest.deleteNotificationSubscription(xpath)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, xpath)
    }

    def 'get notification subscription for given xpath'() {
        given: 'details for notification subscription'
            def xpath = '/dataspaces/dataspace[@name="ds01]'
        when: 'delete notification subscription is called'
            objectUnderTest.getNotificationSubscription(xpath)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
    }

    def 'is notification enabled for given anchor'() {
        given: 'data nodes available for given anchor'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    [new DataNodeBuilder().withXpath('/xpath-1').build()]
        when: 'is notification enabled is called'
            boolean isNotificationEnabled = objectUnderTest.isNotificationEnabled(dataspaceName, anchorName)
        then: 'the notification is enabled'
            isNotificationEnabled == true
    }

    def 'is notification disabled for given anchor'() {
        given: 'data nodes not available for given anchor'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']/anchors/anchor[@name='anchor-01']", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    {  throw new DataNodeNotFoundException(dataspaceName, anchorName) }
        when: 'is notification enabled is called'
            boolean isNotificationEnabled = objectUnderTest.isNotificationEnabled('ds01', 'anchor-01')
        then: 'notification enabled is false'
            isNotificationEnabled == false
    }

    def 'is notification enabled for all anchors in a dataspace'() {
        given: 'data nodes available for given dataspace'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    [new DataNodeBuilder().withXpath('/xpath-1').build()]
        and: 'data nodes not available for any specific anchor'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']/anchors", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    {  throw new DataNodeNotFoundException(dataspaceName, anchorName) }
        when: 'is notification enabled is called'
            boolean isNotificationEnabled = objectUnderTest.isNotificationEnabledForAllAnchors('ds01')
        then: 'notification enabled is false'
            isNotificationEnabled == true
    }

    def 'is notification disabled for all anchors in a dataspace'() {
        given: 'data nodes available for given dataspace'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                [new DataNodeBuilder().withXpath('/xpath-1').build()]
        and: 'data nodes also available for any specific anchor'
            mockCpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, "/dataspaces/dataspace[@name='ds01']/anchors", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
                    [new DataNodeBuilder().withXpath('/xpath-1').build()]
        when: 'is notification enabled is called'
            boolean isNotificationEnabled = objectUnderTest.isNotificationEnabledForAllAnchors('ds01')
        then: 'notification enabled is false'
            isNotificationEnabled == false
    }

    def setupSchemaSetMocks(String... yangResources) {
        def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
        mockYangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        def yangResourceNameToContent = TestUtils.getYangResourcesAsMap(yangResources)
        def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }
}
