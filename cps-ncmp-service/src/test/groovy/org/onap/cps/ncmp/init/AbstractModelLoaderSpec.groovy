/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.init

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.exceptions.NcmpStartUpException
import org.onap.cps.api.parameters.CascadeDeleteAllowed
import org.onap.cps.api.exceptions.AlreadyDefinedException
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
    def logger = (Logger) LoggerFactory.getLogger(AbstractModelLoader)
    def loggingListAppender

    void setup() {
        yangResourceToContentMap = objectUnderTest.createYangResourcesToContentMap('cm-data-subscriptions@2024-02-12.yang')
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
        applicationContext.refresh()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CmDataSubscriptionModelLoader.class)).detachAndStopAllAppenders()
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
            objectUnderTest.onboardOrUpgradeModel() >> { throw new NcmpStartUpException('test message','details are not logged') }
        when: 'Application (started) event is triggered'
            objectUnderTest.onApplicationEvent(new ApplicationStartedEvent(new SpringApplication(), null, applicationContext, null))
        then: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            assert logs.contains('test message')
    }

    def 'Create schema set.'() {
        when: 'creating a schema set'
            objectUnderTest.createSchemaSet('some dataspace','new name','cm-data-subscriptions@2024-02-12.yang')
        then: 'the operation is delegated to the admin service'
            1 * mockCpsModuleService.createSchemaSet('some dataspace','new name',_)
    }

    def 'Create schema set with already defined exception.'() {
        given: 'the module service throws an already defined exception'
            mockCpsModuleService.createSchemaSet(*_) >>  { throw AlreadyDefinedException.forSchemaSet('name','context',null) }
        when: 'attempt to create a schema set'
            objectUnderTest.createSchemaSet('some dataspace','new name','cm-data-subscriptions@2024-02-12.yang')
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
            def thrown = thrown(NcmpStartUpException)
            assert thrown.message.contains('Creating schema set failed')
            assert thrown.details.contains('unable to read file')
    }

    def 'Delete unused schema sets.'() {
        when: 'several unused schemas are deleted '
            objectUnderTest.deleteUnusedSchemaSets('some dataspace','schema set 1', 'schema set 2')
        then: 'a request to delete each (without cascade) is delegated to the module service'
            1 * mockCpsModuleService.deleteSchemaSet('some dataspace', 'schema set 1', CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED)
            1 * mockCpsModuleService.deleteSchemaSet('some dataspace', 'schema set 2', CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED)

    }

    def 'Delete unused schema sets with exception.'() {
        given: 'deleting the first schemaset causes an exception'
            mockCpsModuleService.deleteSchemaSet(_, 'schema set 1', _) >> { throw new RuntimeException('test message')}
        when: 'several unused schemas are deleted '
            objectUnderTest.deleteUnusedSchemaSets('some dataspace','schema set 1', 'schema set 2')
        then: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            assert logs.contains('Deleting schema set failed')
            assert logs.contains('test message')
        and: 'the second schema set is still deleted'
            1 * mockCpsModuleService.deleteSchemaSet('some dataspace', 'schema set 2', CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED)
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
            def thrown = thrown(NcmpStartUpException)
            assert thrown.message.contains('Creating anchor failed')
            assert thrown.details.contains('test message')
    }

    def 'Create top level node.'() {
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
            def thrown = thrown(NcmpStartUpException)
            assert thrown.message.contains('Creating data node failed')
            assert thrown.details.contains('test message')
    }

    def 'Update anchor schema set.'() {
        when: 'a schema set for an anchor is updated'
            objectUnderTest.updateAnchorSchemaSet('some dataspace', 'anchor', 'new schema set')
        then: 'the request is delegated to the admin service'
            1 * mockCpsAnchorService.updateAnchorSchemaSet('some dataspace', 'anchor', 'new schema set')
    }

    def 'Update anchor schema set with exception.'() {
        given: 'the admin service throws an exception'
            mockCpsAnchorService.updateAnchorSchemaSet(*_) >> { throw new RuntimeException('test message') }
        when: 'a schema set for an anchor is updated'
            objectUnderTest.updateAnchorSchemaSet('some dataspace', 'anchor', 'new schema set')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(NcmpStartUpException)
            assert thrown.message.contains('Updating schema set failed')
            assert thrown.details.contains('test message')
    }

    class TestModelLoader extends AbstractModelLoader {

        TestModelLoader(final CpsDataspaceService cpsDataspaceService,
                        final CpsModuleService cpsModuleService,
                        final CpsAnchorService cpsAnchorService,
                        final CpsDataService cpsDataService) {
            super(cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService)
            super.maximumAttemptCount = 2
            super.retryTimeMs = 1
        }

        @Override
        void onboardOrUpgradeModel() { }
    }

}
