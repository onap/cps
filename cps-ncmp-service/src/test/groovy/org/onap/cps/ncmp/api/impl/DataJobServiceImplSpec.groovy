/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import org.onap.cps.ncmp.api.models.datajob.DataJobReadRequest
import org.onap.cps.ncmp.api.models.datajob.DataJobWriteRequest
import org.onap.cps.ncmp.api.models.datajob.DataJobMetadata
import org.onap.cps.ncmp.api.models.datajob.ReadOperation
import org.onap.cps.ncmp.api.models.datajob.WriteOperation
import spock.lang.Specification

class DataJobServiceImplSpec extends Specification{

    def objectUnderTest = new DataJobServiceImpl()

    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        setupLogger()
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(DataJobServiceImpl.class)).detachAndStopAllAppenders()
    }

    def '#operation data job request.'() {
        given: 'data job metadata'
            def dataJobMetadata = new DataJobMetadata('client-topic', 'application/vnd.3gpp.object-tree-hierarchical+json', 'application/3gpp-json-patch+json')
        when: 'read/write data job request is processed'
            if (operation == 'read') {
                objectUnderTest.readDataJob('some-job-id', dataJobMetadata, new DataJobReadRequest([getWriteOrReadOperationRequest(operation)]))
            } else {
                objectUnderTest.writeDataJob('some-job-id', dataJobMetadata, new DataJobWriteRequest([getWriteOrReadOperationRequest(operation)]))
            }
        then: 'the data job id is correctly logged'
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.INFO
            assert loggingEvent.formattedMessage.contains('data job id for ' + operation + ' operation is: some-job-id')
        where: 'the following data job operations are used'
            operation << ['read', 'write']
    }

    def getWriteOrReadOperationRequest(operation) {
        if (operation == 'write') {
            return new WriteOperation('some/write/path', 'add', 'some-operation-id', 'some-value')
        }
        return new ReadOperation('some/read/path', 'read', 'some-operation-id', ['some-attrib-1'], ['some-field-1'], 'some-filter', 'some-scope-type', 1)
    }

    def setupLogger() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(DataJobServiceImpl.class))
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
    }
}
