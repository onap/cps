/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.aop;

import java.util.Arrays;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
@ConditionalOnExpression(
        "'${logging.level.org.onap.cps:INFO}'.equalsIgnoreCase('DEBUG')"
)
public class CpsLoggingAspectService {

    private static final String ALL_CPS_METHODS = "execution(* org.onap.cps..*(..)))";

    /**
     * To measure method execution time as a logging.
     * @param proceedingJoinPoint exposes the proceed(..) method in order to support around advice.
     * @return empty in case of void otherwise an object of return type
     */
    @Around(ALL_CPS_METHODS)
    @SneakyThrows
    public Object logMethodExecutionTime(final ProceedingJoinPoint proceedingJoinPoint) {
        final StopWatch stopWatch = new StopWatch();

        //Calculate method execution time
        stopWatch.start();
        final Object logObject = Optional.ofNullable(proceedingJoinPoint.proceed()).orElse("");
        stopWatch.stop();
        final MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        //Log method execution time
        log.debug("Execution time of : {}.{}() with argument[s] = {} having result = {} :: {} ms",
                methodSignature.getDeclaringType().getSimpleName(),
                methodSignature.getName(), Arrays.toString(proceedingJoinPoint.getArgs()), logObject,
                stopWatch.getTotalTimeMillis());

        return logObject;
    }

}
