/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation
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

package org.onap.cps.integration.performance.cps

import org.onap.cps.integration.performance.base.CpsPerfTestBase
import org.onap.cps.api.model.ModuleReference

class ModuleQueryPerfTest extends CpsPerfTestBase {

    static final KILOBYTE = 1000
    static final TOTAL_TEST_ANCHORS = 10_000
    static final SCHEMA_SET_PREFIX = 'mySchemaSet'
    static final ANCHOR_PREFIX = 'myAnchor'
    static final MODULE_REVISION = '2024-04-25'
    static final MODULE_TEMPLATE = """
        module <MODULE_NAME> {
            yang-version 1.1;
            namespace "org:onap:cps:test:<MODULE_NAME>";
            prefix tree;
            revision "<MODULE_REVISION>" {
                description "<DESCRIPTION>";
            }
            container tree {
                list branch {
                    key "name";
                    leaf name {
                        type string;
                    }
                }
            }
        }
    """

    def 'Module query - Preload test data (needed for other tests).'() {
        given: 'a schema set with different sizes of Yang modules is created'
            cpsModuleService.createSchemaSet(CPS_PERFORMANCE_TEST_DATASPACE, SCHEMA_SET_PREFIX + '0', [
                    'module0.yang': makeYangModuleOfLength('module0', 1 * KILOBYTE),
                    'module1.yang': makeYangModuleOfLength('module1', 1000 * KILOBYTE)
            ])
        and: 'these modules will be used again to create many schema sets'
            def allModuleReferences = [
                    new ModuleReference('module0', MODULE_REVISION),
                    new ModuleReference('module1', MODULE_REVISION)
            ]
        when: 'many schema sets and anchors are created using those modules'
            resourceMeter.start()
            (1..TOTAL_TEST_ANCHORS).each {
                def schemaSetName = SCHEMA_SET_PREFIX + it
                def anchorName = ANCHOR_PREFIX + it
                cpsModuleService.createSchemaSetFromModules(CPS_PERFORMANCE_TEST_DATASPACE, schemaSetName, [:], allModuleReferences)
                cpsAnchorService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, schemaSetName, anchorName)
            }
            resourceMeter.stop()
        then: 'operation takes less than expected duration'
            recordAndAssertResourceUsage('Module query test setup',
                    45, resourceMeter.totalTimeInSeconds,
                    500, resourceMeter.totalMemoryUsageInMB
            )
    }

    def 'Querying anchors by module name is NOT dependant on the file size of the module.'() {
        when: 'we search for anchors with given Yang module name'
            resourceMeter.start()
            def result = cpsAnchorService.queryAnchorNames(CPS_PERFORMANCE_TEST_DATASPACE, [yangModuleName])
            resourceMeter.stop()
        then: 'expected number of anchors is returned'
            assert result.size() == TOTAL_TEST_ANCHORS
        and: 'operation completes with expected resource usage'
            recordAndAssertResourceUsage("Query for anchors with ${scenario}",
                    expectedTimeInSeconds, resourceMeter.totalTimeInSeconds,
                    5, resourceMeter.totalMemoryUsageInMB)
        where: 'the following parameters are used'
            scenario         | yangModuleName || expectedTimeInSeconds
            '1 KB module'    | 'module0'      || 0.05
            '1000 KB module' | 'module1'      || 0.05
    }

    def 'Module query - Clean up test data.'() {
        cleanup:
            // FIXME this API has extremely high memory usage, therefore external batching must be used
            for (int i = 1; i <= TOTAL_TEST_ANCHORS; i += 100) {
                cpsModuleService.deleteSchemaSetsWithCascade(CPS_PERFORMANCE_TEST_DATASPACE, (i..i+100).collect {SCHEMA_SET_PREFIX + it})
            }
            cpsModuleService.deleteSchemaSetsWithCascade(CPS_PERFORMANCE_TEST_DATASPACE, [SCHEMA_SET_PREFIX + '0'])
            cpsModuleService.deleteAllUnusedYangModuleData(CPS_PERFORMANCE_TEST_DATASPACE)
    }

    // This makes a Yang module of approximately target length in bytes by padding the description field with many '*'
    private static def makeYangModuleOfLength(moduleName, targetLength) {
        def padding = String.valueOf('*').repeat(targetLength - MODULE_TEMPLATE.size()) // not exact
        return MODULE_TEMPLATE
                .replaceAll('<MODULE_NAME>', moduleName)
                .replaceAll('<MODULE_REVISION>', MODULE_REVISION)
                .replaceAll('<DESCRIPTION>', padding)
    }
}
