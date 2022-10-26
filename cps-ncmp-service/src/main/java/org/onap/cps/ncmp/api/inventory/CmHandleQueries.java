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

package org.onap.cps.ncmp.api.inventory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;

public interface CmHandleQueries {

    /**
     * Query CmHandles based on PrivateProperties.
     *
     * @param publicPropertyQueryPairs private properties for query
     * @return CmHandles which have these private properties
     */
    Map<String, NcmpServiceCmHandle> queryCmHandlePrivateProperties(Map<String, String> publicPropertyQueryPairs);

    /**
     * Query CmHandles based on PublicProperties.
     *
     * @param publicPropertyQueryPairs public properties for query
     * @return CmHandles which have these public properties
     */
    Map<String, NcmpServiceCmHandle> queryCmHandlePublicProperties(Map<String, String> publicPropertyQueryPairs);

    /**
     * Combine Maps of CmHandles.
     *
     * @param firstQuery  first CmHandles Map
     * @param secondQuery second CmHandles Map
     * @return combined Map of CmHandles
     */
    Map<String, NcmpServiceCmHandle> combineCmHandleQueries(Map<String, NcmpServiceCmHandle> firstQuery,
            Map<String, NcmpServiceCmHandle> secondQuery);

    /**
     * Method which returns cm handles by the cm handles state.
     *
     * @param cmHandleState cm handle state
     * @return a list of cm handles
     */
    List<DataNode> queryCmHandlesByState(CmHandleState cmHandleState);

    /**
     * Method to return data nodes representing the cm handles.
     *
     * @param cpsPath cps path for which the cmHandle is requested
     * @return a list of data nodes representing the cm handles.
     */
    List<DataNode> queryCmHandleDataNodesByCpsPath(String cpsPath, FetchDescendantsOption fetchDescendantsOption);

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
     * @return a list of cm handles
     */
    List<DataNode> queryCmHandlesByOperationalSyncState(DataStoreSyncState dataStoreSyncState);

    /**
     * Get all cm handles by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @return set of cm handles
     */
    Set<NcmpServiceCmHandle> getCmHandlesByDmiPluginIdentifier(String dmiPluginIdentifier);
}
