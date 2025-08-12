/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd
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
import org.w3c.dom.DOMException
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
            scenario                                  | deltaReports || expectedXmlOutput
                    'Empty deltaReports list'         | []           || '<deltaReports/>'
                    'delta report with Replace action' | [new DeltaReport(action: 'replace', xpath: '/bookstore', sourceData: ['bookstore-name': 'Easons-1'], targetData: ['bookstore-name': 'Crossword Bookstores'])] || '<deltaReports><deltaReport id="1"><action>replace</action><xpath>/bookstore</xpath><source-data><bookstore-name>Easons-1</bookstore-name></source-data><target-data><bookstore-name>Crossword Bookstores</bookstore-name></target-data></deltaReport></deltaReports>'
                    'delta report with Delete action'  | [new DeltaReport(action: 'Delete', xpath: "/bookstore/categories[@code=5]/books[@title= Book-1]", sourceData: ['price': '1'], targetData: null)] || '<deltaReports><deltaReport id="1"><action>Delete</action><xpath>/bookstore/categories[@code=5]/books[@title= Book-1]</xpath><source-data><price>1</price></source-data></deltaReport></deltaReports>'
                    'delta report with Add action'     | [new DeltaReport(action: 'Add', xpath: "/bookstore/categories[@code=5]/books[@title= Book-1]", sourceData: null, targetData: ['price': '1'])] || '<deltaReports><deltaReport id="1"><action>Add</action><xpath>/bookstore/categories[@code=5]/books[@title= Book-1]</xpath><target-data><price>1</price></target-data></deltaReport></deltaReports>'
    }

    def 'Converting delta report objects to XML'() {
        given: 'A delta report with nested collections in sourceData and targetData'
            def deltaReports = [
                new DeltaReport(
                        action: 'Update',
                        xpath: '/bookstore',
                        sourceData: ['books': ['Book-1', 'Book-2', 'Book-3']],
                        targetData: ['authors': ['Author-1', 'Author-2']]
                )
        ]
        when: 'deltaReports are converted to XML'
            def result = buildXml(deltaReports)
        then: 'the result contains the expected XML with nested collection values as comma-separated strings'
            assert result == '<deltaReports><deltaReport id="1"><action>Update</action><xpath>/bookstore</xpath><source-data><books>Book-1, Book-2, Book-3</books></source-data><target-data><authors>Author-1, Author-2</authors></target-data></deltaReport></deltaReports>'
    }

    def 'Converting delta report to XML with document syntax error'() {
        given: 'A list of maps with an invalid entry'
           def deltaReport = [new DeltaReport(action: 'replace', xpath: '/bookstore', sourceData: ['bookstore-name': 'Easons-1'], targetData: ['invalid<tag>': 'value'])]
        when: 'convert the delta report to XML'
               buildXml(deltaReport)
        then: 'a validation exception is thrown'
           def exception = thrown(DataValidationException)
        and: 'the cause is a document object model exception'
           assert exception.cause instanceof DOMException
    }
}
