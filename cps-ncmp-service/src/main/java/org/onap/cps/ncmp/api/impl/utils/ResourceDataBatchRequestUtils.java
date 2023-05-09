/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.api.impl.operations.DmiBatchRequestBody;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.models.BatchOperationDetail;
import org.onap.cps.ncmp.api.models.ResourceDataBatchRequest;

@Slf4j
public class ResourceDataBatchRequestUtils {

    /**
     * Filter and collects all distinct cmHandle ids.
     *
     * @param resourceDataBatchRequest batchF request for resource data
     * @return {@code Set<String>} set of cm handle ids
     */
    public static Set<String> extractAllDistinctCmHandleIds(final ResourceDataBatchRequest resourceDataBatchRequest) {
        return resourceDataBatchRequest.getOperations().stream()
                .filter(batchOperationDetail ->
                        !batchOperationDetail.getCmHandleIds().isEmpty())
                .flatMap(batchOperationDetail ->
                        batchOperationDetail.getCmHandleIds().stream()).collect(Collectors.toSet());
    }

    /**
     * populate batch request (DmiBatchRequestBody) and group it for dmi service.
     *
     * @param resourceDataBatchRequest            batch request details for resource data
     * @param dmiServiceNameCmHandlePropertiesMap map of dmi service name with cm handle having its properties
     * @return {@code Map<String, List<DmiBatchRequestBody>>} dmi service url having batch request body details
     */
    public static Map<String, List<DmiBatchRequestBody>> populateGroupsOutPerDmiServiceName(
            final ResourceDataBatchRequest resourceDataBatchRequest,
            final Map<String, Map<String, Map<String, String>>> dmiServiceNameCmHandlePropertiesMap) {
        final Map<String, List<DmiBatchRequestBody>> groupsOutPerDmiServiceName = new HashMap<>();
        resourceDataBatchRequest.getOperations().forEach(batchOperationDetail -> {
            final DmiBatchRequestBody.DmiBatchRequestBodyBuilder dmiBatchRequestBodyBuilder
                    = getDmiBatchRequestBodyBuilder(batchOperationDetail);
            final List<String> nonEmptyCmHandleIds = batchOperationDetail.getCmHandleIds().stream()
                    .filter(cmHandleId -> !cmHandleId.isBlank()).collect(Collectors.toUnmodifiableList());
            searchCmHandlePropertiesAndSetDmiBatchRequestBody(groupsOutPerDmiServiceName,
                    dmiServiceNameCmHandlePropertiesMap, nonEmptyCmHandleIds, dmiBatchRequestBodyBuilder);
        });
        return groupsOutPerDmiServiceName;
    }

    private static void searchCmHandlePropertiesAndSetDmiBatchRequestBody(final Map<String, List<DmiBatchRequestBody>>
                                                                                  groupsOutPerDmiServiceName,
                                                                          final Map<String,
                                                                                  Map<String, Map<String, String>>>
                                                                                  dmiServiceNameCmHandlePropertiesMap,
                                                                          final List<String> nonEmptyCmHandleIds,
                                                                          final DmiBatchRequestBody
                                                                                  .DmiBatchRequestBodyBuilder
                                                                                  dmiBatchRequestBodyBuilder) {
        nonEmptyCmHandleIds.forEach(requestedCmHandleId ->
                dmiServiceNameCmHandlePropertiesMap.entrySet().forEach(dmiServiceNameCmHandlePropertyEntry -> {
                    final String dmiServiceName = dmiServiceNameCmHandlePropertyEntry.getKey();
                    final Map<String, Map<String, String>> cmHandleIdWithProperties
                            = dmiServiceNameCmHandlePropertyEntry.getValue();
                    searchCmHandlePropertiesAndSetDmiBatchRequestBody(groupsOutPerDmiServiceName, nonEmptyCmHandleIds,
                            dmiBatchRequestBodyBuilder, requestedCmHandleId, dmiServiceName, cmHandleIdWithProperties);
                }));
    }

