/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.api.exceptions.AnchorNotFoundException
import org.onap.cps.api.model.Dataspace
import org.onap.cps.api.model.ModuleDefinition
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR

class InventoryModelLoaderSpec extends Specification {

    def mockCpsAdminService = Mock(CpsDataspaceService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockApplicationEventPublisher = Mock(ApplicationEventPublisher)
    def objectUnderTest = new InventoryModelLoader(mockCpsAdminService, mockCpsModuleService, mockCpsAnchorService, mockCpsDataService, mockApplicationEventPublisher)

    def applicationContext = new AnnotationConfigApplicationContext()

    def expectedPreviousYangResourceToContentMap
    def expectedNewYangResourceToContentMap
    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender

    void setup() {
        expectedPreviousYangResourceToContentMap = objectUnderTest.mapYangResourcesToContent('dmi-registry@2024-02-23.yang')
        expectedNewYangResourceToContentMap = objectUnderTest.mapYangResourcesToContent('dmi-registry@2025-07-22.yang')
        objectUnderTest.newRevisionEnabled = true
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

    def 'Onboard subscription model via application ready event.'() {
        given: 'dataspace is ready for use'
            objectUnderTest.newRevisionEnabled = false
            mockCpsAdminService.getDataspace(NCMP_DATASPACE_NAME) >> new Dataspace('')
        when: 'the application is started'
            objectUnderTest.onApplicationEvent(Mock(ApplicationStartedEvent))
        then: 'the module service is used to create the new schema set from the correct resource'
            1 * mockCpsModuleService.createSchemaSet(NCMP_DATASPACE_NAME, 'dmi-registry-2024-02-23', expectedPreviousYangResourceToContentMap)
        and: 'No schema sets are being removed by the module service (yet)'
            0 * mockCpsModuleService.deleteSchemaSet(NCMP_DATASPACE_NAME, _, _)
        and: 'application event publisher is called once'
            1 * mockApplicationEventPublisher.publishEvent(_)
    }

    def 'Install new model revision'() {
        given: 'the anchor does not exist'
            mockCpsAnchorService.getAnchor(_, _) >> { throw new AnchorNotFoundException('', '') }
        when: 'the inventory model loader is triggered'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'a new schema set for the 2025-07-22 revision is installed'
            1 * mockCpsModuleService.createSchemaSet(NCMP_DATASPACE_NAME, 'dmi-registry-2025-07-22', expectedNewYangResourceToContentMap)
    }

    def 'Upgrade model revision'() {
        given: 'the anchor exists and new module revision is not installed'
            mockCpsAnchorService.getAnchor(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) >> {}
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(_, _, _, _) >> Collections.emptyList()
        when: 'the inventory model loader is triggered'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the new schema set for the 2025-07-22 revision is created'
            1 * mockCpsModuleService.createSchemaSet(NCMP_DATASPACE_NAME, 'dmi-registry-2025-07-22', expectedNewYangResourceToContentMap)
        and: 'the anchor is updated to point to the new schema set'
            1 * mockCpsAnchorService.updateAnchorSchemaSet(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, 'dmi-registry-2025-07-22')
        and: 'log messages confirm successful upgrade'
            assert loggingListAppender.list.any { it.message.contains("Inventory upgraded successfully") }
    }

    def 'Skip upgrade model revision when new revision already installed'() {
        given: 'the anchor exists and the new model revision is already installed'
            mockCpsAnchorService.getAnchor(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) >> {}
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(_, _, _, _) >> [new ModuleDefinition('', '', '')]
        when: 'the inventory model loader is triggered'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'no new schema set is created'
            0 * mockCpsModuleService.createSchemaSet(_, _, _)
        and: 'a log message confirms the revision is already installed'
            assert loggingListAppender.list.any { it.message.contains("already installed") }
    }
}
