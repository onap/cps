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

import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.CloudEventSerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Specification

@SpringBootTest(classes = [CloudEventsPublisherConfig])
class CloudEventsPublisherConfigSpec extends Specification {

    @Autowired
    @Qualifier("cloudEventKafkaTemplate")
    KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate

    def 'Verify the CloudEvent Kafkatemplate configuration'() {
        expect: 'kafkatemplate is instantiated'
            assert cloudEventKafkaTemplate != null
            assert cloudEventKafkaTemplate.properties['beanName'] == 'cloudEventKafkaTemplate'
        and: 'it has correct configurations for serializing'
            assert cloudEventKafkaTemplate.properties['producerFactory'].configs['key.serializer'].asType(String.class).contains(StringSerializer.getCanonicalName())
            assert cloudEventKafkaTemplate.properties['producerFactory'].configs['value.serializer'].asType(String.class).contains(CloudEventSerializer.getCanonicalName())
        and: 'also deserializing'
            assert cloudEventKafkaTemplate.properties['consumerFactory'].configs['key.deserializer'].asType(String.class).contains(StringDeserializer.getCanonicalName())
            assert cloudEventKafkaTemplate.properties['consumerFactory'].configs['value.deserializer'].asType(String.class).contains(CloudEventDeserializer.getCanonicalName())
    }
}
