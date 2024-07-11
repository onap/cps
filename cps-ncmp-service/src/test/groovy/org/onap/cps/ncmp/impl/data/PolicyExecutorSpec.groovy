package org.onap.cps.ncmp.impl.data

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.OperationType.PATCH

@SpringBootTest
@ContextConfiguration(classes = [PolicyExecutor])
class PolicyExecutorSpec extends Specification {

    @Autowired
    PolicyExecutor objectUnderTest

    def logAppender = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        setupLogger()
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(PolicyExecutor)).detachAndStopAllAppenders()
    }

    def 'Configuration properties.'() {
        expect: 'properties used from application.yml'
            assert objectUnderTest.enabled
            assert objectUnderTest.serverAddress == 'http://localhost'
            assert objectUnderTest.serverPort == '8785'
    }

    def 'Permission check logging.'() {
        when: 'permission is checked for an operation'
            def yangModelCmHandle = new YangModelCmHandle(id:'ch-1', alternateId:'fdn1')
            objectUnderTest.checkPermission(yangModelCmHandle, PATCH, 'my credentials','my resource','my change')
        then: 'correct details are logged '
            assert getLogEntry(0) == 'Policy Executor Enabled'
            assert getLogEntry(3).contains('my credentials')
            assert getLogEntry(4).contains('cm_patch')
            assert getLogEntry(5).contains('fdn1')
            assert getLogEntry(6).contains('ch-1')
            assert getLogEntry(7).contains('my resource')
            assert getLogEntry(8).contains('my change')
    }

    def 'Permission check with feature disabled.'() {
        given: 'feature is disabled'
            objectUnderTest.enabled = false
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), PATCH, 'my credentials','my resource','my change')
        then: 'nothing is logged'
            assert logAppender.list.isEmpty()
    }

    def setupLogger() {
        def logger = LoggerFactory.getLogger(PolicyExecutor)
        logger.setLevel(Level.DEBUG)
        logger.addAppender(logAppender)
        logAppender.start()
    }

    def getLogEntry(index) {
        logAppender.list[index].formattedMessage
    }
}
