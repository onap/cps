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
        when:'deltaReports are converted to XML'
             def result = buildXml(deltaReports)
        then:'the result contains the expected XML'
             assert result == expectedXmlOutput
        where:
             scenario                          | deltaReports                                                                                                                                            || expectedXmlOutput
            'Empty deltaReports list'          | []                                                                                                                                                      ||'<deltaReports/>'
            'delta report with replace action' | [new DeltaReport(action:'replace', xpath:'/bookstore',sourceData: ['bookstore-name':'Easons-1'],targetData: ['bookstore-name':'Crossword Bookstores'])] ||'<deltaReports><deltaReport id="1"><action>replace</action><xpath>/bookstore</xpath><source-data><bookstore-name>Easons-1</bookstore-name></source-data><target-data><bookstore-name>Crossword Bookstores</bookstore-name></target-data></deltaReport></deltaReports>'
            'delta report with remove action'  | [new DeltaReport(action:'remove', xpath:'/bookstore/categories[@code=5]/books[@title= Book-1]',sourceData: ['price':'1'], targetData: null)]            ||'<deltaReports><deltaReport id="1"><action>remove</action><xpath>/bookstore/categories[@code=5]/books[@title= Book-1]</xpath><source-data><price>1</price></source-data></deltaReport></deltaReports>'
            'delta report with create action'  | [new DeltaReport(action:'create', xpath:'/bookstore/categories[@code=5]/books[@title= Book-1]', sourceData: null, targetData: ['price':'1'])]           ||'<deltaReports><deltaReport id="1"><action>create</action><xpath>/bookstore/categories[@code=5]/books[@title= Book-1]</xpath><target-data><price>1</price></target-data></deltaReport></deltaReports>'
    }

    def 'Converting delta report objects to XML with nested collections'() {
        given:'A delta report with nested collections in sourceData and targetData'
             def deltaReports = [new DeltaReport(action:'replace', xpath:'/bookstore', sourceData: ['books': ['Book-1','Book-2','Book-3']], targetData: ['authors': ['Author-1','Author-2']])]
        when:'deltaReports are converted to XML'
            def result = buildXml(deltaReports)
        then:'the result contains the expected XML with nested collection values as comma-separated strings'
            assert result =='<deltaReports><deltaReport id="1"><action>replace</action><xpath>/bookstore</xpath><source-data><books>Book-1, Book-2, Book-3</books></source-data><target-data><authors>Author-1, Author-2</authors></target-data></deltaReport></deltaReports>'
    }

    def 'Converting delta report to XML with document syntax error'() {
        given:'A list of maps with an invalid entry'
            def deltaReports = [
                new DeltaReport(action:'replace', xpath:'/bookstore',
                        sourceData: ['bookstore-name':'Easons-1'],
                        targetData: ['invalid<tag>':'value'])
        ]
        when:'convert the delta report to XML'
            buildXml(deltaReports)
        then:'a validation exception is thrown'
            def exception = thrown(DataValidationException)
        and:'the cause is a document object model exception'
            exception.message =='Data Validation Failed'
    }

    def 'BuildXml converts delta reports with nested collection in targetData'() {
        given:'A delta report with a nested collection in targetData'
            def deltaReports = [new DeltaReport(action:'replace',xpath:'/bookstore',targetData: ['authors': ['Author-1']])]
        when:'deltaReports are converted to XML'
            def result = buildXml(deltaReports)
        then:'the result contains the expected escaped XML'
            assert result =='<deltaReports><deltaReport id="1"><action>replace</action><xpath>/bookstore</xpath><target-data><authors>Author-1</authors></target-data></deltaReport></deltaReports>'
    }

    def 'BuildXml converts delta reports with nested map in sourceData'() {
        given:'A delta report with nested map values in sourceData '
            def deltaReports = [new DeltaReport(action:'replace',xpath:'/bookstore',sourceData: ['details': ['name':'Bookstore','price':'500',address : [line1: 'Main Rd',line2: 'Sector 10']],],targetData: [:])]
        when:'deltaReports are converted to XML'
            def result = buildXml(deltaReports)
        then:'Nested elements are produced for map entries'
            assert result =='<deltaReports><deltaReport id="1"><action>replace</action><xpath>/bookstore</xpath><source-data><details><name>Bookstore</name><price>500</price><address><line1>Main Rd</line1><line2>Sector 10</line2></address></details></source-data></deltaReport></deltaReports>'
    }

    def 'BuildXml converts delta reports with collection of maps in sourceData'() {
        given:'A delta report with nested map values'
            def deltaReports = [new DeltaReport(action:'replace',xpath:'/bookstore',sourceData: ['details': [['name':'Bookstore'],['code':'1']]],targetData: ['authors': [['Author-1','Author-2'],['Author-1','Author-2']]])];
        when:'deltaReports are converted to XML'
            def result = buildXml(deltaReports)
        then:'Nested elements are produced for map entries'
            assert result =='<deltaReports><deltaReport id="1"><action>replace</action><xpath>/bookstore</xpath><source-data><details><name>Bookstore</name><code>1</code></details></source-data><target-data><authors>[Author-1, Author-2], [Author-1, Author-2]</authors></target-data></deltaReport></deltaReports>'
    }

    def 'lambda in appendJoinedScalarCollection maps null elements to empty string'() {
        given:'A delta report whose sourceData contains a scalar collection with a null'
            def deltaReports = [new DeltaReport(action: 'replace', xpath: '/bookstore', sourceData: ['details': ['A', null, 'B']],targetData: [:])]
        when:'We build XML (containsMap=false path → appendJoinedScalarCollection)'
            def result = buildXml(deltaReports)
        then:'Joined output contains empty segment for null'
            assert result == '<deltaReports><deltaReport id="1"><action>replace</action><xpath>/bookstore</xpath><source-data><details>A, , B</details></source-data></deltaReport></deltaReports>'
    }
}