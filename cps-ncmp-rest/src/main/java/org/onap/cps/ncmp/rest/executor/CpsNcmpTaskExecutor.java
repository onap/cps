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

package org.onap.cps.ncmp.rest.executor;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.rest.exceptions.CpsTaskExecutionException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsNcmpTaskExecutor {

    /**
     * Execute task asynchronously and publish response to supplied topic.
     *
     * @param taskSupplier functional method is get() task need to executed asynchronously
     */
    public void executeTask(final Supplier<Object> taskSupplier) {
        CompletableFuture.supplyAsync(taskSupplier::get)
            .orTimeout(2, SECONDS)
            .whenCompleteAsync((responseAsJson, error) -> {
                if (error == null) {
                    log.info("Async task completed successfully.");
                } else {
                    throw new CpsTaskExecutionException("Async task failed.", error.getMessage(), error);
                }
            });
    }
}



