package org.onap.cps.ncmp.impl.cmsubscription.producers

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.Data
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionMappersFacade
import org.onap.cps.ncmp.impl.cmsubscription.cache.DmiCacheHandler
import org.onap.cps.ncmp.utils.events.CloudEventMapper
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CmNotificationSubscriptionNcmpOutEventProducerSpec extends Specification {

    def mockEventsPublisher = Mock(EventsPublisher)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCmNotificationSubscriptionMappersHandler = Mock(CmNotificationSubscriptionMappersFacade)
    def mockDmiCmNotificationSubscriptionCacheHandler = Mock(DmiCacheHandler)

    def objectUnderTest = new CmNotificationSubscriptionNcmpOutEventProducer(mockEventsPublisher, jsonObjectMapper,
        mockCmNotificationSubscriptionMappersHandler, mockDmiCmNotificationSubscriptionCacheHandler)

    def 'Create and #scenario Cm Notification Subscription NCMP out event'() {
        given: 'a cm subscription response for the client'
            def subscriptionId = 'test-subscription-id-2'
            def eventType = 'subscriptionCreateResponse'
            def cmNotificationSubscriptionNcmpOutEvent = new CmNotificationSubscriptionNcmpOutEvent(data: new Data(subscriptionId: 'test-subscription-id-2', acceptedTargets: ['ch-1', 'ch-2']))
        and: 'also we have target topic for publishing to client'
            objectUnderTest.cmNotificationSubscriptionNcmpOutEventTopic = 'client-test-topic'
        and: 'a deadline to an event'
            objectUnderTest.cmNotificationSubscriptionDmiOutEventTimeoutInMs = 1000
        when: 'the event is published'
            objectUnderTest.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId, eventType, cmNotificationSubscriptionNcmpOutEvent, eventPublishingTaskToBeScheduled)
        then: 'we conditionally wait for a while'
            Thread.sleep(delayInMs)
        then: 'the event contains the required attributes'
            1 * mockEventsPublisher.publishCloudEvent(_, _, _) >> {
                args ->
                    {
                        assert args[0] == 'client-test-topic'
                        assert args[1] == subscriptionId
                        def cmNotificationSubscriptionNcmpOutEventAsCloudEvent = (args[2] as CloudEvent)
                        assert cmNotificationSubscriptionNcmpOutEventAsCloudEvent.getExtension('correlationid') == subscriptionId
                        assert cmNotificationSubscriptionNcmpOutEventAsCloudEvent.type == 'subscriptionCreateResponse'
                        assert cmNotificationSubscriptionNcmpOutEventAsCloudEvent.source.toString() == 'NCMP'
                        assert CloudEventMapper.toTargetEvent(cmNotificationSubscriptionNcmpOutEventAsCloudEvent, CmNotificationSubscriptionNcmpOutEvent) == cmNotificationSubscriptionNcmpOutEvent
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
            def cmNotificationSubscriptionNcmpOutEvent = new CmNotificationSubscriptionNcmpOutEvent(data: new Data(subscriptionId: 'test-subscription-id-3', acceptedTargets: ['ch-2', 'ch-3']))
        and: 'also we have target topic for publishing to client'
            objectUnderTest.cmNotificationSubscriptionNcmpOutEventTopic = 'client-test-topic'
        and: 'a deadline to an event'
            objectUnderTest.cmNotificationSubscriptionDmiOutEventTimeoutInMs = 1000
        when: 'the event is scheduled to be published'
            objectUnderTest.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId, eventType, cmNotificationSubscriptionNcmpOutEvent, true)
        then: 'we wait for 10ms and then we receive response from DMI'
            Thread.sleep(10)
        and: 'we receive response from DMI so we publish the message on demand'
            objectUnderTest.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId, eventType, cmNotificationSubscriptionNcmpOutEvent, false)
        then: 'the event contains the required attributes'
            1 * mockEventsPublisher.publishCloudEvent(_, _, _) >> {
                args ->
                    {
                        assert args[0] == 'client-test-topic'
                        assert args[1] == subscriptionId
                        def cmNotificationSubscriptionNcmpOutEventAsCloudEvent = (args[2] as CloudEvent)
                        assert cmNotificationSubscriptionNcmpOutEventAsCloudEvent.getExtension('correlationid') == subscriptionId
                        assert cmNotificationSubscriptionNcmpOutEventAsCloudEvent.type == 'subscriptionCreateResponse'
                        assert cmNotificationSubscriptionNcmpOutEventAsCloudEvent.source.toString() == 'NCMP'
                        assert CloudEventMapper.toTargetEvent(cmNotificationSubscriptionNcmpOutEventAsCloudEvent, CmNotificationSubscriptionNcmpOutEvent) == cmNotificationSubscriptionNcmpOutEvent
                    }
            }
        then: 'the cache handler is called once to remove accepted and rejected entries in cache'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.removeAcceptedAndRejectedDmiCmNotificationSubscriptionEntries(subscriptionId)
    }


}
