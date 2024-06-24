/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.sync;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AsyncTaskExecutor {

    @Value("${ncmp.modules-sync-watchdog.async-executor.parallelism-level:10}")
    @Getter
    private int asyncTaskParallelismLevel;
    private ExecutorService executorService;
    private static final int DEFAULT_PARALLELISM_LEVEL = 10;

    /**
     *  Set up executor service with thread-pool size as per configuration parameter.
     *  If modules-sync-watchdog.async-executor.parallelism-level not set a default of 10 threads will be applied.
     */
    @PostConstruct
    public void setupThreadPool() {
        executorService = Executors.newWorkStealingPool(
                asyncTaskParallelismLevel == 0 ? DEFAULT_PARALLELISM_LEVEL : asyncTaskParallelismLevel);
    }

    /**
     * Execute supplied task asynchronously.
     *
     * @param taskSupplier    functional method is get() task need to executed asynchronously
     * @param timeOutInMillis the task timeout value in milliseconds
     */
    public void executeTask(final Supplier<Object> taskSupplier, final long timeOutInMillis) {
        CompletableFuture.supplyAsync(taskSupplier::get, executorService)
                .orTimeout(timeOutInMillis, MILLISECONDS)
                .whenCompleteAsync(this::handleTaskCompletion);
    }

    private void handleTaskCompletion(final Object response, final Throwable throwable) {
        if (throwable != null) {
            if (throwable instanceof TimeoutException) {
                log.warn("Async task didn't completed within the required time.");
            } else {
                log.debug("Watchdog async batch failed. caused by : {}", throwable.getMessage());
            }
        }
    }
}
