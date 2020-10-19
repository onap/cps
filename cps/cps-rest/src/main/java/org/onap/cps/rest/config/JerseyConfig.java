/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation. All rights reserved.
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

package org.onap.cps.rest.config;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;
import org.onap.cps.rest.controller.ModelController;
import org.onap.cps.rest.controller.RestController;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("api")
public class JerseyConfig extends ResourceConfig {

    /**
     * This method is used to setup Jersey related configuration.
     */
    @PostConstruct
    public void init() {
        register(MultiPartFeature.class);
        register(OpenApiResource.class);
        register(AcceptHeaderOpenApiResource.class);

        // Register controllers
        register(ModelController.class);
        register(RestController.class);

        configureSwagger();
        configureSwaggerUI();
    }

    private void configureSwagger() {
        try {
            new JaxrsOpenApiContextBuilder<>().buildContext(true).read();
        } catch (final OpenApiConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void configureSwaggerUI() {
        // Enable Jersey filter forwarding to next filter for 404 responses.
        // This configuration lets Jersey servlet container forwarding static swagger ui requests to spring mvc filter
        // to be handle by spring mvc dispatcher servlet.
        property(ServletProperties.FILTER_FORWARD_ON_404, true);
    }

}
