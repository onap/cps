/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps;

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.startup.InstanceStartupDelayManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class Application {

    static final InstanceStartupDelayManager instanceStartupDelayManager = new InstanceStartupDelayManager();

    /**
     * The main method which serves as the entry point to the Spring Boot application.
     * It first applies a hostname-based startup delay to avoid potential race conditions
     * during schema migration in distributed environments. After applying the delay,
     * it initializes the Spring Application context and logs the application startup status.
     *
     * @param args Command-line arguments passed to the application (not used in this implementation).
     */
    public static void main(final String[] args) {
        instanceStartupDelayManager.applyHostNameBasedStartupDelay();
        log.info("Initializing Spring Application context...");
        SpringApplication.run(Application.class, args);
        log.info("🚀 APPLICATION STARTED");
    }
}
