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

package org.onap.cps.rest.controller;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.onap.cps.rest.model.AnchorDetails;
import org.onap.cps.rest.model.SchemaSetDetails;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.SchemaSet;

@Mapper(componentModel = "spring")
public interface CpsRestInputMapper {

    @Mapping(source = "extendedModuleReferences", target = "moduleReferences")
    SchemaSetDetails toSchemaSetDetails(final SchemaSet schemaSet);

    AnchorDetails toAnchorDetails(final Anchor anchor);

}
