/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
*/

package org.onap.cps.event.schema

import spock.lang.Specification

/**
 * Test class for CpsDataUpdatedEvent.
 */
class CpsDataUpdatedEventSpec extends Specification {

    def anId = 'an-id'
    def aTimestamp = 'a-timestamp'
    def aDataName = 'a-data-name'
    def aDataValue = 'a-data-value'

    def objectUnderTest

    def 'Event object generation and availability'() {
        when: 'An event object is created'
            objectUnderTest =
                    new CpsDataUpdatedEvent()
                            .withId(anId)
                            .withContent(
                                    new Content()
                                            .withTimestamp(aTimestamp)
                                            .withData(new Data().withAdditionalProperty(aDataName, aDataValue)))
        then: 'The event object keeps event information'
            objectUnderTest.getId() == anId
            objectUnderTest.getContent().getTimestamp() == aTimestamp
            objectUnderTest.getContent().getData().getAdditionalProperties().get(aDataName) == aDataValue
    }

}