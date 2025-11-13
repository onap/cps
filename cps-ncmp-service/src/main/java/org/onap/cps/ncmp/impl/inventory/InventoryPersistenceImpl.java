/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS;

import com.google.common.collect.Lists;
import com.hazelcast.map.IMap;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.api.exceptions.DataNodeNotFoundException;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.CmHandleStateUpdate;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryPersistenceImpl extends NcmpPersistenceImpl implements InventoryPersistence {

    private static final int CM_HANDLE_BATCH_SIZE = 300;

    private final CpsModuleService cpsModuleService;
    private final CpsValidator cpsValidator;
    private final IMap<String, String> cmHandleIdPerAlternateId;

    /**
     * initialize an inventory persistence object.
     *
     * @param cpsValidator         cps validation service instance
     * @param jsonObjectMapper     json mapper object
     * @param cpsAnchorService     cps anchor service instance
     * @param cpsModuleService     cps module service instance
     * @param cpsDataService       cps data service instance
     */
    public InventoryPersistenceImpl(final CpsValidator cpsValidator,
                                    final JsonObjectMapper jsonObjectMapper,
                                    final CpsAnchorService cpsAnchorService,
                                    final CpsModuleService cpsModuleService,
                                    final CpsDataService cpsDataService,
                                    @Qualifier("cmHandleIdPerAlternateId")
                                    final IMap<String, String> cmHandleIdPerAlternateId) {
        super(jsonObjectMapper, cpsAnchorService, cpsDataService);
        this.cpsModuleService = cpsModuleService;
        this.cpsValidator = cpsValidator;
        this.cmHandleIdPerAlternateId = cmHandleIdPerAlternateId;
    }

    @Override
    public CompositeState getCmHandleState(final String cmHandleId) {
        final DataNode stateAsDataNode = cpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                        getXPathForCmHandleById(cmHandleId) + "/state", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
                .iterator().next();
        cpsValidator.validateNameCharacters(cmHandleId);
        return new CompositeStateBuilder().fromDataNode(stateAsDataNode).build();
    }

    @Override
    public void saveCmHandleState(final String cmHandleId, final CompositeState compositeState) {
        final String cmHandleJsonData = createStateJsonData(jsonObjectMapper.asJsonString(compositeState));
        cpsDataService.updateDataNodeAndDescendants(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                getXPathForCmHandleById(cmHandleId), cmHandleJsonData, OffsetDateTime.now(), ContentType.JSON);
    }

    @Override
    public void saveCmHandleStateBatch(final Map<String, CompositeState> cmHandleStatePerCmHandleId) {
        final Map<String, String> cmHandlesJsonDataMap = new HashMap<>();
        cmHandleStatePerCmHandleId.forEach((cmHandleId, compositeState) -> {
            if (exists(cmHandleId)) {
                cmHandlesJsonDataMap.put(getXPathForCmHandleById(cmHandleId),
                        createStateJsonData(jsonObjectMapper.asJsonString(compositeState)));
            } else {
                log.warn("Failure to save state for cmHandle id '{}' as it does not exist in cache", cmHandleId);
            }
        });
        if (!cmHandlesJsonDataMap.isEmpty()) {
            cpsDataService.updateDataNodesAndDescendants(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    cmHandlesJsonDataMap, OffsetDateTime.now(), ContentType.JSON);
        }
    }

    @Override
    public YangModelCmHandle getYangModelCmHandle(final String cmHandleId) {
        cpsValidator.validateNameCharacters(cmHandleId);
        final DataNode dataNode =
                getCmHandleDataNodeByCmHandleId(cmHandleId, INCLUDE_ALL_DESCENDANTS).iterator().next();
        return YangDataConverter.toYangModelCmHandle(dataNode);
    }

    @Override
    public Collection<YangModelCmHandle> getYangModelCmHandles(final Collection<String> cmHandleIds) {
        return getYangModelCmHandlesWithDescendantsOption(cmHandleIds, INCLUDE_ALL_DESCENDANTS);
    }

    @Override
    public Collection<YangModelCmHandle> getYangModelCmHandlesWithoutProperties(final Collection<String> cmHandleIds) {
        return getYangModelCmHandlesWithDescendantsOption(cmHandleIds, OMIT_DESCENDANTS);
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(final String cmHandleId) {
        return cpsModuleService.getModuleDefinitionsByAnchorName(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleAndModule(final String cmHandleId,
                                                                                final String moduleName,
                                                                                final String moduleRevision) {
        cpsValidator.validateNameCharacters(cmHandleId, moduleName);
        return cpsModuleService.getModuleDefinitionsByAnchorAndModule(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                cmHandleId, moduleName, moduleRevision);
    }

    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleId) {
        cpsValidator.validateNameCharacters(cmHandleId);
        return cpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
    }

    @Override
    public void saveCmHandle(final YangModelCmHandle yangModelCmHandle) {
        saveCmHandleBatch(Collections.singletonList(yangModelCmHandle));
    }

    @Override
    public void saveCmHandleBatch(final List<YangModelCmHandle> yangModelCmHandles) {
        for (final List<YangModelCmHandle> yangModelCmHandleBatch :
                Lists.partition(yangModelCmHandles, CM_HANDLE_BATCH_SIZE)) {
            final String cmHandlesJsonData = createCmHandlesJsonData(yangModelCmHandleBatch);
            cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    NCMP_DMI_REGISTRY_PARENT, cmHandlesJsonData, NO_TIMESTAMP, ContentType.JSON);
        }
    }

    @Override
    public Collection<DataNode> getCmHandleDataNodeByCmHandleId(final String cmHandleId,
                                                                final FetchDescendantsOption fetchDescendantsOption) {
        return this.getDataNode(getXPathForCmHandleById(cmHandleId), fetchDescendantsOption);
    }

    @Override
    public Collection<String> getCmHandleReferencesWithGivenModules(final Collection<String> moduleNamesForQuery,
                                                                    final boolean outputAlternateId) {
        final Collection<String> cmHandleIds =
                cpsAnchorService.queryAnchorNames(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, moduleNamesForQuery);
        if (outputAlternateId) {
            return getAlternateIdsForCmHandleIds(cmHandleIds);
        }
        return cmHandleIds;
    }

    @Override
    public boolean isExistingCmHandleId(final String cmHandleId) {
        try {
            return  !getCmHandleDataNodeByCmHandleId(cmHandleId, OMIT_DESCENDANTS).isEmpty();
        } catch (final DataNodeNotFoundException exception) {
            return false;
        }
    }

    /**
     * Updates the specified field of a CM handle with a new value in the DMI registry.
     *
     * @param cmHandleId                                the unique identifier of the CM handle to be updated
     * @param fieldName                                 the name of the field within the CM handle to be updated
     * @param fieldValue                                the new value to be set for
     *                                                  the specified field of the CM handle
     */
    @Override
    public void updateCmHandleField(final String cmHandleId, final String fieldName,
                                    final String fieldValue) {
        updateCmHandleFields(Collections.singletonMap(cmHandleId, fieldValue), fieldName);
    }

    @Override
    public void updateCmHandleFields(final Map<String, String> cmHandleIdToValueMap, final String fieldName) {
        final Map<String, Object> cmHandleStateUpdates = new HashMap<>();
        final List<Map<String, String>> cmStateUpdatesPerCmHandleId = new ArrayList<>();

        for (final Map.Entry<String, String> entry : cmHandleIdToValueMap.entrySet()) {
            final Map<String, String> cmHandleData = new HashMap<>();
            cmHandleData.put("id", entry.getKey());
            cmHandleData.put(fieldName, entry.getValue());
            cmStateUpdatesPerCmHandleId.add(cmHandleData);
            log.debug("Updating {} for cmHandle {} to {}", fieldName, entry.getKey(), entry.getValue());
        }
        cmHandleStateUpdates.put("cm-handles", cmStateUpdatesPerCmHandleId);
        cpsDataService.updateNodeLeaves(
                NCMP_DATASPACE_NAME,
                NCMP_DMI_REGISTRY_ANCHOR,
                NCMP_DMI_REGISTRY_PARENT,
                jsonObjectMapper.asJsonString(cmHandleStateUpdates),
                OffsetDateTime.now(),
                ContentType.JSON);
    }

    @Override
    public void bulkUpdateCmHandleStates(final List<CmHandleStateUpdate> updates) {
        final Map<String, String> cmHandleStateUpdates = updates.stream()
            .collect(Collectors.toMap(
                    CmHandleStateUpdate::cmHandleId,
                    CmHandleStateUpdate::state
            ));
        updateCmHandleFields(cmHandleStateUpdates, "cm-handle-state");
    }

    private static String getXPathForCmHandleById(final String cmHandleId) {
        return NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']";
    }

    private static String createStateJsonData(final String state) {
        return "{\"state\":" + state + "}";
    }

    private String createCmHandlesJsonData(final List<YangModelCmHandle> yangModelCmHandles) {
        return "{\"cm-handles\":" + jsonObjectMapper.asJsonString(yangModelCmHandles) + "}";
    }

    private Collection<String> getAlternateIdsForCmHandleIds(final Collection<String> cmHandleIds) {
        final Collection<DataNode> dataNodes = getCmHandleDataNodes(cmHandleIds, OMIT_DESCENDANTS);
        return dataNodes.stream()
                .map(DataNode::getLeaves)
                .map(leaves -> (String) leaves.get("alternate-id"))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private Collection<YangModelCmHandle> getYangModelCmHandlesWithDescendantsOption(final Collection<String>
                                                                                             cmHandleIds,
                                                                                     final FetchDescendantsOption
                                                                                             fetchDescendantsOption) {
        final Collection<String> validCmHandleIds = new ArrayList<>(cmHandleIds.size());
        cmHandleIds.forEach(cmHandleId -> {
            try {
                cpsValidator.validateNameCharacters(cmHandleId);
                validCmHandleIds.add(cmHandleId);
            } catch (final DataValidationException dataValidationException) {
                log.error("DataValidationException in CmHandleId {} to be ignored",
                        dataValidationException.getMessage());
            }
        });
        return YangDataConverter.toYangModelCmHandles(getCmHandleDataNodes(validCmHandleIds, fetchDescendantsOption));
    }

    private Collection<DataNode> getCmHandleDataNodes(final Collection<String> cmHandleIds,
                                                      final FetchDescendantsOption fetchDescendantsOption) {
        final Collection<String> xpaths = new ArrayList<>(cmHandleIds.size());
        cmHandleIds.forEach(cmHandleId -> xpaths.add(getXPathForCmHandleById(cmHandleId)));
        return this.getDataNodes(xpaths, fetchDescendantsOption);
    }

    private boolean exists(final String cmHandleId) {
        return cmHandleIdPerAlternateId.containsKey(cmHandleId) || cmHandleIdPerAlternateId.containsValue(cmHandleId);
    }

}
