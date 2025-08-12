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
import spock.lang.Specification
import org.onap.cps.api.model.DeltaReport
import static org.onap.cps.utils.deltareport.DeltaReportUtils.buildXml

class DeltaReportUtilsSpec extends Specification {

    def 'Converting delta report to XML scenario'() {
        when: 'deltaReports are converted to XML'
            def result = buildXml(deltaReports)
        then: 'the result contains the expected XML'
            assert result == expectedXmlOutput
        where:
            scenario                           | deltaReports                                                                                                                                            || expectedXmlOutput
            'Empty deltaReports list'          | []                                                                                                                                                      ||'<deltaReports/>'
            'delta report with replace action' | [new DeltaReport(action:'replace', xpath:'/bookstore',sourceData: ['bookstore-name':'Easons-1'],targetData: ['bookstore-name':'Crossword Bookstores'])] ||'<deltaReports><deltaReport><action>replace</action><xpath>/bookstore</xpath><sourceData><bookstore-name>Easons-1</bookstore-name></sourceData><targetData><bookstore-name>Crossword Bookstores</bookstore-name></targetData></deltaReport></deltaReports>'
            'delta report with remove action'  | [new DeltaReport(action:'remove', xpath:'/bookstore/categories[@code=5]/books[@title= Book-1]',sourceData: ['price':'1'], targetData: null)]            ||'<deltaReports><deltaReport><action>remove</action><xpath>/bookstore/categories[@code=5]/books[@title= Book-1]</xpath><sourceData><price>1</price></sourceData></deltaReport></deltaReports>'
            'delta report with create action'  | [new DeltaReport(action:'create', xpath:'/bookstore/categories[@code=5]/books[@title= Book-1]', sourceData: null, targetData: ['price':'1'])]           ||'<deltaReports><deltaReport><action>create</action><xpath>/bookstore/categories[@code=5]/books[@title= Book-1]</xpath><targetData><price>1</price></targetData></deltaReport></deltaReports>'
    }

    def 'Converting delta report to XML with document syntax error'() {
        given: 'deltaReports with an invalid entry'
            def deltaReports = [new DeltaReport(action:'replace', xpath:'/bookstore', sourceData: ['bookstore-name':'Easons-1'],targetData: ['bad': { -> "value" }])]
        when: 'convert the deltaReports to XML'
            buildXml(deltaReports)
        then: 'Excepted exception is thrown'
            def thrownException = thrown(DataValidationException)
        and: 'the cause is a document object model exception'
            thrownException.message =='Data Validation Failed'
    }
}