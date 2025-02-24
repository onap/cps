/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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

package org.onap.cps.ncmp.rest.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters;
import org.onap.cps.ncmp.rest.model.ConditionProperties;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CmHandleSearchExecutionCounter {

    private static final String NO_CONDITION = "NONE";

    private final MeterRegistry meterRegistry;

    /**
     * Counts the number of invocations of the methods annotated with @CountCmHandleSearchExecution based on the search
     * conditions dynamically added. If search is executed without condition then it would be tagged as NONE , otherwise
     * the conditions are concatenated with _ as separator.
     *
     * @param joinPoint                    join point
     * @param countCmHandleSearchExecution count the cm handle search conditions
     */
    @Before("@annotation(countCmHandleSearchExecution)")
    public void cmHandleSearchExecutionCounter(final JoinPoint joinPoint,
            final CountCmHandleSearchExecution countCmHandleSearchExecution) {
        final Object[] args = joinPoint.getArgs();

        if (args.length == 0 || !(args[0] instanceof CmHandleQueryParameters cmHandleQueryParameters)) {
            log.warn("Method {} is missing required CmHandleQueryParameters argument", joinPoint.getSignature());
            return;
        }

        final Set<String> conditionTypes =
                cmHandleQueryParameters.getCmHandleQueryParameters().stream().map(ConditionProperties::getConditionName)
                        .collect(Collectors.toCollection(TreeSet::new));

        final String conditionTag = conditionTypes.isEmpty() ? NO_CONDITION : String.join("_", conditionTypes);

        Counter.builder("cm_handle_search_invocations")
                .tag("method", countCmHandleSearchExecution.methodName())
                .tag("cps-interface", countCmHandleSearchExecution.interfaceName())
                .tag("conditions", conditionTag)
                .description("Number of invocations of search methods based on condition types")
                .register(meterRegistry)
                .increment();
    }
}
