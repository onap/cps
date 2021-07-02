/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.ncmp.api.impl.NetworkCmProxyDataServiceImpl
import org.onap.cps.spi.FetchDescendantsOption
import spock.lang.Specification

class NetworkCmProxyDataServiceImplSpec extends Specification {
    def objectUnderTest = new NetworkCmProxyDataServiceImpl()
    def mockcpsDataService = Mock(CpsDataService)
    def mockcpsQueryService = Mock(CpsQueryService)

    def setup() {
        objectUnderTest.cpsDataService = mockcpsDataService
        objectUnderTest.cpsQueryService = mockcpsQueryService
    }

    def cmHandle = 'some handle'
    def expectedDataspaceName = 'NFP-Operational'

    def expectedCmRegistrationHandlerAnchorName = 'ncmp-dmi-registry'
    def expectedCmRegistrationHandlerDataspaceName = 'NCMP-Admin'

    def 'Query data nodes by cps path with #fetchDescendantsOption.'() {
        given: 'a cm Handle and a cps path'
            def cpsPath = '/cps-path'
        when: 'queryDataNodes is invoked'
            objectUnderTest.queryDataNodes(cmHandle, cpsPath, fetchDescendantsOption)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockcpsQueryService.queryDataNodes(expectedDataspaceName, cmHandle, cpsPath, fetchDescendantsOption)
        where: 'all fetch descendants options are supported'
            fetchDescendantsOption << FetchDescendantsOption.values()
    }

    def 'Create full data node: #scenario.'() {
        given: 'a cm handle and root xpath'
            def jsonData = 'some json'
        when: 'createDataNode is invoked'
            objectUnderTest.createDataNode(cmHandle, xpath, jsonData)
        then: 'the CPS service method is invoked once with the expected parameters'
            1 * mockcpsDataService.saveData(expectedDataspaceName, cmHandle, jsonData)
        where: 'following parameters were used'
            scenario           | xpath
            'no xpath'         | ''
            'root level xpath' | '/'
    }

    def 'Create child data node.'() {
        given: 'a cm handle and parent node xpath'
            def jsonData = 'some json'
            def xpath = '/test-node'
        when: 'createDataNode is invoked'
            objectUnderTest.createDataNode(cmHandle, xpath, jsonData)
        then: 'the CPS service method is invoked once with the expected parameters'
            1 * mockcpsDataService.saveData(expectedDataspaceName, cmHandle, xpath, jsonData)
    }

    def 'Add list-node elements.'() {
        given: 'a cm handle and parent node xpath'
            def jsonData = 'some json'
            def xpath = '/test-node'
        when: 'addListNodeElements is invoked'
            objectUnderTest.addListNodeElements(cmHandle, xpath, jsonData)
        then: 'the CPS service method is invoked once with the expected parameters'
            1 * mockcpsDataService.saveListNodeData(expectedDataspaceName, cmHandle, xpath, jsonData)
    }

    def 'Update data node leaves.'() {
        given: 'a cm Handle and a cps path'
            def xpath = '/xpath'
            def jsonData = 'some json'
        when: 'updateNodeLeaves is invoked'
            objectUnderTest.updateNodeLeaves(cmHandle, xpath, jsonData)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockcpsDataService.updateNodeLeaves(expectedDataspaceName, cmHandle, xpath, jsonData)
    }

    def 'Replace data node tree.'() {
        given: 'a cm Handle and a cps path'
            def xpath = '/xpath'
            def jsonData = 'some json'
        when: 'replaceNodeTree is invoked'
            objectUnderTest.replaceNodeTree(cmHandle, xpath, jsonData)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockcpsDataService.replaceNodeTree(expectedDataspaceName, cmHandle, xpath, jsonData)
    }

    def 'Register CM Handle.'(){
        given: 'a dataspace, anchor and json data'
            def jsonData = 'json data'
        when: 'registerCmHandleEvent is invoked'
            objectUnderTest.registerCmHandleEvent(jsonData)
        then: 'the CPS service method is invoked once with the expected parameters'
            1 * mockcpsDataService.saveData(expectedCmRegistrationHandlerDataspaceName, expectedCmRegistrationHandlerAnchorName, jsonData)
    }

}
