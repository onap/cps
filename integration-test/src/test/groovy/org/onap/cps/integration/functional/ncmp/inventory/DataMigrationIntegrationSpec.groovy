/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.impl.cache.CmHandleIdPerReferenceMap
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.utils.YangDataConverter
import org.onap.cps.ncmp.init.InventoryModelLoader
import org.onap.cps.utils.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT

class DataMigrationIntegrationSpec extends CpsIntegrationSpecBase {

    @Autowired
    InventoryModelLoader objectUnderTest

    @Autowired
    InventoryPersistence inventoryPersistence

    @Autowired
    CmHandleIdPerReferenceMap cmHandleIdPerReferenceMap

     def 'Migrate inventory with batch processing when the alternate-id cache is not yet populated.'() {
        given: 'start with the old model (no top-level cm-handle-state leaf written on registration)'
            ReflectionTestUtils.setField(inventoryPersistence, 'useOptimizedModel', false)
        and: 'DMI will return modules when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId = (1..2).collectEntries{ ['ch-'+it, ['M1']] }
        and: 'multiple CM handles registered with old model, each with an additional property'
            (1..2).each {
                registerCmHandle(DMI1_URL, 'ch-'+it, NO_MODULE_SET_TAG)
                cpsDataService.saveListElements('NCMP-Admin', 'ncmp-dmi-registry', "/dmi-registry/cm-handles[@id='ch-${it}']",
                        '{"additional-properties":[{"name":"prop1","value":"value1"}]}', now, ContentType.JSON)
            }
        and: 'the top-level cm-handle-state leaf is confirmed absent before migration'
            getYangModelCmHandles('ch-').each {
                assert it.getCmHandleStatus() == null
                assert it.getCompositeState().getCmHandleState().name() == 'READY'
            }
        and: 'the alternate-id cache is emptied to mimic a fresh pod startup (cache is populated only after model loading)'
            getYangModelCmHandles('ch-').each { cmHandleIdPerReferenceMap.removeAll([it]) }
            assert cmHandleIdPerReferenceMap.isEmpty()
        when: 'the new (more performant) model is enabled and inventory is upgraded (which triggers migration)'
            ReflectionTestUtils.setField(inventoryPersistence, 'useOptimizedModel', true)
            ReflectionTestUtils.setField(objectUnderTest, 'ignoreR20260423Model', false)
            objectUnderTest.onboardOrUpgradeModel()
        then: 'all CM handles have top-level cm-handle-state and dmi-properties populated, despite the empty cache'
            def migratedCmHandles = getYangModelCmHandles('ch-')
            assert migratedCmHandles.size() == 2
            migratedCmHandles.each {
                assert it.getCmHandleStatus() == 'READY'
                assert it.getDmiProperties() == '{"prop1":"value1"}'
            }
        cleanup: 'deregister CM handles and restore flag'
            ReflectionTestUtils.setField(inventoryPersistence, 'useOptimizedModel', false)
            ReflectionTestUtils.setField(objectUnderTest, 'ignoreR20260423Model', true)
            deregisterCmHandles(DMI1_URL, (1..2).collect{ 'ch-'+it })
    }

    def getYangModelCmHandles(cmHandleIdPrefix) {
        def childDataNodes = inventoryPersistence.getDataNode(NCMP_DMI_REGISTRY_PARENT, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
                .iterator().next().getChildDataNodes()
        return childDataNodes.collect { YangDataConverter.toYangModelCmHandle(it) }.findAll { it.getId().startsWith(cmHandleIdPrefix) }
    }
}
