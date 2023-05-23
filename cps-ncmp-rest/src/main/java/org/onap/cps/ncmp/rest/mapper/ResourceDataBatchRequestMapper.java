/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.onap.cps.ncmp.api.models.BatchOperationDefinition;
import org.onap.cps.ncmp.api.models.ResourceDataBatchRequest;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
public interface ResourceDataBatchRequestMapper {

    @Mapping(source = "operations", target = "batchOperationDefinitions")
    ResourceDataBatchRequest toResourceDataBatchRequest(
            org.onap.cps.ncmp.rest.model.ResourceDataBatchRequest resourceDataBatchRequest);

    @Mapping(source = "targetIds", target = "cmHandleIds")
    BatchOperationDefinition toBatchOperationDefinition(
            org.onap.cps.ncmp.rest.model.BatchOperationDefinition batchOperationDefinition);
}
