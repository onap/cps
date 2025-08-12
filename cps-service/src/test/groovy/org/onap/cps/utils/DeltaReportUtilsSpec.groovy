/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG.
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
package org.onap.cps.utils

import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.utils.deltareport.DeltaReportUtils
import org.onap.cps.utils.deltareport.XmlObjectMapper
import spock.lang.Specification
import org.onap.cps.api.model.DeltaReport

class DeltaReportUtilsSpec extends Specification {

    def deltaReportUtils = new DeltaReportUtils(new XmlObjectMapper())
    def 'Converting delta report to XML scenario'() {
        when: 'deltaReports are converted to XML'
            def result = deltaReportUtils.buildXml(deltaReports)
        then: 'the result contains the expected XML'
            assert result == expectedResult
        where:
            scenario                           | deltaReports                                                                                                                                            || expectedResult
            'Empty delta report'               | []                                                                                                                                                      ||'<deltaReports/>'
            'delta report with replace action' | [new DeltaReport(action:'replace', xpath:'/bookstore',sourceData: ['bookstore-name':'Easons-1'],targetData: ['bookstore-name':'Crossword Bookstores'])] ||'<deltaReports><deltaReport><action>replace</action><xpath>/bookstore</xpath><sourceData><bookstore-name>Easons-1</bookstore-name></sourceData><targetData><bookstore-name>Crossword Bookstores</bookstore-name></targetData></deltaReport></deltaReports>'
    }

    def 'Converting invalid delta report to xml'() {
        given: 'syntactically incorrect delta report'
            def deltaReports = [new DeltaReport(action:'replace', xpath:'/bookstore', sourceData: ['bookstore-name':'Easons-1'],targetData: ['bad': { -> "value" }])]
        when: 'attempt to build XML'
        deltaReportUtils.buildXml(deltaReports)
        then: 'excepted exception is thrown'
            def thrownException = thrown(DataValidationException)
        and: 'the exception has correct message'
            thrownException.message =='Data Validation Failed'
    }
}