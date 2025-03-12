/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.DmiWriteOperation;
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey;
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.dmi.DmiServiceNameResolver;
import org.onap.cps.ncmp.impl.inventory.ParameterizedCmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WriteRequestExaminer {

    private final AlternateIdMatcher alternateIdMatcher;
    private final ParameterizedCmHandleQueryService parameterizedCmHandleQueryService;
    private static final String PATH_SEPARATOR = "/";

    /**
     * Splitting incoming data job write request into Dmi Write Operations by ProducerKey.
     *
     * @param dataJobId data job identifier
     * @param dataJobWriteRequest incoming data job write request
     * @return {@code Map} map of Dmi Write Operations per Producer Key
     */
    public Map<ProducerKey, List<DmiWriteOperation>> splitDmiWriteOperationsFromRequest(
            final String dataJobId, final DataJobWriteRequest dataJobWriteRequest) {
        final Map<ProducerKey, List<DmiWriteOperation>> dmiWriteOperationsPerProducerKey = new HashMap<>();
        final Map<String, NcmpServiceCmHandle> cmHandlePerAlternateId = getAllCmHandlesByAlternateId();
        for (final WriteOperation writeOperation : dataJobWriteRequest.data()) {
            examineWriteOperation(dataJobId, dmiWriteOperationsPerProducerKey, writeOperation, cmHandlePerAlternateId);
        }
        return dmiWriteOperationsPerProducerKey;
    }

    private Map<String, NcmpServiceCmHandle> getAllCmHandlesByAlternateId() {
        final Collection<NcmpServiceCmHandle> allCmHandles
                = parameterizedCmHandleQueryService.getAllCmHandlesWithoutProperties();
        return allCmHandles.stream()
                .collect(Collectors.toMap(NcmpServiceCmHandle::getAlternateId, UnaryOperator.identity()));
    }

    private void examineWriteOperation(final String dataJobId,
                                       final Map<ProducerKey, List<DmiWriteOperation>> dmiWriteOperationsPerProducerKey,
                                       final WriteOperation writeOperation,
                                       final Map<String, NcmpServiceCmHandle> cmHandlePerAlternateId) {
        log.debug("data job id for write operation is: {}", dataJobId);
        final NcmpServiceCmHandle ncmpServiceCmHandle = alternateIdMatcher
                .getCmHandleByLongestMatchingAlternateId(writeOperation.path(), PATH_SEPARATOR, cmHandlePerAlternateId);

        final DmiWriteOperation dmiWriteOperation = createDmiWriteOperation(writeOperation, ncmpServiceCmHandle);

        final ProducerKey producerKey = createProducerKey(ncmpServiceCmHandle);
        final List<DmiWriteOperation> dmiWriteOperations;
        if (dmiWriteOperationsPerProducerKey.containsKey(producerKey)) {
            dmiWriteOperations = dmiWriteOperationsPerProducerKey.get(producerKey);
        } else {
            dmiWriteOperations = new ArrayList<>();
            dmiWriteOperationsPerProducerKey.put(producerKey, dmiWriteOperations);
        }
        dmiWriteOperations.add(dmiWriteOperation);
    }

    private ProducerKey createProducerKey(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final String dmiDataServiceName =
                DmiServiceNameResolver.resolveDmiServiceName(RequiredDmiService.DATA, ncmpServiceCmHandle);
        return new ProducerKey(dmiDataServiceName, ncmpServiceCmHandle.getDataProducerIdentifier());
    }

    private DmiWriteOperation createDmiWriteOperation(final WriteOperation writeOperation,
                                                      final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return new DmiWriteOperation(
                writeOperation.path(),
                writeOperation.op(),
                ncmpServiceCmHandle.getModuleSetTag(),
                writeOperation.value(),
                writeOperation.operationId(),
                ncmpServiceCmHandle.getDmiProperties());
    }

    private Map<String, String> getPrivatePropertiesFromDataNode(final YangModelCmHandle yangModelCmHandle) {
        final Map<String, String> cmHandleDmiProperties = new LinkedHashMap<>();
        yangModelCmHandle.getDmiProperties()
                .forEach(dmiProperty -> cmHandleDmiProperties.put(dmiProperty.getName(), dmiProperty.getValue()));
        return cmHandleDmiProperties;
    }

}
