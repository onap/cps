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
import org.onap.cps.ncmp.api.datajobs.models.DmiWriteOperation
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey
import org.onap.cps.ncmp.impl.datajobs.DataJobServiceImpl
import org.onap.cps.ncmp.impl.datajobs.DmiSubJobRequestHandler
import org.onap.cps.ncmp.impl.datajobs.WriteRequestExaminer
import org.slf4j.LoggerFactory
import org.onap.cps.ncmp.api.datajobs.models.DataJobReadRequest
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.ReadOperation
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import spock.lang.Specification

class DataJobServiceImplSpec extends Specification {

    def mockWriteOperationExaminer = Mock(WriteRequestExaminer)
    def mockDmiSubJobClient = Mock(DmiSubJobRequestHandler)
    def objectUnderTest = new DataJobServiceImpl(mockDmiSubJobClient, mockWriteOperationExaminer)
    def static singleWriteOperation = [new WriteOperation('fdn-1', 'add', 'some-operation-id', new Object())]

    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        setupLogger()
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(DataJobServiceImpl.class)).detachAndStopAllAppenders()
    }

    def 'Read data job request.'() {
        given: 'data job metadata'
            def dataJobMetadata = new DataJobMetadata('client-topic', 'application/vnd.3gpp.object-tree-hierarchical+json', 'application/3gpp-json-patch+json')
        when: 'read/write data job request is processed'
            objectUnderTest.readDataJob('some-job-id', dataJobMetadata, new DataJobReadRequest([getWriteOrReadOperationRequest('read')] as List<ReadOperation>))
        then: 'the data job id is correctly logged'
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.INFO
            assert loggingEvent.formattedMessage.contains('data job id for read operation is: some-job-id')
    }

    def 'Create sub-job request.'() {
        given: 'data job metadata'
            def dataJobMetadata = new DataJobMetadata('client-topic', 'application/vnd.3gpp.object-tree-hierarchical+json', 'application/3gpp-json-patch+json')
            def dataJobWriteRequest = new DataJobWriteRequest(singleWriteOperation as List<WriteOperation>)
        and: 'the examiner service returns a map of a producer key and DMI 3gpp write operation'
            def dmi3gppWriteOperation = new DmiWriteOperation('fdn-1', 'create', 'module-set-tag', new Object(), 'some-operation-id', ['dmi-service-name':'my-dmi-service'])
            def dmi3ggpWriteOperationsPerProducerKey = [new ProducerKey('my-dmi-service', 'my-data-producer-identifier') : [dmi3gppWriteOperation]] as HashMap
            mockWriteOperationExaminer.splitDmiWriteOperationsFromRequest(_, _) >> dmi3ggpWriteOperationsPerProducerKey
        when: 'read/write data job request is processed'
            objectUnderTest.writeDataJob('some-job-id', dataJobMetadata, dataJobWriteRequest)
        then: 'the data job id is correctly logged'
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.INFO
            assert loggingEvent.formattedMessage.contains('data job id for write operation is: some-job-id')
            1 * mockWriteOperationExaminer.splitDmiWriteOperationsFromRequest('some-job-id', dataJobWriteRequest)
            1 * mockDmiSubJobClient.sendRequestsToDmi('some-job-id', dataJobMetadata, dmi3ggpWriteOperationsPerProducerKey)
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
