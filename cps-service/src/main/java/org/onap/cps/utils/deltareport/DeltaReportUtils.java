/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG.
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

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.DeltaReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeltaReportUtils {

    @Autowired
    private final XmlObjectMapper xmlMapper;

    /**
     * Converts a collection of DeltaReport objects to XML format using XmlMapper.
     */

    public String buildXml(final List<DeltaReport> deltaReports) {
        try {
            return xmlMapper.toXml(new DeltaReportWrapper(deltaReports), "deltaReports");
        } catch (final Exception
                exception) {
            throw new DataValidationException("Data Validation Failed", "Failed to build XML deltaReport: "
                    + exception.getMessage(), exception);
        }
    }
}