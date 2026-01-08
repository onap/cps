/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Deutsche Telekom AG
 *  Modifications Copyright (c) 2022-2025 OpenInfra Foundation Europe.
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
import org.onap.cps.api.model.Dataspace
import org.onap.cps.init.actuator.ReadinessManager
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

class CpsNotificationSubscriptionModelLoaderSpec extends Specification {
    def mockCpsDataspaceService = Mock(CpsDataspaceService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockReadinessManager = Mock(ReadinessManager)
    def mockModelLoaderLock = Mock(ModelLoaderLock)
    def objectUnderTest = new CpsNotificationSubscriptionModelLoader(mockModelLoaderLock, mockCpsDataspaceService, mockCpsModuleService, mockCpsAnchorService, mockCpsDataService, mockReadinessManager)

    def applicationContext = new AnnotationConfigApplicationContext()

    def expectedYangResourcesToContents
    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender

    def CPS_DATASPACE_NAME = 'CPS-Admin'
    def ANCHOR_NAME = 'cps-notification-subscriptions'
    def SCHEMASET_NAME = 'cps-notification-subscriptions'
    def MODEL_FILENAME = 'cps-notification-subscriptions@2024-07-03.yang'

    void setup() {
        objectUnderTest.isMaster = true
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

    def 'Onboard subscription model via application ready event.'() {
        given: 'dataspace is already present'
            mockCpsDataspaceService.getAllDataspaces() >> [new Dataspace('test')]
        when: 'the model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the module service to create schema set is called once'
            1 * mockCpsModuleService.createSchemaSet(CPS_DATASPACE_NAME, SCHEMASET_NAME, expectedYangResourcesToContents)
        and: 'the anchor service to create an anchor set is called once'
            1 * mockCpsAnchorService.createAnchor(CPS_DATASPACE_NAME, SCHEMASET_NAME, ANCHOR_NAME)
        and: 'the data service to create a top level datanode is called once'
            1 * mockCpsDataService.saveData(CPS_DATASPACE_NAME, ANCHOR_NAME, '{"dataspaces":{}}', _)
    }

    def 'Onboarding is skipped when instance is not master'() {
        given: 'instance is not master'
            objectUnderTest.isMaster = false
        when: 'the model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the onboard/upgrade methods are not executed'
            0 * mockCpsModuleService.createSchemaSet(*_)
    }
}
