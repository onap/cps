/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.datajobs.subscription.ncmp

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataJob
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataJobSubscriptionOperationInEvent
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.Event
import org.onap.cps.ncmp.impl.utils.JexParser
import org.onap.cps.ncmp.utils.TestUtils
import spock.lang.Specification

class NcmpInEventConsumerSpec extends Specification {

    def objectMapper = new ObjectMapper()

    def mockCmSubscriptionHandler = Mock(CmSubscriptionHandlerImpl)
    def objectUnderTest = new NcmpInEventConsumer(mockCmSubscriptionHandler)

    def 'Consuming CREATE cm data job subscription request.'() {
        given: 'a JSON file for create event'
            def jsonData = TestUtils.getResourceFileContent(
                'datajobs/subscription/cmNotificationSubscriptionNcmpInEvent.json')
            def myEventType = "dataJobCreated"
            jsonData = jsonData.replace('#myEventType', myEventType)
        and: 'the event'
            def event = objectMapper.readValue(jsonData, DataJobSubscriptionOperationInEvent)
        and: 'the list of data node selectors'
            def dataNodeSelectorList = getDataNodeSelectorsAsXpaths(event)
        and: 'the other data job event attributes'
            def dataSelector = getDataSelector(event)
        when: 'the event is consumed'
            objectUnderTest.consumeSubscriptionEvent(event)
        then: 'subscription create request is called'
            1 * mockCmSubscriptionHandler.createSubscription(dataSelector, "myDataJobId", dataNodeSelectorList)
    }

    def 'Consuming DELETE cm data job subscription request.'() {
        given: 'a JSON file for delete event'
            def jsonData = TestUtils.getResourceFileContent(
                    'datajobs/subscription/cmNotificationSubscriptionNcmpInEvent.json')
            def myEventType = "dataJobDeleted"
            jsonData = jsonData.replace('#myEventType', myEventType)
        and: 'the event'
            def event = objectMapper.readValue(jsonData, DataJobSubscriptionOperationInEvent)
        when: 'the event is consumed'
            objectUnderTest.consumeSubscriptionEvent(event)
        then: 'subscription delete request is called'
            1 * mockCmSubscriptionHandler.deleteSubscription("myDataJobId")
    }

    def 'Consuming subscription request with unknown event type.'() {
        given: 'a subscription event with invalid event type'
            def event = new DataJobSubscriptionOperationInEvent()
            event.setEvent(new Event())
            event.setEventType('invalidEventType')
            event.getEvent().setDataJob(new DataJob())
            event.getEvent().getDataJob().setId('someId')
        when: 'the event is consumed'
            objectUnderTest.consumeSubscriptionEvent(event)
        then: 'no error thrown'
            noExceptionThrown()
        and: 'request was not delegated to be handled as CREATE or DELETE'
            0 * mockCmSubscriptionHandler.deleteSubscription(_)
            0 * mockCmSubscriptionHandler.createSubscription(_)
    }

    def getDataNodeSelectorsAsXpaths(event) {
        return JexParser.toXpaths(event.event.dataJob.productionJobDefinition.targetSelector.dataNodeSelector)
    }

    def getDataSelector(event) {
        return event.event.dataJob.productionJobDefinition.dataSelector
    }
}
