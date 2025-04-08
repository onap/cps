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
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceStartupDelayManager {

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
            final long startupDelayInMillis;
            if (Character.isDigit(lastCharacterOfHostName)) {
                startupDelayInMillis = Character.getNumericValue(lastCharacterOfHostName) * 1_000L;
            } else {
                startupDelayInMillis = Math.abs(hostname.hashCode() % 3_000L);
            }
            log.info("Startup delay applied for Hostname: {} | Delay: {} ms", hostname, startupDelayInMillis);
            haveALittleSleepInMs(startupDelayInMillis);
        } catch (final InterruptedException e) {
            log.warn("Sleep interrupted, re-interrupting the thread");
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            log.info("Exception during startup delay ignored. {}", e.getMessage());
        }
    }

    protected String getHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    protected void haveALittleSleepInMs(final long timeInMs) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(timeInMs);
    }
}