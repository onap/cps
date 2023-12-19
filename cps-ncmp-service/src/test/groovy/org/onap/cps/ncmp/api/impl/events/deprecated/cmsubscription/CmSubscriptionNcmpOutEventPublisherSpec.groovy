/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.events.deprecated.cmsubscription

import static org.onap.cps.ncmp.api.NcmpResponseStatus.SUCCESSFULLY_APPLIED_SUBSCRIPTION
import static org.onap.cps.ncmp.api.NcmpResponseStatus.SUBSCRIPTION_PENDING
import static org.onap.cps.ncmp.api.NcmpResponseStatus.SUBSCRIPTION_NOT_APPLICABLE
import static org.onap.cps.ncmp.api.NcmpResponseStatus.PARTIALLY_APPLIED_SUBSCRIPTION

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.deprecated.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.api.impl.utils.DataNodeBaseSpec
import org.onap.cps.ncmp.api.impl.utils.SubscriptionOutcomeCloudMapper
import org.onap.cps.ncmp.api.models.CmSubscriptionEvent
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_client.CmSubscriptionNcmpOutEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper, CmSubscriptionEventToCmSubscriptionNcmpOutEventMapper, CmSubscriptionNcmpOutEventPublisher])
class CmSubscriptionNcmpOutEventPublisherSpec extends DataNodeBaseSpec {

    @Autowired
    CmSubscriptionNcmpOutEventPublisher objectUnderTest

