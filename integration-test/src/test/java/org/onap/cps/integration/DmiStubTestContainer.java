/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class DmiStubTestContainer extends GenericContainer<DmiStubTestContainer> {

    public static final String IMAGE_NAME_AND_VERSION =
                                        "nexus3.onap.org:10003/onap/dmi-plugin-demo-and-csit-stub:latest";
    public static final String DMI_STUB_URL = "http://localhost:8784";

    private static DmiStubTestContainer dmiStubTestContainer;

    private DmiStubTestContainer() {
        super(DockerImageName.parse(IMAGE_NAME_AND_VERSION));
    }

    /**
     * Provides an instance of the Dmi Plugin Stub test container wrapper.
     * This will allow to interact with the DMI Stub in our acceptance tests.
     *
     * @return DmiStubTestContainer
     */
    public static DmiStubTestContainer getInstance() {
        if (dmiStubTestContainer == null) {
            dmiStubTestContainer = new DmiStubTestContainer();
            dmiStubTestContainer.addFixedExposedPort(8784, 8092);
            Runtime.getRuntime().addShutdownHook(new Thread(dmiStubTestContainer::close));
        }
        return dmiStubTestContainer;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        // Method intentionally left blank
    }
}
