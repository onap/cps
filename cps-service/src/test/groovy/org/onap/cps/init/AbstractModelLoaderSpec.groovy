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
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

class AbstractModelLoaderSpec extends Specification {

    def mockCpsDataspaceService = Mock(CpsDataspaceService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def objectUnderTest = Spy(new TestModelLoader(mockCpsDataspaceService, mockCpsModuleService, mockCpsAnchorService, mockCpsDataService))

    def applicationContext = new AnnotationConfigApplicationContext()

    def yangResourceToContentMap
    def logger = (Logger) LoggerFactory.getLogger(AbstractCpsModuleLoader)
    def loggingListAppender

    void setup() {
        yangResourceToContentMap = objectUnderTest.createYangResourcesToContentMap('cps-notification-subscriptions@2024-07-03.yang')
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

    def 'Application started event'() {
        when: 'Application (started) event is triggered'
            objectUnderTest.onApplicationEvent(Mock(ApplicationStartedEvent))
        then: 'the onboard/upgrade method is executed'
            1 * objectUnderTest.onboardOrUpgradeModel()
    }

    def 'Application started event with start up exception'() {
        given: 'a start up exception is thrown doing model onboarding'
            objectUnderTest.onboardOrUpgradeModel() >> { throw new CpsStartupException('test message','details are not logged') }
        when: 'Application (started) event is triggered'
            objectUnderTest.onApplicationEvent(new ApplicationStartedEvent(new SpringApplication(), null, applicationContext, null))
        then: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            assert logs.contains('test message')
    }

    def 'Create dataspace.'() {
        when: 'creating a dataspace'
            objectUnderTest.createDataspace('some dataspace')
        then: 'the operation is delegated to the admin service'
            1 * mockCpsDataspaceService.createDataspace('some dataspace')
    }

    def 'Create dataspace with already defined exception.'() {
        given: 'dataspace service throws an already defined exception'
            mockCpsDataspaceService.createDataspace(*_) >> { throw AlreadyDefinedException.forDataNodes([], 'some context') }
        when: 'creating a dataspace'
            objectUnderTest.createDataspace('some dataspace')
        then: 'the exception is ignored i.e. no exception thrown up'
            noExceptionThrown()
        and: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            assert logs.contains('Dataspace already exists')
    }

    def 'Create dataspace with with any other exception.'() {
        given: 'dataspace service throws an already defined exception'
            mockCpsDataspaceService.createDataspace(*_) >> { throw new RuntimeException('test message')  }
        when: 'creating a dataspace'
            objectUnderTest.createDataspace('some dataspace')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(CpsStartupException)
            assert thrown.message.contains('Creating dataspace failed')
            assert thrown.details.contains('test message')
    }

    def 'Create schema set.'() {
        when: 'creating a schema set'
            objectUnderTest.createSchemaSet('some dataspace','new name','cps-notification-subscriptions@2024-07-03.yang')
        then: 'the operation is delegated to the admin service'
            1 * mockCpsModuleService.createSchemaSet('some dataspace','new name',_)
    }

    def 'Create schema set with already defined exception.'() {
        given: 'the module service throws an already defined exception'
            mockCpsModuleService.createSchemaSet(*_) >>  { throw AlreadyDefinedException.forSchemaSet('name','context',null) }
        when: 'attempt to create a schema set'
            objectUnderTest.createSchemaSet('some dataspace','new name','cps-notification-subscriptions@2024-07-03.yang')
        then: 'the exception is ignored i.e. no exception thrown up'
            noExceptionThrown()
            and: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            assert logs.contains('Creating new schema set failed as schema set already exists')
    }

    def 'Create schema set with non existing yang file.'() {
        when: 'attempt to create a schema set from a non existing file'
            objectUnderTest.createSchemaSet('some dataspace','some name','no such yang file')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(CpsStartupException)
            assert thrown.message.contains('Creating schema set failed')
            assert thrown.details.contains('unable to read file')
    }

    def 'Create anchor.'() {
        when: 'creating an anchor'
            objectUnderTest.createAnchor('some dataspace','some schema set','new name')
        then: 'the operation is delegated to the admin service'
            1 * mockCpsAnchorService.createAnchor('some dataspace','some schema set', 'new name')
    }

    def 'Create anchor with already defined exception.'() {
        given: 'the admin service throws an already defined exception'
            mockCpsAnchorService.createAnchor(*_)>>  { throw AlreadyDefinedException.forAnchor('name','context',null) }
        when: 'attempt to create anchor'
            objectUnderTest.createAnchor('some dataspace','some schema set','new name')
        then: 'the exception is ignored i.e. no exception thrown up'
            noExceptionThrown()
        and: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            assert logs.contains('Creating new anchor failed as anchor already exists')
    }

    def 'Create anchor with any other exception.'() {
        given: 'the admin service throws a exception'
            mockCpsAnchorService.createAnchor(*_)>>  { throw new RuntimeException('test message') }
        when: 'attempt to create anchor'
            objectUnderTest.createAnchor('some dataspace','some schema set','new name')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(CpsStartupException)
            assert thrown.message.contains('Creating anchor failed')
            assert thrown.details.contains('test message')
    }

    def 'Create top level data node.'() {
        when: 'top level node is created'
            objectUnderTest.createTopLevelDataNode('dataspace','anchor','new node')
        then: 'the correct json is saved using the data service'
            1 * mockCpsDataService.saveData('dataspace','anchor', '{"new node":{}}',_)
    }

    def 'Create top level node with already defined exception.'() {
        given: 'the data service throws an Already Defined exception'
            mockCpsDataService.saveData(*_) >> { throw AlreadyDefinedException.forDataNodes([], 'some context') }
        when: 'attempt to create top level node'
            objectUnderTest.createTopLevelDataNode('dataspace','anchor','new node')
            then: 'the exception is ignored i.e. no exception thrown up'
            noExceptionThrown()
            and: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            assert logs.contains('failed as data node already exists')
    }

    def 'Create top level node with any other exception.'() {
        given: 'the data service throws an exception'
            mockCpsDataService.saveData(*_) >> { throw new RuntimeException('test message') }
        when: 'attempt to create top level node'
            objectUnderTest.createTopLevelDataNode('dataspace','anchor','new node')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(CpsStartupException)
        assert thrown.message.contains('Creating data node failed')
        assert thrown.details.contains('test message')
    }

    class TestModelLoader extends AbstractCpsModuleLoader {

        TestModelLoader(final CpsDataspaceService cpsDataspaceService,
                        final CpsModuleService cpsModuleService,
                        final CpsAnchorService cpsAnchorService,
                        final CpsDataService cpsDataService) {
            super(cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService)
        }

        @Override
        void onboardOrUpgradeModel() { }
    }
}
