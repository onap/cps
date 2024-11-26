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

package org.onap.cps.spi.model

import org.onap.cps.api.model.DeltaReportBuilder
import spock.lang.Specification

class DeltaReportBuilderSpec extends Specification{

    def 'Generating delta report for "create" action'() {
        when: 'delta report is generated'
            def result = new DeltaReportBuilder()
                    .actionCreate()
                    .withXpath('/xpath')
                    .withTargetData(['data':'leaf-data'])
                    .build()
        then: 'the delta report contains the "create" action with expected target data'
            assert result.action == 'create'
            assert result.xpath == '/xpath'
            assert result.targetData == ['data': 'leaf-data']
    }

    def 'Generating delta report with attributes for "remove" action'() {
        when: 'delta report is generated'
            def result = new DeltaReportBuilder()
                    .actionRemove()
                    .withXpath('/xpath')
                    .withSourceData(['data':'leaf-data'])
                    .build()
        then: 'the delta report contains the "remove" action with expected source data'
            assert result.action == 'remove'
            assert result.xpath == '/xpath'
            assert result.sourceData == ['data': 'leaf-data']
    }
}
