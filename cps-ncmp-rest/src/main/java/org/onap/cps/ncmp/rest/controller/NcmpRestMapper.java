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

package org.onap.cps.ncmp.rest.controller;

import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.rest.model.CmHandlePublicProperties;
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration;
import org.onap.cps.ncmp.rest.model.RestInputCmHandle;
import org.onap.cps.ncmp.rest.model.RestModuleReference;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandlePublicProperties;
import org.onap.cps.spi.model.ModuleReference;

@Mapper(componentModel = "spring")
public interface NcmpRestMapper {

    @Mapping(source = "createdCmHandles", target = "createdCmHandles",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    @Mapping(source = "updatedCmHandles", target = "updatedCmHandles",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    @Mapping(source = "removedCmHandles", target = "removedCmHandles",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    DmiPluginRegistration toDmiPluginRegistration(final RestDmiPluginRegistration restDmiPluginRegistration);

    @Mapping(source = "cmHandle", target = "cmHandleId")
    @Mapping(source = "cmHandleProperties", target = "dmiProperties")
    @Mapping(source = "publicCmHandleProperties", target = "publicProperties")
    NcmpServiceCmHandle toNcmpServiceCmHandle(final RestInputCmHandle restInputCmHandle);

    RestModuleReference toRestModuleReference(
        final ModuleReference moduleReference);

    @Mapping(source = "cmHandleId", target = "cmHandle")
    @Mapping(source = "publicProperties", target = "publicCmHandleProperties",
        qualifiedByName = "mapCmHandlePublicProperties")
    RestOutputCmHandlePublicProperties toRestOutputCmHandlePublicProperties(
        final NcmpServiceCmHandle ncmpServiceCmHandle);

    /**
     * Map public properties to CmHandlePublicProperties.
     *
     * @param publicProperties public properties.
     * @return a collection of cm handle public properties.
     */
    @Named("mapCmHandlePublicProperties")
    static CmHandlePublicProperties mapCmHandlePublicProperties(final Map<String, String> publicProperties) {
        final CmHandlePublicProperties cmHandlePublicProperties = new CmHandlePublicProperties();
        cmHandlePublicProperties.add(publicProperties);
        return cmHandlePublicProperties;

    }
}