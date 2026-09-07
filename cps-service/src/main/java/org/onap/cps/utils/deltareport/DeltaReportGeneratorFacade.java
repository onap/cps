/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG
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
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeltaReportGeneratorFacade {
    private final DeltaReportGenerator deltaReportGenerator;
    private final GroupedDeltaReportGenerator groupedDeltaReportGenerator;

    /**
     * Generate delta reports between the given source and target data nodes.
     *
     * @param sourceDataNodes the source data nodes
     * @param targetDataNodes the target data nodes
     * @param groupDataNodes the group data nodes
     * @return                a list of delta reports
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