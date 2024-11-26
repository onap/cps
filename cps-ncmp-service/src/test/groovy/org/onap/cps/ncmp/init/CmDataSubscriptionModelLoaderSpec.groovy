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
import org.onap.cps.spi.api.exceptions.AlreadyDefinedException
import org.onap.cps.spi.api.model.Dataspace
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME

class CmDataSubscriptionModelLoaderSpec extends Specification {

    def mockCpsDataspaceService = Mock(CpsDataspaceService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def objectUnderTest = new CmDataSubscriptionModelLoader(mockCpsDataspaceService, mockCpsModuleService, mockCpsAnchorService, mockCpsDataService)

    def applicationContext = new AnnotationConfigApplicationContext()

    def expectedYangResourcesToContentMap
    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender

    void setup() {
        expectedYangResourcesToContentMap = objectUnderTest.createYangResourcesToContentMap('cm-data-subscriptions@2024-02-12.yang')
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

    def 'Onboard subscription model via application started event.'() {
        given: 'dataspace is ready for use'
            mockCpsDataspaceService.getDataspace(NCMP_DATASPACE_NAME) >> new Dataspace('')
        when: 'the application is ready'
            objectUnderTest.onApplicationEvent(Mock(ApplicationStartedEvent))
        then: 'the module service to create schema set is called once'
            1 * mockCpsModuleService.createSchemaSet(NCMP_DATASPACE_NAME, 'cm-data-subscriptions', expectedYangResourcesToContentMap)
        and: 'the admin service to create an anchor set is called once'
            1 * mockCpsAnchorService.createAnchor(NCMP_DATASPACE_NAME, 'cm-data-subscriptions', 'cm-data-subscriptions')
        and: 'the data service to create a top level datanode is called once'
            1 * mockCpsDataService.saveData(NCMP_DATASPACE_NAME, 'cm-data-subscriptions', '{"datastores":{}}', _)
        and: 'the data service is called once to create datastore for Passthrough-operational'
            1 * mockCpsDataService.saveData(NCMP_DATASPACE_NAME, 'cm-data-subscriptions', '/datastores',
                '{"datastore":[{"name":"ncmp-datastore:passthrough-operational","cm-handles":{}}]}', _, _)
        and: 'the data service is called once to create datastore for Passthrough-running'
            1 * mockCpsDataService.saveData(NCMP_DATASPACE_NAME, 'cm-data-subscriptions', '/datastores',
                '{"datastore":[{"name":"ncmp-datastore:passthrough-running","cm-handles":{}}]}', _, _)
    }

    def 'Create node for datastore with already defined exception.'() {
        given: 'the data service throws an Already Defined exception'
            mockCpsDataService.saveData(*_) >> { throw AlreadyDefinedException.forDataNodes([], 'some context') }
        when: 'attempt to create datastore'
            objectUnderTest.createDatastore('some datastore')
        then: 'the exception is ignored i.e. no exception thrown up'
            noExceptionThrown()
        and: 'the exception message is logged'
            def logs = loggingListAppender.list.toString()
            logs.contains("Creating new child data node 'some datastore' for data node 'datastores' failed as data node already exists")
    }

    def 'Create node for datastore with any other exception.'() {
        given: 'the data service throws an exception'
            mockCpsDataService.saveData(*_) >> { throw new RuntimeException('test message') }
        when: 'attempt to create datastore'
            objectUnderTest.createDatastore('some datastore')
        then: 'a startup exception with correct message and details is thrown'
            def thrown = thrown(NcmpStartUpException)
            assert thrown.message.contains('Creating data node failed')
            assert thrown.details.contains('test message')
    }

}
