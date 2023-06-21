/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (c) 2022-2023 Nordix Foundation
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
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters;
import org.onap.cps.ncmp.rest.model.ResourceDataBatchRequest;
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
                final Optional<InputStream> optional = resourceProvider
                        .getResourceInputStream("passthrough-operational-example.json");
                if (optional.isPresent()) {
                    try (InputStream inputStream = optional.get()) {
                        final String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        final ObjectMapper mapper = new ObjectMapper();
                        responseObject = mapper.readValue(string, Object.class);
                    }
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
            final Optional<InputStream> optional = resourceProvider.getResourceInputStream("cmHandlesSearch.json");
            if (optional.isPresent()) {
                try (InputStream inputStream = optional.get()) {
                    final String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    final ObjectMapper mapper = new ObjectMapper();
                    final List<RestOutputCmHandle> restOutputCmHandles = Arrays
                            .asList(mapper.readValue(string, RestOutputCmHandle[].class));
                    return ResponseEntity.ok(restOutputCmHandles);
                }
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

    @Override
    public ResponseEntity<Void> createResourceDataRunningForCmHandle(@NotNull @Valid final String resourceIdentifier,
                                                                     final String datastoreName, final String cmHandle,
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
    public ResponseEntity<List<RestModuleDefinition>> getModuleDefinitionsByCmHandleId(final String cmHandle) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<List<RestModuleReference>> getModuleReferencesByCmHandle(final String cmHandle) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Object> getResourceDataForCmHandleBatch(@NotNull @Valid final String topic,
                                                                  @Valid final ResourceDataBatchRequest body) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Object> patchResourceDataRunningForCmHandle(@NotNull @Valid final String resourceIdentifier,
                                                                      final String datastoreName, final String cmHandle,
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
    public ResponseEntity<Object> updateResourceDataRunningForCmHandle(@NotNull @Valid final String resourceIdentifier,
                                                                       final String datastoreName,
                                                                       final String cmHandle, @Valid final Object body,
                                                                       final String contentType) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }
}
