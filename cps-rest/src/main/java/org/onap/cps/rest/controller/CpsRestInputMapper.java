/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

package org.onap.cps.rest.controller;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.Dataspace;
import org.onap.cps.api.model.SchemaSet;
import org.onap.cps.rest.model.AnchorDetails;
import org.onap.cps.rest.model.DataspaceDetails;
import org.onap.cps.rest.model.SchemaSetDetails;

@Mapper(componentModel = "spring")
public interface CpsRestInputMapper {

    @Mapping(source = "moduleReferences", target = "moduleReferences",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    SchemaSetDetails toSchemaSetDetails(final SchemaSet schemaSet);

    AnchorDetails toAnchorDetails(final Anchor anchor);

    DataspaceDetails toDataspaceDetails(final Dataspace dataspace);
}
