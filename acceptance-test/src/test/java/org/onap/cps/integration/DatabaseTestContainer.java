/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The Postgresql database test container wrapper.
 * Singleton implementation allows saving time on database initialization which otherwise would occur on each test.
 * for debugging/developing purposes you can suspend any test and connect to this database:
 *  docker exec -it {container-id} sh
 *  psql -d test -U test
 */
public class DatabaseTestContainer extends PostgreSQLContainer<DatabaseTestContainer> {
    private static final String IMAGE_VERSION = "registry.nordix.org/onaptest/postgres:14.1";
    private static DatabaseTestContainer databaseTestContainer;

    private DatabaseTestContainer() {
        super(DockerImageName.parse(IMAGE_VERSION).asCompatibleSubstituteFor("postgres"));
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
            Runtime.getRuntime().addShutdownHook(new Thread(databaseTestContainer::terminate));
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
        // do nothing on test completion, image removal will be performed via terminate() on JVM shutdown
    }

    private void terminate() {
        super.stop();
    }
}
