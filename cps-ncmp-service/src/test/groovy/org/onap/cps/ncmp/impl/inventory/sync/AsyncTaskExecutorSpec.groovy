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

package org.onap.cps.ncmp.impl.inventory.sync


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import java.util.concurrent.TimeoutException
import java.util.function.Supplier

@SpringBootTest(classes = AsyncTaskExecutor)
class AsyncTaskExecutorSpec extends Specification {

    @Autowired
    AsyncTaskExecutor objectUnderTest
    def mockTaskSupplier = Mock(Supplier<Object>)

    def 'Parallelism level configuration.'() {
        expect: 'Parallelism level is configured with the correct value'
            assert objectUnderTest.getAsyncTaskParallelismLevel() == 3
    }

    def 'Task completion with #caseDescriptor.'() {
        when: 'task completion is handled'
            def irrelevantResponse = null
            objectUnderTest.handleTaskCompletion(irrelevantResponse, exception);
        then: 'any exception is swallowed by the task completion (logged)'
            noExceptionThrown()
        where: 'following cases are tested'
            caseDescriptor         | exception
            'no exception'         | null
            'time out exception'   | new TimeoutException("time-out")
            'unexpected exception' | new Exception("some exception")
    }

    def 'Task execution.'() {
        when: 'a task is submitted for execution'
            objectUnderTest.executeTask(() -> mockTaskSupplier, 0)
        then: 'the task submission is successful'
            noExceptionThrown()
    }

}
