/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.config.config

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import spock.lang.Specification

@SpringBootTest(classes = [EventsPublisherConfig])
class EventsPublisherConfigSpec extends Specification {

    @Autowired
    @Qualifier("eventKafkaTemplate")
    KafkaTemplate<String, String> eventKafkaTemplate

    def 'Verify the Event Kafkatemplate configuration'() {
        expect: 'kafkatemplate is instantiated'
            assert eventKafkaTemplate != null
            assert eventKafkaTemplate.properties['beanName'] == 'eventKafkaTemplate'
        and: 'it has correct configurations for serializing'
            assert eventKafkaTemplate.properties['producerFactory'].configs['key.serializer'].asType(String.class).contains(StringSerializer.getCanonicalName())
            assert eventKafkaTemplate.properties['producerFactory'].configs['value.serializer'].asType(String.class).contains(JsonSerializer.getCanonicalName())
        and: 'also deserializing'
            assert eventKafkaTemplate.properties['consumerFactory'].configs['spring.deserializer.key.delegate.class'].asType(String.class).contains(StringDeserializer.getCanonicalName())
            assert eventKafkaTemplate.properties['consumerFactory'].configs['spring.deserializer.value.delegate.class'].asType(String.class).contains(JsonDeserializer.getCanonicalName())
        and: 'has correct error handler'
            assert eventKafkaTemplate.properties['consumerFactory'].configs['key.deserializer'].asType(String.class).contains(ErrorHandlingDeserializer.getCanonicalName())
            assert eventKafkaTemplate.properties['consumerFactory'].configs['value.deserializer'].asType(String.class).contains(ErrorHandlingDeserializer.getCanonicalName())
    }
}
