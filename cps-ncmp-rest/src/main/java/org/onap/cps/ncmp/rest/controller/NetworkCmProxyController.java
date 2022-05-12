/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
 *  Modification Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications (C) 2021-2022 Bell Canada
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

package org.onap.cps.ncmp.rest.controller;

import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.CREATE;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.DELETE;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.PATCH;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.UPDATE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.InvalidTopicException;
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.CmHandlePublicProperties;
import org.onap.cps.ncmp.rest.model.CmHandleQueryRestParameters;
import org.onap.cps.ncmp.rest.model.RestModuleReference;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${rest.api.ncmp-base-path}")
@RequiredArgsConstructor
public class NetworkCmProxyController implements NetworkCmProxyApi {

    private static final String NO_BODY = null;
    private static final String NO_REQUEST_ID = null;
    private static final String NO_TOPIC = null;
    public static final String ASYNC_REQUEST_ID = "requestId";

    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final JsonObjectMapper jsonObjectMapper;
    private final NcmpRestInputMapper ncmpRestInputMapper;

    /**
     * Get resource data from operational datastore.
     *
     * @param cmHandle cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param optionsParamInQuery options query parameter
     * @param topicParamInQuery topic query parameter
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Object> getResourceDataOperationalForCmHandle(final String cmHandle,
                                                                        final @NotNull @Valid String resourceIdentifier,
                                                                        final @Valid String optionsParamInQuery,
                                                                        final @Valid String topicParamInQuery) {
        final ResponseEntity<Map<String, Object>> asyncResponse = populateAsyncResponse(topicParamInQuery);
        final Map<String, Object> asyncResponseData = asyncResponse.getBody();

        final Object responseObject = networkCmProxyDataService.getResourceDataOperationalForCmHandle(cmHandle,
                resourceIdentifier,
                optionsParamInQuery,
                asyncResponseData == null ? NO_TOPIC : topicParamInQuery,
                asyncResponseData == null ? NO_REQUEST_ID : asyncResponseData.get(ASYNC_REQUEST_ID).toString());

        if (asyncResponseData == null) {
            return ResponseEntity.ok(responseObject);
        }
        return ResponseEntity.ok(asyncResponse);
    }

    /**
     * Get resource data from pass-through running datastore.
     *
     * @param cmHandle cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param optionsParamInQuery options query parameter
     * @param topicParamInQuery topic query parameter
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Object> getResourceDataRunningForCmHandle(final String cmHandle,
                                                                    final @NotNull @Valid String resourceIdentifier,
                                                                    final @Valid String optionsParamInQuery,
                                                                    final @Valid String topicParamInQuery) {
        final ResponseEntity<Map<String, Object>> asyncResponse = populateAsyncResponse(topicParamInQuery);
        final Map<String, Object> asyncResponseData = asyncResponse.getBody();

        final Object responseObject = networkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier,
                optionsParamInQuery,
                asyncResponseData == null ? NO_TOPIC : topicParamInQuery,
                asyncResponseData == null ? NO_REQUEST_ID : asyncResponseData.get(ASYNC_REQUEST_ID).toString());

        if (asyncResponseData == null) {
            return ResponseEntity.ok(responseObject);
        }
        return ResponseEntity.ok(asyncResponse);
    }

    @Override
    public ResponseEntity<Object> patchResourceDataRunningForCmHandle(final String resourceIdentifier,
        final String cmHandle,
        final Object requestBody, final String contentType) {
        final Object responseObject = networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
            resourceIdentifier, PATCH, jsonObjectMapper.asJsonString(requestBody), contentType);
        return ResponseEntity.ok(responseObject);
    }

    /**
     * Create resource data in datastore pass-through running for given cm-handle.
     *
     * @param resourceIdentifier resource identifier
     * @param cmHandle cm handle identifier
     * @param requestBody the request body
     * @param contentType content type of body
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Void> createResourceDataRunningForCmHandle(final String resourceIdentifier,
        final String cmHandle, final Object requestBody, final String contentType) {
        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier, CREATE, jsonObjectMapper.asJsonString(requestBody), contentType);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Update resource data in datastore pass-through running for given cm-handle.
     *
     * @param resourceIdentifier resource identifier
     * @param cmHandle cm handle identifier
     * @param requestBody the request body
     * @param contentType content type of the body
     * @return response entity
     */
    @Override
    public ResponseEntity<Object> updateResourceDataRunningForCmHandle(final String resourceIdentifier,
                                                                       final String cmHandle,
                                                                       final Object requestBody,
                                                                       final String contentType) {
        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier, UPDATE, jsonObjectMapper.asJsonString(requestBody), contentType);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    /**
     *  Delete resource data in datastore pass-through running for a given cm-handle.
     *
     * @param resourceIdentifier resource identifier
     * @param cmHandle cm handle identifier
     * @param contentType content type of the body
     * @return response entity no content if request is successful
     */
    @Override
    public ResponseEntity<Void> deleteResourceDataRunningForCmHandle(final String cmHandle,
                                                                     final String resourceIdentifier,
                                                                     final String contentType) {
        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
            resourceIdentifier, DELETE, NO_BODY, contentType);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryRestParameters the cm handle query parameters
     * @return collection of cm handles
     */
    @Override
    public ResponseEntity<List<RestOutputCmHandle>> executeCmHandleSearch(
            final CmHandleQueryRestParameters cmHandleQueryRestParameters) {
        final Set<NcmpServiceCmHandle> cmHandles = networkCmProxyDataService.queryCmHandles(
                jsonObjectMapper.convertToValueType(cmHandleQueryRestParameters, CmHandleQueryApiParameters.class));
        return ResponseEntity.ok(cmHandles.stream().map(this::toRestOutputCmHandle).collect(Collectors.toList()));
    }

