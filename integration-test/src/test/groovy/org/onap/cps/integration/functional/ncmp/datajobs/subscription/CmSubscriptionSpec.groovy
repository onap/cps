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

import ch.qos.logback.classic.Logger
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

    def setupSpec() {
        registerCmHandlesForSubscriptions()
    }

    def setup() {
        registerCmHandlesForSubscriptions()
        kafkaTestContainer.start()
        dmiInConsumer = kafkaTestContainer.getConsumer('test-group', CloudEventDeserializer.class)
        dmiInConsumer.subscribe([dmiInTopic])
        dmiInConsumer.poll(Duration.ofMillis(500))
        testRequestProducer = kafkaTestContainer.createProducer("test-group", StringSerializer.class)
        testResponseProducer = kafkaTestContainer.createProducer("test-group", CloudEventSerializer.class)
        logger = (Logger) LoggerFactory.getLogger(NcmpInEventConsumer)
        listAppender.start()
        logger.addAppender(listAppender)
    }

    def cleanup() {
        dmiInConsumer.unsubscribe()
        dmiInConsumer.close()
        testRequestProducer.close()
        testResponseProducer.close()
        kafkaTestContainer.close()
    }

    def cleanupSpec() {
        deregisterCmHandles("dmi-0", ["cmHandle0"])
        deregisterCmHandles("dmi-1", ["cmHandle1", "cmHandle2"])
        deregisterCmHandles("dmi-2", ["cmHandle3", "cmHandle4"])
    }

    def 'Create subscription A and send to multiple DMIs'() {
        given: 'event with the following details for subscription create request'
            def eventPayload = readResourceDataFile('datajobs/subscription/createSubscriptionEvent.json')
            def eventType = "dataJobCreated"
            def dataJobId = "dataJobA"
            def dataNodeSelector = "" +
                "/parent[id=\\\"1\\\"]\\n" +
                "/parent[id=\\\"2\\\"]/child\\n" +
                "/parent[id=\\\"3\\\"]/child"
            eventPayload = eventPayload.replace('#eventType', eventType)
            eventPayload = eventPayload.replace('#dataJobId', dataJobId)
            eventPayload = eventPayload.replace('#dataNodeSelector', dataNodeSelector)
        when: 'a subscription create request is sent'
            sendSubscriptionCreateRequest(subscriptionTopic, "key", eventPayload)
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains("dataJobA") && msg.contains("dataJobCreated")}
        and: 'the data node selectors for the given data job id is persisted'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors(dataJobId).size() == 3
        and: 'correct number of events is sent to the correct dmis'
            def correlationIds = getAllConsumedCorrelationIds()
            assert correlationIds.size() == 2
            assert correlationIds.contains("dataJobA#dmi-1")
            assert correlationIds.contains("dataJobA#dmi-2")
    }

    def 'Update subscription status'() {
        given: 'a persisted subscription'
            def eventPayload = readResourceDataFile('datajobs/subscription/createSubscriptionEvent.json')
            def eventType = "dataJobCreated"
            def dataJobId = "newDataJob"
            def dataNodeSelector = "/parent[id=\\\"0\\\"]\\n"
            eventPayload = eventPayload.replace('#eventType', eventType)
            eventPayload = eventPayload.replace('#dataJobId', dataJobId)
            eventPayload = eventPayload.replace('#dataNodeSelector', dataNodeSelector)
            sendSubscriptionCreateRequest(subscriptionTopic, dataJobId, eventPayload)
        when: 'dmi accepts the subscription create request'
            sendDmiResponse("1", "ACCEPTED", "subscriptionCreateResponse", "dmi-0", "newDataJob#dmi-0")
        then: 'there are no more inactive data node selector for given datajob id'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors(dataJobId).size() == 0
        and: 'status for the data node selector for given data job id is ACCEPTED'
            def affectedDataNodes =  cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                '//subscription/dataJobId[text()=\'newDataJob\']', DIRECT_CHILDREN_ONLY)
            assert affectedDataNodes.leaves.every( entry -> entry.get("status") == "ACCEPTED")
    }

    def 'Create new subscription which partially overlaps with an existing active subscription'() {
        given: 'event with the following details for subscription create request'
            def eventPayload = readResourceDataFile('datajobs/subscription/createSubscriptionEvent.json')
            def eventType = "dataJobCreated"
            def dataJobId = "partialOverlappingDataJob"
            def dataNodeSelector = "" +
                "/parent[id=\\\"1\\\"]\\n" +
                "/parent[id=\\\"3\\\"]/child\\n" +
                "/parent[id=\\\"4\\\"]"
            eventPayload = eventPayload.replace('#eventType', eventType)
            eventPayload = eventPayload.replace('#dataJobId', dataJobId)
            eventPayload = eventPayload.replace('#dataNodeSelector', dataNodeSelector)
        and: 'an existing active subscription in database'
            createAndAcceptSubscriptionA()
        when: 'a new subscription create request is sent'
            sendSubscriptionCreateRequest(subscriptionTopic, dataJobId, eventPayload)
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains(dataJobId) && msg.contains("dataJobCreated")}
        and: 'the data node selectors for the given data job id is persisted'
            assert cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                '//subscription/dataJobId[text()=\'partialOverlappingDataJob\']', DIRECT_CHILDREN_ONLY).size() == 3
        and: 'only one data node selector is not active'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors(dataJobId).size() == 1
        and: 'the event is sent only to the correct dmi'
            def correlationIds = getAllConsumedCorrelationIds()
            assert !correlationIds.contains("partialOverlappingDataJob#dmi-1")
            assert correlationIds.contains("partialOverlappingDataJob#dmi-2")
    }

    def 'Create new subscription which completely overlaps with an active existing subscriptions'() {
        given: 'event with the following details for subscription create request'
            def eventPayload = readResourceDataFile('datajobs/subscription/createSubscriptionEvent.json')
            def eventType = "dataJobCreated"
            def dataJobId = "fullyOverlappingDataJob"
            def dataNodeSelector = "" +
                "/parent[id=\\\"1\\\"]\\n" +
                "/parent[id=\\\"2\\\"]/child\\n"
            eventPayload = eventPayload.replace('#eventType', eventType)
            eventPayload = eventPayload.replace('#dataJobId', dataJobId)
            eventPayload = eventPayload.replace('#dataNodeSelector', dataNodeSelector)
        and: 'existing active subscriptions in database'
            createAndAcceptSubscriptionA()
            createAndAcceptSubscriptionB()
        when: 'a new subscription create request is sent'
            sendSubscriptionCreateRequest(subscriptionTopic, dataJobId, eventPayload)
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains(dataJobId) && msg.contains("dataJobCreated")}
        and: 'the data node selectors for the given data job id is persisted'
            assert cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                '//subscription/dataJobId[text()=\'fullyOverlappingDataJob\']', DIRECT_CHILDREN_ONLY).size() == 2
        and: 'there are no inactive data node selector'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors(dataJobId).size() == 0
        and: 'no event is sent to any dmi'
            def correlationIds = getAllConsumedCorrelationIds()
            assert !correlationIds.any { correlationId -> correlationId.startsWith(dataJobId) }
    }

    def registerCmHandlesForSubscriptions() {
        registerCmHandle("dmi-0", "cmHandle0", "","/parent=0")
        registerCmHandle("dmi-1", "cmHandle1", "","/parent=1")
        registerCmHandle("dmi-1", "cmHandle2", "","/parent=2")
        registerCmHandle("dmi-2", "cmHandle3", "","/parent=3")
        registerCmHandle("dmi-2", "cmHandle4", "","/parent=4")
    }

    def createAndAcceptSubscriptionA() {
        def eventPayload = readResourceDataFile('datajobs/subscription/createSubscriptionEvent.json')
        def eventType = "dataJobCreated"
        def dataJobId = "dataJobA"
        def dataNodeSelector = "" +
            "/parent[id=\\\"1\\\"]\\n" +
            "/parent[id=\\\"2\\\"]/child\\n" +
            "/parent[id=\\\"3\\\"]/child"
        eventPayload = eventPayload.replace('#eventType', eventType)
        eventPayload = eventPayload.replace('#dataJobId', dataJobId)
        eventPayload = eventPayload.replace('#dataNodeSelector', dataNodeSelector)

        sendSubscriptionCreateRequest(subscriptionTopic, "dataJobA", eventPayload)
        sendDmiResponse("1", "ACCEPTED", "subscriptionCreateResponse", "dmi-1", "dataJobA#dmi-1")
        sendDmiResponse("1", "ACCEPTED", "subscriptionCreateResponse", "dmi-2", "dataJobA#dmi-2")
    }

    def createAndAcceptSubscriptionB() {
        def eventPayload = readResourceDataFile('datajobs/subscription/createSubscriptionEvent.json')
        def eventType = "dataJobCreated"
        def dataJobId = "dataJobB"
        def dataNodeSelector = "" +
            "/parent[id=\\\"1\\\"]\\n" +
            "/parent[id=\\\"3\\\"]/child\\n" +
            "/parent[id=\\\"4\\\"]"
        eventPayload = eventPayload.replace('#eventType', eventType)
        eventPayload = eventPayload.replace('#dataJobId', dataJobId)
        eventPayload = eventPayload.replace('#dataNodeSelector', dataNodeSelector)

        sendSubscriptionCreateRequest(subscriptionTopic, "dataJobB", eventPayload)
        sendDmiResponse("1", "ACCEPTED", "subscriptionCreateResponse", "dmi-2", "dataJobB#dmi-2")
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
        def event = new ProducerRecord<>(dmiOutTopic, "key", cloudEvent);
        testResponseProducer.send(event)
        sleep(2000)
    }

    def getAllConsumedCorrelationIds() {
        def consumedEvents = dmiInConsumer.poll(Duration.ofMillis(1000))
        def headersMap = getAllHeaders(consumedEvents)
        def correlationIds = headersMap.get("ce_correlationid")
        return correlationIds
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
