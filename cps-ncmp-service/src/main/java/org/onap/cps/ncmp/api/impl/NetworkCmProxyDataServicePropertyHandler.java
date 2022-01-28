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

package org.onap.cps.ncmp.api.impl;

import static org.onap.cps.ncmp.api.impl.NetworkCmProxyDataServiceImpl.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.NetworkCmProxyDataServiceImpl.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.api.impl.NetworkCmProxyDataServiceImpl.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.api.impl.NetworkCmProxyDataServiceImpl.NO_TIMESTAMP;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyDataServicePropertyHandler {

    private static final String CM_HANDLE_XPATH_TEMPLATE = NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='%s']";
    private static final String PATH_LIMITER = "/";
    private static final String DMI_PROPERTY_XPATH_PREFIX = PATH_LIMITER + "additional-properties";
    private static final String PUBLIC_PROPERTY_XPATH_PREFIX = PATH_LIMITER + "public-properties";
    private static final Pattern LIST_INDEX_PATTERN = Pattern.compile(".*\\[@(\\w+)[^\\/]'([^']*)'*]");

    private final CpsDataService cpsDataService;

    /**
     * Iterates over incoming cmHandles and update the dataNodes based on the updated attributes.
     * The attributes which are not passed will remain as is.
     *
     * @param cmHandles collection of cmHandles
     */
    public void updateDataNodeLeaves(final Collection<CmHandle> cmHandles) throws DataNodeNotFoundException {
        for (final CmHandle cmHandle : cmHandles) {
            try {
                final String cmHandleXpath = String.format(CM_HANDLE_XPATH_TEMPLATE, cmHandle.getCmHandleID());
                final DataNode existingCmHandleDataNode =
                        cpsDataService.getDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cmHandleXpath,
                                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
                processUpdates(existingCmHandleDataNode, cmHandle);
            } catch (final DataNodeNotFoundException e) {
                log.error("Unable to find dataNode for cmHandleId {} with cause {}", cmHandle.getCmHandleID(),
                        e.getMessage());
                throw e;
            }
        }
    }

    private void processUpdates(final DataNode existingCmHandleDataNode, final CmHandle incomingCmHandle) {
        if (!incomingCmHandle.getPublicProperties().isEmpty()) {
            updateProperties(existingCmHandleDataNode, PUBLIC_PROPERTY_XPATH_PREFIX,
                    incomingCmHandle.getPublicProperties());
        }
        if (!incomingCmHandle.getDmiProperties().isEmpty()) {
            updateProperties(existingCmHandleDataNode, DMI_PROPERTY_XPATH_PREFIX, incomingCmHandle.getDmiProperties());
        }
    }

    private void updateProperties(final DataNode existingCmHandleDataNode, final String targetXpathPropertyPrefix,
            final Map<String, String> incomingProperties) {
        final Collection<DataNode> replacementPropertyDataNodes = new ArrayList<>();
        keepExistingProperties(existingCmHandleDataNode, targetXpathPropertyPrefix, incomingProperties,
                replacementPropertyDataNodes);
        addIncomingProperties(existingCmHandleDataNode, targetXpathPropertyPrefix, incomingProperties,
                replacementPropertyDataNodes);
        cpsDataService.replaceListContent(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                existingCmHandleDataNode.getXpath(), replacementPropertyDataNodes, NO_TIMESTAMP);
    }

    private void keepExistingProperties(final DataNode existingCmHandleDataNode, final String targetXpathPropertyPrefix,
            final Map<String, String> incomingProperties, final Collection<DataNode> replacementPropertyDataNodes) {
        final Collection<DataNode> existingPropertyDataNodes =
                getPropertyDataNodes(existingCmHandleDataNode, targetXpathPropertyPrefix);
        for (final DataNode existingPropertyDataNode : existingPropertyDataNodes) {
            final Matcher matcher = LIST_INDEX_PATTERN.matcher(existingPropertyDataNode.getXpath());
            final String keyName = matcher.find() ? matcher.group(2) : null;
            if (keyName != null && !incomingProperties.containsKey(keyName)) {
                replacementPropertyDataNodes.add(existingPropertyDataNode);
            }
        }
    }

    private void addIncomingProperties(final DataNode existingCmHandleDataNode, final String targetXpathPropertyPrefix,
            final Map<String, String> incomingProperties, final Collection<DataNode> replacementPropertyDataNodes) {
        incomingProperties.forEach((updatedAttributeKey, updatedAttributeValue) -> {
            final String propertyXpath =
                    getAttributeXpath(existingCmHandleDataNode, targetXpathPropertyPrefix, updatedAttributeKey);
            if (updatedAttributeValue != null) {
                log.info("Creating a new DataNode with xpath {} , key : {} and value : {}", propertyXpath,
                        updatedAttributeKey, updatedAttributeValue);
                replacementPropertyDataNodes.add(
                        buildDataNode(propertyXpath, updatedAttributeKey, updatedAttributeValue));
            }
        });
    }

    private Collection<DataNode> getPropertyDataNodes(final DataNode existingDataNode,
            final String propertyXpathPrefix) {
        return existingDataNode.getChildDataNodes().stream()
                .filter(childNode -> childNode.getXpath().contains(propertyXpathPrefix)).collect(Collectors.toList());
    }

    private String getAttributeXpath(final DataNode cmHandle, final String targetXpathPropertyPrefix,
            final String attributeKey) {
        return cmHandle.getXpath() + targetXpathPropertyPrefix + String.format("[@name='%s']", attributeKey);
    }

    private DataNode buildDataNode(final String xpath, final String attributeKey, final String attributeValue) {
        final Map<String, String> updatedLeaves = new LinkedHashMap<>(1);
        updatedLeaves.put("name", attributeKey);
        updatedLeaves.put("value", attributeValue);
        log.debug("Building a new node with xpath {} with leaves (name : {} , value : {})", xpath, attributeKey,
                attributeValue);
        return new DataNodeBuilder().withXpath(xpath).withLeaves(ImmutableMap.copyOf(updatedLeaves)).build();
    }
}
