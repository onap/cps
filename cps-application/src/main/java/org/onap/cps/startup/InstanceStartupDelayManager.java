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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceStartupDelayManager {
    private static final Pattern HOST_NAME_WITH_SEQUENCE_PATTERN = Pattern.compile(".*-([\\d][\\d]?)$");

    /**
     * Applies a consistent startup delay based on the host's name to avoid race conditions during liquibase set steps.
     * This method is useful in environments with multiple instances.
     * (e.g., Docker Compose, Kubernetes), where simultaneous Liquibase executions might result in conflicts.
     * Delay calculation:
     * - For host names that match {host-name}-{sequence-number} the delay will be 2 second times the sequence number.
     * - please note, the sequence number can be 2 digits at most.
     * - For other names the delay is calculated as the hash code of that name modulus 10,000 ms i.e. up to 10,000 ms.
     */
    public void applyHostNameBasedStartupDelay() {
        try {
            final String hostName = getHostName();
            log.info("Host name: {}", hostName);
            final Matcher matcher = HOST_NAME_WITH_SEQUENCE_PATTERN.matcher(hostName);
            final long startupDelayInMillis;
            if (matcher.matches()) {
                startupDelayInMillis = Integer.parseInt(matcher.group(1)) * 2_000L;
                log.info("Sequenced host name detected, calculated delay = {} ms", startupDelayInMillis);
            } else {
                startupDelayInMillis = Math.abs(hostName.hashCode() % 10_000L);
                log.warn("No Sequenced host name detected (<host-name>-<number>), hash-based delay = {} ms",
                    startupDelayInMillis);
            }
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
