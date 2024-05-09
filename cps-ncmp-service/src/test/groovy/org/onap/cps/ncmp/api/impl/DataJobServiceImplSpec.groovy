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
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse
import org.onap.cps.ncmp.api.impl.config.DmiProperties
import org.onap.cps.ncmp.api.impl.operations.OperationType
import org.onap.cps.ncmp.impl.datajobs.DataJobServiceImpl
import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.utils.AlternateIdMatcher
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.onap.cps.ncmp.api.datajobs.models.DataJobReadRequest
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.ReadOperation
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class DataJobServiceImplSpec extends Specification {

    def mockAlternateIdMatcher = Mock(AlternateIdMatcher)
    def mockDmiRestClient = Mock(DmiRestClient)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockDmiProperties = Mock(DmiProperties)
    def objectUnderTest = new DataJobServiceImpl(mockAlternateIdMatcher, mockDmiRestClient, mockDmiProperties, jsonObjectMapper)

    def NO_AUTH = null

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
    def 'Write data job request.'() {
        given: 'data job metadata'
            def dataJobMetadata = new DataJobMetadata('client-topic', 'application/vnd.3gpp.object-tree-hierarchical+json', 'application/3gpp-json-patch+json')
        and: 'the dmi base path returned'
            mockDmiProperties.getDmiBasePath() >> 'dmi'
        and: 'the inventory persistence returns a data node'
            mockAlternateIdMatcher.getCmHandleDataNodeByLongestMatchAlternateId('some/write/path', '/') >> new DataNode(leaves: [id: 'ch-1', 'dmi-service-name': 'my-dmi-service-name', 'data-producer-identifier': 'my-data-producer-identifier'])
        and: 'the DMI Plugin returns a response'
            def response = new ResponseEntity<>(new SubJobWriteResponse('my-sub-job-id', 'my-dmi-service-name', 'my-data-producer-identifier'), HttpStatus.OK)
            def expectedJson = '[{"dataAcceptType":"application/vnd.3gpp.object-tree-hierarchical+json","dataContentType":"application/3gpp-json-patch+json","dataProducerId":"some-job-id","data":[{"path":"some/write/path","op":"add","moduleSetTag":"some-operation-id","value":"some-value","operationId":null,"privateProperties":{}}]},{"dataAcceptType":"application/vnd.3gpp.object-tree-hierarchical+json","dataContentType":"application/3gpp-json-patch+json","dataProducerId":"some-job-id","data":[{"path":"some/write/path","op":"add","moduleSetTag":"some-operation-id","value":"some-value","operationId":null,"privateProperties":{}}]}]'
            mockDmiRestClient.postOperationWithJsonData('my-dmi-service-name/dmi/v1/writeJob/some-job-id', expectedJson, OperationType.CREATE, NO_AUTH) >> response
        when: 'read/write data job request is processed'
            objectUnderTest.writeDataJob('some-job-id', dataJobMetadata, new DataJobWriteRequest([getWriteOrReadOperationRequest('write'), getWriteOrReadOperationRequest('write')] as List<WriteOperation>))
        then: 'the data job id is correctly logged'
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.INFO
            assert loggingEvent.formattedMessage.contains('data job id for write operation is: some-job-id')
            1 * mockDmiRestClient.postOperationWithJsonData('my-dmi-service-name/dmi/v1/writeJob/some-job-id', expectedJson, OperationType.CREATE, NO_AUTH)
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
