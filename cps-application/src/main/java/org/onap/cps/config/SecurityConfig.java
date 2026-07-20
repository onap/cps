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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Security configuration for CPS REST APIs.
 * Controlled by the property security.auth.enabled (default: false).
 */
@Configuration
@EnableWebSecurity
@SuppressWarnings("squid:S4502") // CSRF disabled intentionally - stateless REST API
public class SecurityConfig {

    /**
     * Configures Spring Security's HTTP firewall to allow URL-encoded slashes (%2F) in request paths.
     *
     * <p>Required for alternate IDs containing hierarchical paths (e.g. {@code /SubNetwork=Europe/ManagedElement=X1}).
     * This is one of three layers that must permit encoded slashes; the embedded server (Jetty) is relaxed
     * separately in {@link JettyConfig}.
     *
     * <p><b>Note:</b> {@code StrictHttpFirewall} with {@code setAllowUrlEncodedSlash(true)} is used here
     * deliberately. {@code DefaultHttpFirewall} cannot be used - it unconditionally rejects encoded slashes.
     *
     * @return WebSecurityCustomizer that applies the encoded-slash-tolerant firewall
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        final StrictHttpFirewall strictHttpFirewall = new StrictHttpFirewall();
        strictHttpFirewall.setAllowUrlEncodedSlash(true);
        strictHttpFirewall.setAllowUrlEncodedPercent(true);
        strictHttpFirewall.setAllowUrlEncodedDoubleSlash(true);
        return web -> web.httpFirewall(strictHttpFirewall);
    }

    /**
     * Security filter chain with JWT authentication enabled.
     *
     * @param httpSecurity the HttpSecurity configuration
     * @return configured SecurityFilterChain
     */
    @Bean
    @ConditionalOnProperty(name = "security.auth.enabled", havingValue = "true")
    public SecurityFilterChain authEnabledFilterChain(final HttpSecurity httpSecurity) {
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
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
    public SecurityFilterChain authDisabledFilterChain(final HttpSecurity httpSecurity) {
        httpSecurity.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return httpSecurity.build();
    }
}
