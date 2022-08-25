/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.onap.cps.ncmp.rest.util.TopicValidator;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
@Slf4j
public abstract class NcmpDatastoreResourceRequestHandler {

    private static final String NO_REQUEST_ID = null;
    private static final String NO_TOPIC = null;

    protected final NetworkCmProxyDataService networkCmProxyDataService;
    protected final CpsNcmpTaskExecutor cpsNcmpTaskExecutor;
    protected final int timeOutInMilliSeconds;
    protected final boolean notificationFeatureEnabled;

    protected abstract Supplier<Object> getTask(final String cmHandle,
                                                final String resourceIdentifier,
                                                final String optionsParamInQuery,
                                                final String topicParamInQuery,
                                                final String requestId,
                                                final Boolean includeDescendant);


    /**
     * Is user wants async response boolean.
     *
     * @param topicParamInQuery the topic param in query
     * @return the boolean
     */
    protected Boolean isAsyncResponseRequested(final String topicParamInQuery) {
        return topicParamInQuery != null;
    }

    /**
     * Get resource data from datastore.
     *
     * @param cmHandleId          the cm handle
     * @param resourceIdentifier  the resource identifier
     * @param optionsParamInQuery the options param in query
     * @param topicParamInQuery   the topic param in query
     * @param includeDescendants  whether include descendants
     * @return the response entity
     */
    public ResponseEntity<Object> getResourceData(final String cmHandleId,
                                                  final String resourceIdentifier,
                                                  final String optionsParamInQuery,
                                                  final String topicParamInQuery,
                                                  final Boolean includeDescendants) {

        final String requestId = UUID.randomUUID().toString();

        if (isAsyncResponseRequested(topicParamInQuery) && notificationFeatureEnabled) {
            TopicValidator.validateTopicName(topicParamInQuery);
            cpsNcmpTaskExecutor.executeTask(
                    getTask(cmHandleId, resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId,
                            includeDescendants), timeOutInMilliSeconds);
            return ResponseEntity.ok(Map.of("requestId", requestId));
        }

        final Object responseObject =
                getTask(cmHandleId, resourceIdentifier, optionsParamInQuery, NO_TOPIC, NO_REQUEST_ID,
                        includeDescendants).get();

        return ResponseEntity.ok(responseObject);
    }
}
