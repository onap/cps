/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024-2025 Nordix Foundation.
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.DataJobSubscriptionOperationInEvent
import org.onap.cps.ncmp.impl.utils.JexParser
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class NcmpInEventConsumerSpec extends Specification {

    def objectUnderTest = new NcmpInEventConsumer()
    def logger = new ListAppender<ILoggingEvent>()

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    void setup() {
        ((Logger) LoggerFactory.getLogger(NcmpInEventConsumer.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(NcmpInEventConsumer.class)).detachAndStopAllAppenders()
    }

    def 'Consuming CM Data Notification #scenario datatype id.'() {
        given: 'a JSON file containing a subscription event'
            def jsonData = TestUtils.getResourceFileContent('sample_dataJobSubscriptionInEvent.json')
            if (dataTypeId == null) {
                jsonData = jsonData.replace("\"${'$'}{dataTypeId}\"", "null")
            } else {
                jsonData = jsonData.replace("\${dataTypeId}", dataTypeId)
            }
            def event = objectMapper.readValue(jsonData, DataJobSubscriptionOperationInEvent)
            def fdns = JexParser.extractFdnsFromLocationPaths("my data node selector")
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEvent(event)
        then: 'an event is logged with level INFO'
            def loggingEvent = logger.list.last()
            loggingEvent.level == Level.INFO
            loggingEvent.formattedMessage.contains("my job id")
            loggingEvent.formattedMessage.contains("my event type")
            loggingEvent.formattedMessage.contains("dataType=${dataTypeId}")
            loggingEvent.formattedMessage.contains("fdns=${fdns}")
        where: 'the following data type ids are used'
            scenario               | dataTypeId
            'with data type id'    | 'my data type'
            'without data type id' | null
    }
}
