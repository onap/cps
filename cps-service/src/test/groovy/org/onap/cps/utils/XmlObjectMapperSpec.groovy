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

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.DeltaReport
import org.onap.cps.impl.DeltaReportBuilder
import org.onap.cps.utils.deltareport.DeltaReportWrapper
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectWriter
import spock.lang.Specification

class XmlObjectMapperSpec extends Specification {

    def spiedXmlMapper = Spy(XmlMapper)
    def xmlObjectMapper = new XmlObjectMapper(spiedXmlMapper)

    def 'Converting valid delta report to xml'() {
        given: 'a valid delta report'
            def expectedResult='<deltaReports><deltaReport><action>replace</action><xpath>some xpath</xpath><sourceData><some_key>some value</some_key></sourceData><targetData><some_key>some value</some_key></targetData></deltaReport></deltaReports>'
            def xpath = 'some xpath'
            def deltaReport = new DeltaReportBuilder(action:'replace', xpath:'/bookstore',sourceData: ['bookstore-name':'Easons-1'],targetData: ['bookstore-name':'Crossword Bookstores'])
                .actionReplace()
                .withXpath(xpath)
                .withSourceData(['some_key': 'some value'])
                .withTargetData(['some_key': 'some value'])
                .build()
            def deltaReportWrapper = new DeltaReportWrapper<DeltaReport>([deltaReport])
        when: 'attempt to build XML'
            def result = xmlObjectMapper.asXmlString(deltaReportWrapper, 'deltaReports')
        then: 'the result is as expected'
            assert result == expectedResult
    }

    def 'Default constructor creates XmlMapper'() {
        given: 'an XmlObjectMapper created using the no-arg constructor'
            def xmlObjectMapper = new XmlObjectMapper()
            def expectedResult =  '<deltaReports><action>replace</action><sourceData>some value</sourceData><targetData>some value</targetData></deltaReports>'
        and: 'a simple object to serialize'
            def deltaReport = [action: 'replace', sourceData: 'some value',targetData :'some value']
        when: 'attempt to build XML'
            def result = xmlObjectMapper.asXmlString(deltaReport, 'deltaReports')
        then: 'we get non-empty XML with the chosen root element'
            assert result == expectedResult
    }

    def 'Converting invalid delta report to xml.'() {
        given: 'syntactically incorrect delta report'
            def deltaReport = []
            def deltaReportWrapper = new DeltaReportWrapper<>([deltaReport])
        and: 'the XmlMapper chain throws an exception'
            def writer = Mock(ObjectWriter)
            spiedXmlMapper.writer() >> writer
            writer.withRootName('deltaReports') >> writer
            writer.writeValueAsString(_) >> { throw new Exception() }
        when: 'attempt to build XML'
            xmlObjectMapper.asXmlString(deltaReportWrapper, 'deltaReports')
        then: 'the exception has correct message'
            def thrownException = thrown(DataValidationException)
            thrownException.message == 'Data Validation Failed'
    }
}

