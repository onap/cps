/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api;

import java.util.Collection;
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;

public interface NetworkCmProxyCmHandleQueryService {
    /**
     * Query and return cm handle ids that match the given query parameters.
     * Supported query types:
     *      public properties
     *      modules
     *      cps-path
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    Collection<String> queryCmHandleIds(CmHandleQueryServiceParameters cmHandleQueryServiceParameters);

    /**
     * Query and return cm handle ids that match the given query parameters.
     * Supported query types:
     *      public properties
     *      private (additional) properties
     *      dmi-names
     * The inventory interface also allows conditions on private (additional) properties and dmi names
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    Collection<String> queryCmHandleIdsForInventory(CmHandleQueryServiceParameters cmHandleQueryServiceParameters);

    /**
     * Query and return cm handle objects that match the given query parameters.
     * Supported query types:
     *      public properties
     *      modules
     *      cps-path
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handles
     */
    Collection<NcmpServiceCmHandle> queryCmHandles(CmHandleQueryServiceParameters cmHandleQueryServiceParameters);

    /**
     * Query and return all cm handle objects.
     *
     * @return collection of cm handles
     */
    Collection<NcmpServiceCmHandle> getAllCmHandles();
}
