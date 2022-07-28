/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.inventory;

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP;
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class InventoryPersistence {

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";

    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private static final String NCMP_DMI_REGISTRY_PARENT = "/dmi-registry";

    private static final String CM_HANDLE_XPATH_TEMPLATE = "/dmi-registry/cm-handles[@id='" + "%s" + "']";

    private static final String ANCESTOR_CM_HANDLES = "\"]/ancestor::cm-handles";

    private final JsonObjectMapper jsonObjectMapper;

    private final CpsDataService cpsDataService;

    private final CpsModuleService cpsModuleService;

    private final CpsDataPersistenceService cpsDataPersistenceService;

    private final CpsAdminPersistenceService cpsAdminPersistenceService;

    /**
     * Get the Cm Handle Composite State from the data node.
     *
     * @param cmHandleId cm handle id
     * @return the cm handle composite state
     */
    public CompositeState getCmHandleState(final String cmHandleId) {
        final DataNode stateAsDataNode = cpsDataService.getDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            String.format(CM_HANDLE_XPATH_TEMPLATE, cmHandleId) + "/state",
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        return new CompositeStateBuilder().fromDataNode(stateAsDataNode).build();
    }

    /**
     * Save the cm handles state.
     *
     * @param cmHandleId    cm handle id
     * @param compositeState composite state
     */
    public void saveCmHandleState(final String cmHandleId, final CompositeState compositeState) {
        final String cmHandleJsonData = String.format("{\"state\":%s}",
            jsonObjectMapper.asJsonString(compositeState));
        cpsDataService.replaceNodeTree(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            String.format(CM_HANDLE_XPATH_TEMPLATE, cmHandleId),
            cmHandleJsonData, OffsetDateTime.parse(DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(OffsetDateTime.now())));
    }

    /**
     * Method which returns cm handles by the cm handles state.
     *
     * @param cmHandleState cm handle state
     * @return a list of cm handles
     */
    public List<DataNode> getCmHandlesByState(final CmHandleState cmHandleState) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR, "//state[@cm-handle-state=\""
                + cmHandleState + ANCESTOR_CM_HANDLES,
            FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    /**
     * Method to return data nodes representing the cm handles.
     *
     * @param cpsPath cps path for which the cmHandle is requested
     * @param fetchDescendantsOption defines the scope of data to fetch: either single node or all the descendant nodes
     * @return a list of data nodes representing the cm handles.
     */
    public List<DataNode> getCmHandleDataNodesByCpsPath(final String cpsPath,
                                                        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.queryDataNodes(
            NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath, fetchDescendantsOption);
    }

    /**
     * Method which returns cm handles by the cm handle id and state.
     * @param cmHandleId cm handle id
     * @param cmHandleState cm handle state
     * @return a list of cm handles
     */
    public List<DataNode> getCmHandlesByIdAndState(final String cmHandleId, final CmHandleState cmHandleState) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR, "//cm-handles[@id='" + cmHandleId + "']/state[@cm-handle-state=\""
                + cmHandleState + ANCESTOR_CM_HANDLES,
            FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    /**
     * Method which returns cm handles by the operational sync state of cm handle.
     * @param dataStoreSyncState sync state
     * @return a list of cm handles
     */
    public List<DataNode> getCmHandlesByOperationalSyncState(final DataStoreSyncState dataStoreSyncState) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR, "//state/datastores"
                + "/operational[@sync-state=\"" + dataStoreSyncState + ANCESTOR_CM_HANDLES,
            FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    /**
     * This method retrieves DMI service name, DMI properties and the state for a given cm handle.
     * @param cmHandleId the id of the cm handle
     * @return yang model cm handle
     */
    public YangModelCmHandle getYangModelCmHandle(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return YangDataConverter.convertCmHandleToYangModel(getCmHandleDataNode(cmHandleId), cmHandleId);
    }

    /**
     * Method to return module definitions by cmHandleId.
     *
     * @param cmHandleId cm handle ID
     * @return a collection of module definitions (moduleName, revision and yang resource content)
     */
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(final String cmHandleId) {
        return cpsModuleService.getModuleDefinitionsByAnchorName(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
    }

    /**
     * Method to return module references by cmHandleId.
     *
     * @param cmHandleId cm handle ID
     * @return a collection of module references (moduleName and revision)
     */
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return cpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
    }

    /**
     * Method to save cmHandle.
     *
     * @param yangModelCmHandle cmHandle represented as Yang Model
     */
    public void saveCmHandle(final YangModelCmHandle yangModelCmHandle) {
        final String cmHandleJsonData =
                String.format("{\"cm-handles\":[%s]}", jsonObjectMapper.asJsonString(yangModelCmHandle));
        cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                cmHandleJsonData, NO_TIMESTAMP);
    }

    /**
     * Method to delete a list or a list element.
     *
     * @param listElementXpath list element xPath
     */
    public void deleteListOrListElement(final String listElementXpath) {
        cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                listElementXpath, NO_TIMESTAMP);
    }

    /**
     * Method to delete a schema set.
     *
     * @param schemaSetName schema set name
     */
    public void deleteSchemaSetWithCascade(final String schemaSetName) {
        try {
            CpsValidator.validateNameCharacters(schemaSetName);
            cpsModuleService.deleteSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                    CASCADE_DELETE_ALLOWED);
        } catch (final SchemaSetNotFoundException schemaSetNotFoundException) {
            log.warn("Schema set {} does not exist or already deleted", schemaSetName);
        }
    }

    /**
     * Query data nodes via cps path.
     *
     * @param cpsPath cps path
     * @return List of data nodes
     */
    public List<DataNode> queryDataNodes(final String cpsPath) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cpsPath, INCLUDE_ALL_DESCENDANTS);
    }

    /**
     * Get data node via xpath.
     *
     * @param xpath xpath
     * @return data node
     */
    public DataNode getDataNode(final String xpath) {
        return cpsDataPersistenceService.getDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                xpath, INCLUDE_ALL_DESCENDANTS);
    }

    /**
     * Get data node of given cm handle.
     *
     * @param cmHandleId cmHandle ID
     * @return data node
     */
    public DataNode getCmHandleDataNode(final String cmHandleId) {
        return this.getDataNode(String.format(CM_HANDLE_XPATH_TEMPLATE, cmHandleId));
    }

    /**
     * Query anchors via module names.
     *
     * @param moduleNamesForQuery module names
     * @return Collection of anchors
     */
    public Collection<Anchor> queryAnchors(final Collection<String> moduleNamesForQuery) {
        return  cpsAdminPersistenceService.queryAnchors(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, moduleNamesForQuery);
    }

    /**
     * Method to get all anchors.
     *
     * @return Collection of anchors
     */
    public Collection<Anchor> getAnchors() {
        return cpsAdminPersistenceService.getAnchors(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME);
    }

    /**
     * Replaces list content by removing all existing elements and inserting the given new elements as data nodes.
     *
     * @param parentNodeXpath   parent node xpath
     * @param dataNodes         datanodes representing the updated data
     */
    public void replaceListContent(final String parentNodeXpath, final Collection<DataNode> dataNodes) {
        cpsDataService.replaceListContent(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                parentNodeXpath, dataNodes, NO_TIMESTAMP);
    }

    /**
     * Deletes data node for given anchor and dataspace.
     *
     * @param dataNodeXpath data node xpath
     */
    public void deleteDataNode(final String dataNodeXpath) {
        cpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, dataNodeXpath,
                NO_TIMESTAMP);
    }
}