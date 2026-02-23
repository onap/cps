/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.spockframework.spring.EnableSharedInjection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.test.context.TestPropertySource
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(classes = [KafkaProperties, EosKafkaConfig])
@EnableSharedInjection
@EnableConfigurationProperties
@TestPropertySource(properties = [
        "ncmp.kafka.eos.enabled=true",
        "ncmp.notifications.avc-event-producer.transaction-id-prefix=tx-myPrefix-",
        "ncmp.notifications.avc-event-consumer.concurrency=2",
        "ncmp.notifications.avc-event-consumer.max-poll-records=500"
])
class EosKafkaConfigSpec extends Specification {

    @Shared
    @Autowired
    ConsumerFactory<String, CloudEvent> cloudEventConsumerFactoryForEos

    @Shared
    @Autowired
    ProducerFactory<String, CloudEvent> cloudEventProducerFactoryForEos

    @Shared
    @Autowired
    ConcurrentKafkaListenerContainerFactory<String, CloudEvent> cloudEventConcurrentKafkaListenerContainerFactoryForEos

    def 'Verify EOS kafka configuration'() {
        expect: 'consumer has read_committed isolation level'
            cloudEventConsumerFactoryForEos.configurationProperties[ConsumerConfig.ISOLATION_LEVEL_CONFIG] == 'read_committed'
        and: 'consumer has auto commit disabled'
            cloudEventConsumerFactoryForEos.configurationProperties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] == false
        and: 'consumer has max poll records configured'
            cloudEventConsumerFactoryForEos.configurationProperties[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] == '500'
        and: 'listener uses BATCH ack mode'
            cloudEventConcurrentKafkaListenerContainerFactoryForEos.containerProperties.ackMode == ContainerProperties.AckMode.BATCH
        and: 'listener is batch listener'
            cloudEventConcurrentKafkaListenerContainerFactoryForEos.batchListener == true
        and: 'concurrency is configured'
            cloudEventConcurrentKafkaListenerContainerFactoryForEos.concurrency == 2
        and: 'producer transaction ID prefix is as expected (correct start and has UUID)'
            cloudEventProducerFactoryForEos.transactionIdPrefix.matches('cps-tx-myPrefix-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}-')
        and: 'KafkaTransactionManager is used instead of primary transaction manager'
            cloudEventConcurrentKafkaListenerContainerFactoryForEos.containerProperties.kafkaAwareTransactionManager instanceof KafkaTransactionManager
    }
}
