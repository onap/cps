/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS;

import com.google.common.collect.Lists;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.spi.utils.CpsValidator;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryPersistenceImpl extends NcmpPersistenceImpl implements InventoryPersistence {

    private static final int CMHANDLE_BATCH_SIZE = 100;

    private final CpsModuleService cpsModuleService;
    private final CpsAnchorService cpsAnchorService;
    private final CpsValidator cpsValidator;
    private final CmHandleQueryService cmHandleQueryService;

    /**
     * initialize an inventory persistence object.
     *
     * @param jsonObjectMapper json mapper object
     * @param cpsDataService   cps data service instance
     * @param cpsModuleService cps module service instance
     * @param cpsValidator     cps validation service instance
     * @param cpsAnchorService  cps anchor service instance
     */
    public InventoryPersistenceImpl(final JsonObjectMapper jsonObjectMapper, final CpsDataService cpsDataService,
                                    final CpsModuleService cpsModuleService, final CpsValidator cpsValidator,
                                    final CpsAnchorService cpsAnchorService,
                                    final CmHandleQueryService cmHandleQueryService) {
        super(jsonObjectMapper, cpsDataService, cpsModuleService, cpsValidator);
        this.cpsModuleService = cpsModuleService;
        this.cpsAnchorService = cpsAnchorService;
        this.cpsValidator = cpsValidator;
        this.cmHandleQueryService = cmHandleQueryService;
    }


    @Override
    public CompositeState getCmHandleState(final String cmHandleId) {
        final DataNode stateAsDataNode = cpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                        getXPathForCmHandleById(cmHandleId) + "/state",
                        FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, ContentType.JSON)
                .iterator().next();
        cpsValidator.validateNameCharacters(cmHandleId);
        return new CompositeStateBuilder().fromDataNode(stateAsDataNode).build();
    }

    @Override
    public void saveCmHandleState(final String cmHandleId, final CompositeState compositeState) {
        final String cmHandleJsonData = createStateJsonData(jsonObjectMapper.asJsonString(compositeState));
        cpsDataService.updateDataNodeAndDescendants(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                getXPathForCmHandleById(cmHandleId), cmHandleJsonData, OffsetDateTime.now());
    }

    @Override
    public void saveCmHandleStateBatch(final Map<String, CompositeState> cmHandleStatePerCmHandleId) {
        final Map<String, String> cmHandlesJsonDataMap = new HashMap<>();
        cmHandleStatePerCmHandleId.forEach((cmHandleId, compositeState) -> cmHandlesJsonDataMap.put(
                getXPathForCmHandleById(cmHandleId),
                createStateJsonData(jsonObjectMapper.asJsonString(compositeState))));
        cpsDataService.updateDataNodesAndDescendants(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cmHandlesJsonDataMap, OffsetDateTime.now());
    }

    @Override
    public YangModelCmHandle getYangModelCmHandle(final String cmHandleId) {
        cpsValidator.validateNameCharacters(cmHandleId);
        final DataNode dataNode = getCmHandleDataNodeByCmHandleId(cmHandleId).iterator().next();
        return YangDataConverter.toYangModelCmHandle(dataNode);
    }

    @Override
    public Collection<YangModelCmHandle> getYangModelCmHandles(final Collection<String> cmHandleIds) {
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
        return YangDataConverter.toYangModelCmHandles(getCmHandleDataNodes(validCmHandleIds));
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
                Lists.partition(yangModelCmHandles, CMHANDLE_BATCH_SIZE)) {
            final String cmHandlesJsonData = createCmHandlesJsonData(yangModelCmHandleBatch);
            cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    NCMP_DMI_REGISTRY_PARENT, cmHandlesJsonData, NO_TIMESTAMP);
        }
    }

    @Override
    public Collection<DataNode> getCmHandleDataNodeByCmHandleId(final String cmHandleId) {
        return this.getDataNode(getXPathForCmHandleById(cmHandleId));
    }

    @Override
    public DataNode getCmHandleDataNodeByAlternateId(final String alternateId) {
        final String cpsPathForCmHandleByAlternateId = getCpsPathForCmHandleByAlternateId(alternateId);
        final Collection<DataNode> dataNodes = cmHandleQueryService
            .queryNcmpRegistryByCpsPath(cpsPathForCmHandleByAlternateId, OMIT_DESCENDANTS);
        if (dataNodes.isEmpty()) {
            throw new DataNodeNotFoundException(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cpsPathForCmHandleByAlternateId);
        }
        return dataNodes.iterator().next();
    }

    @Override
    public Collection<DataNode> getCmHandleDataNodes(final Collection<String> cmHandleIds) {
        final Collection<String> xpaths = new ArrayList<>(cmHandleIds.size());
        cmHandleIds.forEach(cmHandleId -> xpaths.add(getXPathForCmHandleById(cmHandleId)));
        return this.getDataNodes(xpaths);
    }

    @Override
    public Collection<String> getCmHandleIdsWithGivenModules(final Collection<String> moduleNamesForQuery) {
        return cpsAnchorService.queryAnchorNames(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, moduleNamesForQuery);
    }

    private static String getXPathForCmHandleById(final String cmHandleId) {
        return NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']";
    }

    private static String getCpsPathForCmHandleByAlternateId(final String alternateId) {
        return NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@alternate-id='" + alternateId + "']";
    }

    private static String createStateJsonData(final String state) {
        return "{\"state\":" + state + "}";
    }

    private String createCmHandlesJsonData(final List<YangModelCmHandle> yangModelCmHandles) {
        return "{\"cm-handles\":" + jsonObjectMapper.asJsonString(yangModelCmHandles) + "}";
    }
}
