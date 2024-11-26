/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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

package org.onap.cps.integration.performance.base

import org.onap.cps.integration.ResourceMeter
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.utils.ContentType

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT

class NcmpPerfTestBase extends PerfTestBase {

    def static NCMP_PERFORMANCE_TEST_DATASPACE = 'ncmpPerformance'
    def static REGISTRY_ANCHOR = NCMP_DMI_REGISTRY_ANCHOR
    def static REGISTRY_PARENT = NCMP_DMI_REGISTRY_PARENT
    def static REGISTRY_SCHEMA_SET = 'registrySchemaSet'
    def static TOTAL_CM_HANDLES = 20_000
    def static CM_DATA_SUBSCRIPTIONS_ANCHOR = 'cm-data-subscriptions'
    def static CM_DATA_SUBSCRIPTIONS_SCHEMA_SET = 'cmDataSubscriptionsSchemaSet'

    def datastore1cmHandlePlaceHolder = '{"datastores":{"datastore":[{"name":"ds-1","cm-handles":{"cm-handle":[]}}]}}'
    def xPathForDataStore1CmHandles = '/datastores/datastore[@name="ds-1"]/cm-handles'
    def numberOfCmDataSubscribers = 200
    def numberOfFiltersPerCmHandle = 10
    def numberOfCmHandlesPerCmDataSubscription = 200

    ResourceMeter resourceMeter = new ResourceMeter()

    def subscriberIdPrefix = 'some really long subscriber id to see if this makes any difference to the performance'
    def xpathPrefix = 'some really long xpath/with/loads/of/children/grandchildren/and/whatever/else/I/can/think/of to see if this makes any difference to the performance'
    def cmHandlePrefix = 'some really long cm handle id to see if this makes any difference to the performance'

    def printTitle() {
        println('##                  N C M P   P E R F O R M A N C E   T E S T   R E S U L T S                   ##')
    }

    def isInitialised() {
        return dataspaceExists(NCMP_PERFORMANCE_TEST_DATASPACE)
    }

    def setupPerformanceInfraStructure() {
        cpsDataspaceService.createDataspace(NCMP_PERFORMANCE_TEST_DATASPACE)
        createRegistrySchemaSet()
        createCmDataSubscriptionsSchemaSet()
    }

    def createInitialData() {
        addRegistryData()
        addRegistryDataWithAlternateIdAsPath()
        addCmSubscriptionData()
    }

    def createRegistrySchemaSet() {
        def modelAsString = readResourceDataFile('inventory/dmi-registry@2024-02-23.yang')
        cpsModuleService.createSchemaSet(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_SCHEMA_SET, [registry: modelAsString])
    }

    def addRegistryData() {
        cpsAnchorService.createAnchor(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_SCHEMA_SET, REGISTRY_ANCHOR)
        cpsDataService.saveData(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, '{"dmi-registry": []}', now)
        def cmHandleJsonTemplate = readResourceDataFile('inventory/cmHandleTemplate.json')
        def batchSize = 100
        for (def i = 0; i < TOTAL_CM_HANDLES; i += batchSize) {
            def data = '{ "cm-handles": [' + (1..batchSize).collect { cmHandleJsonTemplate.replace('CM_HANDLE_ID_HERE', (it + i).toString()) }.join(',') + ']}'
            cpsDataService.saveListElements(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, REGISTRY_PARENT, data, now, ContentType.JSON)
        }
    }

    def addRegistryDataWithAlternateIdAsPath() {
        def cmHandleWithAlternateIdTemplate = readResourceDataFile('inventory/cmHandleWithAlternateIdTemplate.json')
        def batchSize = 10
        for (def i = 0; i < TOTAL_CM_HANDLES; i += batchSize) {
            def data = '{ "cm-handles": [' + (1..batchSize).collect {
                cmHandleWithAlternateIdTemplate.replace('CM_HANDLE_ID_HERE', (it + i).toString())
                        .replace('ALTERNATE_ID_AS_PATH', (it + i).toString())
            }.join(',') + ']}'
            cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, REGISTRY_PARENT, data, now, ContentType.JSON)
        }
    }

    def createCmDataSubscriptionsSchemaSet() {
        def modelAsString = readResourceDataFile('cm-data-subscriptions/cm-data-subscriptions@2023-09-21.yang')
        cpsModuleService.createSchemaSet(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_SCHEMA_SET, [registry: modelAsString])
    }

    def addCmSubscriptionData() {
        cpsAnchorService.createAnchor(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_SCHEMA_SET, CM_DATA_SUBSCRIPTIONS_ANCHOR)
        cpsDataService.saveData(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, datastore1cmHandlePlaceHolder, now)
        def subscribers = createLeafList('subscribers',numberOfCmDataSubscribers, subscriberIdPrefix)
        def filters = '"filters":' + createJsonArray('filter',numberOfFiltersPerCmHandle,'xpath',xpathPrefix,subscribers)
        def cmHandles = createJsonArray('cm-handle',numberOfCmHandlesPerCmDataSubscription,'id',cmHandlePrefix, filters)
        cpsDataService.saveData(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, xPathForDataStore1CmHandles, cmHandles, now)
    }

    def 'NCMP pre-load test data'() {
        when: 'dummy get data nodes runs so that populating the DB does not get included in other test timings'
            resourceMeter.start()
            def result = cpsDataService.getDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, '/', FetchDescendantsOption.OMIT_DESCENDANTS)
            resourceMeter.stop()
        then: 'expected data exists'
            assert result.xpath == [REGISTRY_PARENT]
        and: 'operation completes within expected time'
            recordAndAssertResourceUsage('NCMP pre-load test data',
                    15, resourceMeter.totalTimeInSeconds,
                    600, resourceMeter.totalMemoryUsageInMB)
    }

}
