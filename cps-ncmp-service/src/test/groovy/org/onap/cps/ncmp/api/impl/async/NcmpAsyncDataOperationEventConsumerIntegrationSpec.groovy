package org.onap.cps.ncmp.api.impl.async

import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.kafka.CloudEventSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers
import java.util.concurrent.TimeUnit

@SpringBootTest(classes =[NcmpAsyncDataOperationEventConsumer, DataOperationRecordFilterStrategy])
@DirtiesContext
@Testcontainers
@EnableAutoConfiguration
class NcmpAsyncDataOperationEventConsumerIntegrationSpec extends MessagingBaseSpec {

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry

    @SpringBean
    EventsPublisher mockEventsPublisher = Mock()

    def activateListeners() {
        kafkaListenerEndpointRegistry.getListenerContainers().forEach(
                messageListenerContainer -> { ContainerTestUtils.waitForAssignment(messageListenerContainer, 1) }
        )
    }

    def 'Filtering Events.'() {
        given: 'a cloud event of type: #eventType'
            def cloudEvent = CloudEventBuilder.v1().withId("some-uuid")
                    .withType(eventType)
                    .withSource(URI.create("sample-test-source"))
                    .build();
        and: 'activate message listener container'
            activateListeners()
        when: 'send the cloud event'
            ProducerRecord<String, CloudEvent> record = new ProducerRecord<>('ncmp-async-m2m', cloudEvent)
            KafkaProducer<String, CloudEvent> producer = new KafkaProducer<>(eventProducerConfigProperties(CloudEventSerializer))
            producer.send(record);
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(100)
        then: 'the event has only been forwarded for the correct type'
            expectedNUmberOfCallsToPublishForwardedEvent * mockEventsPublisher.publishCloudEvent(_, _, _)
        where:
            eventType                                        || expectedNUmberOfCallsToPublishForwardedEvent
            'DataOperationEvent'                             || 1
            'other type'                                     || 0
            'any type contain the word "DataOperationEvent"' || 1
    }
}

