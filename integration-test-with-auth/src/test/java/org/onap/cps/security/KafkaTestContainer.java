/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.security;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@Slf4j
public class KafkaTestContainer extends ConfluentKafkaContainer {
    private static volatile KafkaTestContainer instance;

    private KafkaTestContainer() {
        super("confluentinc/cp-kafka:7.8.0");
    }

    /**
     * Get singleton instance of the Kafka test container.
     *
     * @return the Kafka test container instance
     */
    public static KafkaTestContainer getInstance() {
        if (instance == null) {
            synchronized (KafkaTestContainer.class) {
                if (instance == null) {
                    instance = new KafkaTestContainer();
                    Runtime.getRuntime().addShutdownHook(new Thread(instance::stop));
                }
            }
        }
        return instance;
    }

    /**
     * Starts the Kafka container if not already running and sets the bootstrap servers system property.
     */
    @Override
    public void start() {
        if (!isRunning()) {
            super.start();
            System.setProperty("spring.kafka.properties.bootstrap.servers", getBootstrapServers());
            log.info("KafkaTestContainer started at {}", getBootstrapServers());
        }
    }
}
