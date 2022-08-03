/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@Service
public class ScheduledWatchdogTaskExecutor {

    /**
     * Execute watchdog task batch asynchronously.
     *
     * @param taskSupplier functional method is get() task need to executed asynchronously
     * @param timeOutInMillis the timeout value in milliseconds
     */
    public void executeTask(final Supplier<Object> taskSupplier, final int timeOutInMillis) {
        CompletableFuture.supplyAsync(taskSupplier::get, Executors.newWorkStealingPool(100))
            .orTimeout(timeOutInMillis, MILLISECONDS)
                .whenCompleteAsync(this::handleTaskCompletion);
    }

    private void handleTaskCompletion(final Object response,final Throwable throwable) {
        if (throwable == null) {
            log.info("Watchdog async batch completed successfully. {}", response);
        } else {
            log.error("Watchdog async batch failed. caused by : {}", throwable.getMessage());
        }
    }
}



