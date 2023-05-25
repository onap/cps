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

package org.onap.cps.ncmp.api.impl.operations;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.onap.cps.ncmp.api.models.BatchOperationDefinition;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
@JsonPropertyOrder({"operation", "operationId", "datastore", "options", "resourceIdentifier", "cmHandles"})
public class DmiBatchOperation {

    @JsonProperty("operation")
    private OperationType operationType;
    private String operationId;
    private String datastore;
    private String options;
    private String resourceIdentifier;

    private final List<CmHandle> cmHandles = new ArrayList<>();

    /**
     * Create and initialise a (outgoing) DMI batch operation.
     *
     * @param batchOperationDefinition batchOperationDefinition definition of incoming of batch request
     * @return mapped dmi operation details
     */
    public static DmiBatchOperation buildDmiBatchRequestBodyWithoutCmHandles(
            final BatchOperationDefinition batchOperationDefinition) {

        return DmiBatchOperation.builder()
                .operationType(OperationType.fromOperationName(batchOperationDefinition.getOperation()))
                .operationId(batchOperationDefinition.getOperationId())
                .datastore(DatastoreType.fromDatastoreName(batchOperationDefinition.getDatastore()).getDatastoreName())
                .options(batchOperationDefinition.getOptions())
                .resourceIdentifier(batchOperationDefinition.getResourceIdentifier())
                .build();
    }
}
