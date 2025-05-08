/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.DmiWriteOperation;
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey;
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation;
import org.onap.cps.ncmp.impl.dmi.DmiServiceNameResolver;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WriteRequestExaminer {

    private static final String PATH_SEPARATOR = "/";

    private final AlternateIdMatcher alternateIdMatcher;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Splitting incoming data job write request into Dmi Write Operations by ProducerKey.
     *
     * @param dataJobId data job identifier
     * @param dataJobWriteRequest incoming data job write request
     * @return {@code Map} map of Dmi Write Operations per Producer Key
     */
    public Map<ProducerKey, List<DmiWriteOperation>> splitDmiWriteOperationsFromRequest(
            final String dataJobId, final DataJobWriteRequest dataJobWriteRequest) {
        final Map<String, YangModelCmHandle> cmHandlePerAlternateId = preloadCmHandles(dataJobWriteRequest);
        final Map<ProducerKey, List<DmiWriteOperation>> dmiWriteOperationsPerProducerKey = new HashMap<>();
        for (final WriteOperation writeOperation : dataJobWriteRequest.data()) {
            examineWriteOperation(dataJobId,
                                  dmiWriteOperationsPerProducerKey,
                                  cmHandlePerAlternateId,
                                  writeOperation);
        }
        return dmiWriteOperationsPerProducerKey;
    }

    private void examineWriteOperation(final String dataJobId,
                                       final Map<ProducerKey, List<DmiWriteOperation>> dmiWriteOperationsPerProducerKey,
                                       final Map<String, YangModelCmHandle> cmHandlePerAlternateId,
                                       final WriteOperation writeOperation) {
        log.debug("data job id for write operation is: {}", dataJobId);
        final YangModelCmHandle yangModelCmHandle = alternateIdMatcher
                .getCmHandleByLongestMatchingAlternateId(writeOperation.path(), PATH_SEPARATOR, cmHandlePerAlternateId);

        final DmiWriteOperation dmiWriteOperation = createDmiWriteOperation(writeOperation, yangModelCmHandle);

        final ProducerKey producerKey = createProducerKey(yangModelCmHandle);
        final List<DmiWriteOperation> dmiWriteOperations;
        if (dmiWriteOperationsPerProducerKey.containsKey(producerKey)) {
            dmiWriteOperations = dmiWriteOperationsPerProducerKey.get(producerKey);
        } else {
            dmiWriteOperations = new ArrayList<>();
            dmiWriteOperationsPerProducerKey.put(producerKey, dmiWriteOperations);
        }
        dmiWriteOperations.add(dmiWriteOperation);
    }

    private Map<String, YangModelCmHandle> preloadCmHandles(final DataJobWriteRequest dataJobWriteRequest) {
        final Collection<String> uniquePaths
            = dataJobWriteRequest.data().stream().map(operation -> operation.path()).collect(Collectors.toSet());
        final Collection<String> cmHandleIds
            = alternateIdMatcher.getCmHandleIdsByLongestMatchingAlternateIds(uniquePaths, PATH_SEPARATOR);
        final Collection<YangModelCmHandle> yangModelCmHandles
            = inventoryPersistence.getYangModelCmHandlesWithoutProperties(cmHandleIds);
        return yangModelCmHandles.stream()
            .collect(Collectors.toMap(YangModelCmHandle::getAlternateId, yangModelCmHandle -> yangModelCmHandle));
    }

    private ProducerKey createProducerKey(final YangModelCmHandle yangModelCmHandle) {
        final String dmiDataServiceName =
                DmiServiceNameResolver.resolveDmiServiceName(RequiredDmiService.DATA, yangModelCmHandle);
        return new ProducerKey(dmiDataServiceName, yangModelCmHandle.getDataProducerIdentifier());
    }

    private DmiWriteOperation createDmiWriteOperation(final WriteOperation writeOperation,
                                                      final YangModelCmHandle yangModelCmHandle) {
        return new DmiWriteOperation(
                writeOperation.path(),
                writeOperation.op(),
                yangModelCmHandle.getModuleSetTag(),
                writeOperation.value(),
                writeOperation.operationId());
    }

}
