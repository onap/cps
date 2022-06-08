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

package org.onap.cps.ncmp.api.impl.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.model.DataNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class YangDataConverter {

    /**
     * This method convert yang model cm handle to ncmp service cm handle.
     * @param yangModelCmHandle the yang model of the cm handle
     * @return ncmp service cm handle
     */
    public static NcmpServiceCmHandle convertYangModelCmHandleToNcmpServiceCmHandle(
            final YangModelCmHandle yangModelCmHandle) {
        final NcmpServiceCmHandle ncmpServiceCmHandle = new NcmpServiceCmHandle();
        final List<YangModelCmHandle.Property> dmiProperties = yangModelCmHandle.getDmiProperties();
        final List<YangModelCmHandle.Property> publicProperties = yangModelCmHandle.getPublicProperties();
        ncmpServiceCmHandle.setCmHandleId(yangModelCmHandle.getId());
        ncmpServiceCmHandle.setCompositeState(yangModelCmHandle.getCompositeState());
        setDmiProperties(dmiProperties, ncmpServiceCmHandle);
        setPublicProperties(publicProperties, ncmpServiceCmHandle);
        return ncmpServiceCmHandle;
    }

    /**
     * This method convert yang model cm handle properties to simple map.
     * @param properties the yang model cm handle properties
     * @param propertiesMap the String, String map for the results
     */
    public static void asPropertiesMap(final List<YangModelCmHandle.Property> properties,
                                       final Map<String, String> propertiesMap) {
        for (final YangModelCmHandle.Property property : properties) {
            propertiesMap.put(property.getName(), property.getValue());
        }
    }

    /**
     * This method convert cm handle data node to yang model cm handle.
     * @param cmHandleDataNode the datanode of the cm handle
     * @param cmHandleId the id of the cm handle
     * @return yang model cm handle
     */
    public static YangModelCmHandle convertCmHandleToYangModel(final DataNode cmHandleDataNode,
                                                               final String cmHandleId) {
        final NcmpServiceCmHandle ncmpServiceCmHandle = new NcmpServiceCmHandle();
        ncmpServiceCmHandle.setCmHandleId(cmHandleId);
        populateCmHandleDetails(cmHandleDataNode, ncmpServiceCmHandle);
        return YangModelCmHandle.toYangModelCmHandle(
                (String) cmHandleDataNode.getLeaves().get("dmi-service-name"),
                (String) cmHandleDataNode.getLeaves().get("dmi-data-service-name"),
                (String) cmHandleDataNode.getLeaves().get("dmi-model-service-name"),
                ncmpServiceCmHandle
        );
    }

    private static void populateCmHandleDetails(final DataNode cmHandleDataNode,
                                                final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Map<String, String> dmiProperties = new LinkedHashMap<>();
        final Map<String, String> publicProperties = new LinkedHashMap<>();
        final CompositeStateBuilder compositeStateBuilder = new CompositeStateBuilder();
        CompositeState compositeState = compositeStateBuilder.build();
        for (final DataNode childDataNode: cmHandleDataNode.getChildDataNodes()) {
            if (childDataNode.getXpath().contains("/additional-properties[@name=")) {
                addProperty(childDataNode, dmiProperties);
            } else if (childDataNode.getXpath().contains("/public-properties[@name=")) {
                addProperty(childDataNode, publicProperties);
            } else if (childDataNode.getXpath().endsWith("/state")) {
                compositeState = compositeStateBuilder.fromDataNode(childDataNode).build();
            }
        }
        ncmpServiceCmHandle.setDmiProperties(dmiProperties);
        ncmpServiceCmHandle.setPublicProperties(publicProperties);
        ncmpServiceCmHandle.setCompositeState(compositeState);
    }

    private static void addProperty(final DataNode propertyDataNode, final Map<String, String> propertiesAsMap) {
        propertiesAsMap.put(String.valueOf(propertyDataNode.getLeaves().get("name")),
                String.valueOf(propertyDataNode.getLeaves().get("value")));
    }

    private static void setDmiProperties(final List<YangModelCmHandle.Property> dmiProperties,
                                  final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Map<String, String> dmiPropertiesMap = new LinkedHashMap<>(dmiProperties.size());
        asPropertiesMap(dmiProperties, dmiPropertiesMap);
        ncmpServiceCmHandle.setDmiProperties(dmiPropertiesMap);
    }

    private static void setPublicProperties(final List<YangModelCmHandle.Property> publicProperties,
                                     final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Map<String, String> publicPropertiesMap = new LinkedHashMap<>();
        asPropertiesMap(publicProperties, publicPropertiesMap);
        ncmpServiceCmHandle.setPublicProperties(publicPropertiesMap);
    }
}
