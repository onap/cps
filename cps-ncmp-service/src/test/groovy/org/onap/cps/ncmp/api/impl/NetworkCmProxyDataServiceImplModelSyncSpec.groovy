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
import org.jetbrains.annotations.NotNull
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration.DmiProperties
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.model.ModuleReference
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import org.onap.cps.ncmp.api.impl.operation.DmiOperations

class NetworkCmProxyDataServiceImplModelSyncSpec extends Specification {

    public static final String dmiUrl = "http://someUrl/v1/ch/some%20handle/"
    def mockCpsDataService = null
    def mockCpsQueryService = null
    def mockDmiOperations = Mock(DmiOperations)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockRestTemplate = Mock(RestTemplate)
    def mockDmiProperties = Mock(DmiProperties)

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockDmiProperties, mockDmiOperations, mockCpsModuleService,
            mockCpsDataService, mockCpsQueryService, new ObjectMapper(), mockRestTemplate)

    def cmHandle = 'some handle'
    def expectedDataspaceName = 'NFP-Operational'
    def yangResourceToContentMap = Map.ofEntries(
            new AbstractMap.SimpleEntry<String, String>("someModule", "someResource"));
    def listOfCmHandleModules;

    def 'Sync model for a (new) cm handle'() {
        given: 'DMI PLug-in returns a list of module references'
            def jsonData = TestUtils.getResourceFileContent('cmHandleModules.json')
            def moduleReferencesFromCmHandleAsJson = new ResponseEntity<String>(jsonData, HttpStatus.OK);
            mockDmiProperties.getAuthUsername() >> 'someUser'
            mockDmiProperties.getAuthPassword() >> 'somePassword'
            mockDmiProperties.getDmiPluginBasePath() >> 'someUrl'
            mockRestTemplate.postForEntity(dmiUrl + 'modules', *_) >> moduleReferencesFromCmHandleAsJson
        and: 'CPS-Core returns list of known modules'
            def knownModule1 = new ModuleReference('module1','some namespace','1')
            def knownModule2 = new ModuleReference('some other module','some namespace','some revision')
            def knownModules = [ knownModule1, knownModule2 ]
            mockCpsModuleService.getAllYangResourcesModuleReferences() >> knownModules
            def misingModule = new ModuleReference('module2','some namespace','1')
            listOfCmHandleModules = List.of(knownModule1, misingModule);
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            def moduleResources = new ResponseEntity<String>('[{"name" : "someModule", "revision" : "1",' +
                    ' "yang-source": "someResource"}]', HttpStatus.OK);
            mockRestTemplate.postForEntity(dmiUrl + 'moduleResources', *_) >> moduleResources
            // Advanced checks (later) 1) New module is present., existing module is NOT present
        when: 'module Sync is triggered'
            objectUnderTest.modelSync(cmHandle)
        then: 'the CPS module service is called once with the correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(expectedDataspaceName, cmHandle, yangResourceToContentMap , listOfCmHandleModules)
    }
}
