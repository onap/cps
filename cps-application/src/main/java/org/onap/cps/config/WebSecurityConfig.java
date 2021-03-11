/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2021 Bell Canada.
 *  Modification Copyright (C) 2021 Pantheon.tech
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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Configuration class to implement application security.
 * It enforces Basic Authentication access control.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String USER_ROLE = "USER";

    private final String username;
    private final String password;
    private final String[] permitUris;

    /**
     * Constructor. Accepts parameters from configuration.
     *
     * @param permitUris comma-separated list of uri patterns for endpoints permitted
     * @param username   username
     * @param password   password
     */
    public WebSecurityConfig(
        @Autowired @Value("${security.permit-uri}") final String permitUris,
        @Autowired @Value("${security.auth.username}") final String username,
        @Autowired @Value("${security.auth.password}") final String password
    ) {
        super();
        this.permitUris = permitUris.isEmpty() ? new String[] {"/v3/api-docs"} : permitUris.split("\\s*,\\s*");
        this.username = username;
        this.password = password;
    }

    @Override
    // The team decided to disable default CSRF Spring protection and not implement CSRF tokens validation.
    // CPS is a stateless REST API that is not as vulnerable to CSRF attacks as web applications running in
    // web browsers are. CPS  does not manage sessions, each request requires the authentication token in the header.
    // See https://docs.spring.io/spring-security/site/docs/5.3.8.RELEASE/reference/html5/#csrf
    @SuppressWarnings("squid:S4502")
    protected void configure(final HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers(permitUris).permitAll()
            .anyRequest().authenticated()
            .and().httpBasic();
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser(username).password("{noop}" + password).roles(USER_ROLE);
    }
}
