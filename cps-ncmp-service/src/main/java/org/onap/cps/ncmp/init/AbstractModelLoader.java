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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.exception.NcmpStartUpException;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
abstract class AbstractModelLoader implements ModelLoader {

    private final CpsAdminService cpsAdminService;
    private final CpsModuleService cpsModuleService;
    private final CpsDataService cpsDataService;

    private static final int EXIT_CODE_ON_ERROR = 1;

    private final JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper());

    @Value("${ncmp.model-loader.maximum-attempt-count:20}")
    int maximumAttemptCount;

    @Value("${ncmp.timers.model-loader.retry-time-ms:1000}")
    long retryTimeMs;

    @Override
    public void onApplicationEvent(@NonNull final ApplicationReadyEvent applicationReadyEvent) {
        try {
            onboardOrUpgradeModel();
        } catch (final NcmpStartUpException ncmpStartUpException) {
            log.error("Onboarding model for NCMP failed: {} ", ncmpStartUpException.getMessage());
            SpringApplication.exit(applicationReadyEvent.getApplicationContext(), () -> EXIT_CODE_ON_ERROR);
        }
    }

    void waitUntilDataspaceIsAvailable(final String dataspaceName) {
        int attemptCount = 0;
        while (cpsAdminService.getDataspace(dataspaceName) == null) {
            if (attemptCount < maximumAttemptCount) {
                try {
                    Thread.sleep(attemptCount * retryTimeMs);
                    log.info("Retrieving dataspace {} ... {} attempt(s) ", dataspaceName, ++attemptCount);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                throw new NcmpStartUpException("Retrieval of NCMP dataspace failed",
                    dataspaceName + " not available (yet)");
            }
        }
    }

    void createSchemaSet(final String dataspaceName, final String schemaSetName, final String resourceName) {
        try {
            final Map<String, String> yangResourceContentMap = createYangResourceToContentMap(resourceName);
            cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentMap);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.warn("Creating new schema set failed as schema set already exists");
        } catch (final Exception exception) {
            log.error("Creating schema set failed: {} ", exception.getMessage());
            throw new NcmpStartUpException("Creating schema set failed", exception.getMessage());
        }
    }

    void deleteUnusedSchemaSets(final String dataspaceName, final String... schemaSetNames) {
        for (final String schemaSetName : schemaSetNames) {
            try {
                cpsModuleService.deleteSchemaSet(
                    dataspaceName, schemaSetName, CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED);
            } catch (final Exception exception) {
                log.warn("Deleting schema set failed: {} ", exception.getMessage());
            }
        }
    }

    void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        try {
            cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.warn("Creating new anchor failed as anchor already exists");
        } catch (final Exception exception) {
            log.error("Creating anchor failed: {} ", exception.getMessage());
            throw new NcmpStartUpException("Creating anchor failed", exception.getMessage());
        }
    }

    void createTopLevelDataNode(final String dataspaceName, final String anchorName, final String dataNodeName) {
        final String nodeData = jsonObjectMapper.asJsonString(Map.of(dataNodeName, Map.of()));
        try {
            cpsDataService.saveData(dataspaceName, anchorName, nodeData, OffsetDateTime.now());
        } catch (final AlreadyDefinedException exception) {
            log.warn("Creating new data node '{}' failed as data node already exists", dataNodeName);
        } catch (final Exception exception) {
            log.error("Creating data node failed: {}", exception.getMessage());
            throw new NcmpStartUpException("Creating data node failed", exception.getMessage());
        }
    }

    void updateAnchorSchemaSet(final String dataspaceName, final String anchorName, final String schemaSetName) {
        try {
            cpsAdminService.updateAnchorSchemaSet(dataspaceName, anchorName, schemaSetName);
        } catch (final Exception exception) {
            log.error("Updating schema set failed: {}", exception.getMessage());
            throw new NcmpStartUpException("Updating schema set failed", exception.getMessage());
        }
    }

    Map<String, String> createYangResourceToContentMap(final String resourceName) {
        return Map.of(resourceName, getFileContentAsString("models/" + resourceName));
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

}
