/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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

package org.onap.cps.utils.deltareport;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeltaReportGeneratorFacade {
    private final DeltaReportGenerator deltaReportGenerator;
    private final GroupedDeltaReportGenerator groupedDeltaReportGenerator;

    /**
     * Create delta reports for the supplied source and target data node collections.
     *
     *  <p>If groupDataNodes is true, condensed delta reports are created by delegating to GroupedDeltaReportGenerator.
     *  Otherwise, standard delta reports are created by delegating to DeltaReportGenerator.
     *
     * @param sourceDataNodes source data nodes used as the baseline for comparison
     * @param targetDataNodes target data nodes compared against the source data nodes
     * @param groupDataNodes whether to generate condensed grouped delta reports instead of standard delta reports
     * @return list of generated delta reports
     */
    public List<DeltaReport> createDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                final Collection<DataNode> targetDataNodes,
                                                final boolean groupDataNodes) {
        if (groupDataNodes) {
            return groupedDeltaReportGenerator.createCondensedDeltaReports(sourceDataNodes, targetDataNodes);
        }
        return deltaReportGenerator.createDeltaReports(sourceDataNodes, targetDataNodes);
    }
}