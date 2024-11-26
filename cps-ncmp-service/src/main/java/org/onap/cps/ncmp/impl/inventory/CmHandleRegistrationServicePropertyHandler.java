/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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

package org.onap.cps.ncmp.impl.inventory;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.ALTERNATE_ID_ALREADY_ASSOCIATED;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_FOUND;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLE_INVALID_ID;
import static org.onap.cps.ncmp.impl.inventory.CmHandleRegistrationServicePropertyHandler.PropertyType.DMI_PROPERTY;
import static org.onap.cps.ncmp.impl.inventory.CmHandleRegistrationServicePropertyHandler.PropertyType.PUBLIC_PROPERTY;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;

import com.google.common.collect.ImmutableMap;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.spi.api.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.api.exceptions.DataValidationException;
import org.onap.cps.spi.api.model.DataNode;
import org.onap.cps.spi.api.model.DataNodeBuilder;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
//Accepting the security hotspot as the string checked is generated from inside code and not user input.
@SuppressWarnings("squid:S5852")
public class CmHandleRegistrationServicePropertyHandler {

    private final InventoryPersistence inventoryPersistence;
    private final CpsDataService cpsDataService;
    private final JsonObjectMapper jsonObjectMapper;
    private final AlternateIdChecker alternateIdChecker;

    /**
     * Iterates over incoming updatedNcmpServiceCmHandles and update the dataNodes based on the updated attributes.
     * The attributes which are not passed will remain as is.
     *
     * @param updatedNcmpServiceCmHandles collection of CmHandles
     */
    public List<CmHandleRegistrationResponse> updateCmHandleProperties(
            final Collection<NcmpServiceCmHandle> updatedNcmpServiceCmHandles) {
        final Collection<String> rejectedCmHandleIds = alternateIdChecker
            .getIdsOfCmHandlesWithRejectedAlternateId(updatedNcmpServiceCmHandles, AlternateIdChecker.Operation.UPDATE);
        final List<CmHandleRegistrationResponse> failureResponses =
            CmHandleRegistrationResponse.createFailureResponses(rejectedCmHandleIds, ALTERNATE_ID_ALREADY_ASSOCIATED);
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponses = new ArrayList<>(failureResponses);
        for (final NcmpServiceCmHandle updatedNcmpServiceCmHandle : updatedNcmpServiceCmHandles) {
            final String cmHandleId = updatedNcmpServiceCmHandle.getCmHandleId();
            if (!rejectedCmHandleIds.contains(cmHandleId)) {
                try {
                    final DataNode existingCmHandleDataNode = inventoryPersistence
                            .getCmHandleDataNodeByCmHandleId(cmHandleId).iterator().next();
                    processUpdates(existingCmHandleDataNode, updatedNcmpServiceCmHandle);
                    cmHandleRegistrationResponses.add(CmHandleRegistrationResponse.createSuccessResponse(cmHandleId));
                } catch (final DataNodeNotFoundException e) {
                    log.error("Unable to find dataNode for cmHandleId : {} , caused by : {}", cmHandleId,
                            e.getMessage());
                    cmHandleRegistrationResponses.add(
                            CmHandleRegistrationResponse.createFailureResponse(cmHandleId, CM_HANDLES_NOT_FOUND));
                } catch (final DataValidationException e) {
                    log.error("Unable to update cm handle : {}, caused by : {}", cmHandleId, e.getMessage());
                    cmHandleRegistrationResponses.add(
                            CmHandleRegistrationResponse.createFailureResponse(cmHandleId, CM_HANDLE_INVALID_ID));
                } catch (final Exception exception) {
                    log.error("Unable to update cmHandle : {} , caused by : {}", cmHandleId, exception.getMessage());
                    cmHandleRegistrationResponses.add(
                            CmHandleRegistrationResponse.createFailureResponse(cmHandleId, exception));
                }
            }
        }
        return cmHandleRegistrationResponses;
    }

    private void processUpdates(final DataNode existingCmHandleDataNode,
                                final NcmpServiceCmHandle updatedNcmpServiceCmHandle) {
        updateAlternateId(updatedNcmpServiceCmHandle);
        updateDataProducerIdentifier(existingCmHandleDataNode, updatedNcmpServiceCmHandle);
        if (!updatedNcmpServiceCmHandle.getPublicProperties().isEmpty()) {
            updateProperties(existingCmHandleDataNode, PUBLIC_PROPERTY,
                updatedNcmpServiceCmHandle.getPublicProperties());
        }
        if (!updatedNcmpServiceCmHandle.getDmiProperties().isEmpty()) {
            updateProperties(existingCmHandleDataNode, DMI_PROPERTY, updatedNcmpServiceCmHandle.getDmiProperties());
        }
    }

    private void updateAlternateId(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final String newAlternateId = ncmpServiceCmHandle.getAlternateId();
        if (StringUtils.isNotBlank(newAlternateId)) {
            setAndUpdateCmHandleField(ncmpServiceCmHandle.getCmHandleId(), "alternate-id", newAlternateId);
        }
    }

