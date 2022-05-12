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

package org.onap.cps.ncmp.rest.stub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.CmHandleQueryRestParameters;
import org.onap.cps.ncmp.rest.model.CmHandles;
import org.onap.cps.ncmp.rest.model.Conditions;
import org.onap.cps.ncmp.rest.model.RestModuleReference;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ncmp")
@RequiredArgsConstructor
public class NetworkCmProxyStubController implements NetworkCmProxyApi {

    public static final String ASYNC_REQUEST_ID = "requestId";

    @Value("${stub.path}")
    private String pathToResponseFiles;

    @Override
    public ResponseEntity<Void> createResourceDataRunningForCmHandle(@NotNull @Valid final String resourceIdentifier,
        final String cmHandle, @Valid final Object body, final String contentType) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteResourceDataRunningForCmHandle(final String cmHandle,
        @NotNull @Valid final String resourceIdentifier, final String contentType) {
        return null;
    }

    @Override
    public ResponseEntity<CmHandles> executeCmHandleSearch(@Valid final Conditions body) {
        // create Object Mapper
        final ObjectMapper mapper = new ObjectMapper();
        CmHandles cmHandles = new CmHandles();
        // read JSON file and map/convert to java POJO
        final ClassPathResource resource = new ClassPathResource(pathToResponseFiles + "cmHandlesSearch.json");
        try (InputStream inputStream = resource.getInputStream()) {
            final String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            cmHandles = mapper.readValue(string, CmHandles.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(cmHandles);
    }

    @Override
    public ResponseEntity<List<RestModuleReference>> getModuleReferencesByCmHandle(final String cmHandle) {
        return null;
    }

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
        final ObjectMapper mapper = new ObjectMapper();
        Object responseObject = null;
        // read JSON file and map/convert to java POJO
        final ClassPathResource resource = new ClassPathResource(pathToResponseFiles
            + "passthrough-operational-example.json");
        try (InputStream inputStream = resource.getInputStream()) {
            final String string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(string);
            responseObject = mapper.readValue(string, Object.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }


        if (asyncResponseData == null) {
            return ResponseEntity.ok(responseObject);
        }
        return ResponseEntity.ok(asyncResponse);

    }

    @Override
    public ResponseEntity<Object> getResourceDataRunningForCmHandle(final String cmHandle,
        @NotNull @Valid final String resourceIdentifier, @Valid final String options, @Valid final String topic) {
        return null;
    }

    @Override
    public ResponseEntity<Object> patchResourceDataRunningForCmHandle(@NotNull @Valid final String resourceIdentifier,
        final String cmHandle, @Valid final Object body, final String contentType) {
        return null;
    }

    @Override
    public ResponseEntity<List<String>> queryCmHandles(@Valid final CmHandleQueryRestParameters body) {
        return null;
    }

    @Override
    public ResponseEntity<RestOutputCmHandle> retrieveCmHandleDetailsById(final String cmHandle) {
        return null;
    }

    @Override
    public ResponseEntity<Object> updateResourceDataRunningForCmHandle(@NotNull @Valid final String resourceIdentifier,
        final String cmHandle, @Valid final Object body, final String contentType) {
        return null;
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
        throw new IllegalStateException("Topic name " + topicName + " is invalid");
    }

    private Map<String, Object> getAsyncResponseData() {
        final Map<String, Object> asyncResponseData = new HashMap<>(1);
        final String resourceDataRequestId = UUID.randomUUID().toString();
        asyncResponseData.put(ASYNC_REQUEST_ID, resourceDataRequestId);
        return asyncResponseData;
    }
}
