/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

import static org.awaitility.Awaitility.await;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
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
        performGcAndWait();
        resetPeakHeapUsage();
        memoryUsedBefore = getPeakHeapUsage();
        stopWatch.start();
    }

    /**
     * Stop measurement.
     */
    public void stop() {
        stopWatch.stop();
        memoryUsedAfter = getPeakHeapUsage();
    }

    /**
     * Get the total time in seconds.
     * @return total time in seconds
     */
    public double getTotalTimeInSeconds() {
        return stopWatch.getTotalTimeSeconds();
    }

    /**
     * Get the total memory used in megabytes.
     * @return total memory used in megabytes
     */
    public double getTotalMemoryUsageInMB() {
        return (memoryUsedAfter - memoryUsedBefore) / 1_000_000.0;
    }

    static void performGcAndWait() {
        final long gcCountBefore = getGcCount();
        System.gc();
        await().until(() -> getGcCount() > gcCountBefore);
    }

    private static long getGcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(gcCount -> gcCount != -1)
                .sum();
    }

    private static long getPeakHeapUsage() {
        return ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP)
                .mapToLong(pool -> pool.getPeakUsage().getUsed())
                .sum();
    }

    private static void resetPeakHeapUsage() {
        ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP)
                .forEach(MemoryPoolMXBean::resetPeakUsage);
    }
}
