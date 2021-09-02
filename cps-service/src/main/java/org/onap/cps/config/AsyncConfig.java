/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.config;

import javax.validation.constraints.Min;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;

@EnableAsync
@Configuration
@ConditionalOnProperty(name = "notification.async.enabled", havingValue = "true", matchIfMissing = false)
@ConfigurationProperties("notification.async.executor")
@Validated
@Setter
public class AsyncConfig {

    @Min(0)
    private int corePoolSize = 2;
    @Min(2)
    private int maxPoolSize = 10;
    @Min(0)
    private int queueCapacity = Integer.MAX_VALUE;
    private boolean waitForTasksToCompleteOnShutdown = true;
    private String threadNamePrefix = "Async-";

    /**
     * Creates TaskExecutor for processing data-updated events.
     *
     * @return TaskExecutor
     */
    @Bean("notificationExecutor")
    public TaskExecutor getThreadAsyncExecutorForNotification() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        executor.setThreadNamePrefix(threadNamePrefix);
        return executor;
    }

}
