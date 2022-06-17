/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
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

package org.onap.cps.ncmp.rest.controller

import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.SyncState
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.rest.mapper.RestOutputCmHandleStateMapper
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor
import org.onap.cps.ncmp.rest.util.DeprecationHelper
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.PATCH
import static org.onap.cps.ncmp.api.inventory.CompositeState.DataStores
import static org.onap.cps.ncmp.api.inventory.CompositeState.Operational
import static org.onap.cps.ncmp.api.inventory.CompositeState.Running
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.CREATE
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.UPDATE
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.DELETE

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
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

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(objectMapper)

    @SpringBean
    NcmpRestInputMapper ncmpRestInputMapper = Mappers.getMapper(NcmpRestInputMapper)

    @SpringBean
    RestOutputCmHandleStateMapper restOutputCmHandleStateMapper = Mappers.getMapper(RestOutputCmHandleStateMapper)

    @SpringBean
    CpsNcmpTaskExecutor spiedCpsTaskExecutor = Spy()

    @SpringBean
    DeprecationHelper stubbedDeprecationHelper = Stub()

    @Value('${rest.api.ncmp-base-path}/v1')
    def ncmpBasePathV1

    def requestBody = '{"some-key":"some-value"}'

    @Shared
    def NO_TOPIC = null
    def NO_REQUEST_ID = null

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    def 'Get Resource Data from pass-through operational.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-operational" +
                    "?resourceIdentifier=parent/child&options=(a=1,b=2)"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                    get(getUrl)
                            .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
        then: 'the NCMP data service is called with getResourceDataOperationalForCmHandle'
            1 * mockNetworkCmProxyDataService.getResourceDataOperationalForCmHandle('testCmHandle',
                    'parent/child',
                    '(a=1,b=2)',
                    NO_TOPIC,
                    NO_REQUEST_ID)
        and: 'response status is Ok'
            response.status == HttpStatus.OK.value()
    }

    def 'Get Resource Data from #datastoreInUrl with #scenario.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:${datastoreInUrl}" +
                    "?resourceIdentifier=parent/child&options=(a=1,b=2)${topicQueryParam}"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                    get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'task executor is called appropriate number of times'
            expectedNumberOfExecutorExecutions * spiedCpsTaskExecutor.executeTask(_, 2000)
        and: 'response status is expected'
            response.status == HttpStatus.OK.value()
        where: 'the following parameters are used'
            scenario                               | datastoreInUrl            | topicQueryParam        || expectedTopicName | expectedNumberOfExecutorExecutions
            'url with valid topic'                 | 'passthrough-operational' | '&topic=my-topic-name' || 'my-topic-name'   | 1
            'no topic in url'                      | 'passthrough-operational' | ''                     || NO_TOPIC          | 0
            'null topic in url'                    | 'passthrough-operational' | '&topic=null'          || 'null'            | 1
            'url with valid topic'                 | 'passthrough-running'     | '&topic=my-topic-name' || 'my-topic-name'   | 1
            'no topic in url'                      | 'passthrough-running'     | ''                     || NO_TOPIC          | 0
            'null topic in url'                    | 'passthrough-running'     | '&topic=null'          || 'null'            | 1
    }

    def 'Fail to get Resource Data from #datastoreInUrl when #scenario.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:${datastoreInUrl}" +
                "?resourceIdentifier=parent/child&options=(a=1,b=2)${topicQueryParam}"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'abad request is returned'
            response.status == HttpStatus.BAD_REQUEST.value()
        where: 'the following parameters are used'
            scenario                               | datastoreInUrl            | topicQueryParam
            'empty topic in url'                   | 'passthrough-operational' | '&topic=\"\"'
            'missing topic in url'                 | 'passthrough-operational' | '&topic='
            'blank topic value in url'             | 'passthrough-operational' | '&topic=\" \"'
            'invalid non-empty topic value in url' | 'passthrough-operational' | '&topic=1_5_*_#'
            'empty topic in url'                   | 'passthrough-running'     | '&topic=\"\"'
            'missing topic in url'                 | 'passthrough-running'     | '&topic='
            'blank topic value in url'             | 'passthrough-running'     | '&topic=\" \"'
            'invalid non-empty topic value in url' | 'passthrough-running'     | '&topic=1_5_*_#'
    }

    def 'Get Resource Data from pass-through running with #scenario value in resource identifier param.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                    "?resourceIdentifier=" + resourceIdentifier + "&options=(a=1,b=2)"
        and: 'ncmp service returns json object'
            mockNetworkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    resourceIdentifier,
                    '(a=1,b=2)',
                    NO_TOPIC,
                    NO_REQUEST_ID) >> '{valid-json}'
        when: 'get data resource request is performed'
            def response = mvc.perform(
                    get(getUrl)
                            .contentType(MediaType.APPLICATION_JSON)
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

    def 'Update resource data from pass-through running.' () {
        given: 'update resource data url'
            def updateUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                "?resourceIdentifier=parent/child"
        when: 'update data resource request is performed'
            def response = mvc.perform(
                put(updateUrl)
                    .contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)
            ).andReturn().response
        then: 'ncmp service method to update resource is called'
            1 * mockNetworkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'parent/child', UPDATE, requestBody, 'application/json;charset=UTF-8')
        and: 'the response status is OK'
            response.status == HttpStatus.OK.value()
    }

    def 'Create Resource Data from pass-through running with #scenario.' () {
        given: 'resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                    "?resourceIdentifier=parent/child"
            def requestBody = '{"some-key":"some-value"}'
        when: 'create resource request is performed'
            def response = mvc.perform(
                    post(url)
                            .contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)
            ).andReturn().response
        then: 'ncmp service method to create resource called'
            1 * mockNetworkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'parent/child', CREATE, requestBody, 'application/json;charset=UTF-8')
        and: 'resource is created'
            response.status == HttpStatus.CREATED.value()
    }

    def 'Get module references for the given dataspace and cm handle.' () {
        given: 'get module references url'
            def getUrl = "$ncmpBasePathV1/ch/some-cmhandle/modules"
        when: 'get module resource request is performed'
            def response =mvc.perform(get(getUrl)).andReturn().response
        then: 'ncmp service method to get yang resource module references is called'
            mockNetworkCmProxyDataService.getYangResourcesModuleReferences('some-cmhandle')
                    >> [new ModuleReference(moduleName: 'some-name1',revision: '2021-10-03')]
        and: 'response contains an array with the module name and revision'
            response.getContentAsString() == '[{"moduleName":"some-name1","revision":"2021-10-03"}]'
        and: 'response returns an OK http code'
            response.status == HttpStatus.OK.value()
    }

    def 'Retrieve cm handles.'() {
        given: 'an endpoint and json data'
            def searchesEndpoint = "$ncmpBasePathV1/ch/searches"
            String jsonString = TestUtils.getResourceFileContent('cmhandle-search.json')
        and: 'the service method is invoked with module names and returns two cm handles'
            def cmHandle1 = new NcmpServiceCmHandle()
            cmHandle1.cmHandleId = 'some-cmhandle-id1'
            cmHandle1.publicProperties = [color:'yellow']
            def cmHandle2 = new NcmpServiceCmHandle()
            cmHandle2.cmHandleId = 'some-cmhandle-id2'
            cmHandle2.publicProperties = [color:'green']
            mockNetworkCmProxyDataService.executeCmHandleSearch(_) >> [cmHandle1, cmHandle2]
        when: 'the searches api is invoked'
            def response = mvc.perform(post(searchesEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonString)).andReturn().response
        then: 'response status returns OK'
            response.status == HttpStatus.OK.value()
        and: 'the expected response content is returned'
            response.contentAsString == '[{"cmHandle":"some-cmhandle-id1","publicCmHandleProperties":[{"color":"yellow"}],"state":null},{"cmHandle":"some-cmhandle-id2","publicCmHandleProperties":[{"color":"green"}],"state":null}]'
    }

    def 'Get Cm Handle details by Cm Handle id.'() {
        given: 'an endpoint and a cm handle'
            def cmHandleDetailsEndpoint = "$ncmpBasePathV1/ch/some-cm-handle"
        and: 'an existing ncmp service cm handle'
            def compositeState = new CompositeState(cmHandleState: CmHandleState.ADVISED,
                lastUpdateTime: formattedDateAndTime.toString(),
                dataStores: dataStores())
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'some-cm-handle', compositeState: compositeState)
        and: 'the service method is invoked with the cm handle id'
            1 * mockNetworkCmProxyDataService.getNcmpServiceCmHandle('some-cm-handle') >> ncmpServiceCmHandle
        when: 'the cm handle details api is invoked'
            def response = mvc.perform(get(cmHandleDetailsEndpoint)).andReturn().response
        then: 'the correct response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response returns the correct state and timestamp'
            response.contentAsString.contains('some-cm-handle')
            response.contentAsString.contains('ADVISED')
            response.contentAsString.contains('2022-12-31T20:30:40.000+0000')
    }

    def 'Get Cm Handle public properties by Cm Handle id.' () {
        given: 'a cm handle properties endpoint'
            def cmHandlePropertiesEndpoint = "$ncmpBasePathV1/ch/some-cm-handle/properties"
        and: 'some cm handle public properties'
            def publicProperties =  [ 'public prop':'some public property' ]
        and: 'the service method is invoked with the cm handle id returning the cm handle public properties'
            1 * mockNetworkCmProxyDataService.getCmHandlePublicProperties('some-cm-handle') >> publicProperties
        when: 'the cm handle properties api is invoked'
            def response = mvc.perform(get(cmHandlePropertiesEndpoint)).andReturn().response
        then: 'the correct response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response returns public properties and the correct properties'
            response.contentAsString.equals('{"publicCmHandleProperties":[{"public prop":"some public property"}]}')
    }

    def 'Call execute cm handle searches with unrecognized condition name.'() {
        given: 'an endpoint and json data'
            def searchesEndpoint = "$ncmpBasePathV1/ch/searches"
            String jsonString = TestUtils.getResourceFileContent('invalid-cmhandle-search.json')
        and: 'the service method is invoked with module names and returns two cm handles'
            def cmHandel1 = new NcmpServiceCmHandle()
            cmHandel1.cmHandleId = 'some-cmhandle-id1'
            cmHandel1.publicProperties = [color:'yellow']
            def cmHandel2 = new NcmpServiceCmHandle()
            cmHandel2.cmHandleId = 'some-cmhandle-id2'
            cmHandel2.publicProperties = [color:'green']
            mockNetworkCmProxyDataService.executeCmHandleSearch(_) >> [cmHandel1, cmHandel2]
        when: 'the searches api is invoked'
            def response = mvc.perform(post(searchesEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonString)).andReturn().response
        then: 'an empty cm handle identifier is returned'
            response.contentAsString == '[{"cmHandle":"some-cmhandle-id1","publicCmHandleProperties":[{"color":"yellow"}],"state":null},{"cmHandle":"some-cmhandle-id2","publicCmHandleProperties":[{"color":"green"}],"state":null}]'
    }

    def 'Query for cm handles matching query parameters'() {
        given: 'an endpoint and json data'
            def searchesEndpoint = "$ncmpBasePathV1/ch/id-searches"
        and: 'the service method is invoked with module names and returns cm handle ids'
            1 * mockNetworkCmProxyDataService.executeCmHandleIdSearch(_) >> ['some-cmhandle-id1', 'some-cmhandle-id2']
        when: 'the searches api is invoked'
            def response = mvc.perform(post(searchesEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content('{}')).andReturn().response
        then: 'cm handle ids are returned'
            response.contentAsString == '["some-cmhandle-id1","some-cmhandle-id2"]'
    }

    def 'Query for cm handles with invalid request payload'() {
        when: 'the searches api is invoked'
            def searchesEndpoint = "$ncmpBasePathV1/ch/id-searches"
            def invalidInputData = '{invalidJson}'
            def response = mvc.perform(post(searchesEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidInputData)).andReturn().response
        then: 'BAD_REQUEST is returned'
            response.getStatus() == 400
    }

    def 'Patch resource data in pass-through running datastore.' () {
        given: 'patch resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                    "?resourceIdentifier=parent/child"
        when: 'patch data resource request is performed'
            def response = mvc.perform(
                    patch(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON).content(requestBody)
            ).andReturn().response
        then: 'ncmp service method to update resource is called'
            1 * mockNetworkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'parent/child', PATCH, requestBody, 'application/json;charset=UTF-8')
        and: 'the response status is OK'
            response.status == HttpStatus.OK.value()
    }

    def 'Delete resource data in pass-through running datastore.' () {
        given: 'delete resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                     "?resourceIdentifier=parent/child"
        when: 'delete data resource request is performed'
            def response = mvc.perform(
                delete(url).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'the ncmp service method to delete resource is called (with null as body)'
            1 * mockNetworkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'parent/child', DELETE, null, 'application/json;charset=UTF-8')
        and: 'the response is No Content'
            response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Get resource data from DMI with valid topic i.e. async request for #scenario'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:${datastoreInUrl}" +
                    "?resourceIdentifier=parent/child&options=(a=1,b=2)&topic=my-topic-name"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                    get(getUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON_VALUE)
            ).andReturn().response
        then: 'async request id is generated'
            assert response.contentAsString.contains("requestId")
        where: 'the following parameters are used'
            scenario                   | datastoreInUrl
            ':passthrough-operational' | 'passthrough-operational'
            ':passthrough-running'     | 'passthrough-running'
    }

    def dataStores() {
        DataStores.builder()
            .operationalDataStore(Operational.builder()
                .syncState(SyncState.NONE_REQUESTED)
                .lastSyncTime(formattedDateAndTime.toString()).build()).build()
    }

}

