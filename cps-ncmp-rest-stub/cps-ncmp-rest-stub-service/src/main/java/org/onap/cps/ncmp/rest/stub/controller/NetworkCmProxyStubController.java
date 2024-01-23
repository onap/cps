/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (c) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.rest.stub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters;
import org.onap.cps.ncmp.rest.model.DataOperationRequest;
import org.onap.cps.ncmp.rest.model.RestModuleDefinition;
import org.onap.cps.ncmp.rest.model.RestModuleReference;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandleCompositeState;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandlePublicProperties;
import org.onap.cps.ncmp.rest.stub.providers.ResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${rest.api.ncmp-stub-base-path}")
public class NetworkCmProxyStubController implements NetworkCmProxyApi {

    @Autowired
    private ResourceProvider resourceProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ASYNC_REQUEST_ID = "requestId";

    @Override
    public ResponseEntity<Object> getResourceDataForCmHandle(final String datastoreName, final String cmHandle,
                                                             final String resourceIdentifier,
                                                             final String optionsParamInQuery,
                                                             final String topicParamInQuery,
                                                             final Boolean includeDescendants) {
        if (DatastoreType.PASSTHROUGH_OPERATIONAL == DatastoreType.fromDatastoreName(datastoreName)) {
            final ResponseEntity<Map<String, Object>> asyncResponse = populateAsyncResponse(topicParamInQuery);
            final Map<String, Object> asyncResponseData = asyncResponse.getBody();
            Object responseObject = null;
            // read JSON file and map/convert to java POJO
            try {
                final Optional<Object> optionalResponseObject = getResponseObject(
                        "passthrough-operational-example.json", Object.class);
                if (optionalResponseObject.isPresent()) {
                    responseObject = optionalResponseObject.get();
                }

            } catch (final IOException ioException) {
                log.error("Error reading the file.", ioException);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            if (asyncResponseData == null) {
                return ResponseEntity.ok(responseObject);
            }
            return ResponseEntity.ok(asyncResponse);
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<RestOutputCmHandle>> searchCmHandles(
            final CmHandleQueryParameters cmHandleQueryParameters) {
        // read JSON file and map/convert to java POJO
        try {
            final Optional<RestOutputCmHandle[]> optionalResponseObject = getResponseObject("cmHandlesSearch.json",
                    RestOutputCmHandle[].class);
            if (optionalResponseObject.isPresent()) {
                final List<RestOutputCmHandle> restOutputCmHandles = Arrays.asList(optionalResponseObject.get());
                return ResponseEntity.ok(restOutputCmHandles);
            }
        } catch (final IOException ioException) {
            log.error("Error reading the file.", ioException);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(Collections.<RestOutputCmHandle>emptyList());
    }

    private ResponseEntity<Map<String, Object>> populateAsyncResponse(final String topicParamInQuery) {
        final Map<String, Object> responseData;
        if (topicParamInQuery == null) {
            responseData = null;
        } else {
            responseData = getAsyncResponseData();
        }
        return ResponseEntity.ok().body(responseData);
    }

    private Map<String, Object> getAsyncResponseData() {
        final Map<String, Object> asyncResponseData = new HashMap<>(1);
        final String resourceDataRequestId = UUID.randomUUID().toString();
        asyncResponseData.put(ASYNC_REQUEST_ID, resourceDataRequestId);
        return asyncResponseData;
    }

    private <T> Optional<T> getResponseObject(final String filename, final Class<T> type) throws IOException {
        final Optional<InputStream> optionalInputStream = resourceProvider.getResourceInputStream(filename);
        if (optionalInputStream.isPresent()) {
            final String content = new String(optionalInputStream.get().readAllBytes(), StandardCharsets.UTF_8);
            return Optional.of(objectMapper.readValue(content, type));
        }
        return Optional.empty();
    }

    @Override
    public ResponseEntity<Void> createResourceDataRunningForCmHandle(final String datastoreName, final String cmHandle,
                                                                     @NotNull @Valid final String resourceIdentifier, 
                                                                     @Valid final Object body, 
                                                                     final String contentType) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
    
    @Override
    public ResponseEntity<Void> deleteResourceDataRunningForCmHandle(final String datastoreName, final String cmHandle,
                                                                     @NotNull @Valid final String resourceIdentifier,
                                                                     final String contentType) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<RestOutputCmHandlePublicProperties> getCmHandlePublicPropertiesByCmHandleId(
            final String cmHandle) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<RestOutputCmHandleCompositeState> getCmHandleStateByCmHandleId(final String cmHandle) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<RestModuleDefinition>> getModuleDefinitions(final String cmHandleId,
                                                                           final String moduleName,
                                                                           final String revision) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<RestModuleReference>> getModuleReferencesByCmHandle(final String cmHandle) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Object> executeDataOperationForCmHandles(final String topicParamInQuery,
                                                                   final DataOperationRequest dataOperationRequest) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Object> patchResourceDataRunningForCmHandle(final String datastoreName, final String cmHandle,
                                                                      @NotNull @Valid final String resourceIdentifier, 
                                                                      @Valid final Object body, 
                                                                      final String contentType) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
    
    @Override
    public ResponseEntity<Object> queryResourceDataForCmHandle(final String datastoreName, final String cmHandle,
                                                               @Valid final String cpsPath, @Valid final String options,
                                                               @Valid final String topic,
                                                               @Valid final Boolean includeDescendants) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<RestOutputCmHandle> retrieveCmHandleDetailsById(final String cmHandle) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    @Override
    public ResponseEntity<List<String>> searchCmHandleIds(@Valid final CmHandleQueryParameters body) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    @Override
    public ResponseEntity<Object> setDataSyncEnabledFlagForCmHandle(final String cmHandle,
                                                                    @NotNull @Valid final Boolean dataSyncEnabled) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }

    @Override
    public ResponseEntity<Object> updateResourceDataRunningForCmHandle(final String datastoreName, 
                                                                       final String cmHandle, 
                                                                       @NotNull @Valid final String resourceIdentifier, 
                                                                       @Valid final Object body,
                                                                       final String contentType) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }
}
