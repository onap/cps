package org.onap.cps.ncmp.api.impl.events.cmsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.Data
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CmNotificationSubscriptionNcmpOutEventProducerSpec extends Specification {

    def mockEventsPublisher = Mock(EventsPublisher)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def objectUnderTest = new CmNotificationSubscriptionNcmpOutEventProducer(mockEventsPublisher, jsonObjectMapper)

    def 'Create and Publish Cm Notification Subscription DMI In Event'() {
        given: 'a cm subscription response for the client'
            def subscriptionId = 'test-subscription-id'
            def eventType = 'subscriptionCreateResponse'
            def cmNotificationSubscriptionNcmpOutEvent = new CmNotificationSubscriptionNcmpOutEvent(data: new Data(subscriptionId: 'sub-1', acceptedTargets: ['ch-1', 'ch-2']))
        and: 'also we have target topic for publishing to client'
            objectUnderTest.cmNotificationSubscriptionNcmpOutEventTopic = 'client-test-topic'
        when: 'the event is published'
            objectUnderTest.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId, eventType, cmNotificationSubscriptionNcmpOutEvent)
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
    }


}
