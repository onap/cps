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

package org.onap.cps.ncmp.api.inventory.sync.executor

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import spock.lang.Specification
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

class AsyncTaskExecutorSpec extends Specification {
    AsyncTaskExecutor objectUnderTest = new AsyncTaskExecutor()
    def logWatcher = Spy(ListAppender<ILoggingEvent>)

    @BeforeEach
    void setup() {
        objectUnderTest.asyncTaskParallelismLevel = 2;
        objectUnderTest.setupThreadPool();
        ((Logger) LoggerFactory.getLogger(AsyncTaskExecutor.class)).addAppender(logWatcher);
        logWatcher.start();
    }

    @AfterEach
    void teaDown() {
        ((Logger) LoggerFactory.getLogger(AsyncTaskExecutor.class)).detachAndStopAllAppenders();
    }

    def 'Test task executor for handling task completion without any exception.'() {
        when: 'task is submitted'
            objectUnderTest.executeTask(() -> testTask(isExceptionThrown, exception), 500)
        then: 'task is completed and handled successfully'
            assert objectUnderTest.getAsyncTaskParallelismLevel() == 2
            noExceptionThrown()
        where: 'following cases are tested'
            caseDescriptor         | exception                        | isExceptionThrown
            'no exception'         | null                             | false
            'time out exception'   | new TimeoutException("time-out") | true
            'unexpected exception' | new Exception("some exception")  | true
    }

    private CompletableFuture<Void> testTask(boolean isExceptionThrown, Exception exception) {
        if (isExceptionThrown) {
            throw exception;
        }
        return CompletableFuture.completedFuture(null);
    }
}
