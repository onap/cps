/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 TechMahindra Ltd.
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
import org.onap.cps.api.exceptions.ModelStartupException
import org.onap.cps.api.model.Dataspace
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

class CpsNotificationSubscriptionModelLoaderSpec extends Specification {
    def mockCpsDataspaceService = Mock(CpsDataspaceService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def objectUnderTest = new CpsNotificationSubscriptionModelLoader(mockCpsDataspaceService, mockCpsModuleService, mockCpsAnchorService, mockCpsDataService)

    def applicationContext = new AnnotationConfigApplicationContext()

    def expectedYangResourcesToContents
    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender

    def CPS_DATASPACE_NAME = 'CPS-Admin'
    def ANCHOR_NAME = 'cps-notification-subscriptions'
    def SCHEMASET_NAME = 'cps-notification-subscriptions'
    def MODEL_FILENAME = 'cps-notification-subscriptions@2024-07-03.yang'

    void setup() {
        expectedYangResourcesToContents = objectUnderTest.mapYangResourcesToContent(MODEL_FILENAME)
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
        applicationContext.refresh()
    }

    void cleanup() {
        logger.detachAndStopAllAppenders()
        applicationContext.close()
        loggingListAppender.stop()
    }

    def 'Onboard subscription model via application started event.'() {
        given: 'dataspace is already present'
            mockCpsDataspaceService.getAllDataspaces() >> [new Dataspace('test')]
        when: 'the application is ready'
            objectUnderTest.onApplicationEvent(Mock(ApplicationStartedEvent))
        then: 'the module service to create schema set is called once'
            1 * mockCpsModuleService.createSchemaSet(CPS_DATASPACE_NAME, SCHEMASET_NAME, expectedYangResourcesToContents)
        and: 'the admin service to create an anchor set is called once'
            1 * mockCpsAnchorService.createAnchor(CPS_DATASPACE_NAME, SCHEMASET_NAME, ANCHOR_NAME)
        and: 'the data service to create a top level datanode is called once'
            1 * mockCpsDataService.saveData(CPS_DATASPACE_NAME, ANCHOR_NAME, '{"dataspaces":{}}', _)
    }

    def 'Create node for datastore with any other exception.'() {
        given: 'dataspace is already present'
            mockCpsDataspaceService.getAllDataspaces() >> [new Dataspace('test')]
        and: 'the data service throws a Runtime exception'
            mockCpsDataService.saveData(*_) >> { throw new RuntimeException('test message') }
        when: 'attempt to create datastore'
            objectUnderTest.createInitialSubscription()
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(ModelStartupException)
            assert thrown.message.contains('Creating data node failed')
            assert thrown.details.contains('test message')
    }

    def 'Creating initial subscription handles already defined exception gracefully.'() {
        given: 'dataspace is already present'
            mockCpsDataspaceService.getAllDataspaces() >> [new Dataspace('test')]
        and: 'the data service throws a Runtime exception'
            mockCpsDataService.saveData(*_) >> { throw AlreadyDefinedException.forDataNodes([], 'some context')  }
        when: 'attempt to create datastore subscription'
            objectUnderTest.createInitialSubscription()
        then: 'the exception is ignored, and a log message is produced'
            noExceptionThrown()
            assertLogContains('Data node for dataspace \'test\' already exists under \'dataspaces\'')
    }

    def 'Creating initial subscription when dataspaces list is null'() {
        given: 'dataspace is already present'
            mockCpsDataspaceService.getAllDataspaces() >> null
        when: 'attempt to create datastore'
            objectUnderTest.createInitialSubscription()
        then: 'the data service to create a top level datanode is not called'
            0 * mockCpsDataService.saveData(CPS_DATASPACE_NAME, ANCHOR_NAME, _, _)
    }

    private void assertLogContains(String message) {
        def logs = loggingListAppender.list.toString()
        assert logs.contains(message)
    }

}
