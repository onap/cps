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

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.init.ModelLoader;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelLoaderRegistrationOnStartup implements ApplicationListener<ApplicationStartedEvent> {

    private final ReadinessManager readinessManager;
    // Spring will insert all concrete model loader classes here.
    private final List<ModelLoader> modelLoaders;


    /**
     * Register the model loaders as part of the Application Started Phase.
     *
     * @param applicationStartedEvent Application Started Event
     */
    @Override
    public void onApplicationEvent(final ApplicationStartedEvent applicationStartedEvent) {

        modelLoaders.forEach(modelLoader -> {
            log.info("Registering ModelLoader {}", modelLoader.getName());
            readinessManager.registerStartupProcess(modelLoader.getName());
        });
    }
}
