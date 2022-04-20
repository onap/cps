/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.repository;

import java.util.Collection;
import java.util.Set;
import org.onap.cps.spi.model.CmHandleQueryParameters;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;

/**
 * This interface is used in conjunction with {@link ModuleReferenceRepository} to create native sql queries.
 */
public interface ModuleReferenceQuery {

    Collection<ModuleReference> identifyNewModuleReferences(
        final Collection<ModuleReference> moduleReferencesToCheck);

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    Set<String> queryCmHandles(CmHandleQueryParameters cmHandleQueryParameters);

    DataNode queryAdvisedCmHandle(Set<String> newCmHandles);

}
