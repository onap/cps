/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.config

import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.CloudEventSerializer
import org.spockframework.spring.EnableSharedInjection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.test.context.TestPropertySource
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(classes = [KafkaProperties, KafkaConfig])
@EnableSharedInjection
@EnableConfigurationProperties
@TestPropertySource(properties = ["cps.tracing.enabled=true"])
class KafkaConfigSpec extends Specification {

    @Shared
    @Autowired
    KafkaTemplate<String, String> legacyEventKafkaTemplate

    @Shared
    @Autowired
    KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate

    def 'Verify kafka template serializer and deserializer configuration for #eventType.'() {
        expect: 'kafka template is instantiated'
            assert kafkaTemplateInstance.properties['beanName'] == beanName
        and: 'verify event key and value serializer'
            assert kafkaTemplateInstance.properties['producerFactory'].configs['value.serializer'].asType(String.class).contains(valueSerializer.getCanonicalName())
        and: 'verify event key and value deserializer'
            assert kafkaTemplateInstance.properties['consumerFactory'].configs['spring.deserializer.value.delegate.class'].asType(String.class).contains(delegateDeserializer.getCanonicalName())
        where: 'the following event type is used'
            eventType      | kafkaTemplateInstance    || beanName                   | valueSerializer      | delegateDeserializer
            'legacy event' | legacyEventKafkaTemplate || 'legacyEventKafkaTemplate' | JsonSerializer       | JsonDeserializer
            'cloud event'  | cloudEventKafkaTemplate  || 'cloudEventKafkaTemplate'  | CloudEventSerializer | CloudEventDeserializer
    }
}
