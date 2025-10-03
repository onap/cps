/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
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

package org.onap.cps.integration.functional.ncmp.datajobs.subscription

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.kafka.CloudEventSerializer
import io.cloudevents.kafka.CloudEventDeserializer
import java.nio.charset.StandardCharsets
import java.time.Duration
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp.NcmpInEventConsumer
import org.onap.cps.ncmp.impl.datajobs.subscription.utils.CmDataJobSubscriptionPersistenceService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class CmSubscriptionSpec extends CpsIntegrationSpecBase {

    @Autowired
    CmDataJobSubscriptionPersistenceService cmDataJobSubscriptionPersistenceService

    @Value('${app.ncmp.avc.cm-subscription-ncmp-in}')
    def subscriptionTopic

    @Value('${app.ncmp.avc.cm-subscription-dmi-out}')
    def dmiOutTopic

    @Value('${app.ncmp.avc.cm-subscription-dmi-in}')
    def dmiInTopic

    def dmiInConsumer
    def testRequestProducer
    def testResponseProducer

    def listAppender = new ListAppender<ILoggingEvent>()
    def logger

    def setup() {
        registerCmHandlesForSubscriptions()
        kafkaTestContainer.start()
        dmiInConsumer = kafkaTestContainer.getConsumer('test-group', CloudEventDeserializer.class)
        dmiInConsumer.subscribe([dmiInTopic])
        dmiInConsumer.poll(Duration.ofMillis(500))
        testRequestProducer = kafkaTestContainer.createProducer("test-group", StringSerializer.class)
        testResponseProducer = kafkaTestContainer.createProducer("test-group", CloudEventSerializer.class)
        logger = LoggerFactory.getLogger(NcmpInEventConsumer)
        listAppender.start()
        logger.addAppender(listAppender)
    }

    def cleanup() {
        dmiInConsumer.unsubscribe()
        dmiInConsumer.close()
        testRequestProducer.close()
        testResponseProducer.close()
        kafkaTestContainer.close()
        deregisterCmHandles('dmi-0', ['cmHandle0'])
        deregisterCmHandles('dmi-1', ['cmHandle1', 'cmHandle2'])
        deregisterCmHandles('dmi-2', ['cmHandle3', 'cmHandle4'])
    }

    def 'Create subscription and send to multiple DMIs'() {
        given: 'a data node selector that affects DMI-1'
            def dmi1DataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"2\\\"]/child\\n'''
        and: 'a data node selector that affects DMI-2'
            def dmi2DataNodeSelector = '/parent[id=\\\"3\\\"]/child'
        and: 'an event payload'
            def eventDataNodeSelector = (dmi1DataNodeSelector + dmi2DataNodeSelector)
            def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'myDataJobId', eventDataNodeSelector)
        when: 'a subscription create request is sent'
            sendSubscriptionCreateRequest(subscriptionTopic, 'key', eventPayload)
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains('myDataJobId') && msg.contains('dataJobCreated')}
        and: 'the 3 different data node selectors for the given data job id is persisted'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors('myDataJobId').size() == 3
        and: 'get correlation ids from event sent to DMIs'
            def correlationIds = getAllConsumedCorrelationIds()
        and: 'there is correlation IDs (event) for each affected dmi (DMI-1, DMI-2)'
            assert correlationIds.size() == 2
            assert correlationIds.containsAll(['myDataJobId#dmi-1', 'myDataJobId#dmi-2'])
    }

    def 'Update subscription status'() {
        given: 'a persisted subscription'
            def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'newDataJob', '/parent[id=\\\'0\\\']\\n')
            sendSubscriptionCreateRequest(subscriptionTopic, 'newDataJob', eventPayload)
        when: 'dmi accepts the subscription create request'
            sendDmiResponse('1', 'ACCEPTED', 'subscriptionCreateResponse', 'dmi-0', 'newDataJob#dmi-0')
        then: 'there are no more inactive data node selector for given datajob id'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors('newDataJob').size() == 0
        and: 'status for the data node selector for given data job id is ACCEPTED'
            def affectedDataNodes =  cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                '//subscription/dataJobId[text()=\'newDataJob\']', DIRECT_CHILDREN_ONLY)
            assert affectedDataNodes.leaves.every( entry -> entry.get('status') == 'ACCEPTED')
    }

    def 'Create new subscription which partially overlaps with an existing active subscription'() {
        given: 'an existing data node selector that affects DMI-1'
            def existingDmi1DataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"3\\\"]/child\\n'''
        and: 'a new data node selector that affects DMI-2'
            def newDmi2DataNodeSelector = '/parent[id=\\\"4\\\"]'
        and: 'an event payload'
            def eventDataNodeSelector = (existingDmi1DataNodeSelector + newDmi2DataNodeSelector)
            def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'partialOverlappingDataJob', eventDataNodeSelector)
        and: 'an active subscription in database'
            createAndAcceptSubscriptionA()
        when: 'a new subscription create request is sent'
            sendSubscriptionCreateRequest(subscriptionTopic, 'partialOverlappingDataJob', eventPayload)
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains('partialOverlappingDataJob') && msg.contains('dataJobCreated')}
        and: 'the 3 data node selectors for the given data job id is persisted'
            assert cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                '//subscription/dataJobId[text()=\'partialOverlappingDataJob\']', DIRECT_CHILDREN_ONLY).size() == 3
        and: 'only one data node selector is not active'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors('partialOverlappingDataJob').size() == 1
        and: 'get correlation ids from event sent to DMIs'
            def correlationIds = getAllConsumedCorrelationIds()
        and: 'there is correlation IDs (event) for only the affected dmi (DMI-2)'
            assert !correlationIds.contains('partialOverlappingDataJob#dmi-1')
            assert correlationIds.contains('partialOverlappingDataJob#dmi-2')
    }

    def 'Create new subscription which completely overlaps with an active existing subscriptions'() {
        given: 'a new data node selector'
            def dataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"2\\\"]/child\\n'''
        and: 'an event payload'
            def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'fullyOverlappingDataJob', dataNodeSelector)
        and: 'existing active subscriptions in database'
            createAndAcceptSubscriptionA()
            createAndAcceptSubscriptionB()
        when: 'a new subscription create request is sent'
            sendSubscriptionCreateRequest(subscriptionTopic, 'fullyOverlappingDataJob', eventPayload)
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains('fullyOverlappingDataJob') && msg.contains('dataJobCreated')}
        and: 'the 2 data node selectors for the given data job id is persisted'
            assert cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                '//subscription/dataJobId[text()=\'fullyOverlappingDataJob\']', DIRECT_CHILDREN_ONLY).size() == 2
        and: 'there are no inactive data node selector'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors('fullOverlappingDataJob').size() == 0
        and: 'get correlation ids from event sent to DMIs'
            def correlationIds = getAllConsumedCorrelationIds()
        and: 'there is no correlation IDs (event) for any dmi'
            assert !correlationIds.any { correlationId -> correlationId.startsWith('fullOverlappingDataJob') }
    }

    def registerCmHandlesForSubscriptions() {
        registerCmHandle('dmi-0', 'cmHandle0', '','/parent=0')
        registerCmHandle('dmi-1', 'cmHandle1', '','/parent=1')
        registerCmHandle('dmi-1', 'cmHandle2', '','/parent=2')
        registerCmHandle('dmi-2', 'cmHandle3', '','/parent=3')
        registerCmHandle('dmi-2', 'cmHandle4', '','/parent=4')
    }

    def createSubscriptionEventPayload(eventType, dataJobId, dataNodeSelector) {
        def eventPayload = readResourceDataFile('datajobs/subscription/createSubscriptionEvent.json')
        eventPayload = eventPayload.replace('#eventType', eventType)
        eventPayload = eventPayload.replace('#dataJobId', dataJobId)
        eventPayload = eventPayload.replace('#dataNodeSelector', dataNodeSelector)
        return eventPayload
    }

    def createAndAcceptSubscriptionA() {
        def dataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"2\\\"]/child\\n/parent[id=\\\"3\\\"]/child'''
        def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'dataJobA', dataNodeSelector)
        sendSubscriptionCreateRequest(subscriptionTopic, 'dataJobA', eventPayload)
        sendDmiResponse('1', 'ACCEPTED', 'subscriptionCreateResponse', 'dmi-1', 'dataJobA#dmi-1')
        sendDmiResponse('1', 'ACCEPTED', 'subscriptionCreateResponse', 'dmi-2', 'dataJobA#dmi-2')
    }

    def createAndAcceptSubscriptionB() {
        def dataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"3\\\"]/child\\n/parent[id=\\\"4\\\"]'''
        def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'dataJobB', dataNodeSelector)
        sendSubscriptionCreateRequest(subscriptionTopic, 'dataJobB', eventPayload)
        sendDmiResponse('1', 'ACCEPTED', 'subscriptionCreateResponse', 'dmi-2', 'dataJobB#dmi-2')
    }

    def sendSubscriptionCreateRequest(topic, eventKey, eventPayload) {
        def event = new ProducerRecord<>(topic, eventKey, eventPayload);
        testRequestProducer.send(event)
        sleep(1000)
    }

    def sendDmiResponse(statusCode, statusMessage, eventType, eventSource, correlationId) {
        def eventPayload =  readResourceDataFile('datajobs/subscription/dmiSubscriptionResponseEvent.json')
        eventPayload = eventPayload.replace('#statusCode', statusCode)
        eventPayload = eventPayload.replace('#statusMessage', statusMessage)
        def cloudEvent = CloudEventBuilder.v1()
            .withData(eventPayload.getBytes(StandardCharsets.UTF_8))
            .withId('random-uuid')
            .withType(eventType)
            .withSource(URI.create(eventSource))
            .withExtension('correlationid', correlationId).build()
        def event = new ProducerRecord<>(dmiOutTopic, 'key', cloudEvent);
        testResponseProducer.send(event)
        sleep(2000)
    }

    def getAllConsumedCorrelationIds() {
        def consumedEvents = dmiInConsumer.poll(Duration.ofMillis(1000))
        def headersMap = getAllHeaders(consumedEvents)
        return headersMap.get('ce_correlationid')
    }

    def getAllHeaders(consumedEvents) {
        def headersMap = [:].withDefault { [] }
        consumedEvents.each { event ->
            event.headers().each { header ->
                def key = header.key()
                def value = new String(header.value())
                headersMap[key] << value
            }

        }
        return headersMap
    }
}
