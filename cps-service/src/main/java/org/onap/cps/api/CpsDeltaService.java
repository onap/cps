/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.api;

import java.util.Collection;
import java.util.List;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DeltaReport;

public interface CpsDeltaService {

    /**
     * Retrieves delta between source data nodes and target data nodes. Source data nodes contain the data which acts as
     * the point of reference for delta report, whereas target data nodes contain the data being compared against
     * source data node. List of {@link DeltaReport}. Each Delta Report contains information such as action, xpath,
     * source-payload and target-payload.
     *
     * @param sourceDataNodes  collection of {@link DataNode} as source/reference for delta generation
     * @param targetDataNodes  collection of {@link DataNode} as target data for delta generation
     * @return                 list of {@link DeltaReport} containing delta information
     */
    List<DeltaReport> getDeltaReports(Collection<DataNode> sourceDataNodes, Collection<DataNode> targetDataNodes, boolean groupingEnabled);

//    List<DeltaReport> getDeltaReports(String xpath, Collection<DataNode> sourceDataNodes, Collection<DataNode> targetDataNodes);
}
