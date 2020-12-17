/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
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

package org.onap.cps;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The Postgresql database test container wrapper.
 * Singleton implementation allows saving time on database initialization which
 * otherwise would occur on each test.
 */
public class DatabaseTestContainer extends PostgreSQLContainer<DatabaseTestContainer> {
    private static final String IMAGE_VERSION = "postgres"; // latest
    private static DatabaseTestContainer databaseTestContainer;

    private DatabaseTestContainer() {
        super(IMAGE_VERSION);
    }

    /**
     * Provides an instance of test container wrapper.
     * The returned value expected to be assigned to static variable annotated with @ClassRule.
     * This will allow to initialize DB connection env variables before DataSource object
     * is initialized by Spring framework.
     *
     */
    public static DatabaseTestContainer getInstance() {
        if (databaseTestContainer == null) {
            databaseTestContainer = new DatabaseTestContainer();
        }
        return databaseTestContainer;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("DB_URL", databaseTestContainer.getJdbcUrl());
        System.setProperty("DB_USERNAME", databaseTestContainer.getUsername());
        System.setProperty("DB_PASSWORD", databaseTestContainer.getPassword());
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }

}
