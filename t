[1mdiff --git a/cps-application/src/main/resources/application.yml b/cps-application/src/main/resources/application.yml[m
[1mindex 8f608ac2..41dc99a2 100644[m
[1m--- a/cps-application/src/main/resources/application.yml[m
[1m+++ b/cps-application/src/main/resources/application.yml[m
[36m@@ -97,10 +97,13 @@[m [mapp:[m
     ncmp:[m
         async-m2m:[m
             topic: ${NCMP_ASYNC_M2M_TOPIC:ncmp-async-m2m}[m
[32m+[m[32m        avc:[m[41m[m
[32m+[m[32m            forward-topic: ${AVC_FORWARD_TOPIC:cm-events}[m[41m[m
     lcm:[m
         events:[m
             topic: ${LCM_EVENTS_TOPIC:ncmp-events}[m
 [m
[32m+[m[41m[m
 notification:[m
     enabled: true[m
     data-updated:[m
[1mdiff --git a/cps-ncmp-service/src/main/java/org/onap/cps/ncmp/api/impl/notifications/avc/AvcEventProducer.java b/cps-ncmp-service/src/main/java/org/onap/cps/ncmp/api/impl/notifications/avc/AvcEventProducer.java[m
[1mindex 2f4ab786..b1a9a1df 100644[m
[1m--- a/cps-ncmp-service/src/main/java/org/onap/cps/ncmp/api/impl/notifications/avc/AvcEventProducer.java[m
[1m+++ b/cps-ncmp-service/src/main/java/org/onap/cps/ncmp/api/impl/notifications/avc/AvcEventProducer.java[m
[36m@@ -23,8 +23,12 @@[m [mpackage org.onap.cps.ncmp.api.impl.notifications.avc;[m
 import lombok.RequiredArgsConstructor;[m
 import lombok.extern.slf4j.Slf4j;[m
 import org.onap.cps.ncmp.event.model.AvcEvent;[m
[32m+[m[32mimport org.springframework.beans.factory.annotation.Value;[m
 import org.springframework.kafka.core.KafkaTemplate;[m
 import org.springframework.stereotype.Service;[m
[32m+[m[32mimport org.springframework.util.StringUtils;[m
[32m+[m
[32m+[m[32mimport java.util.UUID;[m
 [m
 /**[m
  * Producer for AVC events.[m
[36m@@ -36,13 +40,25 @@[m [mpublic class AvcEventProducer {[m
 [m
     private final KafkaTemplate<String, AvcEvent> kafkaTemplate;[m
 [m
[32m+[m[32m    @Value("${app.ncmp.avc.forward-topic}")[m
[32m+[m[32m    final String forwardTopic;[m
[32m+[m
[32m+[m[32m    private final AvcEventMapper avcEventMapper;[m
[32m+[m
     /**[m
      * Sends message to the configured topic with a message key.[m
      *[m
      * @param avcEvent message payload[m
      */[m
[31m-    public void sendMessage(final AvcEvent avcEvent) {[m
[31m-        log.debug("Forwarding AVC event {} to topic {} ", avcEvent.getEventId(), "clientTopic");[m
[31m-        kafkaTemplate.send("clientTopic", avcEvent.getEventId(), avcEvent);[m
[32m+[m[32m    public void sendMessage(AvcEvent avcEvent) {[m
[32m+[m[32m        // generate new event id while keeping other data[m
[32m+[m[32m        avcEvent = avcEventMapper.toOutgoingAvcEvent(avcEvent);[m
[32m+[m
[32m+[m[32m        if (StringUtils.isEmpty(forwardTopic)) {[m
[32m+[m[32m            log.debug("No forward topic set. Not forwarding message.");[m
[32m+[m[32m        } else {[m
[32m+[m[32m            log.debug("Forwarding AVC event {} to topic {} ", avcEvent.getEventId(), forwardTopic);[m
[32m+[m[32m            kafkaTemplate.send(forwardTopic, avcEvent.getEventId(), avcEvent);[m
[32m+[m[32m        }[m
     }[m
 }[m
[1mdiff --git a/cps-ncmp-service/src/test/groovy/org/onap/cps/ncmp/api/impl/notifications/avc/AvcEventProducerIntegrationSpec.groovy b/cps-ncmp-service/src/test/groovy/org/onap/cps/ncmp/api/impl/notifications/avc/AvcEventProducerIntegrationSpec.groovy[m
[1mindex b2b23f9f..c22563c0 100644[m
[1m--- a/cps-ncmp-service/src/test/groovy/org/onap/cps/ncmp/api/impl/notifications/avc/AvcEventProducerIntegrationSpec.groovy[m
[1m+++ b/cps-ncmp-service/src/test/groovy/org/onap/cps/ncmp/api/impl/notifications/avc/AvcEventProducerIntegrationSpec.groovy[m
[36m@@ -22,6 +22,8 @@[m [mpackage org.onap.cps.ncmp.api.impl.notifications.avc[m
 [m
 import com.fasterxml.jackson.databind.ObjectMapper[m
 import org.apache.kafka.clients.consumer.KafkaConsumer[m
[32m+[m[32mimport org.mapstruct.factory.Mappers[m
[32m+[m[32mimport org.onap.cps.ncmp.api.impl.async.NcmpAsyncRequestResponseEventMapper[m
 import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec[m
 import org.onap.cps.ncmp.event.model.AvcEvent[m
 import org.onap.cps.ncmp.utils.TestUtils[m
[36m@@ -40,7 +42,10 @@[m [mimport java.time.Duration[m
 class AvcEventProducerIntegrationSpec extends MessagingBaseSpec {[m
 [m
     @SpringBean[m
[31m-    AvcEventProducer avcEventProducer = new AvcEventProducer(kafkaTemplate);[m
[32m+[m[32m    AvcEventMapper avcEventMapper = Mappers.getMapper(AvcEventMapper.class)[m
[32m+[m
[32m+[m[32m    @SpringBean[m
[32m+[m[32m    AvcEventProducer avcEventProducer = new AvcEventProducer(kafkaTemplate, "cm-events", avcEventMapper)[m
 [m
     @SpringBean[m
     AvcEventConsumer acvEventConsumer = new AvcEventConsumer(avcEventProducer)[m
[36m@@ -48,11 +53,11 @@[m [mclass AvcEventProducerIntegrationSpec extends MessagingBaseSpec {[m
     @Autowired[m
     JsonObjectMapper jsonObjectMapper[m
 [m
[31m-    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('test'))[m
[32m+[m[32m    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('ncmp-group'))[m
 [m
     def 'Consume and forward valid message'() {[m
         given: 'consumer has a subscription'[m
[31m-            kafkaConsumer.subscribe(['clientTopic'] as List<String>)[m
[32m+[m[32m            kafkaConsumer.subscribe(['cm-events'] as List<String>)[m
         and: 'an event is sent'[m
             def jsonData = TestUtils.getResourceFileContent('avcEvent.json')[m
             def testEventSent = jsonObjectMapper.convertJsonString(jsonData, AvcEvent.class)[m
[36m@@ -62,10 +67,15 @@[m [mclass AvcEventProducerIntegrationSpec extends MessagingBaseSpec {[m
             def records = kafkaConsumer.poll(Duration.ofMillis(1500))[m
         then: 'poll returns one record'[m
             assert records.size() == 1[m
[31m-        and: 'consumed forwarded event id is event id'[m
[32m+[m[32m        and: 'record can be converted to AVC event'[m
             def record = records.iterator().next()[m
[31m-            assert testEventSent.eventId.equalsIgnoreCase([m
[31m-                    jsonObjectMapper.convertJsonString(record.value(), AvcEvent).getEventId())[m
[32m+[m[32m            def convertedAvcEvent = jsonObjectMapper.convertJsonString(record.value(), AvcEvent)[m
[32m+[m[32m        and: 'consumed forwarded NCMP event id differs from DMI event id'[m
[32m+[m[32m            assert testEventSent.eventId != convertedAvcEvent.getEventId()[m
[32m+[m[32m        and: 'correlation id matches'[m
[32m+[m[32m            assert testEventSent.eventCorrelationId == convertedAvcEvent.getEventCorrelationId()[m
[32m+[m[32m        and: 'target matches'[m
[32m+[m[32m            assert testEventSent.eventTarget == convertedAvcEvent.getEventTarget()[m
     }[m
 [m
[31m-}[m
[32m+[m[32m}[m
\ No newline at end of file[m
