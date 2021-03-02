/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;

/*
 * Query interface for handling cps queries.
 */
public interface CpsQueryService {

    /**
     * Get data nodes for the given dataspace and anchor by cps path.
     *
     * @param dataspaceName          dataspace name
     * @param anchorName             anchor name
     * @param cpsPath                cps path
     * @param fetchDescendantsOption defines whether the descendants of the node(s) found by the query should be
     *                               included in the output
     * @return a collection of data nodes
     */
    Collection<DataNode> queryDataNodes(@NonNull String dataspaceName, @NonNull String anchorName,
        @NonNull String cpsPath, @NonNull FetchDescendantsOption fetchDescendantsOption);

}
