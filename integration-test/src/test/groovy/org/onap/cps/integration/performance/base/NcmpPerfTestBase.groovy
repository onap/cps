/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

import java.time.OffsetDateTime
import org.onap.cps.integration.ResourceMeter

class NcmpPerfTestBase extends PerfTestBase {

    def static NCMP_PERFORMANCE_TEST_DATASPACE = 'ncmpPerformance'
    def static REGISTRY_ANCHOR = 'ncmp-registry'
    def static REGISTRY_SCHEMA_SET = 'registrySchemaSet'
    def static CM_DATA_SUBSCRIPTIONS_ANCHOR = 'cm-data-subscriptions'
    def static CM_DATA_SUBSCRIPTIONS_SCHEMA_SET = 'cmDataSubscriptionsSchemaSet'

    def datastore1cmHandlePlaceHolder = '{"datastores":{"datastore":[{"name":"ds-1","cm-handles":{"cm-handle":[]}}]}}'
    def xPathForDataStore1CmHandles = '/datastores/datastore[@name="ds-1"]/cm-handles'
    def numberOfCmDataSubscribers = 200
    def numberOfFiltersPerCmHandle = 10
    def numberOfCmHandlesPerCmDataSubscription = 200

    ResourceMeter resourceMeter = new ResourceMeter()

// SHORT versions for easier debugging
//    def subscriberIdPrefix = 'sub'
//    def xpathPrefix = 'f'
//    def cmHandlePrefix = 'ch'


// LONG versions for performance testing
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
        addCmSubscriptionData()
    }

    def createInitialData() {
        cpsAnchorService.createAnchor(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_SCHEMA_SET, REGISTRY_ANCHOR)
        def data = readResourceDataFile('ncmp-registry/1000-cmhandles.json')
        cpsDataService.saveData(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, data, OffsetDateTime.now())
    }

    def createRegistrySchemaSet() {
        def modelAsString = readResourceDataFile('ncmp-registry/dmi-registry@2023-11-27.yang')
        cpsModuleService.createSchemaSet(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_SCHEMA_SET, [registry: modelAsString])
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
}
