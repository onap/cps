/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.functional.ncmp.inventory

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.init.DataMigration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["ncmp.inventory.model.upgrade.r20250722.enabled=true"])
class DataMigrationIntegrationSpec extends CpsIntegrationSpecBase {

    @Autowired
    DataMigration objectUnderTest

    def 'Migrate inventory with batch processing.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId = (1..2).collectEntries{ ['ch-'+it, ['M1']] }
        and: 'multiple CM handles registered'
            (1..2).each { registerCmHandle(DMI1_URL, 'ch-'+it, NO_MODULE_SET_TAG) }
        when: 'migration is executed'
            objectUnderTest.migrateInventoryToModelRelease20250722(1)
        then: 'all CM handles are processed successfully'
            (1..2).every {
                networkCmProxyInventoryFacade.getNcmpServiceCmHandle('ch-'+it).getCmHandleStatus() == 'READY'
            }
        cleanup: 'deregister CM handles'
            deregisterCmHandles(DMI1_URL, (1..2).collect{ 'ch-'+it })
            cpsAnchorService.deleteAnchor('NCMP-Admin','ncmp-dmi-registry')
            cpsModulePersistenceService.deleteSchemaSet('NCMP-Admin', 'dmi-registry-2025-07-22')
    }
}