    @SpringBean
    SubscriptionPersistence mockSubscriptionPersistence = Mock(SubscriptionPersistence)
    @SpringBean
    EventsPublisher<CloudEvent> mockCmSubscriptionNcmpOutEventPublisher = Mock(EventsPublisher<CloudEvent>)
    @SpringBean
    CmSubscriptionEventToCmSubscriptionNcmpOutEventMapper cmSubscriptionEventToCmSubscriptionNcmpOutEventMapper = Mappers.getMapper(CmSubscriptionEventToCmSubscriptionNcmpOutEventMapper)
    @SpringBean
    SubscriptionOutcomeCloudMapper subscriptionOutcomeCloudMapper = new SubscriptionOutcomeCloudMapper(new ObjectMapper())

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def 'Send response to the client apps successfully'() {
        given: 'a cm subscription event'
            def cmSubscriptionEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionEvent.json')
            def cmSubscriptionEvent = jsonObjectMapper.convertJsonString(cmSubscriptionEventJsonData, CmSubscriptionEvent.class)
        and: 'a ncmp out event'
            def ncmpOutEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpOutEvent2.json')
            def ncmpOutEvent = jsonObjectMapper.convertJsonString(ncmpOutEventJsonData, CmSubscriptionNcmpOutEvent.class)
        and: 'a random id for the cloud event'
            SubscriptionOutcomeCloudMapper.randomId = 'some-id'
        and: 'a cloud event containing the outcome event'
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(ncmpOutEvent))
                .withId('some-id')
                .withType('subscriptionCreatedStatus')
                .withDataSchema(URI.create('urn:cps:' + 'org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_client.CmSubscriptionNcmpOutEvent' + ':1.0.0'))
                .withExtension("correlationid", 'SCO-9989752cm-subscription-001')
                .withSource(URI.create('NCMP')).build()
        and: 'the persistence service return a data node that includes pending cm handles that makes it partial success'
            mockSubscriptionPersistence.getCmHandlesForSubscriptionEvent(*_) >> [dataNode4]
        when: 'the response is being sent'
            objectUnderTest.sendResponse(cmSubscriptionEvent, 'subscriptionCreatedStatus')
        then: 'the publisher publish the cloud event with itself and expected parameters'
            1 * mockCmSubscriptionNcmpOutEventPublisher.publishCloudEvent('subscription-response', 'SCO-9989752cm-subscription-001', testCloudEventSent)
    }

    def 'Create ncmp out message as expected'() {
        given: 'a cm subscription event'
            def cmSubscriptionEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionEvent.json')
            def cmSubscriptionEvent = jsonObjectMapper.convertJsonString(cmSubscriptionEventJsonData, CmSubscriptionEvent.class)
        and: 'a ncmp out event'
            def ncmpOutEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpOutEvent.json')
            def ncmpOutEvent = jsonObjectMapper.convertJsonString(ncmpOutEventJsonData, CmSubscriptionNcmpOutEvent.class)
        and: 'a status code and status message a per #scenarios'
            ncmpOutEvent.getData().setStatusCode(statusCode)
            ncmpOutEvent.getData().setStatusMessage(statusMessage)
        when: 'a cm subscription event is being formed'
            def expectedResult = objectUnderTest.fromCmSubscriptionEvent(cmSubscriptionEvent, ncmpEventResponseCode)
        then: 'the result will be equal to ncmp out event'
            expectedResult == ncmpOutEvent
        where: 'the following values are used'
        scenario             | ncmpEventResponseCode             || statusMessage                       || statusCode
        'is full outcome'    | SUCCESSFULLY_APPLIED_SUBSCRIPTION || 'successfully applied subscription' || 1
        'is partial outcome' | PARTIALLY_APPLIED_SUBSCRIPTION    || 'partially applied subscription'    || 104
    }

    def 'Check cm handle id to status map to see if it is a full outcome response'() {
        when: 'is full outcome response evaluated'
            def response = objectUnderTest.decideOnNcmpEventResponseCodeForSubscription(cmHandleIdToStatusAndDetailsAsMap)
        then: 'the result will be as expected'
            response == expectedOutcomeResponseDecision
        where: 'the following values are used'
        scenario                                          | cmHandleIdToStatusAndDetailsAsMap                                                                                                                                                   || expectedOutcomeResponseDecision
        'The map contains PENDING status'                 | [CMHandle1: [details: 'Subscription forwarded to dmi plugin', status: 'PENDING'] as Map] as Map                                                                                     || SUBSCRIPTION_PENDING
        'The map contains ACCEPTED status'                | [CMHandle1: [details: '', status: 'ACCEPTED'] as Map] as Map                                                                                                                        || SUCCESSFULLY_APPLIED_SUBSCRIPTION
        'The map contains REJECTED status'                | [CMHandle1: [details: 'Cm handle does not exist', status: 'REJECTED'] as Map] as Map                                                                                                || SUBSCRIPTION_NOT_APPLICABLE
        'The map contains PENDING and PENDING statuses'   | [CMHandle1: [details: 'Some details', status: 'PENDING'] as Map, CMHandle2: [details: 'Some details', status: 'PENDING'] as Map as Map] as Map                                      || SUBSCRIPTION_PENDING
        'The map contains ACCEPTED and ACCEPTED statuses' | [CMHandle1: [details: '', status: 'ACCEPTED'] as Map, CMHandle2: [details: '', status: 'ACCEPTED'] as Map as Map] as Map                                                            || SUCCESSFULLY_APPLIED_SUBSCRIPTION
        'The map contains REJECTED and REJECTED statuses' | [CMHandle1: [details: 'Reject details', status: 'REJECTED'] as Map, CMHandle2: [details: 'Reject details', status: 'REJECTED'] as Map as Map] as Map                                || SUBSCRIPTION_NOT_APPLICABLE
        'The map contains PENDING and ACCEPTED statuses'  | [CMHandle1: [details: 'Some details', status: 'PENDING'] as Map, CMHandle2: [details: '', status: 'ACCEPTED'] as Map as Map] as Map                                                 || PARTIALLY_APPLIED_SUBSCRIPTION
        'The map contains REJECTED and ACCEPTED statuses' | [CMHandle1: [details: 'Cm handle does not exist', status: 'REJECTED'] as Map, CMHandle2: [details: '', status: 'ACCEPTED'] as Map as Map] as Map                                    || PARTIALLY_APPLIED_SUBSCRIPTION
        'The map contains PENDING and REJECTED statuses'  | [CMHandle1: [details: 'Subscription forwarded to dmi plugin', status: 'PENDING'] as Map, CMHandle2: [details: 'Cm handle does not exist', status: 'REJECTED'] as Map as Map] as Map || PARTIALLY_APPLIED_SUBSCRIPTION
    }

}
