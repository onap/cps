package org.onap.cps.notification


import org.onap.cps.event.model.Content
import org.onap.cps.event.model.CpsDataUpdatedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFuture
import spock.lang.Specification

// TODO Use test conatiners
class NotificationPublisherSpec extends Specification {

    def mockKafkaTemplate = Mock(KafkaTemplate);

    final TOPIC_NAME = 'eventTopic'
    final ANCHOR_NAME = 'my-anchor'
    final DATASPACE_NAME = 'my-dataspace'

    def cpsDataUpdatedEvent = new CpsDataUpdatedEvent()
        .withContent( new Content()
                        .withDataspaceName(DATASPACE_NAME)
                        .withAnchorName(ANCHOR_NAME))

    NotificationPublisher notificationPublisher = new NotificationPublisher(mockKafkaTemplate, TOPIC_NAME);

    def 'Sending event To Kafka with correct Message Key.'() {
        given: 'Event to be published'
            def eventTobePublised = cpsDataUpdatedEvent

        when: 'Event is sent to Kafka'
            notificationPublisher.sendNotification(eventTobePublised)

        then: 'Event is sent to the expected kafka topic with the expected key'
            interaction {
                def messageKey = DATASPACE_NAME + "-" + ANCHOR_NAME
                1 * mockKafkaTemplate.send(TOPIC_NAME, messageKey, eventTobePublised) >>
                        Mock(ListenableFuture<SendResult<String, CpsDataUpdatedEvent>>)
            }
    }

}
