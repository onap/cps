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

import java.net.InetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class Application {
    public static void main(final String[] args) {
        applyHostnameBasedStartupDelay();
        SpringApplication.run(Application.class, args);
    }

    /**
     * Applies a startup delay based on the host's name to avoid race conditions during schema migration.

     * In environments with multiple instances (e.g., Docker Compose, Kubernetes),
     * this delay helps avoid simultaneous Liquibase executions that may result in conflicts.

     * Delay logic:
     * - If the last character of the hostname is a digit, delay = digit * 1000 ms.
     * - Otherwise, a hash-based fallback delay up to 3000 ms is applied.
     */
    private static void applyHostnameBasedStartupDelay() {
        try {
            final String hostname = InetAddress.getLocalHost().getHostName();
            final char lastChar = hostname.charAt(hostname.length() - 1);

            final int startupDelayInMillis;
            if (Character.isDigit(lastChar)) {
                startupDelayInMillis = Character.getNumericValue(lastChar) * 1000;
            } else {
                startupDelayInMillis = Math.abs(hostname.hashCode() % 3000);
            }
            log.info("[Startup Delay] Hostname: {} | Delay: {} ms", hostname, startupDelayInMillis);
            Thread.sleep(startupDelayInMillis);
        } catch (final Exception e) {
            log.warn("[Startup Delay] Unable to apply delay. Proceeding without delay. {}", e.getMessage());
        }
    }
}
