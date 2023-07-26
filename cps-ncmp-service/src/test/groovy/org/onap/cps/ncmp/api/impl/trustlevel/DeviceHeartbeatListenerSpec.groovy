package org.onap.cps.ncmp.api.impl.trustlevel

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.collection.ISet
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class DeviceHeartbeatListenerSpec extends Specification {

    def mockUntrustworthyCmHandlesSet = Mock(ISet<String>)
    def objectMapper = new ObjectMapper()

    def objectUnderTest = new DeviceHeartbeatListener(mockUntrustworthyCmHandlesSet, objectMapper)

    def 'Consume and store untrustworthy cmhandles for #scenario'() {
        given: 'an event with trustlevel as #trustLevel'
            def incomingEvent = testCloudEvent(trustLevel)
        and: 'transformed as a kafka record'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('test-device-heartbeat', 0, 0, 'cmhandle1', incomingEvent)
        when: 'the event is consumed'
            objectUnderTest.heartbeatListener(consumerRecord)
        then: 'untrustworthy cmhandles are stored'
            untrustworthyCmHandlesSetInvocation * mockUntrustworthyCmHandlesSet.add(_)
        where: 'below scenarios are applicable'
            scenario         | trustLevel          || untrustworthyCmHandlesSetInvocation
            'No trust'       | TrustLevel.NONE     || 1
            'Complete trust' | TrustLevel.COMPLETE || 0

    }

    def 'Cloud event to Concrete Event'() {
        expect: ''
            assert result == (objectUnderTest.toConcreteEvent(testCloudEvent(TrustLevel.COMPLETE), targetClass) != null)
        where: ''
            scenario                  | targetClass            || result
            'Correct concrete type'   | DeviceTrustLevel.class || true
            'Incorrect concrete type' | TrustLevel.class       || false
    }

    def testCloudEvent(trustLevel) {
        return CloudEventBuilder.v1().withData(objectMapper.writeValueAsBytes(new DeviceTrustLevel(trustLevel)))
            .withId("cmhandle1")
            .withSource(URI.create('DMI'))
            .withDataSchema(URI.create('test'))
            .withType('org.onap.cm.events.trustlevel-notification')
            .build()
    }

}
