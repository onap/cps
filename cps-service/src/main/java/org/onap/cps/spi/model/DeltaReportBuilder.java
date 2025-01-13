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

package org.onap.cps.spi.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeltaReportBuilder {


    private String action;
    private String xpath;
    private Map<String, Serializable> sourceData;
    private Map<String, Serializable> targetData;
    private Collection<Map<String, Object>> groupedSourceData;
    private Collection<Map<String, Object>> groupedTargetData;

    public DeltaReportBuilder withXpath(final String xpath) {
        this.xpath = xpath;
        return this;
    }

    public DeltaReportBuilder withSourceData(final Map<String, Serializable> sourceData) {
        this.sourceData = sourceData;
        return this;
    }

    public DeltaReportBuilder withTargetData(final Map<String, Serializable> targetData) {
        this.targetData = targetData;
        return this;
    }

    public DeltaReportBuilder actionCreate() {
        this.action = DeltaReport.CREATE_ACTION;
        return this;
    }

    public DeltaReportBuilder actionRemove() {
        this.action = DeltaReport.REMOVE_ACTION;
        return this;
    }

    public DeltaReportBuilder actionReplace() {
        this.action = DeltaReport.REPLACE_ACTION;
        return this;
    }

    /**
     * To create a single entry of {@link DeltaReport}.
     *
     * @return {@link DeltaReport}
     */
    public DeltaReport build() {
        final DeltaReport deltaReport = new DeltaReport();
        deltaReport.setAction(action);
        deltaReport.setXpath(xpath);
        if (sourceData != null && !sourceData.isEmpty()) {
            deltaReport.setSourceData(sourceData);
        }

        if (targetData != null && !targetData.isEmpty()) {
            deltaReport.setTargetData(targetData);
        }
        return deltaReport;
    }

    /**
     * To create a grouped entry of {@link DeltaReport}.
     *
     * @return {@link DeltaReport}
     */
    public DeltaReport buildGrouped() {
        final DeltaReport deltaReport = new DeltaReport();
        deltaReport.setAction(action);
        deltaReport.setXpath(xpath);
        if (groupedSourceData != null && !groupedSourceData.isEmpty()) {
            deltaReport.setGroupedSourceData(groupedSourceData);
        }

        if (groupedTargetData != null && !groupedTargetData.isEmpty()) {
            deltaReport.setGroupedTargetData(groupedTargetData);
        }
        return deltaReport;
    }

    public DeltaReportBuilder withGroupedTargetData(Collection<Map<String, Object>> groupedTargetData) {
        this.groupedTargetData = groupedTargetData;
        return this;
    }

    public DeltaReportBuilder withGroupedSourceData(Collection<Map<String, Object>> groupedSourceData) {
        this.groupedSourceData = groupedSourceData;
        return this;
    }
}
