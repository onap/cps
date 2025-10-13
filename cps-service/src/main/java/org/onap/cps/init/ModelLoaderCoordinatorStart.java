/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.init;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.init.actuator.ReadinessManager;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@Order(0)
public class ModelLoaderCoordinatorStart implements ApplicationListener<ApplicationStartedEvent> {

    private final ModelLoaderCoordinatorLock modelLoaderCoordinatorLock;
    private final ReadinessManager readinessManager;

    @Getter
    private boolean isMaster = false;

    @Override
    public void onApplicationEvent(final ApplicationStartedEvent applicationStartedEvent) {
        readinessManager.registerStartupProcess(ModelLoaderCoordinatorEnd.class.getSimpleName());
        checkIfThisInstanceIsMaster();
    }

    private void checkIfThisInstanceIsMaster() {
        isMaster = modelLoaderCoordinatorLock.tryLock();
        if (isMaster) {
            log.info("This instance is model loader master");
        } else {
            log.info("Another instance is model loader master");
        }
    }

}
