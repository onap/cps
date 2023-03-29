/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.exception.NcmpStartUpException
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.springframework.boot.SpringApplication
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

class SubscriptionModelLoaderSpec extends Specification {

    def mockCpsAdminService = Mock(CpsAdminService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def objectUnderTest = new SubscriptionModelLoader(mockCpsAdminService, mockCpsModuleService, mockCpsDataService)

    def SUBSCRIPTION_DATASPACE_NAME = objectUnderTest.SUBSCRIPTION_DATASPACE_NAME
    def SUBSCRIPTION_ANCHOR_NAME = objectUnderTest.SUBSCRIPTION_ANCHOR_NAME
    def SUBSCRIPTION_SCHEMASET_NAME = objectUnderTest.SUBSCRIPTION_SCHEMASET_NAME
    def SUBSCRIPTION_REGISTRY_DATANODE_NAME = objectUnderTest.SUBSCRIPTION_REGISTRY_DATANODE_NAME
    def MODEL_SUBSCRIPTION_YANG = objectUnderTest.MODEL_SUBSCRIPTION_YANG
    def SUBSCRIPTION_YANG = objectUnderTest.SUBSCRIPTION_YANG

    def sampleYangContentMap = ['subscription.yang':'module subscription { *sample content* }']

    def context = new AnnotationConfigApplicationContext()


    def applicationReadyEvent = new ApplicationReadyEvent(new SpringApplication(), null, context, null)

    def yangResourceToContentMap
    def logger
    def appender

    @BeforeEach
    void setup() {
        yangResourceToContentMap = objectUnderTest.createYangResourceToContentMap()
        logger = (Logger) LoggerFactory.getLogger(objectUnderTest.getClass())
        appender = new ListAppender()
        logger.setLevel(Level.DEBUG)
        appender.start()
        logger.addAppender(appender)
        context.refresh()
    }

    @AfterEach
    void teardown() {
        ((Logger) LoggerFactory.getLogger(SubscriptionModelLoader.class)).detachAndStopAllAppenders()
    }

    def 'Onboard subscription model successfully via application ready event'() {
        when:'model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = true
        and: 'the application is ready'
            objectUnderTest.onApplicationEvent(applicationReadyEvent)
        then: 'the module service to create schema set is called once'
            1 * mockCpsModuleService.createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME,sampleYangContentMap)
        and: 'the admin service to create an anchor set is called once'
            1 * mockCpsAdminService.createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME)
        and: 'the data service to create a top level datanode is called once'
            1 * mockCpsDataService.saveData(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME, '{"' + SUBSCRIPTION_REGISTRY_DATANODE_NAME + '":{}}', _)
    }

    def 'No subscription model onboarding when subscription model loader is disabled' () {
        when: 'model loader is disabled'
            objectUnderTest.subscriptionModelLoaderEnabled = false
        and: 'application is ready'
            objectUnderTest.onApplicationEvent(applicationReadyEvent)
        then: 'the module service to create schema set was not called'
            0 * mockCpsModuleService.createSchemaSet(*_)
        and: 'the admin service to create an anchor set was not called'
            0 * mockCpsAdminService.createAnchor(*_)
        and: 'the data service to create a top level datanode was not called'
            0 * mockCpsDataService.saveData(*_)
    }

    def 'Exception occurred while schema set creation' () {
        given: 'creating a schema set throws an exception'
            mockCpsModuleService.createSchemaSet(*_) >>  { throw new DataValidationException(*_) }
        and: 'model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = true
        when: 'application is ready'
            objectUnderTest.onApplicationEvent(applicationReadyEvent)
        then: 'the admin service to create an anchor set was not called'
            0 * mockCpsAdminService.createAnchor(*_)
        and: 'the data service to create a top level datanode was not called'
            0 * mockCpsDataService.saveData(*_)
    }

    def 'Onboard subscription model not called if application ready event is null'() {
        when: 'the application ready event is null '
            objectUnderTest.onApplicationEvent(null)
        then: 'Null pointer exception is thrown'
            thrown(NullPointerException)
    }

    def 'Create schema set from model file'() {
        when: 'the method to create schema set is called with the following parameters'
            objectUnderTest.createSchemaSet("myDataspace", "mySchemaSet", yangResourceToContentMap)
        then: 'yang resource to content map is as expected'
            assert sampleYangContentMap == yangResourceToContentMap
        and: 'the module service to create schema set is called once with the correct map'
            1 * mockCpsModuleService.createSchemaSet(_, _, yangResourceToContentMap)
    }

    def 'Create schema set fails due to AlreadyDefined exception'() {
        given: 'creating a schema set throws an exception as it already exists'
            mockCpsModuleService.createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, yangResourceToContentMap) >>
                    { throw AlreadyDefinedException.forSchemaSet(SUBSCRIPTION_SCHEMASET_NAME, "sampleContextName", null) }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'the admin service to create an anchor set is then called once'
            1 * mockCpsAdminService.createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME)
    }

    def 'Create schema set fails due to any other exception'() {
        given: 'creating a schema set throws an exception'
            mockCpsModuleService.createSchemaSet(*_) >> { throw new NcmpStartUpException("Creating schema set failed", "") }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'the log message contains the correct exception message'
            def debugMessage = appender.list[0].toString()
            assert debugMessage.contains("Creating schema set failed")
        and: 'exception is thrown'
            thrown(NcmpStartUpException)
    }

    def 'Create anchor fails due to AlreadyDefined exception'() {
        given: 'creating anchor throws an exception as it already exists'
            mockCpsAdminService.createAnchor(*_) >>
                    { throw AlreadyDefinedException.forSchemaSet(SUBSCRIPTION_SCHEMASET_NAME, "sampleContextName", null) }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'no exception thrown'
            noExceptionThrown()
        and: 'the log message contains the correct exception message'
            def infoMessage = appender.list[0].toString()
            assert infoMessage.contains("already exists")
    }

    def 'Create anchor fails due to any other exception'() {
        given: 'creating an anchor failed'
            mockCpsAdminService.createAnchor(*_) >>
                    { throw new SchemaSetNotFoundException(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME) }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'the log message contains the correct exception message'
            def debugMessage = appender.list[0].toString()
            assert debugMessage.contains("Schema Set not found")
        and: 'exception is thrown'
            thrown(NcmpStartUpException)
    }

    def 'Create top level node fails due to an AlreadyDefined exception'() {
        given: 'the saving of the node data will throw an Already Defined exception'
            mockCpsDataService.saveData(*_) >>
                    { throw AlreadyDefinedException.forDataNode('/xpath', "sampleContextName", null) }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'no exception thrown'
            noExceptionThrown()
        and: 'the log message contains the correct exception message'
            def infoMessage = appender.list[0].toString()
            assert infoMessage.contains("already exists")
    }

    def 'Create top level node fails due to any other exception'() {
        given: 'the saving of the node data will throw an exception'
            mockCpsDataService.saveData(*_) >>
                { throw new DataValidationException("Invalid JSON", "JSON Data is invalid") }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'the log message contains the correct exception message'
            def debugMessage = appender.list[0].toString()
            assert debugMessage.contains("Creating data node for subscription model failed: Invalid JSON")
        and: 'exception is thrown'
            thrown(NcmpStartUpException)
    }

    def 'Create SchemaSet, Anchor, and TopLevelDataNode is not called if the specified key is not available '() {
        given: 'yangResourceToContentMap contains subscription.yang key with empty value'
            yangResourceToContentMap = Map.of('NE.yang', MODEL_SUBSCRIPTION_YANG)
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'the module service to create schema set was not called'
            0 * mockCpsModuleService.createSchemaSet(*_)
        then: 'the admin service to create an anchor set was not called'
            0 * mockCpsAdminService.createAnchor(*_)
        and: 'the data service to create a top level datanode was not called'
            0 * mockCpsDataService.saveData(*_)
    }

    def 'Create SchemaSet, Anchor, and TopLevelDataNode is not called if the specified key is available but value is empty'() {
        given: 'yangResourceToContentMap contains subscription.yang key with empty value'
            yangResourceToContentMap = Map.of(SUBSCRIPTION_YANG, '')
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'the module service to create schema set was not called'
            0 * mockCpsModuleService.createSchemaSet(*_)
        then: 'the admin service to create an anchor set was not called'
            0 * mockCpsAdminService.createAnchor(*_)
        and: 'the data service to create a top level datanode was not called'
            0 * mockCpsDataService.saveData(*_)
    }

    def 'Create SchemaSet, Anchor, and TopLevelDataNode is not called if the specified key is available but value is blank'() {
        given: 'yangResourceToContentMap contains subscription.yang key with empty value'
            yangResourceToContentMap = Map.of(SUBSCRIPTION_YANG, ' ')
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel(yangResourceToContentMap)
        then: 'the module service to create schema set was not called'
            0 * mockCpsModuleService.createSchemaSet(*_)
        then: 'the admin service to create an anchor set was not called'
            0 * mockCpsAdminService.createAnchor(*_)
        and: 'the data service to create a top level datanode was not called'
            0 * mockCpsDataService.saveData(*_)
    }

    def 'Get file content as string'() {
        when: 'the method to get yang content is called'
            objectUnderTest.getFileContentAsString('NonExistingFile')
        then: 'exception is thrown'
            thrown(NcmpStartUpException)
    }
}