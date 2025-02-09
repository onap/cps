/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import reactor.core.publisher.Flux;

public interface ParameterizedCmHandleQueryService {
    /**
     * Query and return cm handle ids or alternate ids that match the given query parameters.
     * Supported query types:
     *      public properties
     *      modules
     *      cps-path
     *      trust level
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @param outputAlternateId Boolean for cm handle reference type either
     *                            cm handle id (false or null) or alternate id (true)
     * @return collection of cm handle ids or alternate ids
     */
    Collection<String> queryCmHandleReferenceIds(CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
                                                 boolean outputAlternateId);

    /**
     * Query and return cm handle ids or alternate ids that match the given query parameters.
     * Supported query types:
     *      public properties
     *      private (additional) properties
     *      dmi-names
     * The inventory interface also allows conditions on private (additional) properties and dmi names
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @param outputAlternateId Boolean for cm handle reference type either
     *                            cm handle id (false or null) or alternate id (true)
     * @return collection of cm handle ids
     */
    Collection<String> queryCmHandleIdsForInventory(CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
                                                    boolean outputAlternateId);

    /**
     * Query and return cm handle objects that match the given query parameters.
     * Supported query types:
     *      public properties
     *      modules
     *      cps-path
     *      trust level
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return cm handle objects as a reactive stream (flux)
     */
    Flux<NcmpServiceCmHandle> queryCmHandles(CmHandleQueryServiceParameters cmHandleQueryServiceParameters);

    /**
     * Retrieves all {@code NcmpServiceCmHandle} instances without their associated properties.
     * This method fetches the relevant data nodes from the inventory persistence layer and
     * converts them into {@code NcmpServiceCmHandle} objects. Only the handles are returned,
     * without any additional properties.
     *
     * @return a collection of {@code NcmpServiceCmHandle} instances without properties.
     */
    Collection<NcmpServiceCmHandle> getAllCmHandlesWithoutProperties();
}
