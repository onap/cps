package org.onap.cps.init

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.CpsStartupException
import org.onap.cps.spi.model.Dataspace
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

class CpsNotificationSubscriptionModuleLoaderSpec extends Specification {
    def mockCpsDataspaceService = Mock(CpsDataspaceService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def objectUnderTest = new CpsNotificationSubscriptionModuleLoader(mockCpsDataspaceService, mockCpsModuleService, mockCpsAnchorService, mockCpsDataService)

    def applicationContext = new AnnotationConfigApplicationContext()

    def expectedYangResourcesToContentMap
    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender

    def CPS_DATASPACE_NAME = "CPS-Admin"
    def ANCHOR_NAME = "cps-notification-subscriptions"
    def SCHEMASET_NAME = "cps-notification-subscriptions"
    def MODEL_FILENAME = "cps-notification-subscriptions@2024-07-03.yang"

    void setup() {
        expectedYangResourcesToContentMap = objectUnderTest.createYangResourcesToContentMap(MODEL_FILENAME)
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
        applicationContext.refresh()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CpsNotificationSubscriptionModuleLoader.class)).detachAndStopAllAppenders()
        applicationContext.close()
    }

    def 'Onboard subscription model via application started event.'() {
        given: 'dataspace is already present'
            mockCpsDataspaceService.getAllDataspaces() >> [new Dataspace('test')]
        when: 'the application is ready'
            objectUnderTest.onApplicationEvent(Mock(ApplicationStartedEvent))
        then: 'the module service to create schema set is called once'
            1 * mockCpsModuleService.createSchemaSet(CPS_DATASPACE_NAME, SCHEMASET_NAME, expectedYangResourcesToContentMap)
        and: 'the admin service to create an anchor set is called once'
            1 * mockCpsAnchorService.createAnchor(CPS_DATASPACE_NAME, SCHEMASET_NAME, ANCHOR_NAME)
        and: 'the data service to create a top level datanode is called once'
            1 * mockCpsDataService.saveData(CPS_DATASPACE_NAME, ANCHOR_NAME, '{"dataspaces":{}}', _)
    }

    def 'Create node for datastore with already defined exception.'() {
        given: 'dataspace is already present'
            mockCpsDataspaceService.getAllDataspaces() >> [new Dataspace('test')]
        and: 'the data service throws an Already Defined exception'
            mockCpsDataService.saveData(*_) >> { throw AlreadyDefinedException.forDataNodes([], 'some context') }
        when: 'attempt to create datastore'
            objectUnderTest.createInitialSubscription(CPS_DATASPACE_NAME, ANCHOR_NAME)
        then: 'the exception is ignored i.e. no exception thrown up'
            noExceptionThrown()
        and: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            logs.contains("Creating new child data node 'test' for data node 'dataspaces' failed as data node already exists")
    }

    def 'Create node for datastore with any other exception.'() {
        given: 'dataspace is already present'
            mockCpsDataspaceService.getAllDataspaces() >> [new Dataspace('test')]
        and: 'the data service throws an exception'
            mockCpsDataService.saveData(*_) >> { throw new RuntimeException('test message') }
        when: 'attempt to create datastore'
            objectUnderTest.createInitialSubscription(CPS_DATASPACE_NAME, ANCHOR_NAME)
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(CpsStartupException)
            assert thrown.message.contains('Creating data node failed')
            assert thrown.details.contains('test message')
    }

}
