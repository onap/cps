/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.init;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.exception.NcmpStartUpException;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionModelLoader implements ModelLoader {

    private final CpsAdminService cpsAdminService;
    private final CpsModuleService cpsModuleService;
    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SUBSCRIPTION_SCHEMASET_NAME = "subscriptions";

    /**
     * Method calls boarding subscription model when Application is ready.
     *
     * @param applicationReadyEvent the event to respond to
     */
    @Override
    public void onApplicationEvent(@NonNull final ApplicationReadyEvent applicationReadyEvent) {
        try {
            onboardSubscriptionModel();
        } catch (final NcmpStartUpException ncmpStartUpException) {
            log.debug("Onboarding model for NCMP failed: {} ", ncmpStartUpException.getMessage());
            SpringApplication.exit(applicationReadyEvent.getApplicationContext(), () -> 1);
        }
    }

    /**
     * Method to onboard subscription model for NCMP.
     */
    private void onboardSubscriptionModel() {
        final Map<String, String> yangResourceContentMap = createYangResourceToContentMap();
        if (!yangResourceContentMap.get("subscription.yang").isEmpty()) {
            createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, yangResourceContentMap);
            createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME);
        }
    }


    @Override
    public boolean createSchemaSet(final String dataspaceName,
                                final String schemaSetName,
                                final Map<String, String> yangResourceContentMap) {
        try {
            cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentMap);
        } catch (final AlreadyDefinedException exception) {
            log.info("Creating new schema set failed as schema set already exists");
        } catch (final Exception exception) {
            log.debug("Creating schema set for subscription model failed: {} ", exception.getMessage());
            throw new NcmpStartUpException("Creating schema set failed", exception.getMessage());
        }
        return true;
    }

    /**
     * Create Anchor.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @param anchorName anchor name
     */
    @Override
    public boolean createAnchor(final String dataspaceName, final String schemaSetName,
                             final String anchorName) {
        try {
            cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName);
        } catch (final AlreadyDefinedException exception) {
            log.info("Creating new anchor failed as anchor already exists");
        } catch (final Exception exception) {
            log.debug("Creating anchor for subscription model failed: {} ", exception.getMessage());
            throw new NcmpStartUpException("Creating anchor failed", exception.getMessage());
        }
        return true;
    }

    private String getFileContentAsString() {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("model/subscription.yang")) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception exception) {
            log.debug("Onboarding failed as unable to read file: {}", exception.getCause().toString());
            throw new NcmpStartUpException("Onboarding failed as unable to read file: {}", exception.getMessage());
        }
    }

    private Map<String, String> createYangResourceToContentMap() {
        return Map.of("subscription.yang", getFileContentAsString());
    }
}
