/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for creating a list of excluded task names.
 */
@Configuration
public class ExcludedTaskNamesConfig {

    @Value("${cps.tracing.excluded-task-names:tasks.scheduled.execution}")
    private String excludedTaskNames;

    /**
     * Creates a List of excluded task names from the configuration property.
     *
     * @return a List of excluded task names
     */
    @Bean
    public List<String> excludedTaskNamesList() {
        return Arrays.stream(excludedTaskNames.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}

