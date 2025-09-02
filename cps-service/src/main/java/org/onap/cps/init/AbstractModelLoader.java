/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.api.exceptions.AlreadyDefinedException;
import org.onap.cps.api.exceptions.AnchorNotFoundException;
import org.onap.cps.api.exceptions.DuplicatedYangResourceException;
import org.onap.cps.api.exceptions.ModelOnboardingException;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.parameters.CascadeDeleteAllowed;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractModelLoader implements ModelLoader {

    protected final CpsDataspaceService cpsDataspaceService;
    private final CpsModuleService cpsModuleService;
    protected final CpsAnchorService cpsAnchorService;
    protected final CpsDataService cpsDataService;

    private final JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper());

    private static final int EXIT_CODE_ON_ERROR = 1;

    @Override
    public void onApplicationEvent(final ApplicationStartedEvent applicationStartedEvent) {
        try {
            onboardOrUpgradeModel();
        } catch (final Exception exception) {
            log.error("Exiting application due to failure in onboarding model: {} ",
                exception.getMessage());
            exitApplication(applicationStartedEvent);
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
        } catch (final DuplicatedYangResourceException duplicatedYangResourceException) {
            log.warn("Ignoring yang resource duplication exception. Assuming model was created by another instance");
        } catch (final Exception exception) {
            log.error("Creating schema set {} failed: {} ", schemaSetName, exception.getMessage());
            throw new ModelOnboardingException("Creating schema set failed", exception.getMessage());
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
            throw new ModelOnboardingException("Creating dataspace failed", exception.getMessage());
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
            throw new ModelOnboardingException("Creating anchor failed", exception.getMessage());
        }
    }

    /**
     * Checks whether the specified anchor exists within the given dataspace.
     *
     * @param dataspaceName the name of the dataspace
     * @param anchorName    the name of the anchor within the dataspace
     * @return {@code true} if the anchor exists, {@code false} otherwise
     */
    public boolean doesAnchorExist(final String dataspaceName, final String anchorName) {
        try {
            cpsAnchorService.getAnchor(dataspaceName, anchorName);
            return true;
        } catch (final AnchorNotFoundException anchorNotFoundException) {
            log.debug("Anchor '{}' not found in dataspace '{}'", anchorName, dataspaceName);
            return false;
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
            throw new ModelOnboardingException("Creating data node failed", exception.getMessage());
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
            throw new ModelOnboardingException("Updating schema set failed", exception.getMessage());
        }
    }

    Map<String, String> mapYangResourcesToContent(final String... resourceNames) {
        final Map<String, String> yangResourceContentByName = new HashMap<>();
        for (final String resourceName: resourceNames) {
            yangResourceContentByName.put(resourceName, getFileContentAsString("models/" + resourceName));
        }
        return yangResourceContentByName;
    }

    private String getFileContentAsString(final String fileName) {
        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception exception) {
            final String message = String.format("Onboarding failed as unable to read file: %s", fileName);
            log.debug(message);
            throw new ModelOnboardingException(message, exception.getMessage());
        }
    }

    /**
     * Checks if the specified revision of a module is installed.
     */
    protected boolean isModuleRevisionInstalled(final String dataspaceName, final String anchorName,
                                                final String moduleName, final String moduleRevision) {
        final Collection<ModuleDefinition> moduleDefinitions =
                cpsModuleService.getModuleDefinitionsByAnchorAndModule(dataspaceName, anchorName, moduleName,
                        moduleRevision);
        return !moduleDefinitions.isEmpty();
    }

    private void exitApplication(final ApplicationStartedEvent applicationStartedEvent) {
        SpringApplication.exit(applicationStartedEvent.getApplicationContext(), () -> EXIT_CODE_ON_ERROR);
    }
}
