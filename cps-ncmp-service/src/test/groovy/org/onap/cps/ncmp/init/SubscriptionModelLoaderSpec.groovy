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
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.exception.NcmpException
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.springframework.boot.SpringApplication
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import spock.lang.Specification

class SubscriptionModelLoaderSpec extends Specification {

    def mockCpsAdminService = Mock(CpsAdminService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def objectUnderTest = new SubscriptionModelLoader(mockCpsAdminService, mockCpsModuleService)

    def SUBSCRIPTION_DATASPACE_NAME = objectUnderTest.SUBSCRIPTION_DATASPACE_NAME;
    def SUBSCRIPTION_ANCHOR_NAME = objectUnderTest.SUBSCRIPTION_ANCHOR_NAME;
    def SUBSCRIPTION_SCHEMASET_NAME = objectUnderTest.SUBSCRIPTION_SCHEMASET_NAME;

    def sampleYangContentMap = ['subscription.yang':'module subscription { *sample content* }']

    def applicationReadyEvent = new ApplicationReadyEvent(new SpringApplication(), null, null, null)

    def logger
    def appender

    @BeforeEach
    void setup() {
        logger = (Logger) LoggerFactory.getLogger(objectUnderTest.getClass())
        appender = new ListAppender()
        logger.setLevel(Level.DEBUG)
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    void teardown() {
        ((Logger) LoggerFactory.getLogger(SubscriptionModelLoader.class)).detachAndStopAllAppenders();
    }

    def 'Onboard subscription model successfully via application ready event'() {
        when: 'the application is ready'
            objectUnderTest.onApplicationEvent(applicationReadyEvent)
        then: 'the module service to create schema set is called once'
            1 * mockCpsModuleService.createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME,sampleYangContentMap)
        and: 'the admin service to create an anchor set is called once'
            1 * mockCpsAdminService.createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME)
    }

    def 'Create schema set from model file'() {
        given: 'the method to create yang resource to content map returns the correct map'
            def yangResourceToContentMap = objectUnderTest.createYangResourceToContentMap()
        when: 'the method to create schema set is called with the following parameters'
            objectUnderTest.createSchemaSet("myDataspace", "mySchemaSet", yangResourceToContentMap)
        then: 'yang resource to content map is as expected'
            assert sampleYangContentMap == yangResourceToContentMap
        and: 'the module service to create schema set is called once with the correct map'
            1 * mockCpsModuleService.createSchemaSet(_, _, yangResourceToContentMap)
    }

    def 'Create schema set fails due to AlreadyDefined exception'() {
        given: 'the method to create yang resource to content map returns the correct map'
            def yangResourceToContentMap = objectUnderTest.createYangResourceToContentMap()
        and: 'creating a schema set throws an exception as it already exists'
            mockCpsModuleService.createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, yangResourceToContentMap) >>
                    { throw AlreadyDefinedException.forSchemaSet(SUBSCRIPTION_SCHEMASET_NAME, "sampleContextName", null) }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel()
        then: 'the admin service to create an anchor set is then called once'
            1 * mockCpsAdminService.createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME)
    }

    def 'Create schema set fails due to any other exception'() {
        given: 'the method to create yang resource to content map returns the correct map'
            def yangResourceToContentMap = objectUnderTest.createYangResourceToContentMap()
        and: 'creating a schema set throws an exception'
            mockCpsModuleService.createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, yangResourceToContentMap) >>
                    { throw new NcmpException("Creating schema set failed", ""); }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel()
        then: 'the log message contains the correct exception message'
            def debugMessage = appender.list[0].toString()
            assert debugMessage.contains("Creating schema set failed")
        and: 'exception is thrown'
            thrown(NcmpException)
    }

    def 'Create anchor fails due to AlreadyDefined exception'() {
        given: 'creating anchor throws an exception as it already exists'
            mockCpsAdminService.createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME) >>
                    { throw AlreadyDefinedException.forSchemaSet(SUBSCRIPTION_SCHEMASET_NAME, "sampleContextName", null) }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel()
        then: 'no exception thrown'
            noExceptionThrown()
    }

    def 'Create anchor fails due to any other exception'() {
        given: 'creating an anchor failed'
            mockCpsAdminService.createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME) >>
                    { throw new SchemaSetNotFoundException(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME) }
        when: 'the method to onboard model is called'
            objectUnderTest.onboardSubscriptionModel()
        then: 'the log message contains the correct exception message'
            def debugMessage = appender.list[0].toString()
            assert debugMessage.contains("Schema Set not found")
        and: 'exception is thrown'
            thrown(NcmpException)
    }

    def 'Get file content as string'() {
        when: 'the method to get yang content is called'
            def response = objectUnderTest.getFileContentAsString()
        then: 'the response is as expected'
            assert response == 'module subscription { *sample content* }'
    }
}