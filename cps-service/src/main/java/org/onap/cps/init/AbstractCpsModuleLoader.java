package org.onap.cps.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsStartupException;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractCpsModuleLoader implements CpsModuleLoader {

    protected final CpsDataspaceService cpsDataspaceService;
    private final CpsModuleService cpsModuleService;
    private final CpsAnchorService cpsAnchorService;
    protected final CpsDataService cpsDataService;

    private static final int EXIT_CODE_ON_ERROR = 1;

    private final JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper());
    @Override
    public void onApplicationEvent(@NonNull final ApplicationReadyEvent applicationReadyEvent) {
        try {
            onboardOrUpgradeModel();
        } catch (final Exception cpsStartUpException) {
            log.error("Onboarding model for CPS failed: {} ", cpsStartUpException.getMessage());
            SpringApplication.exit(applicationReadyEvent.getApplicationContext(), () -> EXIT_CODE_ON_ERROR);
        }
    }

    void createSchemaSet(final String dataspaceName, final String schemaSetName, final String... resourceNames) {
        try {
            final Map<String, String> yangResourcesContentMap = createYangResourcesToContentMap(resourceNames);
            cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, yangResourcesContentMap);
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

    Map<String, String> createYangResourcesToContentMap(final String... resourceNames) {
        final Map<String, String> yangResourcesToContentMap = new HashMap<>();
        for (final String resourceName: resourceNames) {
            yangResourcesToContentMap.put(resourceName, getFileContentAsString(resourceName));
        }
        return yangResourcesToContentMap;
    }

    private String getFileContentAsString(final String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception exception) {
            final String message = String.format("Onboarding failed as unable to read file: %s", fileName);
            log.debug(message);
            throw new CpsStartupException(message, exception.getMessage());
        }
    }
}