    private static void searchCmHandlePropertiesAndSetDmiBatchRequestBody(final Map<String, List<DmiBatchRequestBody>>
                                                                                  groupsOutPerDmiServiceName,
                                                                          final List<String> nonEmptyCmHandleIds,
                                                                          final DmiBatchRequestBody
                                                                                  .DmiBatchRequestBodyBuilder
                                                                                  dmiBatchRequestBodyBuilder,
                                                                          final String requestedCmHandleId,
                                                                          final String dmiServiceName,
                                                                          final Map<String, Map<String, String>>
                                                                                  cmHandleIdWithProperties) {
        if (groupsOutPerDmiServiceName.containsKey(dmiServiceName)) {
            if (cmHandleIdWithProperties.containsKey(requestedCmHandleId)) {
                setDmiBatchRequestBodyPerServiceName(groupsOutPerDmiServiceName, nonEmptyCmHandleIds,
                        dmiBatchRequestBodyBuilder, requestedCmHandleId, dmiServiceName,
                        cmHandleIdWithProperties.get(requestedCmHandleId));
            } else {
                // TODO Need to publish an error response to client given topic.
                //  Code should be implemented into https://jira.onap.org/browse/CPS-1583 (
                //  NCMP : Handle non-existing cm handles)
                log.warn("cm handle {} not found", requestedCmHandleId);
            }
        } else {
            if (cmHandleIdWithProperties.containsKey(requestedCmHandleId)) {
                setCmHandleProperties(dmiBatchRequestBodyBuilder, requestedCmHandleId,
                        cmHandleIdWithProperties.get(requestedCmHandleId));
                setDmiBatchRequestBodyPerServiceName(groupsOutPerDmiServiceName, dmiBatchRequestBodyBuilder,
                        dmiServiceName);
            } else {
                // TODO Need to publish an error response to client given topic.
                //  Code should be implemented into https://jira.onap.org/browse/CPS-1583 (
                //  NCMP : Handle non-existing cm handles)
                log.warn("cm handle {} not found", requestedCmHandleId);
            }
        }
    }

    private static void setDmiBatchRequestBodyPerServiceName(final Map<String, List<DmiBatchRequestBody>>
                                                                     groupsOutPerDmiServiceName,
                                                             final List<String> nonEmptyCmHandleIds,
                                                             final DmiBatchRequestBody.DmiBatchRequestBodyBuilder
                                                                     dmiBatchRequestBodyBuilder,
                                                             final String requestedCmHandleId,
                                                             final String dmiServiceName,
                                                             final Map<String, String> cmHandleProperties) {
        groupsOutPerDmiServiceName.get(dmiServiceName).stream()
                .filter(dmiBatchRequestBody ->
                        isAnyCmHandleIdAlreadyAddedIntoDmiBatchRequestBody(nonEmptyCmHandleIds, dmiBatchRequestBody))
                .findAny()
                .ifPresentOrElse(dmiBatchRequestBody ->
                                dmiBatchRequestBody.getDmiProperties().put(requestedCmHandleId, cmHandleProperties),
                        () -> {
                            setCmHandleProperties(dmiBatchRequestBodyBuilder, requestedCmHandleId, cmHandleProperties);
                            groupsOutPerDmiServiceName.get(dmiServiceName).add(dmiBatchRequestBodyBuilder.build());
                        });
    }

    private static void setDmiBatchRequestBodyPerServiceName(final Map<String, List<DmiBatchRequestBody>>
                                                                     groupsOutPerDmiServiceName,
                                                             final DmiBatchRequestBody.DmiBatchRequestBodyBuilder
                                                                     dmiBatchRequestBodyBuilder,
                                                             final String dmiServiceName) {
        final List<DmiBatchRequestBody> dmiBatchRequestBodies = new ArrayList<>();
        dmiBatchRequestBodies.add(dmiBatchRequestBodyBuilder.build());
        groupsOutPerDmiServiceName.put(dmiServiceName, dmiBatchRequestBodies);
    }

    private static void setCmHandleProperties(final DmiBatchRequestBody.DmiBatchRequestBodyBuilder
                                                      dmiBatchRequestBodyBuilder,
                                              final String requestedCmHandleId,
                                              final Map<String, String> cmHandleProperties) {
        final Map<String, Map<String, String>> cmHandleIdWithProperties = new HashMap<>();
        cmHandleIdWithProperties.put(requestedCmHandleId, cmHandleProperties);
        dmiBatchRequestBodyBuilder.dmiProperties(cmHandleIdWithProperties);
    }

    private static boolean isAnyCmHandleIdAlreadyAddedIntoDmiBatchRequestBody(final List<String> nonEmptyCmHandleIds,
                                                                              final DmiBatchRequestBody
                                                                                      dmiBatchRequestBody) {
        return dmiBatchRequestBody.getDmiProperties().keySet().stream().anyMatch(nonEmptyCmHandleIds::contains);
    }

    private static DmiBatchRequestBody.DmiBatchRequestBodyBuilder getDmiBatchRequestBodyBuilder(
            final BatchOperationDetail
                    batchOperationDetail) {
        return DmiBatchRequestBody.builder().operationType(OperationType.fromOperationName(
                        batchOperationDetail.getOperation()))
                .operationId(batchOperationDetail.getOperationId())
                .datastore(DatastoreType.fromDatastoreName(batchOperationDetail.getDatastore()).getDatastoreName())
                .options(batchOperationDetail.getOptions())
                .resourceIdentifier(batchOperationDetail.getResourceIdentifier());
    }
}
