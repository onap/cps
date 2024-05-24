/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 TechMahindra Ltd.
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
import org.onap.cps.spi.utils.CpsValidator
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.test.context.ContextConfiguration
import org.onap.cps.spi.CpsNotificationPersistenceService
import spock.lang.Specification

@ContextConfiguration(classes = [ObjectMapper, JsonObjectMapper])
class CpsNotificationServiceImplSpec extends Specification {

    def mockCpsNotificationPersistenceService = Mock(CpsNotificationPersistenceService)
    def mockCpsValidator = Mock(CpsValidator)
    def objectUnderTest = new CpsNotificationServiceImpl(mockCpsValidator, mockCpsNotificationPersistenceService)

    def 'notification subscription for list of anchors.'() {
        given: 'details for notification subscription'
            def dataspaceName = 'some-dataspace'
            def anchorList = ['anchor01', 'anchor02']
        when: 'notification subscription is called'
            objectUnderTest.updateNotificationSubscription(dataspaceName, 'subscribe', anchorList)
        then: 'the persistence service is called once with the correct parameters'
            2 * mockCpsNotificationPersistenceService.subscribeNotification(dataspaceName, _)
    }

    def 'notification unsubscription for list of anchors.'() {
        given: 'details for notification subscription'
            def dataspaceName = 'some-dataspace'
            def anchorList = ['anchor01', 'anchor02']
        when: 'notification subscription is called'
            objectUnderTest.updateNotificationSubscription(dataspaceName, 'unsubscribe', anchorList)
        then: 'the persistence service is called once with the correct parameters'
        2 * mockCpsNotificationPersistenceService.unsubscribeNotification(dataspaceName, _)
    }

    def 'notification subscription for all of anchors in a dataspace.'() {
        given: 'details for notification subscription'
        def dataspaceName = 'some-dataspace'
        def anchorList = []
        when: 'notification subscription is called'
        objectUnderTest.updateNotificationSubscription(dataspaceName, 'subscribe', anchorList)
        then: 'the persistence service is called once with the correct parameters'
        1 * mockCpsNotificationPersistenceService.subscribeNotificationForAllAnchors(dataspaceName)
    }

    def 'notification unsubscription for all of anchors in a dataspace.'() {
        given: 'details for notification subscription'
        def dataspaceName = 'some-dataspace'
        def anchorList = []
        when: 'notification unsubscription is called'
        objectUnderTest.updateNotificationSubscription(dataspaceName, 'unsubscribe', anchorList)
        then: 'the persistence service is called once with the correct parameters'
        1 * mockCpsNotificationPersistenceService.unsubscribeNotificationForAllAnchors(dataspaceName)
    }

    def 'verify if notification is enabled for an anchor. #scenario'() {
        given: 'details for notification subscription'
            def dataspaceName = 'some-dataspace'
            def anchorName = 'anchor01'
            mockCpsNotificationPersistenceService.isNotificationSubscribed(dataspaceName, anchorName) >> isNotificationSubscribed
        when: 'check notification subscription is called'
            def result = objectUnderTest.isNotificationEnabled(dataspaceName, anchorName)
        then: 'the persistence service is called once with the correct parameters'
            assert result == expectedRessult
        where: 'the following scenario was used'
        scenario                       | isNotificationSubscribed || expectedRessult
        'notification is subscribed'   | true                     || true
        'notification is unsubscribed' | false                    || false
    }

}
