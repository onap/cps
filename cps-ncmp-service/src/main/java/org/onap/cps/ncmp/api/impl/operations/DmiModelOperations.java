/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.operations;

import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.MODEL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.ncmp.api.models.YangResource;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DmiModelOperations extends DmiOperations {

    /**
     * Constructor for {@code DmiOperations}. This method also manipulates url properties.
     *
     * @param dmiRestClient {@code DmiRestClient}
     */
    public DmiModelOperations(final PersistenceCmHandleRetriever cmHandlePropertiesRetriever,
                              final ObjectMapper objectMapper,
                              final NcmpConfiguration.DmiProperties dmiProperties,
                              final DmiRestClient dmiRestClient) {
        super(cmHandlePropertiesRetriever, objectMapper, dmiProperties, dmiRestClient);
    }

    /**
     * Retrieves module references.
     *
     * @param persistenceCmHandle the persistence cm handle
     * @return module references
     */
    public List<ModuleReference> getModuleReferences(final PersistenceCmHandle persistenceCmHandle) {
        final DmiRequestBody dmiRequestBody = DmiRequestBody.builder()
            .build();
        dmiRequestBody.asCmHandleProperties(persistenceCmHandle.getAdditionalProperties());
        final ResponseEntity<Object> dmiFetchModulesResponseEntity = getResourceFromDmiWithJsonData(
            persistenceCmHandle.resolveDmiServiceName(MODEL),
            getDmiRequestBodyAsString(dmiRequestBody), persistenceCmHandle.getId(), "modules");
        return toModuleReferences((Map) dmiFetchModulesResponseEntity.getBody());
    }

    /**
     * Retrieve yang resources from dmi for any modules that CPS-NCMP hasn't cached before.
     *
     * @param persistenceCmHandle the persistenceCmHandle
     * @param unknownModuleReferences the unknown module references
     * @return yang resources as map of module name to yang(re)source
     */
    public Map<String, String> getNewYangResourcesFromDmi(final PersistenceCmHandle persistenceCmHandle,
                                                          final List<ModuleReference> unknownModuleReferences) {
        final String jsonDataWithDataAndCmHandleProperties = getRequestBodyToFetchYangResources(
            unknownModuleReferences, persistenceCmHandle.getAdditionalProperties());
        final ResponseEntity<Object> responseEntity = getResourceFromDmiWithJsonData(
            persistenceCmHandle.resolveDmiServiceName(MODEL),
            jsonDataWithDataAndCmHandleProperties,
            persistenceCmHandle.getId(),
            "moduleResources");
        return asModuleNameToYangResourceMap(responseEntity);
    }

    /**
     * Get resources from DMI for modules.
     *
     * @param dmiServiceName dmi service name
     * @param jsonData module names and revisions as JSON
     * @param cmHandle cmHandle
     * @param resourceName name of the resource(s)
     * @return {@code ResponseEntity} response entity
     */
    private ResponseEntity<Object> getResourceFromDmiWithJsonData(final String dmiServiceName,
                                                                  final String jsonData,
                                                                  final String cmHandle,
                                                                  final String resourceName) {
        final String dmiResourceDataUrl = getDmiResourceUrl(dmiServiceName, cmHandle, resourceName);
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, jsonData, new HttpHeaders());
    }

    private static String getRequestBodyToFetchYangResources(final List<ModuleReference> unknownModuleReferences,
        final List<PersistenceCmHandle.AdditionalProperty> cmHandleProperties) {
        final JsonArray moduleReferencesAsJson = getModuleReferencesAsJson(unknownModuleReferences);
        final JsonObject data = new JsonObject();
        data.add("modules", moduleReferencesAsJson);
        final JsonObject jsonRequestObject = new JsonObject();
        jsonRequestObject.add("data", data);
        jsonRequestObject.add("cmHandleProperties", toJsonObject(cmHandleProperties));
        return jsonRequestObject.toString();
    }

    private static JsonArray getModuleReferencesAsJson(final List<ModuleReference> unknownModuleReferences) {
        final JsonArray moduleReferences = new JsonArray();

        for (final ModuleReference moduleReference : unknownModuleReferences) {
            final JsonObject moduleReferenceAsJson = new JsonObject();
            moduleReferenceAsJson.addProperty("name", moduleReference.getModuleName());
            moduleReferenceAsJson.addProperty("revision", moduleReference.getRevision());
            moduleReferences.add(moduleReferenceAsJson);
        }
        return moduleReferences;
    }

    private static JsonObject toJsonObject(final List<PersistenceCmHandle.AdditionalProperty> cmHandleProperties) {
        final JsonObject asJsonObject = new JsonObject();
        for (final PersistenceCmHandle.AdditionalProperty additionalProperty : cmHandleProperties) {
            asJsonObject.addProperty(additionalProperty.getName(), additionalProperty.getValue());
        }
        return asJsonObject;
    }

    private List<ModuleReference> toModuleReferences(final Map dmiFetchModulesResponseAsMap) {
        final List<ModuleReference> moduleReferences = new ArrayList<>();

        if (dmiFetchModulesResponseAsMap != null) {
            final List moduleReferencesAsList = (List) dmiFetchModulesResponseAsMap.get("schemas");
            if (moduleReferencesAsList != null) {
                moduleReferencesAsList.forEach(moduleReferenceAsMap -> {
                    final ModuleReference moduleReference =
                        objectMapper.convertValue(moduleReferenceAsMap, ModuleReference.class);
                    moduleReferences.add(moduleReference);
                });
            }
        }
        return moduleReferences;
    }

    private Map<String, String> asModuleNameToYangResourceMap(final ResponseEntity<Object> responseEntity) {
        final Map<String, String> yangResourcesModuleNameToContentMap = new HashMap<>();
        final List<Map<String, String>> yangResourcesAsList = (List) responseEntity.getBody();

        if (yangResourcesAsList != null) {
            yangResourcesAsList.forEach(yangResourceAsMap -> {
                final YangResource yangResource =
                    objectMapper.convertValue(yangResourceAsMap, YangResource.class);
                yangResourcesModuleNameToContentMap.put(yangResource.getModuleName(),
                    yangResource.getYangSource());
            });
        }
        return yangResourcesModuleNameToContentMap;
    }
}
