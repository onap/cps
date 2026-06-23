/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.security;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class DatabaseTestContainer extends PostgreSQLContainer {
    private static final String IMAGE_VERSION = "registry.nordix.org/onaptest/postgres:14.1";
    private static DatabaseTestContainer instance;

    private DatabaseTestContainer() {
        super(DockerImageName.parse(IMAGE_VERSION).asCompatibleSubstituteFor("postgres"));
    }

    /**
     * Get singleton instance of the database test container.
     *
     * @return the database test container instance
     */
    public static DatabaseTestContainer getInstance() {
        if (instance == null) {
            instance = new DatabaseTestContainer();
            Runtime.getRuntime().addShutdownHook(new Thread(instance::terminate));
        }
        return instance;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("DB_URL", getJdbcUrl());
        System.setProperty("DB_USERNAME", getUsername());
        System.setProperty("DB_PASSWORD", getPassword());
    }

    @Override
    public void stop() {
    }

    private void terminate() {
        super.stop();
    }
}
