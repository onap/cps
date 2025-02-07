/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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

package org.onap.cps.ri.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String dbDriver;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.jpa.properties.hibernate.dialect}")
    private String hibernateDialect;

    @Value("${spring.jpa.properties.hibernate.show_sql:false}")
    private String showSql;

    @Value("${spring.jpa.properties.hibernate.format_sql:false}")
    private String formatSql;

    @Value("${spring.jpa.properties.hibernate.id.new_generator_mappings:true}")
    private String newGeneratorMappings;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:100}")
    private String jdbcBatchSize;

    /**
     * Configures and returns a Hibernate {@link org.hibernate.cfg.Configuration} bean.
     * This method sets various Hibernate properties such as connection URL, driver class, username, password, dialect,
     * and SQL formatting options based on Spring Boot application.yml.
     *
     * @return a configured {@link org.hibernate.cfg.Configuration} instance.
     */
    @Bean
    public org.hibernate.cfg.Configuration hibernateConfiguration() {
        final org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
        configuration.setProperty("hibernate.connection.url", dbUrl);
        configuration.setProperty("hibernate.connection.driver_class", dbDriver);
        configuration.setProperty("hibernate.connection.username", dbUsername);
        configuration.setProperty("hibernate.connection.password", dbPassword);
        configuration.setProperty("hibernate.dialect", hibernateDialect);
        configuration.setProperty("hibernate.show_sql", showSql);
        configuration.setProperty("hibernate.format_sql", formatSql);
        configuration.setProperty("hibernate.id.new_generator_mappings", newGeneratorMappings);
        configuration.setProperty("hibernate.jdbc.batch_size", jdbcBatchSize);
        return configuration;
    }
}
