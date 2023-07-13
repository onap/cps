/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2022-2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events.avcsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avcsubscription1_0_0.client_to_ncmp.SubscriptionEvent;
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class SubscriptionEventConsumerSpec extends MessagingBaseSpec {

    def mockSubscriptionEventForwarder = Mock(SubscriptionEventForwarder)
    def mockSubscriptionEventMapper = Mock(SubscriptionEventMapper)
    def mockSubscriptionPersistence = Mock(SubscriptionPersistence)
    def objectUnderTest = new SubscriptionEventConsumer(mockSubscriptionEventForwarder, mockSubscriptionEventMapper, mockSubscriptionPersistence)

    def yangModelSubscriptionEvent = new YangModelSubscriptionEvent()

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper


    def 'Consume, persist and forward valid CM create message'() {
        given: 'an event with data category CM'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
            testEventSent.getData().getDataType().setDataCategory(dataCategory)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventSent))
                .withId('some-event-id')
                .withType(dataType)
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        and: 'notifications are enabled'
            objectUnderTest.notificationFeatureEnabled = isNotificationEnabled
        and: 'subscription model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = isModelLoaderEnabled
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEvent(consumerRecord)
        then: 'the event is mapped to a yangModelSubscription'
            numberOfTimesToPersist * mockSubscriptionEventMapper.toYangModelSubscriptionEvent(testEventSent) >> yangModelSubscriptionEvent
        and: 'the event is persisted'
            numberOfTimesToPersist * mockSubscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent)
        and: 'the event is forwarded'
            numberOfTimesToForward * mockSubscriptionEventForwarder.forwardSubscriptionEvent(testEventSent)
        where: 'given values are used'
            scenario                                                 | dataCategory | dataType              | isNotificationEnabled | isModelLoaderEnabled || numberOfTimesToForward || numberOfTimesToPersist
            'Both model loader and notification are enabled'         | 'CM'         | 'subscriptionCreated' | true                  | true                 || 1                      || 1
            'Both model loader and notification are enabled'         | 'CM'         | 'subscriptionDeleted' | true                  | true                 || 1                      || 0
            'Both model loader and notification are disabled'        | 'CM'         | 'subscriptionCreated' | false                 | false                || 0                      || 0
            'Both model loader and notification are disabled'        | 'CM'         | 'subscriptionDeleted' | false                 | false                || 0                      || 0
            'Model loader enabled and notification  disabled'        | 'CM'         | 'subscriptionCreated' | false                 | true                 || 0                      || 1
            'Model loader enabled and notification  disabled'        | 'CM'         | 'subscriptionDeleted' | false                 | true                 || 0                      || 0
            'Model loader disabled and notification enabled'         | 'CM'         | 'subscriptionCreated' | true                  | false                || 1                      || 0
            'Flags are enabled but data category is FM'              | 'FM'         | 'subscriptionCreated' | true                  | true                 || 0                      || 0
            'Flags are enabled but data category is FM'              | 'FM'         | 'subscriptionDeleted' | true                  | true                 || 0                      || 0
            'Flags are enabled but data type is subscriptionUpdated' | 'CM'         | 'subscriptionUpdated' | true                  | true                 || 0                      || 0
    }

    def 'Consume event with wrong datastore causes an exception'() {
        given: 'an event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'datastore is set to a non passthrough datastore'
            testEventSent.getData().getPredicates().setDatastore('operational')
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventSent))
                .withId('some-event-id')
                .withType('subscriptionCreated')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
            def consumerRecord = new ConsumerRecord<String, SubscriptionEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEvent(consumerRecord)
        then: 'an operation not yet supported exception is thrown'
            thrown(OperationNotYetSupportedException)
    }

}
