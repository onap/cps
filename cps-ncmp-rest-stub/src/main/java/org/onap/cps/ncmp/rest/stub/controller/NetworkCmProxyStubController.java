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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.onap.cps.ncmp.rest.stub.handlers.NetworkCmProxyApiStubDefaultImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${rest.api.ncmp-stub-base-path}")
public class NetworkCmProxyStubController implements NetworkCmProxyApiStubDefaultImpl {

    @Value("${stub.path}")
    private String pathToResponseFiles;

    @Override
    public ResponseEntity<Object> getResourceDataForCmHandle(final String dataStoreName,
                                                             final String cmHandle,
                                                             final String resourceIdentifier,
                                                             final String optionsParamInQuery,
                                                             final String topicParamInQuery,
                                                             final Boolean includeDescendants) {
        if (DatastoreType.PASSTHROUGH_OPERATIONAL == DatastoreType.fromDatastoreName(dataStoreName)) {
            final ResponseEntity<Map<String, Object>> asyncResponse = populateAsyncResponse(topicParamInQuery);
            final Map<String, Object> asyncResponseData = asyncResponse.getBody();
            Object responseObject = null;
            // read JSON file and map/convert to java POJO
            final ClassPathResource resource =
                    new ClassPathResource(pathToResponseFiles + "passthrough-operational-example.json");
            try (InputStream inputStream = resource.getInputStream()) {
                final String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                final ObjectMapper mapper = new ObjectMapper();
                responseObject = mapper.readValue(string, Object.class);
            } catch (final IOException exception) {
                log.error("Error reading the file.", exception);
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
        List<RestOutputCmHandle> restOutputCmHandles = null;
        // read JSON file and map/convert to java POJO
        final ClassPathResource resource = new ClassPathResource(pathToResponseFiles + "cmHandlesSearch.json");
        try (InputStream inputStream = resource.getInputStream()) {
            final String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            final ObjectMapper mapper = new ObjectMapper();
            restOutputCmHandles = Arrays.asList(mapper.readValue(string, RestOutputCmHandle[].class));
        } catch (final IOException exception) {
            log.error("Error reading the file.", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(restOutputCmHandles);
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
}
