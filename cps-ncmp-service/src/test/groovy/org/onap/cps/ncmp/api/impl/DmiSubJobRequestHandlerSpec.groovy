package org.onap.cps.ncmp.api.impl

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DmiWriteOperation
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse
import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.api.impl.config.DmiProperties
import org.onap.cps.ncmp.api.impl.operations.OperationType
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService
import org.onap.cps.ncmp.impl.datajobs.DataJobServiceImpl
import org.onap.cps.ncmp.impl.datajobs.DmiSubJobRequestHandler
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class DmiSubJobRequestHandlerSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockDmiProperties = Mock(DmiProperties)
    def static NO_AUTH = null
    def objectUnderTest = new DmiSubJobRequestHandler(mockDmiRestClient, mockDmiProperties, jsonObjectMapper)

    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        setupLogger()
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(DataJobServiceImpl.class)).detachAndStopAllAppenders()
    }

    def 'Send a sub-job request to the DMI Plugin.'() {
        given: 'a data job id, metadata and a map of producer keys and write operations to create a request'
            def dataJobId = 'some-job-id'
            def dataJobMetadata = new DataJobMetadata('', '', '')
            def dmiWriteOperation = new DmiWriteOperation('', '', '', null, '', [:])
            def dmiWriteOperationsPerProducerKey = [new ProducerKey('', ''): [dmiWriteOperation]]
            def response = new ResponseEntity<>(new SubJobWriteResponse('my-sub-job-id', '', ''), HttpStatus.OK)
        when: 'sending request to DMI invoked'
            objectUnderTest.sendRequestsToDmi(dataJobId, dataJobMetadata, dmiWriteOperationsPerProducerKey)
        then: 'the dmi rest client is called and the expected information from the response is logged'
            1 * mockDmiRestClient.postOperationWithJsonData(RequiredDmiService.DATA, _, _, OperationType.CREATE, NO_AUTH) >> response
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.DEBUG
            assert loggingEvent.formattedMessage.contains('my-sub-job-id')
    }

    def setupLogger() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(DmiSubJobRequestHandler.class))
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
    }
}
