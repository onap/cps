/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modification Copyright (C) 2021 highstreet technologies GmbH
 *  Modification Copyright (C) 2021 Nordix Foundation
 *  Modification Copyright (C) 2021 Bell Canada.
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

package org.onap.cps.ncmp.rest.controller

import org.onap.cps.spi.model.ModuleReference

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import org.onap.cps.TestUtils
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.spi.model.DataNodeBuilder
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

@WebMvcTest(NetworkCmProxyController)
class NetworkCmProxyControllerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    NetworkCmProxyDataService mockNetworkCmProxyDataService = Mock()

    @SpringBean
    ObjectMapper objectMapper = new ObjectMapper()

    @Value('${rest.api.ncmp-base-path}/v1')
    def ncmpBasePathV1

    def cmHandle = 'some handle'
    def xpath = 'some xpath'

    def 'Query data node by cps path for the given cm handle with #scenario.'() {
        given: 'service method returns a list containing a data node'
            def dataNode = new DataNodeBuilder().withXpath('/xpath').build()
            def cpsPath = 'some cps-path'
            mockNetworkCmProxyDataService.queryDataNodes(cmHandle, cpsPath, expectedCpsDataServiceOption) >> [dataNode]
        and: 'the query endpoint'
            def dataNodeEndpoint = "$ncmpBasePathV1/cm-handles/$cmHandle/nodes/query"
        when: 'query data nodes API is invoked'
            def response = mvc.perform(get(dataNodeEndpoint)
                    .param('cps-path', cpsPath)
                    .param('include-descendants', includeDescendantsOption))
                    .andReturn().response
        then: 'the response contains the the datanode in json format'
            response.status == HttpStatus.OK.value()
            def expectedJsonContent = new Gson().toJson(dataNode)
            response.getContentAsString().contains(expectedJsonContent)
        where: 'the following options for include descendants are provided in the request'
            scenario                    | includeDescendantsOption || expectedCpsDataServiceOption
            'no descendants by default' | ''                       || OMIT_DESCENDANTS
            'no descendant explicitly'  | 'false'                  || OMIT_DESCENDANTS
            'descendants'               | 'true'                   || INCLUDE_ALL_DESCENDANTS
    }

    def 'Create data node: #scenario.'() {
        given: 'json data'
            def jsonData = 'json data'
        when: 'post request is performed'
            def response = mvc.perform(
                    post("$ncmpBasePathV1/cm-handles/$cmHandle/nodes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonData)
                            .param('xpath', reqXpath)
            ).andReturn().response
        then: 'the service method is invoked once with expected parameters'
            1 * mockNetworkCmProxyDataService.createDataNode(cmHandle, usedXpath, jsonData)
        and: 'response status indicates success'
            response.status == HttpStatus.CREATED.value()
        where: 'following parameters were used'
            scenario             | reqXpath || usedXpath
            'no xpath parameter' | ''       || '/'
            'root xpath'         | '/'      || '/'
            'parent node xpath'  | '/xpath' || '/xpath'
    }

    def 'Add list-node elements.'() {
        given: 'json data and parent node xpath'
            def jsonData = 'json data'
            def parentNodeXpath = 'parent node xpath'
        when: 'post request is performed'
            def response = mvc.perform(
                    post("$ncmpBasePathV1/cm-handles/$cmHandle/list-node")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonData)
                            .param('xpath', parentNodeXpath)
            ).andReturn().response
        then: 'the service method is invoked once with expected parameters'
            1 * mockNetworkCmProxyDataService.addListNodeElements(cmHandle, parentNodeXpath, jsonData)
        and: 'response status indicates success'
            response.status == HttpStatus.CREATED.value()
    }

    def 'Update data node leaves.'() {
        given: 'json data'
            def jsonData = 'json data'
        and: 'the query endpoint'
            def endpoint = "$ncmpBasePathV1/cm-handles/$cmHandle/nodes"
        when: 'patch request is performed'
            def response = mvc.perform(
                    patch(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonData)
                            .param('xpath', xpath)
            ).andReturn().response
        then: 'the service method is invoked once with expected parameters'
            1 * mockNetworkCmProxyDataService.updateNodeLeaves(cmHandle, xpath, jsonData)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
    }

    def 'Replace data node tree.'() {
        given: 'json data'
            def jsonData = 'json data'
        and: 'the query endpoint'
            def endpoint = "$ncmpBasePathV1/cm-handles/$cmHandle/nodes"
        when: 'put request is performed'
            def response = mvc.perform(
                    put(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonData)
                            .param('xpath', xpath)
            ).andReturn().response
        then: 'the service method is invoked once with expected parameters'
            1 * mockNetworkCmProxyDataService.replaceNodeTree(cmHandle, xpath, jsonData)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
    }

    def 'Get data node.'() {
        given: 'the service returns a data node'
            def xpath = 'some xpath'
            def dataNode = new DataNodeBuilder().withXpath(xpath).withLeaves(["leaf": "value"]).build()
            mockNetworkCmProxyDataService.getDataNode(cmHandle, xpath, OMIT_DESCENDANTS) >> dataNode
        and: 'the query endpoint'
            def endpoint = "$ncmpBasePathV1/cm-handles/$cmHandle/node"
        when: 'get request is performed through REST API'
            def response = mvc.perform(get(endpoint).param('xpath', xpath)).andReturn().response
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
        and: 'response contains expected leaf and value'
            response.contentAsString.contains('"leaf":"value"')
    }

    def 'Register CM Handle Event' () {
        given: 'jsonData'
            def jsonData = TestUtils.getResourceFileContent('dmi-registration.json')
        when: 'post request is performed'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
            ).andReturn().response
        then: 'the cm handles are registered with the service'
            1 * mockNetworkCmProxyDataService.updateDmiRegistrationAndSyncModule(_)
        and: 'response status is created'
            response.status == HttpStatus.CREATED.value()
    }

    def 'Get Resource Data from pass-through operational.' () {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-operational" +
                    "?resourceIdentifier=parent/child&fields=testFields&depth=5"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                    get(getUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            ).andReturn().response
        then: 'the NCMP data service is called with getResourceDataOperationalForCmHandle'
            1 * mockNetworkCmProxyDataService.getResourceDataOperationalForCmHandle('testCmHandle',
                    'parent/child',
                    'application/json',
                    'testFields',
                    5)
        and: 'response status is Ok'
            response.status == HttpStatus.OK.value()
    }

    def 'Get Resource Data from pass-through running with #scenario value in resource identifier param.' () {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                    "?resourceIdentifier=" + resourceIdentifier + "&fields=testFields&depth=5"
        and: 'ncmp service returns json object'
            mockNetworkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    resourceIdentifier,
                    'application/json',
                    'testFields',
                    5) >> '{valid-json}'
        when: 'get data resource request is performed'
            def response = mvc.perform(
                    get(getUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON_VALUE)
            ).andReturn().response
        then: 'response status is Ok'
            response.status == HttpStatus.OK.value()
        and: 'response contains valid object body'
            response.getContentAsString() == '{valid-json}'
        where: 'tokens are used in the resource identifier parameter'
            scenario                       | resourceIdentifier
            '/'                            | 'id/with/slashes'
            '?'                            | 'idWith?'
            ','                            | 'idWith,'
            '='                            | 'idWith='
            '[]'                           | 'idWith[]'
            '? needs to be encoded as %3F' | 'idWith%3F'
    }

    def 'Create Resource Data from pass-through running with #scenario.' () {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                    "?resourceIdentifier=parent/child"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                    post(getUrl)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE).content(requestBody)
            ).andReturn().response
        then: 'ncmp service method to create resource called'
            1 * mockNetworkCmProxyDataService.createResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'parent/child', requestBody, 'application/json;charset=UTF-8')
        and: 'resource is created'
            response.status == HttpStatus.CREATED.value()
        where: 'given request body'
            scenario                        |  requestBody
            'body contains " and new line'  |  'body with " quote and \n new line'
            'body contains normal string'   |  'normal request body'
    }

    def 'Get module references for the given dataspace and cm handle.' () {
        given: 'get module references url'
            def getUrl = "$ncmpBasePathV1/ch/some-cmhandle/modules"
        when: 'get module resource request is performed'
            def response =mvc.perform(get(getUrl)).andReturn().response
        then: 'ncmp service method to get yang resource module references is called'
            mockNetworkCmProxyDataService.getYangResourcesModuleReferences('some-cmhandle')
                    >> [new ModuleReference(moduleName: 'some-name1',revision: 'some-revision1')]
        and: 'response contains an array with the module name and revision'
            response.getContentAsString() == '[{"moduleName":"some-name1","revision":"some-revision1"}]'
        and: 'response returns an OK http code'
            response.status == HttpStatus.OK.value()
    }
}

