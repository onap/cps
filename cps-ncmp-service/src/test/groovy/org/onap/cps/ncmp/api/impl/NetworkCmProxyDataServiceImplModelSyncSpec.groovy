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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration.DmiProperties
import org.onap.cps.ncmp.api.models.PersistenceCmHandle
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.model.ModuleReference
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import org.onap.cps.ncmp.api.impl.operation.DmiOperations

class NetworkCmProxyDataServiceImplModelSyncSpec extends Specification {

    def mockCpsDataService = null
    def mockCpsQueryService = null
    def mockDmiOperations = Mock(DmiOperations)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockDmiProperties = Mock(DmiProperties)

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockDmiOperations, mockCpsModuleService,
            mockCpsDataService, mockCpsQueryService, new ObjectMapper())

    def cmHandle = new PersistenceCmHandle(id:'some cm handle', dmiServiceName: 'some service name')
    def expectedDataspaceName = 'NCMP-Admin'
    def NO_NAMESPACE = null
    def knownModule1 = new ModuleReference('module1', NO_NAMESPACE, '1')
    def knownOtherModule = new ModuleReference('some other module', NO_NAMESPACE, 'some revision')

    def 'Sync model for a (new) cm handle with #scenario'() {
        given: 'DMI PLug-in returns a list of module references'
            getModulesForCmHandle()
        and: 'CPS-Core returns list of known modules'
            mockCpsModuleService.getAllYangResourcesModuleReferences() >> [knownModule1, knownOtherModule]
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            def moduleResources = new ResponseEntity<String>(reponseBody, HttpStatus.OK)
            mockDmiOperations.getResourceFromDmi(_, cmHandle.getId(), 'moduleResources') >> moduleResources
        when: 'module Sync is triggered'
            objectUnderTest.modelSync(cmHandle)
        then: 'the CPS module service is called once with the correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(expectedDataspaceName, cmHandle.getId(), expectedYangResourceToContentMap , [knownModule1])
        where:
            scenario             | reponseBody                                                                 || expectedYangResourceToContentMap
            'one unknown module' | '[{"name" : "someModule", "revision" : "1","yang-source": "someResource"}]' || [someModule: 'someResource']
            'no unknown module'  | '[]'                                                                        || [:]
    }

    def getModulesForCmHandle() {
        def jsonData = TestUtils.getResourceFileContent('cmHandleModules.json')
        def moduleReferencesFromCmHandleAsJson = new ResponseEntity<String>(jsonData, HttpStatus.OK)
        mockDmiProperties.getAuthUsername() >> 'someUser'
        mockDmiProperties.getAuthPassword() >> 'somePassword'
        mockDmiProperties.getDmiPluginBasePath() >> 'someUrl'
        mockDmiOperations.getResourceFromDmi(_, cmHandle.getId(), 'modules') >> moduleReferencesFromCmHandleAsJson
    }

}
