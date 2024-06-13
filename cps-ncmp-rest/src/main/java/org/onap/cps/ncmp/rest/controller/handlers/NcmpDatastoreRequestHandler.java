/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.rest.controller.handlers;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.CmResourceAddress;
import org.onap.cps.ncmp.rest.util.TopicValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class NcmpDatastoreRequestHandler {

    private static final String NO_REQUEST_ID = null;
    private static final String NO_TOPIC = null;

    @Value("${notification.async.executor.time-out-value-in-ms:60000}")
    protected int timeOutInMilliSeconds;
    @Value("${notification.enabled:true}")
    protected boolean notificationFeatureEnabled;

    /**
     * Executes synchronous/asynchronous get request for given cm handle.
     *
     * @param cmResourceAddress   the name of the datastore, cm handle and resource identifier
     * @param optionsParamInQuery the options param in query
     * @param topicParamInQuery   the topic param in query
     * @param includeDescendants  whether include descendants
     * @param authorization       contents of Authorization header, or null if not present
     * @return the response entity
     */
    public ResponseEntity<Object> executeRequest(final CmResourceAddress cmResourceAddress,
                                                 final String optionsParamInQuery,
                                                 final String topicParamInQuery,
                                                 final boolean includeDescendants,
                                                 final String authorization) {

        final boolean asyncResponseRequested = topicParamInQuery != null;
        if (asyncResponseRequested && notificationFeatureEnabled) {
            return fetchResourceDataAsynchronously(cmResourceAddress, optionsParamInQuery, topicParamInQuery,
                includeDescendants, authorization);
        }

        if (asyncResponseRequested) {
            log.warn("Asynchronous request is unavailable as notification feature is currently disabled, "
                    + "will use synchronous operation.");
        }
        final Mono<Object> resourceDataMono = getResourceDataForCmHandle(cmResourceAddress, optionsParamInQuery,
                NO_TOPIC, NO_REQUEST_ID, includeDescendants, authorization);
        return fetchResourceDataSynchronously(resourceDataMono);
    }

    private ResponseEntity<Object> fetchResourceDataSynchronously(final Mono<Object> resourceDataMono) {
        return ResponseEntity.ok(resourceDataMono.block());
    }

    private ResponseEntity<Object> fetchResourceDataAsynchronously(final CmResourceAddress cmResourceAddress,
                                                                   final String optionsParamInQuery,
                                                                   final String topicParamInQuery,
                                                                   final boolean includeDescendants,
                                                                   final String authorization) {
        TopicValidator.validateTopicName(topicParamInQuery);
        final String requestId = UUID.randomUUID().toString();
        getResourceDataForCmHandle(cmResourceAddress, optionsParamInQuery, topicParamInQuery, requestId,
                includeDescendants, authorization)
                .doOnSuccess(result -> log.debug("Async operation succeeded for request id {}: {}", requestId, result))
                .doOnError(error ->
                        log.error("Async operation failed for request id {}: {}", requestId, error.getMessage()))
                .subscribe();
        log.debug("Received Async request with id {}", requestId);
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    protected abstract Mono<Object> getResourceDataForCmHandle(final CmResourceAddress cmResourceAddress,
                                                               final String optionsParamInQuery,
                                                               final String topicParamInQuery,
                                                               final String requestId,
                                                               final boolean includeDescendant,
                                                               final String authorization);
}
