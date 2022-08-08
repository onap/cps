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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.rest.controller.utils.NcmpDatastoreHandlerUtils;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
@Slf4j
public abstract class NcmpDatastoreHandler {

    protected final NetworkCmProxyDataService networkCmProxyDataService;
    protected final CpsNcmpTaskExecutor cpsNcmpTaskExecutor;

    protected final int timeOutInMilliSeconds;

    protected final boolean asyncEnabled;

    /**
     * Generate suppliers for handle method.
     *
     * @param cmHandle            cm handle identifier
     * @param resourceIdentifier  resource identifier
     * @param optionsParamInQuery options query parameter
     * @param topicParamInQuery   topic query parameter
     * @param requestId           the request id parameter
     * @return {@code List<Supplier<Object>} the list of suppliers will be used
     */
    public abstract List<Supplier<Object>> generateSuppliers(final String cmHandle,
                                                             final String resourceIdentifier,
                                                             final String optionsParamInQuery,
                                                             final String topicParamInQuery,
                                                             final String requestId);

    /**
     * Get resource data from datastore.
     *
     * @param cmHandle            the cm handle
     * @param resourceIdentifier  the resource identifier
     * @param optionsParamInQuery the options param in query
     * @param topicParamInQuery   the topic param in query
     * @param includeDescendants  whether include descendants
     * @return the response entity
     */
    public ResponseEntity<Object> handle(final String cmHandle,
                                         final String resourceIdentifier,
                                         final String optionsParamInQuery,
                                         final String topicParamInQuery,
                                         final Boolean includeDescendants) {

        final String requestId = UUID.randomUUID().toString();
        final List<Supplier<Object>> suppliers = generateSuppliers(cmHandle, resourceIdentifier,
                optionsParamInQuery, topicParamInQuery, requestId);

        if (asyncEnabled && NcmpDatastoreHandlerUtils.isValidTopic(topicParamInQuery)) {
            log.info("Received Async passthrough-operational request with id {}", requestId);
            cpsNcmpTaskExecutor.executeTask(() ->
                    suppliers.get(0), timeOutInMilliSeconds
            );
            return ResponseEntity.ok(Map.of("requestId", requestId));
        } else {
            log.warn("Asynchronous messaging is currently disabled for passthrough-operational."
                    + " Will use synchronous operation.");
        }

        final Object responseObject = suppliers.get(1).get();

        return ResponseEntity.ok(responseObject);
    }
}
