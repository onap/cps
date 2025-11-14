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
import java.nio.charset.StandardCharsets
import java.time.Duration
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.EventConsumer
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp.NcmpInEventConsumer
import org.onap.cps.ncmp.impl.datajobs.subscription.utils.CmDataJobSubscriptionPersistenceService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import spock.util.concurrent.PollingConditions

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
    def ncmpInEventLogger = (Logger) LoggerFactory.getLogger(NcmpInEventConsumer)
    def dmiEventConsumerLogger = (Logger) LoggerFactory.getLogger(EventConsumer)

    def setup() {
        registerCmHandlesForSubscriptions()
        kafkaTestContainer.start()
        dmiInConsumer = kafkaTestContainer.getCloudEventConsumer('test-group')
        dmiInConsumer.subscribe([dmiInTopic])
        dmiInConsumer.poll(Duration.ofMillis(500))
        testRequestProducer = kafkaTestContainer.createProducer('test-client-id', StringSerializer.class)
        testResponseProducer = kafkaTestContainer.createProducer('test-client-id', CloudEventSerializer.class)
        listAppender.start()
        ncmpInEventLogger.addAppender(listAppender)
        dmiEventConsumerLogger.addAppender(listAppender)
    }

    def cleanup() {
        ncmpInEventLogger.detachAndStopAllAppenders()
        dmiEventConsumerLogger.detachAndStopAllAppenders()
        dmiInConsumer.unsubscribe()
        dmiInConsumer.close()
        testRequestProducer.close()
        testResponseProducer.close()
        kafkaTestContainer.close()
        deregisterCmHandles('dmi-0', ['cmHandle0'])
        deregisterCmHandles('dmi-1', ['cmHandle1', 'cmHandle2', 'cmHandle5'])
        deregisterCmHandles('dmi-2', ['cmHandle3', 'cmHandle4'])
    }

    def 'Create subscription and send to multiple DMIs'() {
        given: 'data node selector with two paths on DMI-1'
            def dmi1DataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"2\\\"]/child\\n'''
        and: 'data node selector with one path on DMI-2'
            def dmi2DataNodeSelector = '/parent[id=\\\"3\\\"]/child'
        and: 'an event payload'
            def eventDataNodeSelector = (dmi1DataNodeSelector + dmi2DataNodeSelector)
            def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'myDataJobId01', eventDataNodeSelector)
        when: 'a subscription create request is sent'
            sendSubscriptionRequest(subscriptionTopic, 'key', eventPayload, 'myDataJobId01', 'dataJobCreated')
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains('myDataJobId01') && msg.contains('dataJobCreated') }
        and: 'the 3 different data node selectors for the given data job id is persisted'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors('myDataJobId01').size() == 3
        and: 'get correlation ids from event sent to DMIs'
            def correlationIds = getAllConsumedCorrelationIds()
        and: 'there is correlation IDs (event) for each affected dmi (DMI-1, DMI-2)'
            assert correlationIds.size() == 2
            assert correlationIds.containsAll(['myDataJobId01#dmi-1', 'myDataJobId01#dmi-2'])
    }


    def 'Create subscription accepted by DMI.'() {
        given: 'a persisted subscription'
            def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'newDataJob', '/parent[id=\\\"0\\\"]\\n')
            sendSubscriptionRequest(subscriptionTopic, 'some key', eventPayload, 'newDataJob', 'dataJobCreated')
        when: 'dmi accepts the subscription create request'
            sendDmiResponse('1', 'ACCEPTED', 'subscriptionCreateResponse', 'dmi-0', 'newDataJob#dmi-0')
        then: 'there are no more inactive data node selector for given datajob id'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors('newDataJob').size() == 0
        and: 'status for the data node selector for given data job id is ACCEPTED'
            def affectedDataNodes = cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                    '//subscription/dataJobId[text()=\'newDataJob\']', DIRECT_CHILDREN_ONLY)
            assert affectedDataNodes.leaves.every(entry -> entry.get('status') == 'ACCEPTED')
    }

    def 'Create new subscription which partially overlaps with an existing active subscription'() {
        given: 'an active subscription in database'
            createAndAcceptSubscriptionA()
        and: 'and a partial overlapping subscription'
            def overlappingDmi1DataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"3\\\"]/child\\n'''
            def newDmi2DataNodeSelector = '/parent[id=\\\"4\\\"]'
            def eventDataNodeSelector = (overlappingDmi1DataNodeSelector + newDmi2DataNodeSelector)
            def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'partialOverlappingDataJobId', eventDataNodeSelector)
        when: 'create request event for overlapping subscription is sent'
            sendSubscriptionRequest(subscriptionTopic, 'some key', eventPayload, 'partialOverlappingDataJobId', 'dataJobCreated')
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains('partialOverlappingDataJobId') && msg.contains('dataJobCreated') }
        and: 'the 3 data node selectors for the given data job id is persisted'
            assert cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                '//subscription/dataJobId[text()=\'partialOverlappingDataJobId\']', DIRECT_CHILDREN_ONLY).size() == 3
        and: 'only one data node selector is not active'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors('partialOverlappingDataJobId').size() == 1
        and: 'get correlation ids from event sent to DMIs'
            def correlationIds = getAllConsumedCorrelationIds()
        and: 'there is correlation IDs (event) for only the affected dmi (DMI-2)'
            assert !correlationIds.contains('partialOverlappingDataJobId#dmi-1')
            assert correlationIds.contains('partialOverlappingDataJobId#dmi-2')
    }

    def 'Create new subscription which completely overlaps with an active existing subscriptions'() {
        given: 'a new data node selector'
            def dataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"2\\\"]/child\\n'''
        and: 'an event payload'
            def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'fullyOverlappingDataJobId', dataNodeSelector)
        and: 'existing active subscriptions in database'
            createAndAcceptSubscriptionA()
            createAndAcceptSubscriptionB()
        when: 'a new subscription create request is sent'
            sendSubscriptionRequest(subscriptionTopic, 'some key', eventPayload, 'fullyOverlappingDataJobId', 'dataJobCreated')
        then: 'log shows event is consumed by ncmp'
            def messages = listAppender.list*.formattedMessage
            messages.any { msg -> msg.contains('fullyOverlappingDataJobId') && msg.contains('dataJobCreated') }
        and: 'the 2 data node selectors for the given data job id is persisted'
            assert cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                    '//subscription/dataJobId[text()=\'fullyOverlappingDataJobId\']', DIRECT_CHILDREN_ONLY).size() == 2
        and: 'there are no inactive data node selector'
            assert cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors('fullyOverlappingDataJobId').size() == 0
        and: 'get correlation ids from event sent to DMIs'
            def correlationIds = getAllConsumedCorrelationIds()
        and: 'there is no correlation IDs (event) for any dmi'
            assert !correlationIds.any { correlationId -> correlationId.startsWith('fullyOverlappingDataJobId') }
    }

    def 'Delete subscription removes last subscriber.'() {
        given: 'an existing subscription with only one data node selector'
            def dataNodeSelector = '/parent[id=\\\"5\\\"]'
        and: 'a subscription created'
            def createEventPayload = createSubscriptionEventPayload('dataJobCreated', 'lastDataJobId', dataNodeSelector)
            sendSubscriptionRequest(subscriptionTopic, 'lastDataJobId', createEventPayload, 'lastDataJobId', 'dataJobCreated')
        and: 'data nodes is persisted '
            def dataNodes = cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                    "//subscription/dataJobId[text()=\'lastDataJobId\']", DIRECT_CHILDREN_ONLY)
            assert  dataNodes.size() == 1
            assert dataNodes.iterator().next().leaves.dataNodeSelector == '/parent[id="5"]'
        when: 'a delete event is received for the subscription'
            def deleteEventPayload = createSubscriptionEventPayload('dataJobDeleted', 'lastDataJobId', dataNodeSelector)
            sendSubscriptionRequest(subscriptionTopic, 'lastDataJobId', deleteEventPayload, 'lastDataJobId', 'dataJobDeleted')
        then: 'the subscription is fully removed from persistence'
            def remainingDataNodeSelector = cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                    "//subscription/dataJobId[text()=\'lastDataJobId\']", DIRECT_CHILDREN_ONLY)
            assert remainingDataNodeSelector.isEmpty()
        and: 'no other subscriptions exist for the same dataJobId'
            def remainingDataJobId = cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                    "//subscription/dataJobId[text()=\'lastDataJobId\']", DIRECT_CHILDREN_ONLY)
            assert remainingDataJobId.isEmpty()
        and: 'a DMI delete event is published for the affected DMI'
            def correlationIds = getAllConsumedCorrelationIds()
            assert correlationIds.contains('lastDataJobId#dmi-1')
    }

    def 'Delete subscription removes one of multiple subscribers.'() {
        given: 'data node selector that is used by other subscriptions'
            def dataNodeSelector = '/parent[id=\\\"1\\\"]'
            def existingSubscription = cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                    "/dataJob/subscription[@dataNodeSelector='/parent[id=\"1\"]']", DIRECT_CHILDREN_ONLY).iterator().next().leaves.dataJobId
            assert !existingSubscription.isEmpty()
        and: 'a new subscription'
            def createEventPayload1 = createSubscriptionEventPayload('dataJobCreated', 'id-to-remove', dataNodeSelector)
            sendSubscriptionRequest(subscriptionTopic, 'id-to-remove', createEventPayload1, 'id-to-remove', 'dataJobCreated')
        when: 'a delete event is received'
            def deleteEventPayload = createSubscriptionEventPayload('dataJobDeleted', 'id-to-remove', dataNodeSelector)
            sendSubscriptionRequest(subscriptionTopic, 'id-to-remove', deleteEventPayload, 'id-to-remove', 'dataJobDeleted')
        then: 'the data job id does not exist in database'
            def resultForDeletedSubscription = cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                    "//subscription/dataJobId[text()='id-to-remove']", DIRECT_CHILDREN_ONLY)
            assert resultForDeletedSubscription.isEmpty()
        and: 'subscription still exist for the same data node selector'
            def remainingSubscriptions = cpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                    "/dataJob/subscription[@dataNodeSelector='/parent[id=\"1\"]']", DIRECT_CHILDREN_ONLY).iterator().next().leaves.dataJobId
            assert !remainingSubscriptions.isEmpty()
            assert !remainingSubscriptions.contains('id-to-remove')
        and: 'no DMI delete event is published'
            def correlationIds = getAllConsumedCorrelationIds()
            assert !correlationIds.contains(['id-to-remove#dmi-1'])
    }

    def 'Deleting non-existent subscription.'() {
        given: 'an event payload'
            def deleteEventPayload = createSubscriptionEventPayload('dataJobDeleted', 'nonExistingDataJobId', '/nonExisting')
        when: 'a delete event is received for the non-existent subscription'
            sendSubscriptionRequest(subscriptionTopic, 'nonExistingDataJobId', deleteEventPayload, 'nonExistingDataJobId', 'dataJobDeleted')
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'nothing is sent to DMI'
            getAllConsumedCorrelationIds().isEmpty()
    }

    def registerCmHandlesForSubscriptions() {
        registerCmHandle('dmi-0', 'cmHandle0', '', '/parent=0')
        registerCmHandle('dmi-1', 'cmHandle1', '', '/parent=1')
        registerCmHandle('dmi-1', 'cmHandle2', '', '/parent=2')
        registerCmHandle('dmi-2', 'cmHandle3', '', '/parent=3')
        registerCmHandle('dmi-2', 'cmHandle4', '', '/parent=4')
        registerCmHandle('dmi-1', 'cmHandle5', '', '/parent=5')
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
        sendSubscriptionRequest(subscriptionTopic, 'dataJobA', eventPayload, 'dataJobA', 'dataJobCreated')
        sendDmiResponse('1', 'ACCEPTED', 'subscriptionCreateResponse', 'dmi-1', 'dataJobA#dmi-1')
        sendDmiResponse('1', 'ACCEPTED', 'subscriptionCreateResponse', 'dmi-2', 'dataJobA#dmi-2')
    }

    def createAndAcceptSubscriptionB() {
        def dataNodeSelector = '''/parent[id=\\\"1\\\"]\\n/parent[id=\\\"3\\\"]/child\\n/parent[id=\\\"4\\\"]'''
        def eventPayload = createSubscriptionEventPayload('dataJobCreated', 'dataJobB', dataNodeSelector)
        sendSubscriptionRequest(subscriptionTopic, 'dataJobB', eventPayload, 'dataJobB', 'dataJobCreated')
        sendDmiResponse('1', 'ACCEPTED', 'subscriptionCreateResponse', 'dmi-2', 'dataJobB#dmi-2')
    }

    def sendSubscriptionRequest(topic, eventKey, eventPayload, dataJobId, eventType) {
        def event = new ProducerRecord<>(topic, eventKey, eventPayload)
        testRequestProducer.send(event)
        def expectedMessageWhenFinishedProcessingEvent = 'NCMP In Event with eventType=' + eventType + ' has been Processed for dataJobId='+ dataJobId
        assertEventProcessedBasedOnLogging(expectedMessageWhenFinishedProcessingEvent)
    }

    def sendDmiResponse(statusCode, statusMessage, eventType, eventSource, correlationId) {
        def eventPayload = readResourceDataFile('datajobs/subscription/dmiSubscriptionResponseEvent.json')
        eventPayload = eventPayload.replace('#statusCode', statusCode)
        eventPayload = eventPayload.replace('#statusMessage', statusMessage)
        def cloudEvent = CloudEventBuilder.v1()
                .withData(eventPayload.getBytes(StandardCharsets.UTF_8))
                .withId('random-uuid')
                .withType(eventType)
                .withSource(URI.create(eventSource))
                .withExtension('correlationid', correlationId).build()
        def event = new ProducerRecord<>(dmiOutTopic, 'key', cloudEvent)
        testResponseProducer.send(event)
        def expectedMessageWhenFinishedProcessingEvent = 'Finished processing DMI subscription response event with details: | correlationId=' + correlationId + ' | eventType=' + eventType
        assertEventProcessedBasedOnLogging(expectedMessageWhenFinishedProcessingEvent)
    }

    def assertEventProcessedBasedOnLogging(expectedMessageInLog) {
        new PollingConditions().within(2) {
            def messages = listAppender.list*.formattedMessage
            def eventProcessed = messages.any { msg -> msg.contains(expectedMessageInLog) }
            assert eventProcessed
        }
    }

    def getAllConsumedCorrelationIds() {
        def consumedEvents = getLatestConsumerRecordsWithMaxPollOf1Second(dmiInConsumer, 1)
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