    private void updateDataProducerIdentifier(final DataNode cmHandleDataNode,
                                              final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final String newDataProducerIdentifier = ncmpServiceCmHandle.getDataProducerIdentifier();
        if (StringUtils.isNotBlank(newDataProducerIdentifier)) {
            final YangModelCmHandle yangModelCmHandle = YangDataConverter.toYangModelCmHandle(cmHandleDataNode);
            final String existingDataProducerIdentifier = yangModelCmHandle.getDataProducerIdentifier();
            if (StringUtils.isNotBlank(existingDataProducerIdentifier)) {
                if (!existingDataProducerIdentifier.equals(newDataProducerIdentifier)) {
                    log.warn("Unable to update dataProducerIdentifier for cmHandle {}. "
                            + "Value for dataProducerIdentifier has been set previously.",
                        ncmpServiceCmHandle.getCmHandleId());
                } else {
                    log.debug("dataProducerIdentifier for cmHandle {} is already set to {}.",
                        ncmpServiceCmHandle.getCmHandleId(), newDataProducerIdentifier);
                }
            } else {
                setAndUpdateCmHandleField(
                    yangModelCmHandle.getId(), "data-producer-identifier", newDataProducerIdentifier);
            }
        }
    }

    private void updateProperties(final DataNode existingCmHandleDataNode, final PropertyType propertyType,
                                  final Map<String, String> updatedProperties) {
        final Collection<DataNode> replacementPropertyDataNodes =
                getReplacementDataNodes(existingCmHandleDataNode, propertyType, updatedProperties);
        replacementPropertyDataNodes.addAll(
                getUnchangedPropertyDataNodes(existingCmHandleDataNode, propertyType, updatedProperties));
        if (replacementPropertyDataNodes.isEmpty()) {
            removeAllProperties(existingCmHandleDataNode, propertyType);
        } else {
            inventoryPersistence.replaceListContent(existingCmHandleDataNode.getXpath(), replacementPropertyDataNodes);
        }
    }

    private void removeAllProperties(final DataNode existingCmHandleDataNode, final PropertyType propertyType) {
        existingCmHandleDataNode.getChildDataNodes().forEach(dataNode -> {
            final Matcher matcher = propertyType.propertyXpathPattern.matcher(dataNode.getXpath());
            if (matcher.find()) {
                log.info("Deleting dataNode with xpath : [{}]", dataNode.getXpath());
                inventoryPersistence.deleteDataNode(dataNode.getXpath());
            }
        });
    }

    private Collection<DataNode> getUnchangedPropertyDataNodes(final DataNode existingCmHandleDataNode,
                                                               final PropertyType propertyType,
                                                               final Map<String, String> updatedProperties) {
        final Collection<DataNode> unchangedPropertyDataNodes = new HashSet<>();
        for (final DataNode existingPropertyDataNode : existingCmHandleDataNode.getChildDataNodes()) {
            final Matcher matcher = propertyType.propertyXpathPattern.matcher(existingPropertyDataNode.getXpath());
            if (matcher.find()) {
                final String keyName = matcher.group(2);
                if (!updatedProperties.containsKey(keyName)) {
                    unchangedPropertyDataNodes.add(existingPropertyDataNode);
                }
            }
        }
        return unchangedPropertyDataNodes;
    }

    private Collection<DataNode> getReplacementDataNodes(final DataNode existingCmHandleDataNode,
                                                         final PropertyType propertyType,
                                                         final Map<String, String> updatedProperties) {
        final Collection<DataNode> replacementPropertyDataNodes = new HashSet<>();
        updatedProperties.forEach((updatedAttributeKey, updatedAttributeValue) -> {
            final String propertyXpath = getAttributeXpath(existingCmHandleDataNode, propertyType, updatedAttributeKey);
            if (updatedAttributeValue != null) {
                log.info("Creating a new DataNode with xpath {} , key : {} and value : {}", propertyXpath,
                        updatedAttributeKey, updatedAttributeValue);
                replacementPropertyDataNodes.add(
                        buildDataNode(propertyXpath, updatedAttributeKey, updatedAttributeValue));
            }
        });
        return replacementPropertyDataNodes;
    }

    private String getAttributeXpath(final DataNode cmHandle, final PropertyType propertyType,
                                     final String attributeKey) {
        return cmHandle.getXpath() + "/" + propertyType.xpathPrefix + String.format("[@name='%s']", attributeKey);
    }

    private DataNode buildDataNode(final String xpath, final String attributeKey, final String attributeValue) {
        final Map<String, String> updatedLeaves = new LinkedHashMap<>(1);
        updatedLeaves.put("name", attributeKey);
        updatedLeaves.put("value", attributeValue);
        log.debug("Building a new node with xpath {} with leaves (name : {} , value : {})", xpath, attributeKey,
                attributeValue);
        return new DataNodeBuilder().withXpath(xpath).withLeaves(ImmutableMap.copyOf(updatedLeaves)).build();
    }

    private void setAndUpdateCmHandleField(final String cmHandleIdToUpdate, final String fieldName,
                                           final String newFieldValue) {
        final Map<String, Map<String, String>> dmiRegistryData = new HashMap<>(1);
        final Map<String, String> cmHandleData = new HashMap<>(2);
        cmHandleData.put("id", cmHandleIdToUpdate);
        cmHandleData.put(fieldName, newFieldValue);
        dmiRegistryData.put("cm-handles", cmHandleData);
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                jsonObjectMapper.asJsonString(dmiRegistryData), OffsetDateTime.now(), ContentType.JSON);
        log.debug("Updating {} for cmHandle {} with value : {})", fieldName, cmHandleIdToUpdate, newFieldValue);
    }

    enum PropertyType {
        DMI_PROPERTY("additional-properties"), PUBLIC_PROPERTY("public-properties");

        private static final String LIST_INDEX_PATTERN = "\\[@(\\w+)[^\\/]'([^']+)']";

        final String xpathPrefix;
        final Pattern propertyXpathPattern;

        PropertyType(final String xpathPrefix) {
            this.xpathPrefix = xpathPrefix;
            this.propertyXpathPattern = Pattern.compile(xpathPrefix + LIST_INDEX_PATTERN);
        }
    }
}
