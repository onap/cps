/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations
import org.onap.cps.ncmp.api.models.PersistenceCmHandle
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.model.ModuleReference
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class NetworkCmProxyDataServiceImplModelSyncSpec extends Specification {

    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockDmiModelOperations = Mock(DmiModelOperations)

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(null, mockDmiModelOperations,
        mockCpsModuleService, null, null, mockCpsAdminService, null)

    def expectedDataspaceName = 'NFP-Operational'

    def 'Sync model for a (new) cm handle with #scenario'() {
        given: 'persistence cm handle is given'
            def cmHandleForModelSync = new PersistenceCmHandle(id:'some cm handle', dmiServiceName: 'some service name')
        and: 'additional properties are set as required'
            if (additionalProperties!=null) {
                cmHandleForModelSync.asAdditionalProperties(additionalProperties)
            }
        and: 'dmi operations returns some module references'
            def jsonData = TestUtils.getResourceFileContent('cmHandleModules.json')
            def moduleReferencesFromCmHandleAsJson = new ResponseEntity<String>(jsonData, HttpStatus.OK)
            mockDmiModelOperations.getModuleReferences(cmHandleForModelSync) >> moduleReferencesFromCmHandleAsJson
        and: 'CPS-Core returns list of existing module resources'
            mockCpsModuleService.getYangResourceModuleReferences(expectedDataspaceName) >> existingModuleResourcesInCps
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            def moduleResources = new ResponseEntity<String>(sdncReponseBody, HttpStatus.OK)
            mockDmiModelOperations.getNewYangResourcesFromDmi(cmHandleForModelSync, [new ModuleReference('module1', '1')]) >> moduleResources
        when: 'module sync is triggered'
            objectUnderTest.syncModulesAndCreateAnchor(cmHandleForModelSync)
        then: 'the CPS module service is called once with the correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(expectedDataspaceName, cmHandleForModelSync.getId(), expectedYangResourceToContentMap, expectedKnownModules)
        and: 'admin service create anchor method has been called with correct parameters'
            1 * mockCpsAdminService.createAnchor(expectedDataspaceName, cmHandleForModelSync.getId(), cmHandleForModelSync.getId())
        where: 'the following parameters are used'
            scenario                        | additionalProperties | existingModuleResourcesInCps                                               | sdncReponseBody                                                                     || expectedYangResourceToContentMap | expectedKnownModules                                                       | expectedJsonForAdditionalProperties
            'one unknown module'            | ['name1': 'value1']  | [new ModuleReference('module2', '2'), new ModuleReference('module3', '3')] | '[{"moduleName" : "module1", "revision" : "1","yangSource": "[some yang source]"}]' || [module1: 'some yang source']    | [new ModuleReference('module2', '2')]                                      | '{"name1":"value1"}'
            'no add. properties'            | [:]                  | [new ModuleReference('module2', '2'), new ModuleReference('module3', '3')] | '[{"moduleName" : "module1", "revision" : "1","yangSource": "[some yang source]"}]' || [module1: 'some yang source']    | [new ModuleReference('module2', '2')]                                      | '{}'
            'additional properties is null' | null                 | [new ModuleReference('module2', '2'), new ModuleReference('module3', '3')] | '[{"moduleName" : "module1", "revision" : "1","yangSource": "[some yang source]"}]' || [module1: 'some yang source']    | [new ModuleReference('module2', '2')]                                      | '{}'
            'no unknown module'             | [:]                  | [new ModuleReference('module1', '1'), new ModuleReference('module2', '2')] | '[]'                                                                                || [:]                              | [new ModuleReference('module1', '1'), new ModuleReference('module2', '2')] | '{}'
    }
}
