package org.onap.cps.ncmp.api.impl.events.cmsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.events.cmsubscription.mapper.CmNotificationSubscriptionNcmpOutEventMapper
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails
import org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.Data
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CmNotificationSubscriptionNcmpOutEventProducerSpec extends Specification {

    def mockEventsPublisher = Mock(EventsPublisher)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCmNotificationSubscriptionCache = Mock(Map<String, Map<String, DmiCmNotificationSubscriptionDetails>>)
    def mockCmNotificationSubscriptionNcmpOutEventMapper = Mock(CmNotificationSubscriptionNcmpOutEventMapper)

    def objectUnderTest = new CmNotificationSubscriptionNcmpOutEventProducer(mockEventsPublisher, jsonObjectMapper, mockCmNotificationSubscriptionCache, mockCmNotificationSubscriptionNcmpOutEventMapper)

    def 'Create and #scenario Cm Notification Subscription NCMP out event'() {
        given: 'a cm subscription response for the client'
            def subscriptionId = 'test-subscription-id-2'
            def eventType = 'subscriptionCreateResponse'
            def cmNotificationSubscriptionNcmpOutEvent = new CmNotificationSubscriptionNcmpOutEvent(data: new Data(subscriptionId: 'test-subscription-id-2', acceptedTargets: ['ch-1', 'ch-2']))
            def scheduledTaskParameters = new CmNotificationSubscriptionNcmpOutEventPublishingTaskParameters(isScheduledEvent, cancelOngoingTask)
        and: 'also we have target topic for publishing to client'
            objectUnderTest.cmNotificationSubscriptionNcmpOutEventTopic = 'client-test-topic'
        and: 'a deadline to an event'
            objectUnderTest.cmNotificationSubscriptionDmiOutEventTimeoutInMs = 1000
        when: 'the event is published'
            objectUnderTest.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId, eventType, cmNotificationSubscriptionNcmpOutEvent, scheduledTaskParameters)
        then: 'we conditionally wait for a while'
            Thread.sleep(delayInMs)
        then: 'the event contains the required attributes'
            publishCloudEventInvocations * mockEventsPublisher.publishCloudEvent(_, _, _) >> {
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
            scenario                   | delayInMs | isScheduledEvent | cancelOngoingTask || publishCloudEventInvocations
            'publish event now'        | 0         | false            | false             || 1
            'publish event now'        | 0         | false            | true              || 1
            'schedule event for later' | 1500      | true             | false             || 1
            'schedule event for later' | 1500      | true             | true              || 0
    }


}
