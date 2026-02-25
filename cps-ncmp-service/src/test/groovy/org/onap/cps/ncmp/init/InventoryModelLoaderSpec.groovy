/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.init.ModelLoaderLock
import org.onap.cps.init.actuator.ReadinessManager
import org.onap.cps.impl.CpsServicesBundle
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR

class InventoryModelLoaderSpec extends Specification {

    def mockModelLoaderLock = Mock(ModelLoaderLock)
    def mockCpsAdminService = Mock(CpsDataspaceService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def cpsServices = new CpsServicesBundle(
            mockCpsAdminService,
            mockCpsModuleService,
            mockCpsAnchorService,
            mockCpsDataService
    )

    def mockApplicationEventPublisher = Mock(ApplicationEventPublisher)
    def mockReadinessManager = Mock(ReadinessManager)
    def mockDataMigration = Mock(DataMigration)
    def objectUnderTest = new InventoryModelLoader(mockModelLoaderLock, cpsServices, mockApplicationEventPublisher, mockReadinessManager, mockDataMigration)

    def applicationContext = new AnnotationConfigApplicationContext()

    def expectedPreviousYangResourceToContentMap
    def expectedNewYangResourceToContentMap
    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.class)
    def loggingListAppender

    void setup() {
        objectUnderTest.isMaster = true
        expectedPreviousYangResourceToContentMap = objectUnderTest.mapYangResourcesToContent('dmi-registry@2026-01-28.yang')
        expectedNewYangResourceToContentMap = objectUnderTest.mapYangResourcesToContent('dmi-registry@2025-07-22.yang')
        objectUnderTest.ignoreModelR20250722 = false
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
        given: 'dataspace is ready for use with current model'
            objectUnderTest.ignoreModelR20250722 = true
            mockCpsAdminService.getDataspace(NCMP_DATASPACE_NAME) >> new Dataspace('')
        and: 'module revision does not exist'
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(_, _, _, _) >> Collections.emptyList()
        and: 'no schema sets exist in the dataspace'
            mockCpsModuleService.getSchemaSets(_) >> []
        and: 'anchor does not exist'
            mockCpsAnchorService.getAnchor(_, _) >> { throw new AnchorNotFoundException('dataspace', 'anchor') }
        when: 'the application is ready'
            objectUnderTest.onApplicationEvent(new ApplicationReadyEvent(Mock(org.springframework.boot.SpringApplication), null, applicationContext, null))
        then: 'the module service is used to create the new schema set from the correct resource'
            1 * mockCpsModuleService.createSchemaSet(NCMP_DATASPACE_NAME, 'dmi-registry-2026-01-28', expectedPreviousYangResourceToContentMap)
        and: 'application event publisher is called once'
            1 * mockApplicationEventPublisher.publishEvent(_)
    }


    def 'Install new model revision'() {
        given: 'the anchor and module revision does not exist'
            mockCpsAnchorService.getAnchor(_, _) >> { throw new AnchorNotFoundException('dataspace', 'anchor') }
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(_, _, _, _) >> Collections.emptyList()
        and: 'no schema sets exist in the dataspace'
            mockCpsModuleService.getSchemaSets(_) >> []
        when: 'the inventory model loader is triggered'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'a new schema set for the 2025-07-22 revision is installed'
            1 * mockCpsModuleService.createSchemaSet(NCMP_DATASPACE_NAME, 'dmi-registry-2025-07-22', expectedNewYangResourceToContentMap)
    }

    def 'Upgrade model revision'() {
        given: 'the anchor exists with old schema set and new module revision is not installed'
            def mockAnchor = Mock(org.onap.cps.api.model.Anchor) { getSchemaSetName() >> 'dmi-registry-2024-02-23' }
            mockCpsAnchorService.getAnchor(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) >> mockAnchor
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(_, _, _, _) >> Collections.emptyList()
        and: 'dataspace has the old schema set'
            def oldSchemaSet = Mock(org.onap.cps.api.model.SchemaSet) { getName() >> 'dmi-registry-2024-02-23' }
            mockCpsModuleService.getSchemaSets(_) >>> [[oldSchemaSet], [oldSchemaSet]]
        when: 'the inventory model loader is triggered'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the new schema set for the 2025-07-22 revision is created'
            1 * mockCpsModuleService.createSchemaSet(NCMP_DATASPACE_NAME, 'dmi-registry-2025-07-22', expectedNewYangResourceToContentMap)
        and: 'the anchor is updated to point to the new schema set'
            1 * mockCpsAnchorService.updateAnchorSchemaSet(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, 'dmi-registry-2025-07-22')
        and: 'the old schema set is kept (not deleted) as it is the previous version'
            0 * mockCpsModuleService.deleteSchemaSet(NCMP_DATASPACE_NAME, 'dmi-registry-2024-02-23', _)
        and: 'data migration is performed'
            1 * mockDataMigration.migrateInventoryToModelRelease20250722(_)
        and: 'log messages confirm successful upgrade'
            assert loggingListAppender.list.any { it.message.contains("Inventory upgraded successfully") }
    }

