/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl

import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class NetworkCmProxyDataServiceImplModelSyncSpec extends Specification {

    def nullCpsDataService = null
    def mockJsonObjectMapper = Mock(JsonObjectMapper)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockDmiModelOperations = Mock(DmiModelOperations)
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def mockYangModelCmHandleRetriever = Mock(YangModelCmHandleRetriever)
    def nullNetworkCmProxyDataServicePropertyHandler = null

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(nullCpsDataService, mockJsonObjectMapper, mockDmiDataOperations, mockDmiModelOperations,
            mockCpsModuleService, mockCpsAdminService, nullNetworkCmProxyDataServicePropertyHandler,mockYangModelCmHandleRetriever)

    def expectedDataspaceName = 'NFP-Operational'

    def 'Sync model for a (new) cm handle with #scenario'() {
        given: 'a cm handle'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            def dmiServiceName = 'some service name'
            ncmpServiceCmHandle.cmHandleID = 'cm handle id 1'
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle(dmiServiceName, '' , '', ncmpServiceCmHandle)
        and: 'DMI operations returns some module references'
            def moduleReferences =  [ new ModuleReference(moduleName:'module1',revision:'1'),
                                                            new ModuleReference(moduleName:'module2',revision:'2') ]
            mockDmiModelOperations.getModuleReferences(yangModelCmHandle) >> moduleReferences
        and: 'CPS-Core returns list of existing module resources'
            mockCpsModuleService.getYangResourceModuleReferences(expectedDataspaceName) >> toModuleReference(existingModuleResourcesInCps)
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            mockDmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle, [new ModuleReference('module1', '1')]) >> yangResourceToContentMap
        when: 'module sync is triggered'
            mockCpsModuleService.identifyNewModuleReferences(moduleReferences) >> toModuleReference(identifiedNewModuleReferences)
            objectUnderTest.syncModulesAndCreateAnchor(yangModelCmHandle)
        then: 'the CPS module service is called once with the correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(expectedDataspaceName, yangModelCmHandle.getId(), yangResourceToContentMap, toModuleReference(expectedKnownModules))
        and: 'admin service create anchor method has been called with correct parameters'
            1 * mockCpsAdminService.createAnchor(expectedDataspaceName, yangModelCmHandle.getId(), yangModelCmHandle.getId())
        where: 'the following parameters are used'
            scenario             | existingModuleResourcesInCps           | identifiedNewModuleReferences | yangResourceToContentMap      || expectedKnownModules
            'one new module'     | [['module2' : '2'], ['module3' : '3']] | [['module1' : '1']]           | [module1: 'some yang source'] || [['module2' : '2']]
            'no add. properties' | [['module2' : '2'], ['module3' : '3']] | [['module1' : '1']]           | [module1: 'some yang source'] || [['module2' : '2']]
            'no new module'      | [['module1' : '1'], ['module2' : '2']] | []                            | [:]                           || [['module1' : '1'], ['module2' : '2']]
    }

    def toModuleReference(moduleReferenceAsMap) {
        def moduleReferences = [].withDefault { [:] }
        moduleReferenceAsMap.forEach(property ->
            property.forEach((moduleName, revision) -> {
                moduleReferences.add(new ModuleReference('moduleName' : moduleName, 'revision' : revision))
            }))
        return moduleReferences
    }

}
