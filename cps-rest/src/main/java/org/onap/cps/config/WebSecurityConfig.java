/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.config;

import org.apache.commons.lang3.StringUtils;
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

    private static final String ACTUATOR_HEALTH_PATTERN = "/manage/health/**";
    private static final String ACTUATOR_INFO_PATTERN = "/manage/info";
    private static final String DEFAULT_USER_NAME = "cpsuser";
    private static final String DEFAULT_USER_PASSWORD = "cpsr0cks!";
    private static final String USER_NAME =
            StringUtils.defaultIfBlank(System.getenv("CPS_USERNAME"), DEFAULT_USER_NAME);
    private static final String USER_PASSWORD =
            StringUtils.defaultIfBlank(System.getenv("CPS_PASSWORD"), DEFAULT_USER_PASSWORD);
    private static final String USER_ROLE = "USER";

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
                .antMatchers(ACTUATOR_HEALTH_PATTERN, ACTUATOR_INFO_PATTERN).permitAll()
                .anyRequest().authenticated()
                .and().httpBasic();
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser(USER_NAME).password("{noop}" + USER_PASSWORD).roles(USER_ROLE);
    }

}
