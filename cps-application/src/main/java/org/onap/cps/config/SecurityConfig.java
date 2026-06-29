/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for CPS REST APIs.
 * Controlled by the property security.auth.enabled (default: false).
 */
@Configuration
@EnableWebSecurity
@SuppressWarnings("squid:S4502") // CSRF disabled intentionally - stateless REST API
public class SecurityConfig {

    /**
     * Security filter chain with JWT authentication enabled.
     *
     * @param httpSecurity the HttpSecurity configuration
     * @return configured SecurityFilterChain
     */
    @Bean
    @ConditionalOnProperty(name = "security.auth.enabled", havingValue = "true")
    public SecurityFilterChain authEnabledFilterChain(final HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return httpSecurity.build();
    }

    /**
     * Security filter chain that permits all requests when authentication is disabled.
     *
     * @param httpSecurity the HttpSecurity configuration
     * @return configured SecurityFilterChain
     */
    @Bean
    @ConditionalOnProperty(name = "security.auth.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain authDisabledFilterChain(final HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return httpSecurity.build();
    }
}
