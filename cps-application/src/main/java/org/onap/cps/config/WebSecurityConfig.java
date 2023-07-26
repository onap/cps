/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2021 Bell Canada.
 *  Modification Copyright (C) 2021 Pantheon.tech
 *  Modification Copyright (C) 2023 Nordix Foundation
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration class to implement application security.
 * It enforces Basic Authentication access control.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

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
        this.permitUris = permitUris.isEmpty() ? new String[] {"/v3/api-docs"} : permitUris.split("\\s{0,9},\\s{0,9}");
        this.username = username;
        this.password = password;
    }

    /**
     * Return the configuration for secure access to the modules REST end points.
     *
     * @param http the HTTP security settings.
     * @return the HTTP security settings.
     */
    @Bean
    // The team decided to disable default CSRF Spring protection and not implement CSRF tokens validation.
    // CPS is a stateless REST API that is not as vulnerable to CSRF attacks as web applications running in
    // web browsers are. CPS  does not manage sessions, each request requires the authentication token in the header.
    // See https://docs.spring.io/spring-security/site/docs/5.3.8.RELEASE/reference/html5/#csrf
    @SuppressWarnings("squid:S4502")
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
                .httpBasic()
                .and()
                .authorizeHttpRequests()
                .requestMatchers(permitUris).permitAll()
                .anyRequest().authenticated()
                .and()
                .csrf().disable();

        return http.build();
    }

    /**
     * In memory user authentication details.
     *
     * @return in memory authetication
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        final UserDetails user = User.builder()
                .username(username)
                .password("{noop}" + password)
                .roles(USER_ROLE)
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
