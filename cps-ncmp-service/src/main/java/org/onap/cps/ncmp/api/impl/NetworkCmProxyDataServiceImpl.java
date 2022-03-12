/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada
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

package org.onap.cps.ncmp.api.impl;

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum;
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.InvalidTopicException;
import org.onap.cps.ncmp.api.impl.exception.ServerNcmpException;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiOperations;
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandlesList;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private final CpsDataService cpsDataService;

    private final JsonObjectMapper jsonObjectMapper;

    private final DmiDataOperations dmiDataOperations;

    private final DmiModelOperations dmiModelOperations;

    private final CpsModuleService cpsModuleService;

    private final CpsAdminService cpsAdminService;

    private final NetworkCmProxyDataServicePropertyHandler networkCmProxyDataServicePropertyHandler;

    private final YangModelCmHandleRetriever yangModelCmHandleRetriever;

    // valid kafka topic name regex
    private static final Pattern TOPIC_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]([._-](?![._-])|"
            + "[a-zA-Z0-9]){0,120}[a-zA-Z0-9]$");
    private static final String NO_REQUEST_ID = null;
    private static final String NO_TOPIC = null;

    @Override
    public void updateDmiRegistrationAndSyncModule(final DmiPluginRegistration dmiPluginRegistration) {
        dmiPluginRegistration.validateDmiPluginRegistration();
        try {
            if (!dmiPluginRegistration.getCreatedCmHandles().isEmpty()) {
                parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(dmiPluginRegistration);
            }
            if (!dmiPluginRegistration.getUpdatedCmHandles().isEmpty()) {
                parseAndUpdateCmHandlesInDmiRegistration(dmiPluginRegistration);
            }
            parseAndRemoveCmHandlesInDmiRegistration(dmiPluginRegistration);
        } catch (final JsonProcessingException | DataNodeNotFoundException e) {
            final String errorMessage = String.format(
                    "Error occurred while processing the CM-handle registration request, caused by : [%s]",
                    e.getMessage());
            throw new DataValidationException(errorMessage, e.getMessage(), e);
        }
    }

    @Override
    public Object getResourceDataOperationalForCmHandle(final String cmHandleId,
                                                        final String resourceIdentifier,
                                                        final String acceptParamInHeader,
                                                        final String optionsParamInQuery,
                                                        final String topicParamInQuery) {

        return validateTopicNameAndGetResourceData(cmHandleId, resourceIdentifier, acceptParamInHeader,
                DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL, optionsParamInQuery, topicParamInQuery);
    }

    @Override
    public Object getResourceDataPassThroughRunningForCmHandle(final String cmHandleId,
                                                               final String resourceIdentifier,
                                                               final String acceptParamInHeader,
                                                               final String optionsParamInQuery,
                                                               final String topicParamInQuery) {
        return validateTopicNameAndGetResourceData(cmHandleId, resourceIdentifier, acceptParamInHeader,
                DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING, optionsParamInQuery, topicParamInQuery);
    }

    @Override
    public Object writeResourceDataPassThroughRunningForCmHandle(final String cmHandleId,
                                                               final String resourceIdentifier,
                                                               final OperationEnum operation,
                                                               final String requestData,
                                                               final String dataType) {
        return handleResponse(
            dmiDataOperations.writeResourceDataPassThroughRunningFromDmi(
                cmHandleId, resourceIdentifier, operation, requestData, dataType),
            "Not able to " + operation + " resource data.");
    }


    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleId) {
        return cpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
    }

    /**
     * Retrieve cm handle identifiers for the given list of module names.
     *
     * @param moduleNames module names.
     * @return a collection of anchor identifiers
     */
    @Override
    public Collection<String> executeCmHandleHasAllModulesSearch(final Collection<String> moduleNames) {
        return cpsAdminService.queryAnchorNames(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, moduleNames);
    }

    /**
     * Retrieve cm handle details for a given cm handle.
     * @param cmHandleId cm handle identifier
     * @return cm handle details
     */
    @Override
    public NcmpServiceCmHandle getNcmpServiceCmHandle(final String cmHandleId) {
        final NcmpServiceCmHandle ncmpServiceCmHandle = new NcmpServiceCmHandle();
        final YangModelCmHandle yangModelCmHandle =
            yangModelCmHandleRetriever.getDmiServiceNamesAndProperties(cmHandleId);
        final List<YangModelCmHandle.Property> dmiProperties = yangModelCmHandle.getDmiProperties();
        final List<YangModelCmHandle.Property> publicProperties = yangModelCmHandle.getPublicProperties();
        ncmpServiceCmHandle.setCmHandleID(yangModelCmHandle.getId());
        setDmiProperties(dmiProperties, ncmpServiceCmHandle);
        setPublicProperties(publicProperties, ncmpServiceCmHandle);
        return ncmpServiceCmHandle;
    }

    private void setDmiProperties(final List<YangModelCmHandle.Property> dmiProperties,
                                  final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Map<String, String> dmiPropertiesMap = new LinkedHashMap<>(dmiProperties.size());
        asPropertiesMap(dmiProperties, dmiPropertiesMap);
        ncmpServiceCmHandle.setDmiProperties(dmiPropertiesMap);
    }

    private void setPublicProperties(final List<YangModelCmHandle.Property> publicProperties,
                                     final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Map<String, String> publicPropertiesMap = new LinkedHashMap<>();
        asPropertiesMap(publicProperties, publicPropertiesMap);
        ncmpServiceCmHandle.setPublicProperties(publicPropertiesMap);
    }

    private void asPropertiesMap(final List<YangModelCmHandle.Property> properties,
                                 final Map<String, String> propertiesMap) {
        for (final YangModelCmHandle.Property property: properties) {
            propertiesMap.put(property.getName(), property.getValue());
        }
    }

    /**
     * THis method registers a cm handle and initiates modules sync.
     *
     * @param dmiPluginRegistration dmi plugin registration information.
     * @throws JsonProcessingException thrown if json is malformed or missing.
     */
    public void parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(
        final DmiPluginRegistration dmiPluginRegistration) throws JsonProcessingException {
        final YangModelCmHandlesList createdYangModelCmHandlesList =
            getUpdatedYangModelCmHandlesList(dmiPluginRegistration,
                dmiPluginRegistration.getCreatedCmHandles());
        registerAndSyncNewCmHandles(createdYangModelCmHandlesList);
    }

    private static Object handleResponse(final ResponseEntity<?> responseEntity,
                                         final String exceptionMessage) {
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        } else {
            throw new ServerNcmpException(exceptionMessage,
                    "DMI status code: " + responseEntity.getStatusCodeValue()
                            + ", DMI response body: " + responseEntity.getBody());
        }
    }

    private void parseAndUpdateCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        networkCmProxyDataServicePropertyHandler.updateCmHandleProperties(dmiPluginRegistration.getUpdatedCmHandles());
    }

    private YangModelCmHandlesList getUpdatedYangModelCmHandlesList(
        final DmiPluginRegistration dmiPluginRegistration,
        final List<NcmpServiceCmHandle> updatedCmHandles) {
        return YangModelCmHandlesList.toYangModelCmHandlesList(
            dmiPluginRegistration.getDmiPlugin(),
            dmiPluginRegistration.getDmiDataPlugin(),
            dmiPluginRegistration.getDmiModelPlugin(),
            updatedCmHandles);
    }

    private void registerAndSyncNewCmHandles(final YangModelCmHandlesList yangModelCmHandlesList) {
        final String cmHandleJsonData = jsonObjectMapper.asJsonString(yangModelCmHandlesList);
        cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                cmHandleJsonData, NO_TIMESTAMP);

        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandlesList.getYangModelCmHandles()) {
            syncModulesAndCreateAnchor(yangModelCmHandle);
        }
    }

    protected void syncModulesAndCreateAnchor(final YangModelCmHandle yangModelCmHandle) {
        syncAndCreateSchemaSet(yangModelCmHandle);
        createAnchor(yangModelCmHandle);
    }

    private void parseAndRemoveCmHandlesInDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        for (final String cmHandle : dmiPluginRegistration.getRemovedCmHandles()) {
            try {
                attemptToDeleteSchemaSetWithCascade(cmHandle);
                cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    "/dmi-registry/cm-handles[@id='" + cmHandle + "']", NO_TIMESTAMP);
            } catch (final DataNodeNotFoundException e) {
                log.warn("Datanode {} not deleted message {}", cmHandle, e.getMessage());
            }
        }
    }

    private void attemptToDeleteSchemaSetWithCascade(final String schemaSetName) {
        try {
            cpsModuleService.deleteSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                CASCADE_DELETE_ALLOWED);
        } catch (final Exception e) {
            log.warn("Schema set {} delete failed, reason {}", schemaSetName, e.getMessage());
        }
    }

    private void syncAndCreateSchemaSet(final YangModelCmHandle yangModelCmHandle) {
        final Collection<ModuleReference> moduleReferencesFromCmHandle =
            dmiModelOperations.getModuleReferences(yangModelCmHandle);

        final Collection<ModuleReference> identifiedNewModuleReferencesFromCmHandle = cpsModuleService
            .identifyNewModuleReferences(moduleReferencesFromCmHandle);

        final Collection<ModuleReference> existingModuleReferencesFromCmHandle =
            moduleReferencesFromCmHandle.stream().filter(moduleReferenceFromCmHandle ->
                !identifiedNewModuleReferencesFromCmHandle.contains(moduleReferenceFromCmHandle)
            ).collect(Collectors.toList());

        final Map<String, String> newModuleNameToContentMap;
        if (identifiedNewModuleReferencesFromCmHandle.isEmpty()) {
            newModuleNameToContentMap = new HashMap<>();
        } else {
            newModuleNameToContentMap = dmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle,
                identifiedNewModuleReferencesFromCmHandle);
        }
        cpsModuleService
            .createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, yangModelCmHandle.getId(),
                newModuleNameToContentMap, existingModuleReferencesFromCmHandle);
    }

    private void createAnchor(final YangModelCmHandle yangModelCmHandle) {
        cpsAdminService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, yangModelCmHandle.getId(),
            yangModelCmHandle.getId());
    }

    private static boolean isValidTopicName(final String topicName) {
        if (topicName == null) {
            return false;
        }
        if (TOPIC_NAME_PATTERN.matcher(topicName).matches()) {
            return true;
        }
        throw new InvalidTopicException("Topic name " + topicName + " is invalid", "invalid topic");
    }

    private Map<String, Object> buildDmiResponse(final String requestId) {
        final Map<String, Object> dmiResponseMap = new HashMap<>();
        dmiResponseMap.put("requestId", requestId);
        return dmiResponseMap;
    }

    private Object validateTopicNameAndGetResourceData(final String cmHandleId,
                                                       final String resourceIdentifier,
                                                       final String acceptParamInHeader,
                                                       final DmiOperations.DataStoreEnum dataStore,
                                                       final String optionsParamInQuery,
                                                       final String topicParamInQuery) {
        final boolean processAsynchronously = isValidTopicName(topicParamInQuery);
        if (processAsynchronously) {
            final String resourceDataRequestId = UUID.randomUUID().toString();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(buildDmiResponse(resourceDataRequestId));
        }
        final ResponseEntity<?> responseEntity = dmiDataOperations.getResourceDataFromDmi(
                cmHandleId, resourceIdentifier, optionsParamInQuery, acceptParamInHeader,
                dataStore, NO_REQUEST_ID, NO_TOPIC);
        return handleResponse(responseEntity, "Not able to get resource data.");
    }
}