    /**
     * Query and return cm handle ids that match the given query parameters.
     *
     * @param cmHandleQueryRestParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    @Override
    public ResponseEntity<List<String>> executeCmHandleIdSearch(
        final CmHandleQueryRestParameters cmHandleQueryRestParameters) {
        final Set<String> cmHandleIds = networkCmProxyDataService.queryCmHandleIds(
            jsonObjectMapper.convertToValueType(cmHandleQueryRestParameters, CmHandleQueryApiParameters.class));
        return ResponseEntity.ok(List.copyOf(cmHandleIds));
    }

    /**
     * Search for Cm Handle and Properties by Name.
     * @param cmHandleId cm-handle identifier
     * @return cm handle and its properties
     */
    @Override
    public ResponseEntity<RestOutputCmHandle> retrieveCmHandleDetailsById(final String cmHandleId) {
        final NcmpServiceCmHandle ncmpServiceCmHandle = networkCmProxyDataService.getNcmpServiceCmHandle(cmHandleId);
        final RestOutputCmHandle restOutputCmHandle = toRestOutputCmHandle(ncmpServiceCmHandle);
        return ResponseEntity.ok(restOutputCmHandle);
    }

    /**
     * Return module references for a cm handle.
     *
     * @param cmHandle the cm handle
     * @return module references for cm handle. Namespace will be always blank because restConf does not include this.
     */
    public ResponseEntity<List<RestModuleReference>> getModuleReferencesByCmHandle(final String cmHandle) {
        final List<RestModuleReference> restModuleReferences =
            networkCmProxyDataService.getYangResourcesModuleReferences(cmHandle).stream()
            .map(ncmpRestInputMapper::toRestModuleReference)
                .collect(Collectors.toList());
        return new ResponseEntity<>(restModuleReferences, HttpStatus.OK);
    }

    private RestOutputCmHandle toRestOutputCmHandle(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final RestOutputCmHandle restOutputCmHandle = new RestOutputCmHandle();
        final CmHandlePublicProperties cmHandlePublicProperties = new CmHandlePublicProperties();
        restOutputCmHandle.setCmHandle(ncmpServiceCmHandle.getCmHandleId());
        cmHandlePublicProperties.add(ncmpServiceCmHandle.getPublicProperties());
        restOutputCmHandle.setPublicCmHandleProperties(cmHandlePublicProperties);
        return restOutputCmHandle;
    }

    private ResponseEntity<Map<String, Object>> populateAsyncResponse(final String topicParamInQuery) {
        final boolean processAsynchronously = hasTopicParameter(topicParamInQuery);
        final Map<String, Object> responseData;
        if (processAsynchronously) {
            responseData = getAsyncResponseData();
        } else {
            responseData = null;
        }
        return ResponseEntity.ok().body(responseData);
    }

    private static boolean hasTopicParameter(final String topicName) {
        if (topicName == null) {
            return false;
        }
        if (CpsValidator.validateTopicName(topicName)) {
            return true;
        }
        throw new InvalidTopicException("Topic name " + topicName + " is invalid", "invalid topic");
    }

    private Map<String, Object> getAsyncResponseData() {
        final Map<String, Object> asyncResponseData = new HashMap<>(1);
        final String resourceDataRequestId = UUID.randomUUID().toString();
        asyncResponseData.put(ASYNC_REQUEST_ID, resourceDataRequestId);
        return asyncResponseData;
    }

}
