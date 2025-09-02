/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.init.actuator;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ReadinessManager {

    private final Set<String> startupProcesses = ConcurrentHashMap.newKeySet();

    public void registerStartupProcess(final String name) {
        startupProcesses.add(name);
    }

    public void markStartupProcessComplete(final String name) {
        startupProcesses.remove(name);
    }

    public String getStartupProcessesAsString() {
        return String.join(", ", startupProcesses);
    }

    public boolean isReady() {
        return startupProcesses.isEmpty();
    }
}
