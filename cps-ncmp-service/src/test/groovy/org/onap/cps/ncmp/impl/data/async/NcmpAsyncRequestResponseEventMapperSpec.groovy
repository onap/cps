/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.impl.data.async

import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.event.model.ncmp.async_m2m.DmiAsyncRequestResponseEvent
import org.onap.cps.ncmp.event.model.ncmp.async_m2m.EventContent
import org.onap.cps.ncmp.event.model.ncmp.async_m2m.NcmpAsyncRequestResponseEvent
import org.onap.cps.ncmp.event.model.ncmp.async_m2m.ResponseData
import spock.lang.Specification

class NcmpAsyncRequestResponseEventMapperSpec extends Specification {

    def objectUnderTest = Mappers.getMapper(NcmpAsyncRequestResponseEventMapper.class)

    def 'Convert dmi async request response event to ncmp async request response event'() {
        given: 'a dmi async request response event'
            def dmiAsyncRequestResponseEvent = new DmiAsyncRequestResponseEvent()
                    .withEventCorrelationId("correlation-id-123").withEventContent(new EventContent()
                    .withResponseData(new ResponseData()))
        and: 'the event Id and time are empty'
            dmiAsyncRequestResponseEvent.withEventId('').withEventTime('')
        when: 'mapper is called'
            def result = objectUnderTest.toNcmpAsyncEvent(dmiAsyncRequestResponseEvent)
        then: 'result is of the correct type'
            assert result.class == NcmpAsyncRequestResponseEvent.class
        and: 'eventId and eventTime should be overridden by custom method with non-empty values'
            assert result.eventId != ''
            assert result.eventTime != ''
        and: 'target eventCorrelationId of mapped object should be same as source eventCorrelationId'
            assert result.eventCorrelationId == "correlation-id-123"
    }

    def 'Dmi async request response event is mapped correctly to forwarded event'() {
        given: 'a dmi async request response event'
            def dmiAsyncRequestResponseEvent = new DmiAsyncRequestResponseEvent()
                    .withEventContent(new EventContent().withResponseCode('200')
                            .withResponseData(new ResponseData().withAdditionalProperty('property1', 'value1')
                                    .withAdditionalProperty('property2', 'value2')))
        when: 'mapper is called'
            def result = objectUnderTest.toNcmpAsyncEvent(dmiAsyncRequestResponseEvent)
        then: 'result is of the correct type'
            assert result.class == NcmpAsyncRequestResponseEvent.class
        and: 'forwarded event content response code is mapped correctly'
            assert result.forwardedEvent.responseCode == '200'
        and: 'after mapping additional properties should be stored'
            result.forwardedEvent.additionalProperties.'response-data' == ['property2': 'value2', 'property1': 'value1']
    }
}
