/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.functional.cps

import org.hibernate.cfg.Configuration
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.ri.config.HibernateConfig
import org.springframework.beans.factory.annotation.Autowired

class HibernateConfigSpec extends FunctionalSpecBase {

    @Autowired
    HibernateConfig hibernateConfig

    def "Hibernate should be configured with properties from application.yml."() {
        when: "the hibernate configuration is retrieved"
            Configuration configuration = hibernateConfig.hibernateConfiguration()

        then: "the properties match those used in application.yml"
            configuration.getProperty("hibernate.connection.url") == databaseTestContainer.getJdbcUrl()
            configuration.getProperty("hibernate.connection.driver_class") == databaseTestContainer.getJdbcDriverInstance().class.name
            configuration.getProperty("hibernate.connection.username") == databaseTestContainer.getUsername()
            configuration.getProperty("hibernate.connection.password") == databaseTestContainer.getPassword()
            configuration.getProperty("hibernate.dialect") == "org.hibernate.dialect.PostgreSQLDialect"
            configuration.getProperty("hibernate.show_sql") == "false"
            configuration.getProperty("hibernate.format_sql") == "false"
            configuration.getProperty("hibernate.id.new_generator_mappings") == "true"
            configuration.getProperty("hibernate.jdbc.batch_size") == "100"
    }
}
