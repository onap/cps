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

package org.onap.cps.ncmp.rest.executor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CpsNcmpTaskExecutor {

    /**
     * Execute a task asynchronously.
     *
     * @param taskSupplier functional method is get() task need to executed asynchronously
     * @param timeOutInMillis the time-out value in milliseconds
     */
    public void executeTask(final Supplier<Object> taskSupplier, final long timeOutInMillis) {
        CompletableFuture.supplyAsync(taskSupplier::get)
            .orTimeout(timeOutInMillis, MILLISECONDS)
            .whenCompleteAsync((taskResult, throwable) -> handleTaskCompletion(throwable));
    }

    private void handleTaskCompletion(final Throwable throwable) {
        if (throwable == null) {
            log.info("Async task completed successfully.");
        } else {
            log.error("Async task failed. caused by : {}", throwable.toString());
        }
    }
}



