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
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.exception.NcmpStartUpException;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.AlreadyDefinedExceptionBatch;
import org.onap.cps.spi.model.Dataspace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionModelLoader implements ModelLoader {

    private final CpsAdminService cpsAdminService;
    private final CpsModuleService cpsModuleService;
    private final CpsDataService cpsDataService;
    private static final String SUBSCRIPTION_MODEL_FILENAME = "subscription.yang";
    private static final String SUBSCRIPTION_MODEL_RESOURCE_PATH = "model/" + SUBSCRIPTION_MODEL_FILENAME;
    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SUBSCRIPTION_SCHEMASET_NAME = "subscriptions";
    private static final String SUBSCRIPTION_REGISTRY_DATANODE_NAME = "subscription-registry";

    @Value("${ncmp.model-loader.maximum-attempt-count:20}")
    private int maximumAttemptCount;

    @Value("${ncmp.timers.model-loader.retry-time-ms:1000}")
    private long retryTimeMs;

    @Value("${ncmp.model-loader.subscription:true}")
    private boolean subscriptionModelLoaderEnabled;

    /**
     * Method calls boarding subscription model when Application is ready.
     *
     * @param applicationReadyEvent the event to respond to
     */
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) {
        try {
            if (subscriptionModelLoaderEnabled) {
                checkNcmpDataspaceExists();
                onboardSubscriptionModel(createYangResourceToContentMap());
            } else {
                log.info("Subscription Model Loader is disabled");
            }
        } catch (final NcmpStartUpException ncmpStartUpException) {
            log.debug("Onboarding model for NCMP failed: {} ", ncmpStartUpException.getMessage());
            SpringApplication.exit(applicationReadyEvent.getApplicationContext(), () -> 1);
        }
    }

    private void checkNcmpDataspaceExists() {
        boolean ncmpDataspaceExists = false;
        int attemptCount = 0;
        while (!ncmpDataspaceExists) {
            final Dataspace ncmpDataspace = cpsAdminService.getDataspace(SUBSCRIPTION_DATASPACE_NAME);
            if (ncmpDataspace != null) {
                ncmpDataspaceExists = true;
            }
            if (attemptCount < maximumAttemptCount) {
                try {
                    Thread.sleep(attemptCount * retryTimeMs);
                    attemptCount++;
                    log.info("Retrieving NCMP dataspace... {} attempt(s) ", attemptCount);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                throw new NcmpStartUpException("Retrieval of NCMP dataspace fails",
                    "NCMP dataspace does not exist");
            }
        }
    }

    /**
     * Method to onboard subscription model for NCMP.
     */
    private void onboardSubscriptionModel(final Map<String, String> yangResourceContentMap) {
        createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, yangResourceContentMap);
        createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME);
        createTopLevelDataNode(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
            SUBSCRIPTION_REGISTRY_DATANODE_NAME);
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

    private void createTopLevelDataNode(final String dataspaceName,
                                        final String anchorName,
                                        final String dataNodeName) {
        final String nodeData = "{\"" + dataNodeName + "\":{}}";
        try {
            cpsDataService.saveData(dataspaceName, anchorName, nodeData, OffsetDateTime.now());
        } catch (final AlreadyDefinedExceptionBatch exception) {
            log.info("Creating new data node '{}' failed as data node already exists", dataNodeName);
        } catch (final Exception exception) {
            log.debug("Creating data node for subscription model failed: {}", exception.getMessage());
            throw new NcmpStartUpException("Creating data node failed", exception.getMessage());
        }
    }

    private String getFileContentAsString(final String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception exception) {
            final String message = String.format("Onboarding failed as unable to read file: %s", fileName);
            log.debug(message);
            throw new NcmpStartUpException(message, exception.getMessage());
        }
    }

    private Map<String, String> createYangResourceToContentMap() {
        return Map.of(SUBSCRIPTION_MODEL_FILENAME, getFileContentAsString(SUBSCRIPTION_MODEL_RESOURCE_PATH));
    }
}
