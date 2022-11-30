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

package org.onap.cps.ncmp.api;

import java.util.Set;
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;

public interface NetworkCmProxyCmHandlerQueryService {
    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handles
     */
    Set<NcmpServiceCmHandle> queryCmHandles(CmHandleQueryServiceParameters cmHandleQueryServiceParameters);

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    Set<String> queryCmHandleIds(CmHandleQueryServiceParameters cmHandleQueryServiceParameters);

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    Set<String> queryCmHandleIdsForInventory(CmHandleQueryServiceParameters cmHandleQueryServiceParameters);
}
