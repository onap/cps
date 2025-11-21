/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobReadRequest
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.ReadOperation
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import spock.lang.Specification

class DataJobServiceImplSpec extends Specification {

    def mockWriteRequestExaminer = Mock(WriteRequestExaminer)
    def mockDmiSubJobRequestHandler = Mock(DmiSubJobRequestHandler)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)

    def objectUnderTest = new DataJobServiceImpl(mockDmiSubJobRequestHandler, mockWriteRequestExaminer, mockJsonObjectMapper)

    def myDataJobMetadata = new DataJobMetadata('', '', '','')
    def authorization = 'my authorization header'

    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        setupLogger(Level.DEBUG)
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(DataJobServiceImpl.class)).detachAndStopAllAppenders()
    }

    def 'Read data job request.'() {
        when: 'read data job request is processed'
            def readOperation = new ReadOperation('', '', '', [], [], '', '', 1)
            objectUnderTest.readDataJob(authorization, 'my-job-id', myDataJobMetadata, new DataJobReadRequest([readOperation]))
        then: 'the data job id is correctly logged'
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.INFO
            assert loggingEvent.formattedMessage.contains('Data Job ID: my-job-id')
    }

    def 'Write data-job request and verify logging when info enabled.'() {
        given: 'data job metadata and write request'
            def dataJobWriteRequest = new DataJobWriteRequest([new WriteOperation('', '', '', null)])
        and: 'a map of producer key and DMI 3GPP write operations'
            def dmiWriteOperationsPerProducerKey = [:]
        and: 'mocking the splitDmiWriteOperationsFromRequest method to return the expected data'
            mockWriteRequestExaminer.splitDmiWriteOperationsFromRequest(_, _) >> dmiWriteOperationsPerProducerKey
        and: 'mocking the sendRequestsToDmi method to simulate empty sub-job responses from the DMI request handler'
            mockDmiSubJobRequestHandler.sendRequestsToDmi(authorization, 'my-job-id', myDataJobMetadata, dmiWriteOperationsPerProducerKey) >> []
        when: 'the write data job request is processed'
            objectUnderTest.writeDataJob(authorization, 'my-job-id', myDataJobMetadata, dataJobWriteRequest)
        then: 'the examiner service is called and a map is returned'
            1 * mockWriteRequestExaminer.splitDmiWriteOperationsFromRequest('my-job-id', dataJobWriteRequest) >> dmiWriteOperationsPerProducerKey
        and: 'write operation details are logged at debug level'
            with(logger.list.find { it.level == Level.DEBUG }) {
                assert it.formattedMessage.contains("Initiating WRITE operation for Data Job ID: my-job-id")
            }
        and: 'number of operations are logged at info level'
            with(logger.list.find { it.level == Level.INFO }) {
                assert it.formattedMessage.contains("Data Job ID: my-job-id - Total operations received: 1")
            }
    }

    def setupLogger(Level level) {
        def setupLogger = ((Logger) LoggerFactory.getLogger(DataJobServiceImpl.class))
        setupLogger.setLevel(level)
        setupLogger.addAppender(logger)
        logger.start()
    }
}
