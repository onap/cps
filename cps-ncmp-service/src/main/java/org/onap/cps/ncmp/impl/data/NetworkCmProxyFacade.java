/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ncmp.impl.data;

import static org.onap.cps.ncmp.api.data.models.DatastoreType.OPERATIONAL;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.ncmp.api.data.models.CmResourceAddress;
import org.onap.cps.ncmp.api.data.models.DataOperationRequest;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NetworkCmProxyFacade {

    private final NcmpCachedResourceRequestHandler ncmpCachedResourceRequestHandler;
    private final NcmpPassthroughResourceRequestHandler ncmpPassthroughResourceRequestHandler;
    private final DmiDataOperations dmiDataOperations;

    /**
     * Fetches resource data for a given data store using DMI (Data Management Interface).
     * This method retrieves data based on the provided CmResourceAddress and additional query parameters.
     * It supports asynchronous processing and handles authorization if required.
     *
     * @param cmResourceAddress     The target data store, including the CM handle and resource identifier.
     *                              This parameter must not be null.
     * @param optionsParamInQuery   Additional query parameters that may influence the data retrieval process,
     *                              such as filters or limits. This parameter can be null.
     * @param topicParamInQuery     The topic name for triggering asynchronous responses. If specified,
     *                              the response will be sent to this topic. This parameter can be null.
     * @param includeDescendants    include (all) descendants or not
     * @param authorization         The contents of the Authorization header. This parameter can be null
     *                              if authorization is not required.
     * @return the result object, depends on use op topic. With topic a map object with request id is returned
     *         otherwise the result of the request.
     */
    public Object getResourceDataForCmHandle(final CmResourceAddress cmResourceAddress,
                                             final String optionsParamInQuery,
                                             final String topicParamInQuery,
                                             final Boolean includeDescendants,
                                             final String authorization) {

        final NcmpDatastoreRequestHandler ncmpDatastoreRequestHandler
            = getNcmpDatastoreRequestHandler(cmResourceAddress.getDatastoreName());
        return ncmpDatastoreRequestHandler.executeRequest(cmResourceAddress, optionsParamInQuery,
            topicParamInQuery, includeDescendants, authorization);
    }

    /**
     * Executes asynchronous request for group of cm handles to resource data.
     *
     * @param topic                    the topic param in query
     * @param dataOperationRequest     data operation request details for resource data
     * @param authorization            contents of Authorization header, or null if not present
     * @return a map with one entry of request Id for success or status and error when async feature is disabled
     */
    public Object executeDataOperationForCmHandles(final String topic,
                                                   final DataOperationRequest dataOperationRequest,
                                                   final String authorization) {
        return ncmpPassthroughResourceRequestHandler.executeAsynchronousRequest(topic,
                                                                                dataOperationRequest,
                                                                                authorization);
    }

    public Collection<DataNode> queryResourceDataForCmHandle(final String cmHandle,
                                                             final String cpsPath,
                                                             final Boolean includeDescendants) {
        return ncmpCachedResourceRequestHandler.executeRequest(cmHandle, cpsPath, includeDescendants);
    }

    /**
     * Write resource data for data store pass-through running using dmi for given cm-handle.
     *
     * @param cmHandleReference         cm handle or alternate identifier
     * @param resourceIdentifier resource identifier
     * @param operationType      required operation type
     * @param requestData        request body to create resource
     * @param dataType        content type in body
     * @param authorization       contents of Authorization header, or null if not present
     * @return {@code Object} return data
     */
    public Object writeResourceDataPassThroughRunningForCmHandle(final String cmHandleReference,
                                                                 final String resourceIdentifier,
                                                                 final OperationType operationType,
                                                                 final String requestData,
                                                                 final String dataType,
                                                                 final String authorization) {
        return dmiDataOperations.writeResourceDataPassThroughRunningFromDmi(cmHandleReference, resourceIdentifier,
            operationType, requestData, dataType, authorization);
    }

    private NcmpDatastoreRequestHandler getNcmpDatastoreRequestHandler(final String datastoreName) {
        if (OPERATIONAL.equals(DatastoreType.fromDatastoreName(datastoreName))) {
            return ncmpCachedResourceRequestHandler;
        }
        return ncmpPassthroughResourceRequestHandler;
    }

}
