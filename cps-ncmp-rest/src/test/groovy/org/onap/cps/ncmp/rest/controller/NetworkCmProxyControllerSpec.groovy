/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation
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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import org.mapstruct.factory.Mappers
import org.onap.cps.TestUtils
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.impl.data.NetworkCmProxyFacade
import org.onap.cps.ncmp.impl.inventory.DataStoreSyncState
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.rest.model.DataOperationDefinition
import org.onap.cps.ncmp.rest.model.DataOperationRequest
import org.onap.cps.ncmp.rest.util.CmHandleStateMapper
import org.onap.cps.ncmp.rest.util.DataOperationRequestMapper
import org.onap.cps.ncmp.rest.util.DeprecationHelper
import org.onap.cps.ncmp.rest.util.NcmpRestInputMapper
import org.onap.cps.api.model.ModuleDefinition
import org.onap.cps.api.model.ModuleReference
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import reactor.core.publisher.Mono
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE
import static org.onap.cps.ncmp.api.data.models.OperationType.DELETE
import static org.onap.cps.ncmp.api.data.models.OperationType.PATCH
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE
import static org.onap.cps.ncmp.api.inventory.models.CompositeState.DataStores
import static org.onap.cps.ncmp.api.inventory.models.CompositeState.Operational
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@WebMvcTest(NetworkCmProxyController)
class NetworkCmProxyControllerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    NetworkCmProxyFacade mockNetworkCmProxyFacade = Mock()

    @SpringBean
    NetworkCmProxyInventoryFacade mockNetworkCmProxyInventoryFacade = Mock()

    @SpringBean
    AlternateIdMatcher mockAlternateIdMatcher = Mock()

    @SpringBean
    ObjectMapper objectMapper = new ObjectMapper()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(objectMapper)

    @SpringBean
    NcmpRestInputMapper ncmpRestInputMapper = Mappers.getMapper(NcmpRestInputMapper)

    @SpringBean
    CmHandleStateMapper cmHandleStateMapper = Mappers.getMapper(CmHandleStateMapper)

    @SpringBean
    DataOperationRequestMapper dataOperationRequestMapper = Mappers.getMapper(DataOperationRequestMapper)

    @SpringBean
    DeprecationHelper stubbedDeprecationHelper = Stub()

    @Value('${rest.api.ncmp-base-path}/v1')
    def ncmpBasePathV1

    def requestBody = '{"some-key":"some-value"}'

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    @Shared
    def NO_TOPIC = null
    def NO_OPTIONS = null
    def NO_AUTH_HEADER = null

    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        setupLogger()
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(EventsPublisher.class)).detachAndStopAllAppenders()
    }

    def 'Get Resource Data from pass-through operational.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=parent/child&options=(a=1,b=2)"
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'the NCMP data service is called with correct parameters'
            1 * mockNetworkCmProxyFacade.getResourceDataForCmHandle(_, '(a=1,b=2)', NO_TOPIC, false, NO_AUTH_HEADER) >> Mono.just(new ResponseEntity<Object>(HttpStatus.OK))
        and: 'response status is Ok'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Get Resource Data from ncmp-datastore:operational (cached) parameters handling with #scenario.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/h123/data/ds/ncmp-datastore:operational?resourceIdentifier=parent/child${additionalUrlParam}"
        and: 'the expected cm resource address'
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'the NCMP data service is called with correct parameters'
            1 * mockNetworkCmProxyFacade.getResourceDataForCmHandle(_, NO_OPTIONS, NO_TOPIC, expectedIncludeDescendants, NO_AUTH_HEADER)
        and: 'response status is OK'
            assert response.status == HttpStatus.OK.value()
        where: 'the following parameters are used'
            scenario                    | additionalUrlParam           || expectedIncludeDescendants
            'no additional param'       | ''                           || false
            'include descendants true'  | '&include-descendants=true'  || true
            'include descendants TRUE'  | '&include-descendants=true'  || true
            'include descendants false' | '&include-descendants=false' || false
            'include descendants FALSE' | '&include-descendants=FALSE' || false
    }

    def 'Execute (async) data operation to read data from dmi service.'() {
        given: 'data operation url'
            def getUrl = "$ncmpBasePathV1/data?topic=my-topic-name"
            def dataOperationRequestJsonData = jsonObjectMapper.asJsonString(getDataOperationRequest('read', datastore.datastoreName))
        when: 'post data operation request is performed'
            def response = mvc.perform(post(getUrl).contentType(MediaType.APPLICATION_JSON).content(dataOperationRequestJsonData)).andReturn().response
        then: 'response status is Ok'
            assert response.status == HttpStatus.OK.value()
        then: 'the request for (async) data operation invoked once'
            1 * mockNetworkCmProxyFacade.executeDataOperationForCmHandles('my-topic-name', _, NO_AUTH_HEADER)
        where: 'the following data stores are used'
            datastore << [PASSTHROUGH_RUNNING, PASSTHROUGH_OPERATIONAL]
    }

    def 'Query Resource Data from operational.'() {
        given: 'the query resource data url'
            def getUrl = "$ncmpBasePathV1/ch/ch-1/data/ds/ncmp-datastore:operational/query?cps-path=/cps/path"
        when: 'the query data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'the NCMP query service is called with queryResourceDataOperationalForCmHandle'
            1 * mockNetworkCmProxyFacade.queryResourceDataForCmHandle('ch-1','/cps/path', false)
        and: 'response status is Ok'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Query Resource Data with unsupported datastore'() {
        given: 'the query resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running/query?cps-path=/cps/path"
        when: 'the query data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'a 400 BAD_REQUEST is returned for the unsupported datastore'
            assert response.status == 400
        and: 'the error message is that the datastore is not supported'
            assert response.contentAsString.contains("ncmp-datastore:passthrough-running is not supported")
    }

    def 'Get Resource Data from pass-through running with #scenario value in resource identifier param.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=$resourceIdentifier&options=(a=1)"
        and: 'ncmp service returns json object'
            1 * mockNetworkCmProxyFacade.getResourceDataForCmHandle(_, '(a=1)', NO_TOPIC, false, NO_AUTH_HEADER)
                    >> new ResponseEntity<Object>('{valid-json}', HttpStatus.OK)
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status is Ok'
            assert response.status == 200
        and: 'response contains the object returned by the service'
            def responseAsJsonObject = new JsonSlurper().parseText(response.getContentAsString())
            assert responseAsJsonObject.body == '{valid-json}'
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
            def updateUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child"
        when: 'update data resource request is performed'
            def response = mvc.perform(put(updateUrl).contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)).andReturn().response
        then: 'ncmp service method to update resource is called'
            1 * mockNetworkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle('testCmHandle','parent/child', UPDATE, requestBody, 'application/json;charset=UTF-8', NO_AUTH_HEADER)
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Create Resource Data from pass-through running with #scenario.'() {
        given: 'resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child"
        when: 'create resource request is performed'
            def response = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)).andReturn().response
        then: 'ncmp service method to create resource called'
            1 * mockNetworkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle('testCmHandle', 'parent/child', CREATE, requestBody, 'application/json;charset=UTF-8', NO_AUTH_HEADER)
        and: 'resource is created'
            assert response.status == HttpStatus.CREATED.value()
    }

    def 'Get module references for the given dataspace and cm handle.'() {
        given: 'get module references url'
            def getUrl = "$ncmpBasePathV1/ch/some-cmhandle/modules"
        when: 'get module resource request is performed'
            def response = mvc.perform(get(getUrl)).andReturn().response
        then: 'ncmp service method to get yang resource module references is called'
            mockNetworkCmProxyInventoryFacade.getYangResourcesModuleReferences('some-cmhandle') >> [new ModuleReference(moduleName: 'some-name1', revision: '2021-10-03')]
        and: 'response contains an array with the module name and revision'
            response.getContentAsString() == '[{"moduleName":"some-name1","revision":"2021-10-03"}]'
        and: 'response returns an OK http code'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Retrieve cm handles.'() {
        given: 'an endpoint and json data'
            def searchesEndpoint = "$ncmpBasePathV1/ch/searches"
            String jsonString = TestUtils.getResourceFileContent('cmhandle-search.json')
        and: 'the service method is invoked with module names and returns two cm handles'
            def cmHandle1 = new NcmpServiceCmHandle()
            cmHandle1.cmHandleId = 'ch-1'
            cmHandle1.publicProperties = [color: 'yellow']
            cmHandle1.currentTrustLevel = TrustLevel.NONE
            def cmHandle2 = new NcmpServiceCmHandle()
            cmHandle2.cmHandleId = 'ch-2'
            cmHandle2.publicProperties = [color: 'green']
            cmHandle2.alternateId = 'someAlternateId'
            cmHandle2.moduleSetTag = 'someModuleSetTag'
            cmHandle2.dataProducerIdentifier = 'someDataProducerIdentifier'
            mockNetworkCmProxyInventoryFacade.executeCmHandleSearch(_) >> [cmHandle1, cmHandle2]
        when: 'the searches api is invoked'
            def response = mvc.perform(post(searchesEndpoint).contentType(MediaType.APPLICATION_JSON).content(jsonString)).andReturn().response
        then: 'response status returns OK'
            assert response.status == HttpStatus.OK.value()
        and: 'the expected response content is returned'
            assert response.contentAsString == '[{"cmHandle":"ch-1","publicCmHandleProperties":[{"color":"yellow"}],"state":null,"trustLevel":"NONE","moduleSetTag":null,"alternateId":null,"dataProducerIdentifier":null},{"cmHandle":"ch-2","publicCmHandleProperties":[{"color":"green"}],"state":null,"trustLevel":null,"moduleSetTag":"someModuleSetTag","alternateId":"someAlternateId","dataProducerIdentifier":"someDataProducerIdentifier"}]'
    }

    def 'Get complete Cm Handle details by Cm Handle Reference.'() {
        given: 'an endpoint and a cm handle reference'
            def cmHandleDetailsEndpoint = "$ncmpBasePathV1/ch/some-cm-handle-reference"
        and: 'an existing ncmp service cm handle'
            def cmHandleId = 'some-cm-handle'
            def alternateId = 'some-alternate-id'
            def dmiProperties = [prop: 'some DMI property']
            def publicProperties = ["public prop": 'some public property']
            def compositeState = compositeStateTestObject()
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, alternateId: alternateId, dmiProperties: dmiProperties, publicProperties: publicProperties, compositeState: compositeState, currentTrustLevel: TrustLevel.COMPLETE)
        and: 'the service method is invoked with the cm handle reference'
            1 * mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('some-cm-handle-reference') >> ncmpServiceCmHandle
        when: 'the cm handle details api is invoked'
            def response = mvc.perform(
                    get(cmHandleDetailsEndpoint)).andReturn().response
        then: 'the correct response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the public properties'
            assertContainsPublicProperties(response)
        and: 'the response contains the cm handle state'
            assertContainsState(response)
        and: 'the content does not contain dmi properties'
            assert !response.contentAsString.contains("some DMI property")
    }

    def 'Get Cm Handle public properties by Cm Handle Reference.'() {
        given: 'a cm handle properties endpoint'
            def cmHandlePropertiesEndpoint = "$ncmpBasePathV1/ch/some-cm-handle-reference/properties"
        and: 'some cm handle public properties'
            def publicProperties = ['public prop': 'some public property']
        and: 'the service method is invoked with the cm handle id returning the cm handle public properties'
            1 * mockNetworkCmProxyInventoryFacade.getCmHandlePublicProperties('some-cm-handle-reference') >> publicProperties
        when: 'the cm handle properties api is invoked'
            def response = mvc.perform(get(cmHandlePropertiesEndpoint)).andReturn().response
        then: 'the correct response is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains the public properties'
            assertContainsPublicProperties(response)
    }

    def 'Get Cm Handle composite state by Cm Handle Reference.'() {
        given: 'a cm handle state endpoint'
            def cmHandlePropertiesEndpoint = "$ncmpBasePathV1/ch/some-cm-handle-reference/state"
        and: 'some cm handle composite state'
            def compositeState = compositeStateTestObject()
        and: 'the service method is invoked with the cm handle id returning the cm handle composite state'
            1 * mockNetworkCmProxyInventoryFacade.getCmHandleCompositeState('some-cm-handle-reference') >> compositeState
        when: 'the cm handle state api is invoked'
            def response = mvc.perform(get(cmHandlePropertiesEndpoint)).andReturn().response
        then: 'the correct response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the cm handle state'
            assert assertContainsState(response)
    }

    def 'Call execute cm handle searches with unrecognized condition name.'() {
        given: 'an endpoint and json data'
            def searchesEndpoint = "$ncmpBasePathV1/ch/searches"
            String jsonString = TestUtils.getResourceFileContent('invalid-cmhandle-search.json')
        and: 'the service method is invoked with module names and returns two cm handles'
            def cmHandle1 = new NcmpServiceCmHandle()
            cmHandle1.cmHandleId = 'ch-1'
            cmHandle1.publicProperties = [color: 'yellow']
            cmHandle1.currentTrustLevel = TrustLevel.COMPLETE
            def cmHandle2 = new NcmpServiceCmHandle()
            cmHandle2.cmHandleId = 'ch-2'
            cmHandle2.publicProperties = [color: 'green']
            cmHandle2.currentTrustLevel = TrustLevel.NONE
            mockNetworkCmProxyInventoryFacade.executeCmHandleSearch(_) >> [cmHandle1, cmHandle2]
        when: 'the searches api is invoked'
            def response = mvc.perform(post(searchesEndpoint).contentType(MediaType.APPLICATION_JSON).content(jsonString)).andReturn().response
        then: 'an empty cm handle identifier is returned'
            assert response.contentAsString == '[{"cmHandle":"ch-1","publicCmHandleProperties":[{"color":"yellow"}],"state":null,"trustLevel":"COMPLETE","moduleSetTag":null,"alternateId":null,"dataProducerIdentifier":null},{"cmHandle":"ch-2","publicCmHandleProperties":[{"color":"green"}],"state":null,"trustLevel":"NONE","moduleSetTag":null,"alternateId":null,"dataProducerIdentifier":null}]'
    }

    def 'Query for cm handles matching query parameters'() {
        given: 'an endpoint and json data'
            def searchesEndpoint = "$ncmpBasePathV1/ch/id-searches"
        and: 'the service method is invoked with module names and returns cm handle ids'
            1 * mockNetworkCmProxyInventoryFacade.executeCmHandleIdSearch(_, _) >> ['ch-1', 'ch-2']
        when: 'the searches api is invoked'
            def response = mvc.perform(post(searchesEndpoint).contentType(MediaType.APPLICATION_JSON).content('{}')).andReturn().response
        then: 'cm handle ids are returned'
            assert response.contentAsString == '["ch-1","ch-2"]'
    }

    def 'Query for cm handles with invalid request payload'() {
        when: 'the searches api is invoked'
            def searchesEndpoint = "$ncmpBasePathV1/ch/id-searches"
            def invalidInputData = '{invalidJson}'
            def response = mvc.perform(post(searchesEndpoint).contentType(MediaType.APPLICATION_JSON).content(invalidInputData)).andReturn().response
        then: 'BAD_REQUEST is returned'
            assert response.getStatus() == 400
    }

    def 'Patch resource data in pass-through running datastore.'() {
        given: 'patch resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child"
        when: 'patch data resource request is performed'
            def response = mvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(requestBody)).andReturn().response
        then: 'ncmp service method to update resource is called'
            1 * mockNetworkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle('testCmHandle', 'parent/child', PATCH, requestBody, 'application/json;charset=UTF-8', NO_AUTH_HEADER)
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Delete resource data in pass-through running datastore.'() {
        given: 'delete resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child"
        when: 'delete data resource request is performed'
            def response = mvc.perform(delete(url).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'the ncmp service method to delete resource is called (with null as body)'
            1 * mockNetworkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle('testCmHandle', 'parent/child', DELETE, null, 'application/json;charset=UTF-8', NO_AUTH_HEADER)
        and: 'the response is No Content'
            assert response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Getting module definitions for a module'() {
        when: 'get module definition request is performed with module name'
            def response = mvc.perform(get("$ncmpBasePathV1/ch/some-cmhandle/modules/definitions?module-name=sampleModuleName")).andReturn().response
        then: 'ncmp service method is invoked with correct parameters'
            mockNetworkCmProxyInventoryFacade.getModuleDefinitionsByCmHandleAndModule('some-cmhandle', 'sampleModuleName', _)
                >> [new ModuleDefinition('sampleModuleName', '2021-10-03','module sampleModuleName{ sample module content }')]
        and: 'response contains an array with the module name, revision and content'
            response.getContentAsString() == '[{"moduleName":"sampleModuleName","revision":"2021-10-03","content":"module sampleModuleName{ sample module content }"}]'
        and: 'response returns an OK http code'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Getting module definitions filtering on #scenario'() {
        when: 'get module definition request is performed'
            def response = mvc.perform(
                get("$ncmpBasePathV1/ch/some-cmhandle-reference/modules/definitions?module-name=" + moduleName + "&revision=" + revision))
                .andReturn().response
        then: 'ncmp service method to get definitions by cm handle reference is invoked when needed'
            numberOfCallsToByCmHandleId * mockNetworkCmProxyInventoryFacade.getModuleDefinitionsByCmHandleReference('some-cmhandle-reference') >> []
        and: 'ncmp service method to get definitions by module is invoked when needed'
            numberOfCallsToByModule * mockNetworkCmProxyInventoryFacade.getModuleDefinitionsByCmHandleAndModule('some-cmhandle-reference', moduleName, revision) >> []
        and: 'response returns an OK http code'
            response.status == HttpStatus.OK.value()
        and: 'the correct message is logged when needed'
            if (expectLogWarning) {
                def lastLoggingEvent = logger.list[0]
                assert lastLoggingEvent.level == Level.WARN
                assert lastLoggingEvent.formattedMessage.contains('Ignoring revision')
            }
        where: 'following parameters are used'
            scenario                   | moduleName    | revision        || numberOfCallsToByCmHandleId | numberOfCallsToByModule | expectLogWarning
            'module name'              | 'some-module' | ''              || 0                           | 1                       | false
            'module name and revision' | 'some-module' | 'some-revision' || 0                           | 1                       | false
            'no filtering'             | ''            | ''              || 1                           | 0                       | false
            'only revision'            | ''            | 'some-revision' || 1                           | 0                       | true
    }

    def 'Set the data sync enabled based on the cm handle id and the data sync flag is #scenario'() {
        when: 'the set data sync enabled request is invoked'
            def response = mvc.perform(
                    put("$ncmpBasePathV1/ch/some-cm-handle-id/data-sync?dataSyncEnabled=" + dataSyncEnabledFlag))
                    .andReturn().response
        then: 'method to set data sync enabled is called'
            1 * mockNetworkCmProxyInventoryFacade.setDataSyncEnabled('some-cm-handle-id', dataSyncEnabledFlag)
        and: 'the response returns an OK http code'
            response.status == HttpStatus.OK.value()
        where: 'the following parameters are used'
            scenario   | dataSyncEnabledFlag
            'enabled'  | true
            'disabled' | false
    }

    def 'Attempt execute #operation rest operation on resource data with #scenario'() {
        given: 'resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/${datastoreInUrl}?resourceIdentifier=parent/child"
        when: 'selected request for data resource is performed on url'
            def response = mvc.perform(
                    executeRestOperation(operation, url))
                    .andReturn().response
        then: 'the response status is as expected'
            assert response.status == HttpStatus.BAD_REQUEST.value()
        and: 'the response is as expected'
            assert response.getContentAsString().contains(datastoreInUrl)
        where: 'the following parameters are used'
            scenario                | operation | datastoreInUrl
            'unsupported datastore' | 'POST'    | 'ncmp-datastore:operational'
            'invalid datastore'     | 'POST'    | 'invalid'
            'unsupported datastore' | 'PUT'     | 'ncmp-datastore:operational'
            'invalid datastore'     | 'PUT'     | 'invalid'
            'unsupported datastore' | 'PATCH'   | 'ncmp-datastore:operational'
            'invalid datastore'     | 'PATCH'   | 'invalid'
            'unsupported datastore' | 'DELETE'  | 'ncmp-datastore:operational'
            'invalid datastore'     | 'DELETE'  | 'invalid'
    }

    def executeRestOperation(operation, url) {
        if (operation == 'POST') {
            return post(url).contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)
        }
        if (operation == 'PUT') {
            return put(url).contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)
        }
        if (operation == 'PATCH') {
            return patch(url).contentType(MediaType.APPLICATION_JSON_VALUE).content(requestBody)
        }
        if (operation == 'DELETE') {
            return delete(url).contentType(MediaType.APPLICATION_JSON_VALUE)
        }
    }

    def dataStores() {
        DataStores.builder()
                .operationalDataStore(Operational.builder()
                        .dataStoreSyncState(DataStoreSyncState.NONE_REQUESTED)
                        .lastSyncTime(formattedDateAndTime.toString()).build()).build()
    }

    def compositeStateTestObject() {
        new CompositeState(cmHandleState: CmHandleState.ADVISED,
                lockReason: CompositeState.LockReason.builder().lockReasonCategory(LockReasonCategory.MODULE_SYNC_FAILED).details("lock details").build(),
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
                '"lockReason":{"reason":"MODULE_SYNC_FAILED","details":"lock details"}',
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

    def getDataOperationRequest(operation, datastore) {
        def dataOperationRequest = new DataOperationRequest()
        def dataOperationDefinitions = new ArrayList()
        dataOperationDefinitions.add(getDataOperationDefinition(operation, datastore))
        dataOperationRequest.addOperationsItem(dataOperationDefinitions)
        return dataOperationRequest
    }

    def getDataOperationDefinition(operation, datastore) {
        def dataOperationDefinition = new DataOperationDefinition()
        dataOperationDefinition.setOperation(operation)
        dataOperationDefinition.setOperationId("operational-12")
        dataOperationDefinition.setDatastore(datastore)
        dataOperationDefinition.setOptions("some option")
        dataOperationDefinition.setResourceIdentifier("some resource identifier")
        dataOperationDefinition.addTargetIdsItem("some-cm-handle")
        return dataOperationDefinition
    }

    def setupLogger() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(NetworkCmProxyController.class))
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
    }

}

