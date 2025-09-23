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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataJobSubscriptionOperationInEvent
import org.onap.cps.ncmp.impl.utils.JexParser
import org.onap.cps.ncmp.utils.TestUtils
import org.slf4j.LoggerFactory
import spock.lang.Specification

class NcmpInEventConsumerSpec extends Specification {

    def logger = new ListAppender<ILoggingEvent>()
    def objectMapper = new ObjectMapper()

    def mockCmSubscriptionHandler = Mock(CmSubscriptionHandlerImpl)
    def objectUnderTest = new NcmpInEventConsumer(mockCmSubscriptionHandler)

    void setup() {
        ((Logger) LoggerFactory.getLogger(NcmpInEventConsumer.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(NcmpInEventConsumer.class)).detachAndStopAllAppenders()
    }

    def 'Consuming CREATE cm data job subscription request.'() {
        given: 'a JSON file for create event'
            def jsonData = TestUtils.getResourceFileContent(
                'datajobs/subscription/cmNotificationSubscriptionNcmpInEvent.json')
            def myEventType = "CREATE"
            jsonData = jsonData.replace('#myEventType', myEventType)
        and: 'the event'
            def event = objectMapper.readValue(jsonData, DataJobSubscriptionOperationInEvent)
        and: 'the list of data node selectors'
            def dataNodeSelectorList = getDataNodeSelectorsAsXpaths(event)
        and: 'the other data job event attributes'
            def dataSelector = getDataSelector(event)
        when: 'the event is consumed'
            objectUnderTest.consumeSubscriptionEvent(event)
        then: 'event details are logged at level INFO'
            def loggingEvent = logger.list.last()
            assert loggingEvent.level == Level.INFO
            assert loggingEvent.formattedMessage.contains('dataJobId=myDataJobId')
            assert loggingEvent.formattedMessage.contains("eventType=${myEventType}")
        and: 'method to handle process subscription create request is called'
            1 * mockCmSubscriptionHandler.processSubscriptionCreate(dataSelector, "myDataJobId", dataNodeSelectorList)
    }

    def 'Consuming DELETE cm data job subscription request.'() {
        given: 'a JSON file for delete event'
            def jsonData = TestUtils.getResourceFileContent(
                    'datajobs/subscription/cmNotificationSubscriptionNcmpInEvent.json')
            def myEventType = "DELETE"
            jsonData = jsonData.replace('#myEventType', myEventType)
        and: 'the event'
            def event = objectMapper.readValue(jsonData, DataJobSubscriptionOperationInEvent)
        when: 'the event is consumed'
            objectUnderTest.consumeSubscriptionEvent(event)
        then: 'event details are logged at level INFO'
            def loggingEvent = logger.list.last()
            assert loggingEvent.level == Level.INFO
            assert loggingEvent.formattedMessage.contains('dataJobId=myDataJobId')
            assert loggingEvent.formattedMessage.contains("eventType=${myEventType}")
        and: 'method to handle process subscription delete request is called'
            1 * mockCmSubscriptionHandler.processSubscriptionDelete("myDataJobId")
    }

    def getDataNodeSelectorsAsXpaths(event) {
        return JexParser.toXpaths(event.event.dataJob.productionJobDefinition.targetSelector.dataNodeSelector)
    }

    def getDataSelector(event) {
        return event.event.dataJob.productionJobDefinition.dataSelector
    }
}
