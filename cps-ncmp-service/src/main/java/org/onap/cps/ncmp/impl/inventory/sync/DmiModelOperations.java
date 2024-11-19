/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.impl.inventory.sync;

import static org.onap.cps.ncmp.api.data.models.OperationType.READ;
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.MODEL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.YangResource;
import org.onap.cps.ncmp.impl.dmi.DmiProperties;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.DmiRequestBody;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Operations class for DMI Model.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DmiModelOperations {

    private final JsonObjectMapper jsonObjectMapper;
    private final DmiProperties dmiProperties;
    private final DmiRestClient dmiRestClient;

    /**
     * Retrieves module references.
     *
     * @param yangModelCmHandle the yang model cm handle
     * @return module references
     */
    @Timed(value = "cps.ncmp.inventory.module.references.from.dmi",
        description = "Time taken to get all module references for a cm handle from dmi")
    public List<ModuleReference> getModuleReferences(final YangModelCmHandle yangModelCmHandle) {
        final DmiRequestBody dmiRequestBody = DmiRequestBody.builder()
                .moduleSetTag(yangModelCmHandle.getModuleSetTag()).build();
        dmiRequestBody.asDmiProperties(yangModelCmHandle.getDmiProperties());
        final ResponseEntity<Object> dmiFetchModulesResponseEntity = getResourceFromDmiWithJsonData(
            yangModelCmHandle.resolveDmiServiceName(MODEL),
                jsonObjectMapper.asJsonString(dmiRequestBody), yangModelCmHandle.getId(), "modules");
        return toModuleReferences((Map) dmiFetchModulesResponseEntity.getBody());
    }

    /**
     * Retrieve yang resources from dmi for any modules that CPS-NCMP hasn't cached before.
     *
     * @param yangModelCmHandle the yangModelCmHandle
     * @param newModuleReferences the unknown module references
     * @return yang resources as map of module name to yang(re)source
     */
    @Timed(value = "cps.ncmp.inventory.yang.resources.from.dmi",
        description = "Time taken to get list of yang resources from dmi")
    public Map<String, String> getNewYangResourcesFromDmi(final YangModelCmHandle yangModelCmHandle,
                                                          final Collection<ModuleReference> newModuleReferences) {
        if (newModuleReferences.isEmpty()) {
            return Collections.emptyMap();
        }
        final String jsonWithDataAndDmiProperties = getRequestBodyToFetchYangResources(newModuleReferences,
                yangModelCmHandle.getDmiProperties(), yangModelCmHandle.getModuleSetTag());
        final ResponseEntity<Object> responseEntity = getResourceFromDmiWithJsonData(
            yangModelCmHandle.resolveDmiServiceName(MODEL),
            jsonWithDataAndDmiProperties,
            yangModelCmHandle.getId(),
            "moduleResources");
        return asModuleNameToYangResourceMap(responseEntity);
    }

    /**
     * Get resources from DMI for modules.
     *
     * @param dmiServiceName dmi service name
     * @param jsonRequestBody module names and revisions as JSON
     * @param cmHandle cmHandle
     * @param resourceName name of the resource(s)
     * @return {@code ResponseEntity} response entity
     */
    private ResponseEntity<Object> getResourceFromDmiWithJsonData(final String dmiServiceName,
                                                                  final String jsonRequestBody,
                                                                  final String cmHandle,
                                                                  final String resourceName) {
        final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
                .fixedPathSegment("ch")
                .variablePathSegment("cmHandleId", cmHandle)
                .fixedPathSegment(resourceName)
                .createUrlTemplateParameters(dmiServiceName, dmiProperties.getDmiBasePath());
        return dmiRestClient.synchronousPostOperationWithJsonData(MODEL, urlTemplateParameters, jsonRequestBody, READ,
                null);
    }

    private static String getRequestBodyToFetchYangResources(final Collection<ModuleReference> newModuleReferences,
                                                             final List<YangModelCmHandle.Property> dmiProperties,
                                                             final String moduleSetTag) {
        final JsonArray moduleReferencesAsJson = getModuleReferencesAsJson(newModuleReferences);
        final JsonObject data = new JsonObject();
        data.add("modules", moduleReferencesAsJson);
        final JsonObject jsonRequestObject = new JsonObject();
        if (!moduleSetTag.isEmpty()) {
            jsonRequestObject.addProperty("moduleSetTag", moduleSetTag);
        }
        jsonRequestObject.add("data", data);
        jsonRequestObject.add("cmHandleProperties", toJsonObject(dmiProperties));
        return jsonRequestObject.toString();
    }

    private static JsonArray getModuleReferencesAsJson(final Collection<ModuleReference> unknownModuleReferences) {
        final JsonArray moduleReferences = new JsonArray();

        for (final ModuleReference moduleReference : unknownModuleReferences) {
            final JsonObject moduleReferenceAsJson = new JsonObject();
            moduleReferenceAsJson.addProperty("name", moduleReference.getModuleName());
            moduleReferenceAsJson.addProperty("revision", moduleReference.getRevision());
            moduleReferences.add(moduleReferenceAsJson);
        }
        return moduleReferences;
    }

    private static JsonObject toJsonObject(final List<YangModelCmHandle.Property>
                                               dmiProperties) {
        final JsonObject asJsonObject = new JsonObject();
        for (final YangModelCmHandle.Property additionalProperty : dmiProperties) {
            asJsonObject.addProperty(additionalProperty.getName(), additionalProperty.getValue());
        }
        return asJsonObject;
    }

    private List<ModuleReference> toModuleReferences(final Map<String, Object> dmiFetchModulesResponseAsMap) {
        final List<ModuleReference> moduleReferences = new ArrayList<>();

        if (dmiFetchModulesResponseAsMap != null) {
            final List<Object> moduleReferencesAsList = (List) dmiFetchModulesResponseAsMap.get("schemas");
            if (moduleReferencesAsList != null) {
                moduleReferencesAsList.forEach(moduleReferenceAsMap -> {
                    final ModuleReference moduleReference =
                            jsonObjectMapper.convertToValueType(moduleReferenceAsMap, ModuleReference.class);
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
                        jsonObjectMapper.convertToValueType(yangResourceAsMap, YangResource.class);
                yangResourcesModuleNameToContentMap.put(yangResource.getModuleName(),
                    yangResource.getYangSource());
            });
        }
        return yangResourcesModuleNameToContentMap;
    }
}