    def 'Onboarding is skipped when instance is not master'() {
        given: 'instance is not master'
            objectUnderTest.isMaster = false
        when: 'the model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the onboard/upgrade methods are not executed'
            0 * mockCpsModuleService.createSchemaSet(*_)
    }


    def 'Skip upgrade model revision when new revision already installed'() {
        given: 'the anchor exists and the new model revision is already installed'
            mockCpsAnchorService.getAnchor(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) >> {}
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(*_) >> [new ModuleDefinition('', '', '')]
        when: 'the model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'no new schema set is created'
            0 * mockCpsModuleService.createSchemaSet(*_)
        and: 'a log message confirms the revision is already installed'
            assert loggingListAppender.list.any { it.message.contains("already installed") }
    }

    def "Perform inventory data migration to Release20250722"() {
        given: 'the anchor exists with old schema set'
            def mockAnchor = Mock(org.onap.cps.api.model.Anchor) { getSchemaSetName() >> 'dmi-registry-2024-02-23' }
            mockCpsAnchorService.getAnchor(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) >> mockAnchor
        and: 'dataspace has the old schema set'
            def oldSchemaSet = Mock(org.onap.cps.api.model.SchemaSet) { getName() >> 'dmi-registry-2024-02-23' }
            mockCpsModuleService.getSchemaSets(_) >>> [[oldSchemaSet], [oldSchemaSet]]
        when: 'the migration is performed'
            objectUnderTest.upgradeAndMigrateInventoryModel()
        then: 'the call is delegated to the Data Migration service'
            1 * mockDataMigration.migrateInventoryToModelRelease20250722(_)
    }

    def 'Upgrade model revision without migration when ignoreModelR20250722 is true'() {
        given: 'ignoreModelR20250722 is set to true'
            objectUnderTest.ignoreModelR20250722 = true
        and: 'the anchor exists with old schema set and new module revision is not installed'
            def mockAnchor = Mock(org.onap.cps.api.model.Anchor) { getSchemaSetName() >> 'dmi-registry-2025-07-22' }
            mockCpsAnchorService.getAnchor(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) >> mockAnchor
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(_, _, _, _) >> Collections.emptyList()
        and: 'dataspace has the old schema set'
            def oldSchemaSet = Mock(org.onap.cps.api.model.SchemaSet) { getName() >> 'dmi-registry-2025-07-22' }
            mockCpsModuleService.getSchemaSets(_) >>> [[oldSchemaSet], [oldSchemaSet]]
        when: 'the inventory model loader is triggered'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the new schema set for the 2026-01-28 revision is created'
            1 * mockCpsModuleService.createSchemaSet(NCMP_DATASPACE_NAME, 'dmi-registry-2026-01-28', expectedPreviousYangResourceToContentMap)
        and: 'the anchor is updated to point to the new schema set'
            1 * mockCpsAnchorService.updateAnchorSchemaSet(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, 'dmi-registry-2026-01-28')
        and: 'no data migration is performed'
            0 * mockDataMigration.migrateInventoryToModelRelease20250722(_)
        and: 'log messages confirm successful upgrade'
            assert loggingListAppender.list.any { it.message.contains("Inventory upgraded successfully") }
    }

    def 'Skip upgrade when anchor already points to target schema set'() {
        given: 'ignoreModelR20250722 is set to true'
            objectUnderTest.ignoreModelR20250722 = true
        and: 'the anchor already points to the target schema set'
            def mockAnchor = Mock(org.onap.cps.api.model.Anchor) { getSchemaSetName() >> 'dmi-registry-2026-01-28' }
            mockCpsAnchorService.getAnchor(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) >> mockAnchor
            mockCpsModuleService.getModuleDefinitionsByAnchorAndModule(_, _, _, _) >> Collections.emptyList()
        when: 'the inventory model loader is triggered'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'no new schema set is created'
            0 * mockCpsModuleService.createSchemaSet(*_)
        and: 'the anchor is not updated'
            0 * mockCpsAnchorService.updateAnchorSchemaSet(*_)
        and: 'no schema sets are deleted'
            0 * mockCpsModuleService.deleteSchemaSet(*_)
        and: 'log message confirms upgrade was skipped'
            assert loggingListAppender.list.any { it.message.contains("skipping upgrade") }
    }
}

