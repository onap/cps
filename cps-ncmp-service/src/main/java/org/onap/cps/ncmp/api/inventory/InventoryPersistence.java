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

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP;
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InventoryPersistence {

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";

    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private static final String XPATH_TO_CM_HANDLE = "/dmi-registry/cm-handles[@id='" + "%s" + "']";

    private final JsonObjectMapper jsonObjectMapper;

    private final CpsDataService cpsDataService;

    private final CpsModuleService cpsModuleService;

    private final CpsDataPersistenceService cpsDataPersistenceService;

    private static final CompositeStateBuilder compositeStateBuilder = new CompositeStateBuilder();

    /**
     * Get the Cm Handle Composite State from the data node.
     *
     * @param cmHandleId cm handle id
     * @return the cm handle composite state
     */
    public CompositeState getCmHandleState(final String cmHandleId) {
        final DataNode stateAsDataNode = cpsDataService.getDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                String.format(XPATH_TO_CM_HANDLE, cmHandleId) + "/state",
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        return compositeStateBuilder.fromDataNode(stateAsDataNode).build();
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
            String.format(XPATH_TO_CM_HANDLE, cmHandleId),
            cmHandleJsonData, OffsetDateTime.now());
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
                + cmHandleState + "\"]/ancestor::cm-handles",
            FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    /**
     * Method to return data nodes representing the cm handles.
     *
     * @param cpsPath cps path for which the cmHandle is requested
     * @return a list of data nodes representing the cm handles.
     */
    public List<DataNode> getCmHandleDataNodesByCpsPath(final String cpsPath) {
        return cpsDataPersistenceService.queryDataNodes(
                NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath, FetchDescendantsOption.OMIT_DESCENDANTS);
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
                        + cmHandleState + "\"]/ancestor::cm-handles",
                FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    /**
     * Method which returns cm handles by the operational sync state of cm handle.
     * @param syncState sync state
     * @return a list of cm handles
     */
    public List<DataNode> getCmHandlesByOperationalSyncState(final SyncState syncState) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME,
                NCMP_DMI_REGISTRY_ANCHOR, "//state/datastores"
                        + "/operational[@sync-state=\"" + syncState + "\"]/ancestor::cm-handles",
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
     * Register new cmHandle.
     *
     * @param cmHandleJsonData cmHandles mapped in JSON
     */
    public void saveListElements(final String cmHandleJsonData) {
        cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                cmHandleJsonData, NO_TIMESTAMP);
    }

    public void deleteListOrListElement(final String listElementXpath) {
        cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                listElementXpath, NO_TIMESTAMP);
    }

    public void deleteSchemaSet(final String schemaSetName) {
        cpsModuleService.deleteSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                CASCADE_DELETE_ALLOWED);
    }

    public List<DataNode> queryDataNodes(final String cmHandlePath) {
        return cpsDataPersistenceService.queryDataNodes(
                NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cmHandlePath, INCLUDE_ALL_DESCENDANTS);
    }

    private DataNode getCmHandleDataNode(final String cmHandle) {
        return cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR,
            String.format(XPATH_TO_CM_HANDLE, cmHandle),
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }
}
