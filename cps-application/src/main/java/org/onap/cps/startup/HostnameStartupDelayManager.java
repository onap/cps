/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.startup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HostnameStartupDelayManager {

    /**
     * Applies a startup delay based on the host's name to avoid race conditions during schema migration.

     * In environments with multiple instances (e.g., Docker Compose, Kubernetes),
     * this delay helps avoid simultaneous Liquibase executions that may result in conflicts.

     * Delay logic:
     * - If the last character of the hostname is a digit, delay = digit * 1000 ms.
     * - Otherwise, a hash-based fallback delay up to 3000 ms is applied.
     */
    public void applyHostnameBasedStartupDelay() {
        try {
            final String hostname = getHostName();
            final char lastCharacterOfHostName = hostname.charAt(hostname.length() - 1);
            final int startupDelayInMillis;
            if (Character.isDigit(lastCharacterOfHostName)) {
                startupDelayInMillis = Character.getNumericValue(lastCharacterOfHostName) * 1000;
            } else {
                startupDelayInMillis = Math.abs(hostname.hashCode() % 3000);
            }
            log.info("Startup delay applied for Hostname: {} | Delay: {} ms", hostname, startupDelayInMillis);
            doSleep(startupDelayInMillis);
        } catch (final Exception e) {
            log.warn("Startup delay unable to apply delay. Proceeding without delay. {}", e.getMessage());
        }
    }

    protected String getHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    protected void doSleep(final long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}