package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.config.CpsApplicationContext
import org.onap.cps.ncmp.impl.cmnotificationsubscription.cache.DmiCacheHandler
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.Data
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent
import org.onap.cps.ncmp.utils.events.CloudEventMapper
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
@ContextConfiguration(classes = [CpsApplicationContext])
class NcmpOutEventProducerSpec extends Specification {

    def mockEventsPublisher = Mock(EventsPublisher)
    def mockNcmpOutEventMapper = Mock(NcmpOutEventMapper)
    def mockDmiCacheHandler = Mock(DmiCacheHandler)

    def objectUnderTest = new NcmpOutEventProducer(mockEventsPublisher, mockNcmpOutEventMapper, mockDmiCacheHandler)

    def 'Create and #scenario Cm Notification Subscription NCMP out event'() {
        given: 'a cm subscription response for the client'
            def subscriptionId = 'test-subscription-id-2'
            def eventType = 'subscriptionCreateResponse'
            def ncmpOutEvent = new NcmpOutEvent(data: new Data(subscriptionId: 'test-subscription-id-2', acceptedTargets: ['ch-1', 'ch-2']))
        and: 'also we have target topic for publishing to client'
            objectUnderTest.ncmpOutEventTopic = 'client-test-topic'
        and: 'a deadline to an event'
            objectUnderTest.dmiOutEventTimeoutInMs = 1000
        when: 'the event is published'
            objectUnderTest.publishNcmpOutEvent(subscriptionId, eventType, ncmpOutEvent, eventPublishingTaskToBeScheduled)
        then: 'we conditionally wait for a while'
            Thread.sleep(delayInMs)
        then: 'the event contains the required attributes'
            1 * mockEventsPublisher.publishCloudEvent(_, _, _) >> {
                args ->
                    {
                        assert args[0] == 'client-test-topic'
                        assert args[1] == subscriptionId
                        def ncmpOutEventAsCloudEvent = (args[2] as CloudEvent)
                        assert ncmpOutEventAsCloudEvent.getExtension('correlationid') == subscriptionId
                        assert ncmpOutEventAsCloudEvent.type == 'subscriptionCreateResponse'
                        assert ncmpOutEventAsCloudEvent.source.toString() == 'NCMP'
                        assert CloudEventMapper.toTargetEvent(ncmpOutEventAsCloudEvent, NcmpOutEvent) == ncmpOutEvent
                    }
            }
        where: 'following scenarios are considered'
            scenario                                          | delayInMs | eventPublishingTaskToBeScheduled
            'publish event now'                               | 0         | false
            'schedule and publish after the configured time ' | 1500      | true
    }

    def 'Schedule Cm Notification Subscription NCMP out event but later publish it on demand'() {
        given: 'a cm subscription response for the client'
            def subscriptionId = 'test-subscription-id-3'
            def eventType = 'subscriptionCreateResponse'
            def ncmpOutEvent = new NcmpOutEvent(data: new Data(subscriptionId: 'test-subscription-id-3', acceptedTargets: ['ch-2', 'ch-3']))
        and: 'also we have target topic for publishing to client'
            objectUnderTest.ncmpOutEventTopic = 'client-test-topic'
        and: 'a deadline to an event'
            objectUnderTest.dmiOutEventTimeoutInMs = 1000
        when: 'the event is scheduled to be published'
            objectUnderTest.publishNcmpOutEvent(subscriptionId, eventType, ncmpOutEvent, true)
        then: 'we wait for 10ms and then we receive response from DMI'
            Thread.sleep(10)
        and: 'we receive response from DMI so we publish the message on demand'
            objectUnderTest.publishNcmpOutEvent(subscriptionId, eventType, ncmpOutEvent, false)
        then: 'the event contains the required attributes'
            1 * mockEventsPublisher.publishCloudEvent(_, _, _) >> {
                args ->
                    {
                        assert args[0] == 'client-test-topic'
                        assert args[1] == subscriptionId
                        def ncmpOutEventAsCloudEvent = (args[2] as CloudEvent)
                        assert ncmpOutEventAsCloudEvent.getExtension('correlationid') == subscriptionId
                        assert ncmpOutEventAsCloudEvent.type == 'subscriptionCreateResponse'
                        assert ncmpOutEventAsCloudEvent.source.toString() == 'NCMP'
                        assert CloudEventMapper.toTargetEvent(ncmpOutEventAsCloudEvent, NcmpOutEvent) == ncmpOutEvent
                    }
            }
        then: 'the cache handler is called once to remove accepted and rejected entries in cache'
            1 * mockDmiCacheHandler.removeAcceptedAndRejectedDmiSubscriptionEntries(subscriptionId)
    }

    def 'No event published when NCMP out event is null'() {
        given: 'a cm subscription response for the client'
            def subscriptionId = 'test-subscription-id-3'
            def eventType = 'subscriptionCreateResponse'
            def ncmpOutEvent = null
        and: 'also we have target topic for publishing to client'
            objectUnderTest.ncmpOutEventTopic = 'client-test-topic'
        and: 'a deadline to an event'
            objectUnderTest.dmiOutEventTimeoutInMs = 1000
        when: 'the event is scheduled to be published'
            objectUnderTest.publishNcmpOutEvent(subscriptionId, eventType, ncmpOutEvent, true)
        then: 'we wait for 10ms and then we receive response from DMI'
            Thread.sleep(10)
        and: 'we receive NO response from DMI so we publish the message on demand'
            objectUnderTest.publishNcmpOutEvent(subscriptionId, eventType, ncmpOutEvent, false)
        and: 'no event published'
            0 * mockEventsPublisher.publishCloudEvent(*_)
    }

}
