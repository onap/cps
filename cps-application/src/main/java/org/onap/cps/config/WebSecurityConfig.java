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

import java.util.List;
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

    private String username;
    private String password;
    protected String[] permitAntMatchers;

    @Autowired
    protected void setParameters(
        @Value("#{'${security.permit-ant-matchers}'.split(',')}") final List<String> configuredAntMatchers,
        @Value("${security.auth.username}") final String username,
        @Value("${security.auth.password}") final String password
    ) {
        this.permitAntMatchers = configuredAntMatchers.isEmpty()
            ? new String[] {"/swagger-ui/**"}
            : configuredAntMatchers.toArray(String[]::new);
        this.username = username;
        this.password = password;
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers(permitAntMatchers).permitAll()
            .anyRequest().authenticated()
            .and().httpBasic();
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser(username).password("{noop}" + password).roles(USER_ROLE);
    }
}
