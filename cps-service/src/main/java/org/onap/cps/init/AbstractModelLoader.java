/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 TechMahindra Ltd.
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

package org.onap.cps.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.ModelStartupException;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractModelLoader implements ModelLoader {

    protected final CpsDataspaceService cpsDataspaceService;
    private final CpsModuleService cpsModuleService;
    private final CpsAnchorService cpsAnchorService;
    protected final CpsDataService cpsDataService;

    private final JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper());

    private static final int EXIT_CODE_ON_ERROR = 1;

    @Override
    public void onApplicationEvent(final ApplicationStartedEvent applicationStartedEvent) {
        try {
            onboardOrUpgradeModel();
        } catch (final Exception modelStartUpException) {
            log.error("Exiting application due to failure in onboarding model: {} ",
                    modelStartUpException.getMessage());
            SpringApplication.exit(applicationStartedEvent.getApplicationContext(), () -> EXIT_CODE_ON_ERROR);
        }
    }

    /**
     * Create initial schema set.
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @param resourceNames resource names
     */
    public void createSchemaSet(final String dataspaceName, final String schemaSetName, final String... resourceNames) {
        try {
            final Map<String, String> yangResourcesContentByResourceName = mapYangResourcesToContent(resourceNames);
            cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, yangResourcesContentByResourceName);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.warn("Creating new schema set failed as schema set already exists");
        } catch (final Exception exception) {
            log.error("Creating schema set failed: {} ", exception.getMessage());
            throw new ModelStartupException("Creating schema set failed", exception.getMessage());
        }
    }

    /**
     * Create initial dataspace.
     * @param dataspaceName dataspace name
     */
    public void createDataspace(final String dataspaceName) {
        try {
            cpsDataspaceService.createDataspace(dataspaceName);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.debug("Dataspace already exists");
        } catch (final Exception exception) {
            log.error("Creating dataspace failed: {} ", exception.getMessage());
            throw new ModelStartupException("Creating dataspace failed", exception.getMessage());
        }
    }

    /**
     * Create initial anchor.
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @param anchorName anchor name
     */
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        try {
            cpsAnchorService.createAnchor(dataspaceName, schemaSetName, anchorName);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.warn("Creating new anchor failed as anchor already exists");
        } catch (final Exception exception) {
            log.error("Creating anchor failed: {} ", exception.getMessage());
            throw new ModelStartupException("Creating anchor failed", exception.getMessage());
        }
    }

    /**
     * Create initial top level data node.
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param dataNodeName data node name
     */
    public void createTopLevelDataNode(final String dataspaceName, final String anchorName, final String dataNodeName) {
        final String nodeData = jsonObjectMapper.asJsonString(Map.of(dataNodeName, Map.of()));
        try {
            cpsDataService.saveData(dataspaceName, anchorName, nodeData, OffsetDateTime.now());
        } catch (final AlreadyDefinedException exception) {
            log.warn("Creating new data node '{}' failed as data node already exists", dataNodeName);
        } catch (final Exception exception) {
            log.error("Creating data node failed: {}", exception.getMessage());
            throw new ModelStartupException("Creating data node failed", exception.getMessage());
        }
    }

    /**
     * Delete unused schema set.
     * @param dataspaceName dataspace name
     * @param schemaSetNames schema set names
     */
    public void deleteUnusedSchemaSets(final String dataspaceName, final String... schemaSetNames) {
        for (final String schemaSetName : schemaSetNames) {
            try {
                cpsModuleService.deleteSchemaSet(
                        dataspaceName, schemaSetName, CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED);
            } catch (final Exception exception) {
                log.warn("Deleting schema set failed: {} ", exception.getMessage());
            }
        }
    }

    /**
     * Update anchor schema set.
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param schemaSetName schemaset name
     */
    public void updateAnchorSchemaSet(final String dataspaceName, final String anchorName, final String schemaSetName) {
        try {
            cpsAnchorService.updateAnchorSchemaSet(dataspaceName, anchorName, schemaSetName);
        } catch (final Exception exception) {
            log.error("Updating schema set failed: {}", exception.getMessage());
            throw new ModelStartupException("Updating schema set failed", exception.getMessage());
        }
    }

    Map<String, String> mapYangResourcesToContent(final String... resourceNames) {
        final Map<String, String> yangResourceContentByName = new HashMap<>();
        for (final String resourceName: resourceNames) {
            yangResourceContentByName.put(resourceName, getFileContentAsString(resourceName));
        }
        return yangResourceContentByName;
    }

    private String getFileContentAsString(final String fileName) {
        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception exception) {
            final String message = String.format("Onboarding failed as unable to read file: %s", fileName);
            log.debug(message);
            throw new ModelStartupException(message, exception.getMessage());
        }
    }
}
