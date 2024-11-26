/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

import java.util.Collection;
import java.util.Map;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.spi.api.FetchDescendantsOption;
import org.onap.cps.spi.api.model.DataNode;

public interface CmHandleQueryService {

    /**
     * Query Cm Handles based on additional (private) properties.
     *
     * @param additionalPropertyQueryPairs private properties for query
     * @param outputAlternateId boolean for cm handle reference type either cmHandleId (false) or AlternateId (true)
     * @return Ids of Cm Handles which have these private properties
     */
    Collection<String> queryCmHandleAdditionalProperties(Map<String, String> additionalPropertyQueryPairs,
                                                         boolean outputAlternateId);

    /**
     * Query Cm Handles based on public properties.
     *
     * @param publicPropertyQueryPairs public properties for query
     * @param outputAlternateId  boolean for cm handle reference type either cmHandleId (false) or AlternateId (true)
     * @return CmHandles which have these public properties
     */
    Collection<String> queryCmHandlePublicProperties(Map<String, String> publicPropertyQueryPairs,
                                                     boolean outputAlternateId);

    /**
     * Query Cm Handles based on Trust Level.
     *
     * @param trustLevelPropertyQueryPairs trust level properties for query
     * @param outputAlternateId boolean for cm handle reference type either cmHandleId (false) or AlternateId (true)
     * @return Ids of Cm Handles which have desired trust level
     */
    Collection<String> queryCmHandlesByTrustLevel(Map<String, String> trustLevelPropertyQueryPairs,
                                                  boolean outputAlternateId);

    /**
     * Method which returns cm handles by the cm handles state.
     *
     * @param cmHandleState cm handle state
     * @return a list of data nodes representing the cm handles.
     */
    Collection<DataNode> queryCmHandlesByState(CmHandleState cmHandleState);

    /**
     * Method to return data nodes with ancestor representing the cm handles.
     *
     * @param cpsPath cps path for which the cmHandle is requested
     * @return a list of data nodes representing the cm handles.
     */
    Collection<DataNode> queryCmHandleAncestorsByCpsPath(String cpsPath, FetchDescendantsOption fetchDescendantsOption);

    /**
     * Method to return data nodes representing the cm handles.
     *
     * @param cpsPath cps path for which the cmHandle is requested
     * @return a list of data nodes representing the cm handles.
     */
    Collection<DataNode> queryNcmpRegistryByCpsPath(String cpsPath, FetchDescendantsOption fetchDescendantsOption);

    /**
     * Method to check the state of a cm handle with given id.
     *
     * @param cmHandleId            cm handle id
     * @param requiredCmHandleState the required state of the cm handle
     * @return a boolean, true if the state is equal to the required state
     */
    boolean cmHandleHasState(String cmHandleId, CmHandleState requiredCmHandleState);

    /**
     * Method which returns cm handles by the operational sync state of cm handle.
     *
     * @param dataStoreSyncState sync state
     * @return a list of data nodes representing the cm handles.
     */
    Collection<DataNode> queryCmHandlesByOperationalSyncState(DataStoreSyncState dataStoreSyncState);

    /**
     * Get collection of all cm handles references by DMI plugin identifier and alternate id output option.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @param outputAlternateId   boolean for cm handle reference type either cmHandleId (false) or AlternateId (true)
     * @return collection of cm handle references
     */
    Collection<String> getCmHandleReferencesByDmiPluginIdentifier(String dmiPluginIdentifier,
                                                                  boolean outputAlternateId);

    /**
     * Get map of cmHandle references by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @return map of cmHandle references key:CmHandleId Value:AlternateId
     */
    Map<String, String> getCmHandleReferencesMapByDmiPluginIdentifier(String dmiPluginIdentifier);

}
