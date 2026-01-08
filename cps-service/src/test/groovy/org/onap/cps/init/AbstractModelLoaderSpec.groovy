/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modification Copyright (C) 2024 Deutsche Telekom AG
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

package org.onap.cps.init

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.exceptions.AlreadyDefinedException
import org.onap.cps.api.exceptions.AnchorNotFoundException
import org.onap.cps.api.exceptions.DataspaceNotFoundException
import org.onap.cps.api.exceptions.DuplicatedYangResourceException
import org.onap.cps.api.exceptions.ModelOnboardingException
import org.onap.cps.api.model.ModuleDefinition
import org.onap.cps.api.parameters.CascadeDeleteAllowed
import org.onap.cps.init.actuator.ReadinessManager
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

class AbstractModelLoaderSpec extends Specification {

    def mockModelLoaderLock = Mock(ModelLoaderLock)
    def mockCpsDataspaceService = Mock(CpsDataspaceService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockReadinessManager = Mock(ReadinessManager)
    def objectUnderTest = Spy(new TestModelLoader(mockModelLoaderLock, mockCpsDataspaceService, mockCpsModuleService, mockCpsAnchorService, mockCpsDataService, mockReadinessManager))

    def applicationContext = new AnnotationConfigApplicationContext()

    def logger = (Logger) LoggerFactory.getLogger(AbstractModelLoader)
    def loggingListAppender

    def setup() {
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
        applicationContext.refresh()
    }

    def cleanup() {
        logger.detachAndStopAllAppenders()
        applicationContext.close()
        loggingListAppender.stop()
    }

    def 'Application ready.'() {
        when: 'Application ready event is triggered'
            objectUnderTest.onApplicationEvent(Mock(ApplicationReadyEvent))
        then: 'the onboard/upgrade method is executed'
            1 * objectUnderTest.onboardOrUpgradeModel()
    }

    def 'Application ready event with exception.'() {
        given: 'a startup exception is thrown during model onboarding'
            objectUnderTest.onboardOrUpgradeModel() >> { throw new ModelOnboardingException('test message','details are not logged') }
        when: 'Application (ready) event is triggered'
            objectUnderTest.onApplicationEvent(new ApplicationReadyEvent(new SpringApplication(), null, applicationContext, null))
        then: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            assert logs.contains('test message')
    }

    def 'Creating a dataspace.'() {
        when: 'creating a dataspace'
            objectUnderTest.createDataspace('some dataspace')
        then: 'the operation is delegated to the dataspace service'
            1 * mockCpsDataspaceService.createDataspace('some dataspace')
    }

    def 'Creating a dataspace that already exists.'() {
        given: 'dataspace service throws an already defined exception'
            mockCpsDataspaceService.createDataspace(*_) >> { throw AlreadyDefinedException.forDataNodes([], 'some context') }
        when: 'creating a dataspace'
            objectUnderTest.createDataspace('some dataspace')
        then: 'the exception is ignored i.e. no exception thrown up'
            noExceptionThrown()
        and: 'the exception is ignored, and a log message is produced'
            assert logContains('Dataspace already exists')
    }

    def 'Attempt to create a dataspace with other exception.'() {
        given: 'dataspace service throws a runtime exception'
            mockCpsDataspaceService.createDataspace(*_) >> { throw new RuntimeException('test message')  }
        when: 'creating a dataspace'
            objectUnderTest.createDataspace('some dataspace')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(ModelOnboardingException)
            assert thrown.message.contains('Creating dataspace failed')
            assert thrown.details.contains('test message')
    }

    def 'Creating a schema set.'() {
        when: 'creating a schema set'
            objectUnderTest.createSchemaSet('some dataspace','new name','cps-notification-subscriptions@2024-07-03.yang')
        then: 'the operation is delegated to the module service'
            1 * mockCpsModuleService.createSchemaSet('some dataspace','new name',_)
    }

    def 'Creating a schema set with duplicated yang resource.'() {
        given: 'module service throws duplicated yang resource exception'
            mockCpsModuleService.createSchemaSet(*_) >> { throw new DuplicatedYangResourceException('my-yang-resource', 'my-yang-resource-checksum', null) }
        when: 'attempt to create a schema set'
            objectUnderTest.createSchemaSet('some dataspace','some schema set','cps-notification-subscriptions@2024-07-03.yang')
        then: 'exception is ignored, and correct exception message is logged'
            noExceptionThrown()
            assert logContains('Ignoring yang resource duplication exception. Assuming model was created by another instance')
    }

    def 'Creating a schema that already exists.'() {
        given: 'the module service throws an already defined exception'
            mockCpsModuleService.createSchemaSet(*_) >>  { throw AlreadyDefinedException.forSchemaSet('name','context',null) }
        when: 'attempt to create a schema set'
            objectUnderTest.createSchemaSet('some dataspace','new name','cps-notification-subscriptions@2024-07-03.yang')
        then: 'the exception is ignored, and a log message is produced'
            noExceptionThrown()
            assert logContains('Creating new schema set failed as schema set already exists')
    }

    def 'Attempt to creating a schema set from a non-existing YANG file.'() {
        when: 'attempting to create a schema set from a non-existing file'
            objectUnderTest.createSchemaSet('some dataspace','some name','no such yang file')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(ModelOnboardingException)
            assert thrown.message.contains('Creating schema set failed')
            assert thrown.details.contains('unable to read file')
    }

    def 'Creating an anchor.'() {
        when: 'creating an anchor'
            objectUnderTest.createAnchor('some dataspace','some schema set','new name')
        then: 'the operation is delegated to the anchor service'
            1 * mockCpsAnchorService.createAnchor('some dataspace','some schema set', 'new name')
    }

    def 'Creating an anchor that already exists.'() {
        given: 'the anchor service throws an already defined exception'
            mockCpsAnchorService.createAnchor(*_)>>  { throw AlreadyDefinedException.forAnchor('name','context',null) }
        when: 'attempting to create an anchor'
            objectUnderTest.createAnchor('some dataspace','some schema set','new name')
        then: 'the exception is ignored, and a log message is produced'
            noExceptionThrown()
            assert logContains('Creating new anchor failed as anchor already exists')
    }

    def 'Attempt to create an anchor with other exception.'() {
        given: 'the anchor service throws a runtime exception'
            mockCpsAnchorService.createAnchor(*_)>>  { throw new RuntimeException('test message') }
        when: 'attempt to create anchor'
            objectUnderTest.createAnchor('some dataspace','some schema set','new name')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(ModelOnboardingException)
            assert thrown.message.contains('Creating anchor failed')
            assert thrown.details.contains('test message')
    }

    def 'Creating a top-level data node.'() {
        when: 'top-level node is created'
            objectUnderTest.createTopLevelDataNode('dataspace','anchor','new node')
        then: 'the correct JSON is saved using the data service'
            1 * mockCpsDataService.saveData('dataspace','anchor', '{"new node":{}}',_)
    }

    def 'Creating a top-level node that already exists.'() {
        given: 'the data service throws an Already Defined exception'
            mockCpsDataService.saveData(*_) >> { throw AlreadyDefinedException.forDataNodes([], 'some context') }
        when: 'attempting to create a top-level node'
            objectUnderTest.createTopLevelDataNode('dataspace','anchor','new node')
        then: 'the exception is ignored, and a log message is produced'
            noExceptionThrown()
            assert logContains('failed as data node already exists')
    }

    def 'Attempt to create a top-level node with other exception.'() {
        given: 'the data service throws a runtime exception'
            mockCpsDataService.saveData(*_) >> { throw new RuntimeException('test message') }
        when: 'attempt to create a top-level node'
            objectUnderTest.createTopLevelDataNode('dataspace','anchor','new node')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(ModelOnboardingException)
            assert thrown.message.contains('Creating data node failed')
            assert thrown.details.contains('test message')
    }

    def 'Delete unused schema sets.'() {
        when: 'unused schema sets get deleted'
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
            def thrown = thrown(ModelOnboardingException)
            assert thrown.message.contains('Updating schema set failed')
            assert thrown.details.contains('test message')
    }

    def 'Checking if an anchor exists.'() {
        given: 'the anchor service returns an anchor without throwing an exception'
            mockCpsAnchorService.getAnchor('my-dataspace', 'my-anchor') >> {}
        when: 'checking if the anchor exists'
            def result = objectUnderTest.doesAnchorExist('my-dataspace', 'my-anchor')
        then: 'the expected boolean value is returned'
            assert result == true
    }

    def 'Checking if an anchor exists with unknown anchor.'() {
        given: 'the anchor service throws an anchor not found exception'
            def anchorNotFoundException = new AnchorNotFoundException('my-dataspace', 'missing-anchor')
            mockCpsAnchorService.getAnchor('my-dataspace', 'missing-anchor') >> {throw anchorNotFoundException}
        when: 'checking if the anchor exists'
            def result = objectUnderTest.doesAnchorExist('my-dataspace', 'missing-anchor')
        then: 'the expected boolean value is returned'
            assert result == false
    }

    def 'Checking if an anchor exists with unknown dataspace.'() {
        given: 'the anchor service throws a dataspace not fond exception'
            def dataspaceNotFoundException = new DataspaceNotFoundException ('missing-dataspace')
            mockCpsAnchorService.getAnchor('missing-dataspace', 'some anchor') >> {throw dataspaceNotFoundException}
        when: 'checking if the anchor exists'
            def result = objectUnderTest.doesAnchorExist('missing-dataspace', 'some anchor')
        then: 'the expected boolean value is returned'
            assert result == false
    }

    def 'Checking if module revision is installed when: #scenario.'() {
        given: 'the module service returns module definitions'
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule('some-dataspace', 'some-anchor', 'some-module', 'my-revision') >> moduleDefinitions
        when: 'checking if a module revision is installed'
            def result = objectUnderTest.isModuleRevisionInstalled('some-dataspace', 'some-anchor', 'some-module', 'my-revision')
        then: 'the result matches expectation'
            assert result == expectedResult
        where: 'the following scenarios are used'
            scenario                         || moduleDefinitions        || expectedResult
            'Module revision exists'         || [Mock(ModuleDefinition)] || true
            'Module revision does not exist' || []                       || false
    }

    def 'Check if this instance is master when: #scenario.'() {
        given: 'the lock acquisition returns #lockAcquired'
            mockModelLoaderLock.tryLock() >> lockAcquired
        when: 'checking if this instance is master'
            objectUnderTest.checkIfThisInstanceIsMaster()
        then: 'the master status is set correctly'
            assert objectUnderTest.@isMaster == expectedMasterStatus
        and: 'expected log message is produced'
            assert logContains(expectedLogMessage)
        where: 'the following scenarios are used'
            scenario                  | lockAcquired || expectedMasterStatus || expectedLogMessage
            'lock can be acquired'    | true         || true                 || 'This instance is model loader master'
            'lock cannot be acquired' | false        || false                || 'Another instance is model loader master'
    }

    def 'Check if this instance is master when already master.'() {
        given: 'instance is already master'
            objectUnderTest.@isMaster = true
        when: 'check for master again'
            objectUnderTest.checkIfThisInstanceIsMaster()
        then: 'instance remains remains master'
            assert objectUnderTest.@isMaster == true
        and: 'no attempt is made to lock'
            0 * mockModelLoaderLock.tryLock()
        and: 'log reports this instance is master'
            assert logContains('This instance is model loader master')
    }

    def logContains(expectedMessage) {
        return loggingListAppender.list.toString().contains(expectedMessage)
    }

    class TestModelLoader extends AbstractModelLoader {

        TestModelLoader(final ModelLoaderLock modelLoaderLock,
                        final CpsDataspaceService cpsDataspaceService,
                        final CpsModuleService cpsModuleService,
                        final CpsAnchorService cpsAnchorService,
                        final CpsDataService cpsDataService,
                        final ReadinessManager readinessManager) {
            super(modelLoaderLock, cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService, readinessManager)
        }

        @Override
        void onboardOrUpgradeModel() {
            // Not needed for testing
        }

        @Override
        String getName() {
            // Not needed for testing
        }

    }
}
