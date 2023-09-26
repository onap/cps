/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration;

import org.springframework.util.StopWatch;

/**
 * Time and memory stop watch, exposing total running time and memory used.
 */
public class ResourceMeter {
    private final StopWatch stopWatch = new StopWatch();
    private long memoryUsedBefore;
    private long memoryUsedAfter;

    /**
     * Start measurement.
     */
    public void start() {
        System.gc();
        memoryUsedBefore = getCurrentMemoryUsage();
        stopWatch.start();
    }

    /**
     * Stop measurement.
     */
    public void stop() {
        stopWatch.stop();
        memoryUsedAfter = getCurrentMemoryUsage();
    }

    /**
     * Get the total time in milliseconds.
     * @return total time in milliseconds
     */
    public long getTotalTimeMillis() {
        return stopWatch.getTotalTimeMillis();
    }

    /**
     * Get the total memory used in megabytes.
     * @return total memory used in megabytes
     */
    public double getTotalMemoryUsageInMB() {
        return (memoryUsedAfter - memoryUsedBefore) / 1_000_000.0;
    }

    private static long getCurrentMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
}

