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

import static org.onap.cps.utils.ContentType.JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsStartupException;
import org.onap.cps.spi.model.Dataspace;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractModuleLoader implements ModuleLoader {

    protected final CpsDataspaceService cpsDataspaceService;
    private final CpsModuleService cpsModuleService;
    private final CpsAnchorService cpsAnchorService;
    protected final CpsDataService cpsDataService;

    private final JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper());

    private static final int EXIT_CODE_ON_ERROR = 1;
    protected static final String MODEL_FILENAME = "cps-notification-subscriptions@2024-07-03.yang";
    protected static final String SCHEMASET_NAME = "cps-notification-subscriptions";
    protected static final String ANCHOR_NAME = "cps-notification-subscriptions";
    protected static final String CPS_DATASPACE_NAME = "CPS-Admin";
    protected static final String REGISTRY_DATANODE_NAME = "dataspaces";

    @Override
    public void onApplicationEvent(final ApplicationStartedEvent applicationStartedEvent) {
        try {
            onboardOrUpgradeModel();
        } catch (final Exception cpsStartUpException) {
            log.error("Exiting application due to failure in onboarding CPS model: {} ",
                    cpsStartUpException.getMessage());
            SpringApplication.exit(applicationStartedEvent.getApplicationContext(), () -> EXIT_CODE_ON_ERROR);
        }
    }

    void createSchemaSet(final String dataspaceName, final String schemaSetName, final String... resourceNames) {
        try {
            final Map<String, String> yangResourcesContentByResourceName = mapYangResourcesToContent(resourceNames);
            cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, yangResourcesContentByResourceName);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.warn("Creating new schema set failed as schema set already exists");
        } catch (final Exception exception) {
            log.error("Creating schema set failed: {} ", exception.getMessage());
            throw new CpsStartupException("Creating schema set failed", exception.getMessage());
        }
    }

    void createDataspace(final String dataspaceName) {
        try {
            cpsDataspaceService.createDataspace(dataspaceName);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.debug("Dataspace already exists");
        } catch (final Exception exception) {
            log.error("Creating dataspace failed: {} ", exception.getMessage());
            throw new CpsStartupException("Creating dataspace failed", exception.getMessage());
        }
    }


    void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        try {
            cpsAnchorService.createAnchor(dataspaceName, schemaSetName, anchorName);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.warn("Creating new anchor failed as anchor already exists");
        } catch (final Exception exception) {
            log.error("Creating anchor failed: {} ", exception.getMessage());
            throw new CpsStartupException("Creating anchor failed", exception.getMessage());
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
            throw new CpsStartupException("Creating data node failed", exception.getMessage());
        }
    }

    void createInitialSubscription() {
        final Collection<Dataspace> dataspaceList  = cpsDataspaceService.getAllDataspaces();
        if (dataspaceList != null) {
            dataspaceList.forEach(this::subscribeNotificationForDataspace);
        }
    }

    private void subscribeNotificationForDataspace(final Dataspace dataspace) {
        final Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("dataspace", Collections.singletonList(Collections.singletonMap("name", dataspace.getName())));
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            final String nodeData = objectMapper.writeValueAsString(dataMap);
            cpsDataService.saveData(CPS_DATASPACE_NAME, ANCHOR_NAME,
                    "/" + REGISTRY_DATANODE_NAME, nodeData,
                    OffsetDateTime.now(), JSON);
        } catch (final AlreadyDefinedException exception) {
            log.info("Data node for dataspace '{}' already exists under '{}'.",
                    dataspace.getName(), REGISTRY_DATANODE_NAME);
        } catch (final Exception exception) {
            log.error("Failed to create data node for dataspace '{}': {}",
                    dataspace.getName(), exception.getMessage());
            throw new CpsStartupException("Creating data node failed", exception.getMessage());
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
            throw new CpsStartupException(message, exception.getMessage());
        }
    }
}
