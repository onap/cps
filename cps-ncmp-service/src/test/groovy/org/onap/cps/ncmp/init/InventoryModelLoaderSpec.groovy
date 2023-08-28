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
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.spi.model.Dataspace
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

class InventoryModelLoaderSpec extends Specification {

    def mockCpsAdminService = Mock(CpsAdminService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def objectUnderTest = new InventoryModelLoader(mockCpsAdminService, mockCpsModuleService, mockCpsDataService)

    def applicationContext = new AnnotationConfigApplicationContext()

    def expectedYangResourceToContentMap
    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender

    void setup() {
        expectedYangResourceToContentMap = objectUnderTest.createYangResourceToContentMap('dmi-registry@2023-08-23.yang')
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
        applicationContext.refresh()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(SubscriptionModelLoader.class)).detachAndStopAllAppenders()
        applicationContext.close()
    }

    def 'Onboard subscription model via application ready event.'() {
        given: 'dataspace is ready for use'
            mockCpsAdminService.getDataspace('NCMP-Admin') >> new Dataspace('')
        when: 'the application is ready'
            objectUnderTest.onApplicationEvent(Mock(ApplicationReadyEvent))
        then: 'the module service is used to create the new schema set from the correct resource'
            1 * mockCpsModuleService.createSchemaSet('NCMP-Admin', 'dmi-registry-2023-08-23', expectedYangResourceToContentMap)
        and: 'the admin service is used to update the anchor'
            1 * mockCpsAdminService.updateAnchorSchemaSet('NCMP-Admin', 'ncmp-dmi-registry', 'dmi-registry-2023-08-23')
        and: 'No schema sets are being removed by the module service (yet)'
            0 * mockCpsModuleService.deleteSchemaSet('NCMP-Admin', _, _)
    }

}
