/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.inventory;

import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

import io.micrometer.core.annotation.Timed;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.spi.utils.CpsValidator;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class InventoryPersistenceImpl implements InventoryPersistence {

    private final JsonObjectMapper jsonObjectMapper;
    private final CpsDataService cpsDataService;
    private final CpsModuleService cpsModuleService;
    private final CpsAdminService cpsAdminService;
    private final CpsValidator cpsValidator;

    @Override
    public CompositeState getCmHandleState(final String cmHandleId) {
        final DataNode stateAsDataNode = cpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                        createCmHandleXPath(cmHandleId) + "/state", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
                .iterator().next();
        cpsValidator.validateNameCharacters(cmHandleId);
        return new CompositeStateBuilder().fromDataNode(stateAsDataNode).build();
    }

    @Override
    public void saveCmHandleState(final String cmHandleId, final CompositeState compositeState) {
        final String cmHandleJsonData = createStateJsonData(jsonObjectMapper.asJsonString(compositeState));
        cpsDataService.updateDataNodeAndDescendants(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                createCmHandleXPath(cmHandleId), cmHandleJsonData, OffsetDateTime.now());
    }

    @Override
    public void saveCmHandleStateBatch(final Map<String, CompositeState> cmHandleStatePerCmHandleId) {
        final Map<String, String> cmHandlesJsonDataMap = new HashMap<>();
        cmHandleStatePerCmHandleId.forEach((cmHandleId, compositeState) -> cmHandlesJsonDataMap.put(
                createCmHandleXPath(cmHandleId), createStateJsonData(jsonObjectMapper.asJsonString(compositeState))));
        cpsDataService.updateDataNodesAndDescendants(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cmHandlesJsonDataMap, OffsetDateTime.now());
    }

    @Override
    public YangModelCmHandle getYangModelCmHandle(final String cmHandleId) {
        cpsValidator.validateNameCharacters(cmHandleId);
        final DataNode dataNode = getCmHandleDataNode(cmHandleId).iterator().next();
        return YangDataConverter.convertCmHandleToYangModel(dataNode, cmHandleId);
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
        return YangDataConverter.convertDataNodesToYangModelCmHandles(getCmHandleDataNodes(validCmHandleIds));
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(final String cmHandleId) {
        return cpsModuleService.getModuleDefinitionsByAnchorName(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
    }

    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleId) {
        cpsValidator.validateNameCharacters(cmHandleId);
        return cpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
    }

    @Override
    public void saveCmHandle(final YangModelCmHandle yangModelCmHandle) {
        final String cmHandleJsonData =
                createCmHandleJsonData(jsonObjectMapper.asJsonString(yangModelCmHandle));
        cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                cmHandleJsonData, NO_TIMESTAMP);
    }

    @Override
    public void saveCmHandleBatch(final Collection<YangModelCmHandle> yangModelCmHandles) {
        final List<String> cmHandlesJsonData = new ArrayList<>();
        yangModelCmHandles.forEach(yangModelCmHandle -> cmHandlesJsonData.add(
                createCmHandleJsonData(jsonObjectMapper.asJsonString(yangModelCmHandle))));
        cpsDataService.saveListElementsBatch(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                cmHandlesJsonData, NO_TIMESTAMP);
    }

    @Override
    public void deleteListOrListElement(final String listElementXpath) {
        cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, listElementXpath,
                NO_TIMESTAMP);
    }

    @Override
    @Timed(value = "cps.ncmp.inventory.persistence.schemaset.delete",
            description = "Time taken to delete a schemaset")
    public void deleteSchemaSetWithCascade(final String schemaSetName) {
        try {
            cpsValidator.validateNameCharacters(schemaSetName);
            cpsModuleService.deleteSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                    CASCADE_DELETE_ALLOWED);
        } catch (final SchemaSetNotFoundException schemaSetNotFoundException) {
            log.warn("Schema set {} does not exist or already deleted", schemaSetName);
        }
    }

    @Override
    @Timed(value = "cps.ncmp.inventory.persistence.schemaset.delete.batch",
        description = "Time taken to delete multiple schemaset")
    public void deleteSchemaSetsWithCascade(final Collection<String> schemaSetNames) {
        cpsValidator.validateNameCharacters(schemaSetNames);
        cpsModuleService.deleteSchemaSetsWithCascade(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetNames);
    }

    @Override
    @Timed(value = "cps.ncmp.inventory.persistence.datanode.get",
            description = "Time taken to get a data node (from ncmp dmi registry)")
    public Collection<DataNode> getDataNode(final String xpath) {
        return getDataNode(xpath, INCLUDE_ALL_DESCENDANTS);
    }

    @Override
    @Timed(value = "cps.ncmp.inventory.persistence.datanode.get",
            description = "Time taken to get a data node (from ncmp dmi registry)")
    public Collection<DataNode> getDataNode(final String xpath, final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, xpath,
                fetchDescendantsOption);
    }

    @Override
    public Collection<DataNode> getDataNodes(final Collection<String> xpaths) {
        return getDataNodes(xpaths, INCLUDE_ALL_DESCENDANTS);
    }

    @Override
    public Collection<DataNode> getDataNodes(final Collection<String> xpaths,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNodesForMultipleXpaths(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, xpaths,
                fetchDescendantsOption);
    }

    @Override
    public Collection<DataNode> getCmHandleDataNode(final String cmHandleId) {
        return this.getDataNode(createCmHandleXPath(cmHandleId));
    }

    @Override
    public Collection<DataNode> getCmHandleDataNodes(final Collection<String> cmHandleIds) {
        final Collection<String> xpaths = new ArrayList<>(cmHandleIds.size());
        cmHandleIds.forEach(cmHandleId -> xpaths.add(createCmHandleXPath(cmHandleId)));
        return this.getDataNodes(xpaths);
    }

    @Override
    public Collection<String> getCmHandleIdsWithGivenModules(final Collection<String> moduleNamesForQuery) {
        return cpsAdminService.queryAnchorNames(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, moduleNamesForQuery);
    }

    @Override
    public void replaceListContent(final String parentNodeXpath, final Collection<DataNode> dataNodes) {
        cpsDataService.replaceListContent(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, parentNodeXpath, dataNodes,
                NO_TIMESTAMP);
    }

    @Override
    public void deleteDataNode(final String dataNodeXpath) {
        cpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, dataNodeXpath, NO_TIMESTAMP);
    }

    @Override
    public void deleteDataNodes(final Collection<String> dataNodeXpaths) {
        cpsDataService.deleteDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, dataNodeXpaths, NO_TIMESTAMP);
    }

    private static String createCmHandleXPath(final String cmHandleId) {
        return NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']";
    }

    private static String createStateJsonData(final String state) {
        return "{\"state\":" + state + "}";
    }

    private static String createCmHandleJsonData(final String yangModelCmHandleAsJson) {
        return "{\"cm-handles\":[" + yangModelCmHandleAsJson + "]}";
    }
}
