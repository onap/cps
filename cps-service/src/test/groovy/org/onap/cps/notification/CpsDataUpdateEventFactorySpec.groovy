/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
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

package org.onap.cps.notification

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import org.onap.cps.utils.DateTimeUtility
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.event.model.CpsDataUpdatedEvent
import org.onap.cps.event.model.Data
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.util.StringUtils
import spock.lang.Specification

class CpsDataUpdateEventFactorySpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAdminService = Mock(CpsAdminService)

    def objectUnderTest = new CpsDataUpdatedEventFactory(mockCpsDataService, mockCpsAdminService)

    def myDataspaceName = 'my-dataspace'
    def myAnchorName = 'my-anchorname'
    def mySchemasetName = 'my-schemaset-name'
    def dateTimeFormat = 'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ'

    def 'Create a CPS data updated event successfully: #scenario'() {

        given: 'cps admin service is able to return anchor details'
            mockCpsAdminService.getAnchor(myDataspaceName, myAnchorName) >>
                new Anchor(myAnchorName, myDataspaceName, mySchemasetName)
        and: 'cps data service returns the data node details'
            def xpath = '/'
            def dataNode = new DataNodeBuilder().withXpath(xpath).withLeaves(['leafName': 'leafValue']).build()
            mockCpsDataService.getDataNode(
                myDataspaceName, myAnchorName, xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode

        when: 'CPS data updated event is created'
            def cpsDataUpdatedEvent = objectUnderTest.createCpsDataUpdatedEvent(myDataspaceName,
                myAnchorName, DateTimeUtility.toOffsetDateTime(inputObservedTimestamp))

        then: 'CPS data updated event is created with expected values'
            with(cpsDataUpdatedEvent) {
                type == 'org.onap.cps.data-updated-event'
                source == new URI('urn:cps:org.onap.cps')
                schema == CpsDataUpdatedEvent.Schema.URN_CPS_ORG_ONAP_CPS_DATA_UPDATED_EVENT_SCHEMA_1_1_0_SNAPSHOT
                StringUtils.hasText(id)
                content != null
            }
            with(cpsDataUpdatedEvent.content) {
                assert isExpectedDateTimeFormat(observedTimestamp): "$observedTimestamp is not in $dateTimeFormat format"
                if (inputObservedTimestamp != null)
                    observedTimestamp == inputObservedTimestamp
                else
                    OffsetDateTime.now().minusMinutes(2).isBefore(
                        DateTimeUtility.toOffsetDateTime(observedTimestamp))
                anchorName == myAnchorName
                dataspaceName == myDataspaceName
                schemaSetName == mySchemasetName
                data == new Data().withAdditionalProperty('leafName', 'leafValue')
            }
        where:
            scenario                     | inputObservedTimestamp
            'without observed timestamp' | null
            'with observed timestamp'    | '2021-01-01T23:00:00.345-0200'
    }

    def isExpectedDateTimeFormat(String observedTimestamp) {
        try {
            DateTimeFormatter.ofPattern(dateTimeFormat).parse(observedTimestamp)
        } catch (DateTimeParseException) {
            return false
        }
        return true
    }

}
