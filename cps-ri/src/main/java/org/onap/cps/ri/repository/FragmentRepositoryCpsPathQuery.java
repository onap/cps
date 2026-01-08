/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2023 Deutsche Telekom AG
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

package org.onap.cps.ri.repository;

import java.util.List;
import java.util.Set;
import org.onap.cps.api.parameters.PaginationOption;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.FragmentEntity;

public interface FragmentRepositoryCpsPathQuery {

    List<FragmentEntity> findByAnchorAndCpsPath(AnchorEntity anchorEntity, CpsPathQuery cpsPathQuery,
                                                int queryResultLimit);

    <T> Set<T> findAttributeValuesByAnchorAndCpsPath(AnchorEntity anchorEntity, CpsPathQuery cpsPathQuery,
                                                     int queryResultLimit, Class<T> targetClass);

    List<FragmentEntity> findByDataspaceAndCpsPath(DataspaceEntity dataspaceEntity,
                                                   CpsPathQuery cpsPathQuery, List<Long> anchorIds);

    List<Long> findAnchorIdsForPagination(DataspaceEntity dataspaceEntity, CpsPathQuery cpsPathQuery,
                                          PaginationOption paginationOption);

}
