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

import com.fasterxml.jackson.databind.ObjectMapper
import org.mapstruct.factory.Mappers
import org.onap.cps.TestUtils
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.impl.operations.DmiRequestBody
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor
import org.onap.cps.ncmp.rest.mapper.CmHandleStateMapper
import org.onap.cps.ncmp.rest.util.DeprecationHelper
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.ModuleDefinition
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.inventory.CompositeState.DataStores
import static org.onap.cps.ncmp.api.inventory.CompositeState.Operational

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
    CmHandleStateMapper cmHandleStateMapper = Mappers.getMapper(CmHandleStateMapper)

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
                MockMvcRequestBuilders.get(getUrl)
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
                MockMvcRequestBuilders.get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'task executor is called appropriate number of times'
            expectedNumberOfExecutorExecutions * spiedCpsTaskExecutor.executeTask(_, 2000)
        and: 'response status is expected'
            response.status == HttpStatus.OK.value()
        where: 'the following parameters are used'
            scenario               | datastoreInUrl            | topicQueryParam        || expectedTopicName | expectedNumberOfExecutorExecutions
            'url with valid topic' | 'passthrough-operational' | '&topic=my-topic-name' || 'my-topic-name'   | 1
            'no topic in url'      | 'passthrough-operational' | ''                     || NO_TOPIC          | 0
            'null topic in url'    | 'passthrough-operational' | '&topic=null'          || 'null'            | 1
            'url with valid topic' | 'passthrough-running'     | '&topic=my-topic-name' || 'my-topic-name'   | 1
            'no topic in url'      | 'passthrough-running'     | ''                     || NO_TOPIC          | 0
            'null topic in url'    | 'passthrough-running'     | '&topic=null'          || 'null'            | 1
    }

    def 'Fail to get Resource Data from #datastoreInUrl when #scenario.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:${datastoreInUrl}" +
                "?resourceIdentifier=parent/child&options=(a=1,b=2)${topicQueryParam}"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
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
                MockMvcRequestBuilders.get(getUrl)
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

    def 'Update resource data from pass-through running.'() {
        given: 'update resource data url'
            def updateUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                "?resourceIdentifier=parent/child"
        when: 'update data resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.put(updateUrl)
                    .contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)
            ).andReturn().response
        then: 'ncmp service method to update resource is called'
            1 * mockNetworkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'parent/child', DmiRequestBody.OperationEnum.UPDATE, requestBody, 'application/json;charset=UTF-8')
        and: 'the response status is OK'
            response.status == HttpStatus.OK.value()
    }

    def 'Create Resource Data from pass-through running with #scenario.'() {
        given: 'resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                "?resourceIdentifier=parent/child"
            def requestBody = '{"some-key":"some-value"}'
        when: 'create resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)
            ).andReturn().response
        then: 'ncmp service method to create resource called'
            1 * mockNetworkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'parent/child', DmiRequestBody.OperationEnum.CREATE, requestBody, 'application/json;charset=UTF-8')
        and: 'resource is created'
            response.status == HttpStatus.CREATED.value()
    }

    def 'Get module references for the given dataspace and cm handle.'() {
        given: 'get module references url'
            def getUrl = "$ncmpBasePathV1/ch/some-cmhandle/modules"
        when: 'get module resource request is performed'
            def response = mvc.perform(MockMvcRequestBuilders.get(getUrl)).andReturn().response
        then: 'ncmp service method to get yang resource module references is called'
            mockNetworkCmProxyDataService.getYangResourcesModuleReferences('some-cmhandle')
                >> [new ModuleReference(moduleName: 'some-name1', revision: '2021-10-03')]
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
            cmHandle1.publicProperties = [color: 'yellow']
            def cmHandle2 = new NcmpServiceCmHandle()
            cmHandle2.cmHandleId = 'some-cmhandle-id2'
            cmHandle2.publicProperties = [color: 'green']
            mockNetworkCmProxyDataService.executeCmHandleSearch(_) >> [cmHandle1, cmHandle2]
        when: 'the searches api is invoked'
            def response = mvc.perform(MockMvcRequestBuilders.post(searchesEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonString)).andReturn().response
        then: 'response status returns OK'
            response.status == HttpStatus.OK.value()
        and: 'the expected response content is returned'
            response.contentAsString == '[{"cmHandle":"some-cmhandle-id1","publicCmHandleProperties":[{"color":"yellow"}],"state":null},{"cmHandle":"some-cmhandle-id2","publicCmHandleProperties":[{"color":"green"}],"state":null}]'
    }

    def 'Get complete Cm Handle details by Cm Handle id.'() {
        given: 'an endpoint and a cm handle'
            def cmHandleDetailsEndpoint = "$ncmpBasePathV1/ch/some-cm-handle"
        and: 'an existing ncmp service cm handle'
            def cmHandleId = 'some-cm-handle'
            def dmiProperties = [prop: 'some DMI property']
            def publicProperties = ["public prop": 'some public property']
            def compositeState = compositeStateTestObject()
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, dmiProperties: dmiProperties, publicProperties: publicProperties, compositeState: compositeState)
        and: 'the service method is invoked with the cm handle id'
            1 * mockNetworkCmProxyDataService.getNcmpServiceCmHandle('some-cm-handle') >> ncmpServiceCmHandle
        when: 'the cm handle details api is invoked'
            def response = mvc.perform(
                MockMvcRequestBuilders.get(cmHandleDetailsEndpoint)).andReturn().response
        then: 'the correct response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the public properties'
            assertContainsPublicProperties(response)
        and: 'the response contains the cm handle state'
            assertContainsState(response)
        and: 'the content does not contain dmi properties'
            !response.contentAsString.contains("some DMI property")
    }

    def 'Get Cm Handle public properties by Cm Handle id.'() {
        given: 'a cm handle properties endpoint'
            def cmHandlePropertiesEndpoint = "$ncmpBasePathV1/ch/some-cm-handle/properties"
        and: 'some cm handle public properties'
            def publicProperties = ['public prop': 'some public property']
        and: 'the service method is invoked with the cm handle id returning the cm handle public properties'
            1 * mockNetworkCmProxyDataService
                .getCmHandlePublicProperties('some-cm-handle') >> publicProperties
        when: 'the cm handle properties api is invoked'
            def response = mvc.perform(
                MockMvcRequestBuilders.get(cmHandlePropertiesEndpoint)).andReturn().response
        then: 'the correct response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the public properties'
            assertContainsPublicProperties(response)
    }

    def 'Get Cm Handle composite state by Cm Handle id.'() {
        given: 'a cm handle state endpoint'
            def cmHandlePropertiesEndpoint = "$ncmpBasePathV1/ch/some-cm-handle/state"
        and: 'some cm handle composite state'
            def compositeState = compositeStateTestObject()
        and: 'the service method is invoked with the cm handle id returning the cm handle composite state'
            1 * mockNetworkCmProxyDataService
                .getCmHandleCompositeState('some-cm-handle') >> compositeState
        when: 'the cm handle state api is invoked'
            def response = mvc.perform(
                MockMvcRequestBuilders.get(cmHandlePropertiesEndpoint)).andReturn().response
        then: 'the correct response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the cm handle state'
            assertContainsState(response)
    }

    def 'Call execute cm handle searches with unrecognized condition name.'() {
        given: 'an endpoint and json data'
            def searchesEndpoint = "$ncmpBasePathV1/ch/searches"
            String jsonString = TestUtils.getResourceFileContent('invalid-cmhandle-search.json')
        and: 'the service method is invoked with module names and returns two cm handles'
            def cmHandel1 = new NcmpServiceCmHandle()
            cmHandel1.cmHandleId = 'some-cmhandle-id1'
            cmHandel1.publicProperties = [color: 'yellow']
            def cmHandel2 = new NcmpServiceCmHandle()
            cmHandel2.cmHandleId = 'some-cmhandle-id2'
            cmHandel2.publicProperties = [color: 'green']
            mockNetworkCmProxyDataService.executeCmHandleSearch(_) >> [cmHandel1, cmHandel2]
        when: 'the searches api is invoked'
            def response = mvc.perform(
                MockMvcRequestBuilders.post(searchesEndpoint)
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
            def response = mvc.perform(
                MockMvcRequestBuilders.post(searchesEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content('{}')).andReturn().response
        then: 'cm handle ids are returned'
            response.contentAsString == '["some-cmhandle-id1","some-cmhandle-id2"]'
    }

    def 'Query for cm handles with invalid request payload'() {
        when: 'the searches api is invoked'
            def searchesEndpoint = "$ncmpBasePathV1/ch/id-searches"
            def invalidInputData = '{invalidJson}'
            def response = mvc.perform(
                MockMvcRequestBuilders.post(searchesEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidInputData)).andReturn().response
        then: 'BAD_REQUEST is returned'
            response.getStatus() == 400
    }

    def 'Patch resource data in pass-through running datastore.'() {
        given: 'patch resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                "?resourceIdentifier=parent/child"
        when: 'patch data resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.patch(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).content(requestBody)
            ).andReturn().response
        then: 'ncmp service method to update resource is called'
            1 * mockNetworkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'parent/child', DmiRequestBody.OperationEnum.PATCH, requestBody, 'application/json;charset=UTF-8')
        and: 'the response status is OK'
            response.status == HttpStatus.OK.value()
    }

    def 'Delete resource data in pass-through running datastore.'() {
        given: 'delete resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running" +
                "?resourceIdentifier=parent/child"
        when: 'delete data resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.delete(url)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'the ncmp service method to delete resource is called (with null as body)'
            1 * mockNetworkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'parent/child', DmiRequestBody.OperationEnum.DELETE, null, 'application/json;charset=UTF-8')
        and: 'the response is No Content'
            response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Get resource data from DMI with valid topic i.e. async request for #scenario'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:${datastoreInUrl}" +
                "?resourceIdentifier=parent/child&options=(a=1,b=2)&topic=my-topic-name"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.get(getUrl)
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

    def 'Get module definitions based on cmHandleId.'() {
        when: 'get module definition request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.get("$ncmpBasePathV1/ch/some-cmhandle/modules/definitions"))
                .andReturn().response
        then: 'ncmp service method to get module definitions is called'
            mockNetworkCmProxyDataService.getModuleDefinitionsByCmHandleId('some-cmhandle')
                >> [new ModuleDefinition('sampleModuleName', '2021-10-03',
                'module sampleModuleName{ sample module content }')]
        and: 'response contains an array with the module name, revision and content'
            response.getContentAsString() == '[{"moduleName":"sampleModuleName","revision":"2021-10-03","content":"module sampleModuleName{ sample module content }"}]'
        and: 'response returns an OK http code'
            response.status == HttpStatus.OK.value()
    }

    def 'Set the data sync enabled based on the cm handle id and the data sync flag is #scenario'() {
        when: 'the set data sync enabled request is invoked'
            def response = mvc.perform(
                MockMvcRequestBuilders.put("$ncmpBasePathV1/ch/some-cm-handle-id/data-sync?dataSyncEnabled=" + dataSyncEnabledFlag))
                .andReturn().response
        then: 'method to set data sync enabled is called'
            1 * mockNetworkCmProxyDataService.setDataSyncEnabled('some-cm-handle-id', dataSyncEnabledFlag)
        and: 'the response returns an OK http code'
            response.status == HttpStatus.OK.value()
        where: 'the following parameters are used'
            scenario   | dataSyncEnabledFlag
            'enabled'  | true
            'disabled' | false
    }

    def 'Get Resource Data from operational without descendants.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:operational" +
                "?resourceIdentifier=parent/child&include-descendants=false"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.get(getUrl)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
        then: 'the NCMP data service is called with getResourceDataOperational'
            1 * mockNetworkCmProxyDataService.getResourceDataOperational('testCmHandle',
                'parent/child',
                FetchDescendantsOption.OMIT_DESCENDANTS)
        and: 'response status is Ok'
            response.status == HttpStatus.OK.value()
    }

    def 'Get Resource Data from operational including descendants.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:operational" +
                "?resourceIdentifier=parent/child&include-descendants=true"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.get(getUrl)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
        then: 'the NCMP data service is called with getResourceDataOperational'
            1 * mockNetworkCmProxyDataService.getResourceDataOperational('testCmHandle',
                'parent/child',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        and: 'response status is Ok'
            response.status == HttpStatus.OK.value()
    }

    def 'Get Resource Data from operational using getNcmpDatastore.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/new/ncmp-datastore:operational" +
                "?resourceIdentifier=parent/child&options=(a=1,b=2)&topic=my-topic-name"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                MockMvcRequestBuilders.get(getUrl)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
        then: 'the NCMP data service is called with getResourceDataOperational'
            1 * mockNetworkCmProxyDataService.getResourceDataOperational('testCmHandle',
                'parent/child',
                FetchDescendantsOption.OMIT_DESCENDANTS)
        and: 'response status is Ok'
            response.status == HttpStatus.OK.value()
    }


    def dataStores() {
        DataStores.builder()
            .operationalDataStore(Operational.builder()
                .dataStoreSyncState(DataStoreSyncState.NONE_REQUESTED)
                .lastSyncTime(formattedDateAndTime.toString()).build()).build()
    }

    def compositeStateTestObject() {
        new CompositeState(cmHandleState: CmHandleState.ADVISED,
            lockReason: CompositeState.LockReason.builder().lockReasonCategory(LockReasonCategory.LOCKED_MODULE_SYNC_FAILED).details("lock details").build(),
            lastUpdateTime: formattedDateAndTime.toString(),
            dataSyncEnabled: false,
            dataStores: dataStores())
    }

    def assertContainsAll(response, assertContent) {
        assertContent.forEach(string -> { assert (response.contentAsString.contains(string)) })
        return void
    }

    def assertContainsState(response) {
        def expectedContent = [
            '"state":',
            '"cmHandleState":"ADVISED"',
            '"reason":"LOCKED_MISBEHAVING"',
            '"details":"lock details"',
            '"lastUpdateTime":"2022-12-31T20:30:40.000+0000"',
            '"dataSyncEnabled":false',
            '"dataSyncState":',
            '"operational":',
            '"syncState":"NONE_REQUESTED"',
            '"lastSyncTime":"2022-12-31T20:30:40.000+0000"',
            '"running":null'
        ]
        return assertContainsAll(response, expectedContent)
    }

    def assertContainsPublicProperties(response) {
        def expectedContent = [
            '"publicCmHandleProperties":',
            '"public prop"',
            '"some public property"'
        ]
        return assertContainsAll(response, expectedContent)
    }

}

