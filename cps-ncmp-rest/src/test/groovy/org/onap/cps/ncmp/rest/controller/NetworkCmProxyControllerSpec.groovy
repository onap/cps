/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.mapstruct.factory.Mappers
import org.onap.cps.TestUtils
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.ModuleDefinition
import org.onap.cps.api.model.ModuleReference
import org.onap.cps.events.EventProducer
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.LockReasonCategory
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.NetworkCmProxyInventoryFacadeImpl
import org.onap.cps.ncmp.impl.data.NetworkCmProxyFacade
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.rest.model.DataOperationDefinition
import org.onap.cps.ncmp.rest.model.DataOperationRequest
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle
import org.onap.cps.ncmp.rest.util.CmHandleStateMapper
import org.onap.cps.ncmp.rest.util.DataOperationRequestMapper
import org.onap.cps.ncmp.rest.util.DeprecationHelper
import org.onap.cps.ncmp.rest.util.NcmpRestInputMapper
import org.onap.cps.ncmp.rest.util.RestOutputCmHandleMapper
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE
import static org.onap.cps.ncmp.api.data.models.OperationType.DELETE
import static org.onap.cps.ncmp.api.data.models.OperationType.PATCH
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CompositeState.DataStores
import static org.onap.cps.ncmp.api.inventory.models.CompositeState.Operational
import static org.springframework.http.MediaType.APPLICATION_JSON
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
    NetworkCmProxyInventoryFacadeImpl mockNetworkCmProxyInventoryFacade = Mock()

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
    RestOutputCmHandleMapper mockRestOutputCmHandleMapper = Mock()

    @SpringBean
    DeprecationHelper stubbedDeprecationHelper = Stub()

    @Value('${rest.api.ncmp-base-path}/v1')
    def ncmpBasePathV1

    def validRequestBody = '{"some":"valid json"}'

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    def testCompositeState = new CompositeState(cmHandleState: ADVISED,
                                                lockReason: CompositeState.LockReason.builder().lockReasonCategory(LockReasonCategory.MODULE_SYNC_FAILED).details('lock details').build(),
                                                lastUpdateTime: formattedDateAndTime.toString(),
                                                dataSyncEnabled: false,
                                                dataStores: dataStores())

    @Shared
    def NO_TOPIC = null
    def NO_OPTIONS = null
    def NO_AUTH_HEADER = null

    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        setupLogger()
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(EventProducer.class)).detachAndStopAllAppenders()
    }

    def 'Get Resource Data from pass-through operational.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=parent/child&options=(a=1,b=2)"
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(APPLICATION_JSON)).andReturn().response
        then: 'the NCMP facade returns a response entity'
            mockNetworkCmProxyFacade.getResourceDataForCmHandle(_, '(a=1,b=2)', NO_TOPIC, false, NO_AUTH_HEADER) >> Mono.just(new ResponseEntity<Object>(HttpStatus.OK))
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Get Resource Data from ncmp-datastore:operational (cached) parameters handling with #scenario.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/h123/data/ds/ncmp-datastore:operational?resourceIdentifier=parent/child${additionalUrlParam}"
        and: 'the expected cm resource address'
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(APPLICATION_JSON)).andReturn().response
        then: 'the NCMP facade is called with correct parameters'
            1 * mockNetworkCmProxyFacade.getResourceDataForCmHandle(_, NO_OPTIONS, NO_TOPIC, expectedIncludeDescendants, NO_AUTH_HEADER)
        and: 'response status is OK'
            assert response.status == HttpStatus.OK.value()
        where: 'the following additional parameters are used'
            scenario                    | additionalUrlParam           || expectedIncludeDescendants
            'no additional parameter'   | ''                           || false
            'include descendants true'  | '&include-descendants=true'  || true
            'include descendants TRUE'  | '&include-descendants=TRUE'  || true
            'include descendants false' | '&include-descendants=false' || false
            'include descendants FALSE' | '&include-descendants=FALSE' || false
    }

    def 'Execute (async) data operation to read data from dmi service.'() {
        given: 'data operation url'
            def getUrl = "$ncmpBasePathV1/data?topic=my-topic-name"
        and: 'a data operation request as json string'
            def dataOperationRequestAsJsonString = jsonObjectMapper.asJsonString(createDataOperationRequest())
        when: 'post data operation request is performed'
            def response = mvc.perform(post(getUrl).contentType(APPLICATION_JSON).content(dataOperationRequestAsJsonString)).andReturn().response
        then: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
        then: 'the request for (async) data operation invoked once'
            1 * mockNetworkCmProxyFacade.executeDataOperationForCmHandles('my-topic-name', _, NO_AUTH_HEADER)
    }

    def 'Query Resource Data from operational.'() {
        given: 'the query resource data url'
            def getUrl = "$ncmpBasePathV1/ch/ch-1/data/ds/ncmp-datastore:operational/query?cps-path=/cps/path"
        when: 'the query data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(APPLICATION_JSON)).andReturn().response
        then: 'the NCMP query service is called with queryResourceDataOperationalForCmHandle'
            1 * mockNetworkCmProxyFacade.queryResourceDataForCmHandle('ch-1','/cps/path', false)
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Query Resource Data with unsupported datastore'() {
        given: 'the query resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running/query?cps-path=/cps/path"
        when: 'the query data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(APPLICATION_JSON)).andReturn().response
        then: 'a 400 BAD_REQUEST is returned for the unsupported datastore'
            assert response.status == HttpStatus.BAD_REQUEST.value()
        and: 'the error message is that the datastore is not supported'
            assert response.contentAsString.contains("ncmp-datastore:passthrough-running is not supported")
    }

    def 'Get Resource Data from pass-through running with #scenario value in resource identifier param.'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=$resourceIdentifier&options=(a=1)"
        and: 'ncmp facade returns a response entity'
            mockNetworkCmProxyFacade.getResourceDataForCmHandle(_, '(a=1)', NO_TOPIC, false, NO_AUTH_HEADER) >> new ResponseEntity<Object>('data from facade', HttpStatus.OK)
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(APPLICATION_JSON)).andReturn().response
        then: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
        and: 'response contains the data returned by the service'
            assert response.getContentAsString().contains('data from facade')
        where: 'the following special tokens are used in the resource identifier parameter'
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
            def response = mvc.perform(put(updateUrl).contentType(APPLICATION_JSON).content(validRequestBody)).andReturn().response
        then: 'the ncmp facade method to update resource is called'
            1 * mockNetworkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle('testCmHandle','parent/child', UPDATE, validRequestBody, 'application/json;charset=UTF-8', NO_AUTH_HEADER)
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Create Resource Data from pass-through running with #scenario.'() {
        given: 'resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child"
        when: 'create resource request is performed'
            def response = mvc.perform(post(url).contentType(APPLICATION_JSON).content(validRequestBody)).andReturn().response
        then: 'the ncmp facade method to create resource called'
            1 * mockNetworkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle('testCmHandle', 'parent/child', CREATE, validRequestBody, 'application/json;charset=UTF-8', NO_AUTH_HEADER)
        and: 'the response status is Created'
            assert response.status == HttpStatus.CREATED.value()
    }

    def 'Get module references for the given dataspace and cm handle.'() {
        given: 'get module references url'
            def getUrl = "$ncmpBasePathV1/ch/my-cm-handle/modules"
        when: 'get module resource request is performed'
            def response = mvc.perform(get(getUrl)).andReturn().response
        then: 'the inventory facade method to get yang resource module references is called'
            mockNetworkCmProxyInventoryFacade.getYangResourcesModuleReferences('my-cm-handle') >> [new ModuleReference(moduleName: 'my-module', revision: '2021-10-03')]
        and: 'response contains an array with the module name and revision'
            response.getContentAsString() == '[{"moduleName":"my-module","revision":"2021-10-03"}]'
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Execute cm handle search.'() {
        given: 'search endpoint and JSON request'
            def searchesEndpoint = "$ncmpBasePathV1/ch/searches"
            def validSearchRequest = TestUtils.getResourceFileContent('cmhandle-search.json')
        and: 'the inventory facade returns two cm handles'
            def ncmpServiceCmHandle1 = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            def ncmpServiceCmHandle2 = new NcmpServiceCmHandle(cmHandleId: 'ch-2')
            mockNetworkCmProxyInventoryFacade.northboundCmHandleSearch(_) >> Flux.fromIterable([ncmpServiceCmHandle1, ncmpServiceCmHandle2])
        and: 'mapper converts cm handles without private properties'
            def restHandle1 = new RestOutputCmHandle(cmHandle: 'rest ch-1')
            def restHandle2 = new RestOutputCmHandle(cmHandle: 'rest ch-2')
            mockRestOutputCmHandleMapper.toRestOutputCmHandle(ncmpServiceCmHandle1, false) >> restHandle1
            mockRestOutputCmHandleMapper.toRestOutputCmHandle(ncmpServiceCmHandle2, false) >> restHandle2
        when: 'the search endpoint is invoked'
            def response = mvc.perform(post(searchesEndpoint).contentType(APPLICATION_JSON).content(validSearchRequest)).andReturn().response
        then: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains the rest version of both cm handles'
            assert response.contentAsString.contains('rest ch-1')
            assert response.contentAsString.contains('rest ch-2')
    }


    def 'Get complete Cm Handle details by Cm Handle Reference.'() {
        given: 'an endpoint and a cm handle reference with spaces'
            def cmHandleDetailsEndpoint = "$ncmpBasePathV1/ch/cm handle reference with space in request"
        and: 'existing cm handle from inventory facade'
            def cmHandle = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('cm handle reference with space in request') >> cmHandle
        and: 'mapper converts cm handle without private properties'
            def restOutputCmHandle = new RestOutputCmHandle(cmHandle: 'rest version of the cm handle')
            mockRestOutputCmHandleMapper.toRestOutputCmHandle(cmHandle, false) >> restOutputCmHandle
        when: 'the cm handle details api is invoked'
            def response = mvc.perform(get(cmHandleDetailsEndpoint)).andReturn().response
        then: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains the rest version of the cm handle'
            assert response.contentAsString.contains('rest version of the cm handle')
    }

    def 'Get Cm Handle public properties by Cm Handle Reference.'() {
        given: 'a cm handle properties endpoint'
            def cmHandlePropertiesEndpoint = "$ncmpBasePathV1/ch/my-cm-handle-reference/properties"
        and: 'my cm handle public properties'
            def publicProperties = ['public prop': 'my public property']
        and: 'the inventory facade returns the cm handle public properties'
            mockNetworkCmProxyInventoryFacade.getPublicCmHandleProperties('my-cm-handle-reference') >> publicProperties
        when: 'the cm handle properties api is invoked'
            def response = mvc.perform(get(cmHandlePropertiesEndpoint)).andReturn().response
        then: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains the public properties'
            assertContainsPublicProperties(response)
    }

    def 'Get Cm Handle composite state by Cm Handle Reference.'() {
        given: 'a cm handle state endpoint'
            def cmHandlePropertiesEndpoint = "$ncmpBasePathV1/ch/my-cm-handle-reference/state"
        and: 'the inventory facade return a test composite state'
            mockNetworkCmProxyInventoryFacade.getCmHandleCompositeState('my-cm-handle-reference') >> testCompositeState
        when: 'the cm handle state api is invoked'
            def response = mvc.perform(get(cmHandlePropertiesEndpoint)).andReturn().response
        then: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains the cm handle state'
            assert assertContainsTestCompositeState(response)
    }

    def 'Execute cm handle search with a data validation exception.'() {
        given: 'the search endpoint and a valid request'
            def searchesEndpoint = "$ncmpBasePathV1/ch/searches"
            def validSearchRequest = TestUtils.getResourceFileContent('cmhandle-search.json')
        and: 'the inventory facade throws a validation exception'
            mockNetworkCmProxyInventoryFacade.northboundCmHandleSearch(_) >> { throw new DataValidationException('my error', 'my details') }
        when: 'the searches api is invoked'
            def response = mvc.perform(post(searchesEndpoint).contentType(APPLICATION_JSON).content(validSearchRequest)).andReturn().response
        then: 'the response status is Bad Request'
            assert response.status == HttpStatus.BAD_REQUEST.value()
        and: 'the error details are in the response object'
            assert response.contentAsString.contains('my error')
            assert response.contentAsString.contains('my details')
    }

    def 'Query for cm handle ids matching query parameters'() {
        given: 'an endpoint and json data'
            def searchesEndpoint = "$ncmpBasePathV1/ch/id-searches"
        and: 'the inventory facade returns two cm handle ids'
            mockNetworkCmProxyInventoryFacade.northboundCmHandleIdSearch(_, _) >> ['ch-1', 'ch-2']
        when: 'the id searches api is invoked'
            def response = mvc.perform(post(searchesEndpoint).contentType(APPLICATION_JSON).content('{}')).andReturn().response
        then: 'cm handle ids are returned'
            assert response.contentAsString == '["ch-1","ch-2"]'
    }

    def 'Query for cm handle ids with invalid request payload'() {
        when: 'the id searches api is invoked'
            def searchesEndpoint = "$ncmpBasePathV1/ch/id-searches"
            def response = mvc.perform(post(searchesEndpoint).contentType(APPLICATION_JSON).content('{invalidJson}')).andReturn().response
        then: 'the response status is Bad Request'
            assert response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Patch resource data in pass-through running datastore.'() {
        given: 'patch resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child"
        when: 'patch data resource request is performed'
            def response = mvc.perform(patch(url).contentType(APPLICATION_JSON).accept(APPLICATION_JSON).content(validRequestBody)).andReturn().response
        then: 'the inventory facade method to update resource is called'
            1 * mockNetworkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle('testCmHandle', 'parent/child', PATCH, validRequestBody, 'application/json;charset=UTF-8', NO_AUTH_HEADER)
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Delete resource data in pass-through running datastore.'() {
        given: 'delete resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child"
        when: 'delete data resource request is performed'
            def response = mvc.perform(delete(url).contentType(APPLICATION_JSON).accept(APPLICATION_JSON)).andReturn().response
        then: 'the ncmp facade method to delete resource is called (with null as body)'
            1 * mockNetworkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle('testCmHandle', 'parent/child', DELETE, null, 'application/json;charset=UTF-8', NO_AUTH_HEADER)
        and: 'the response is No Content'
            assert response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Getting module definitions for a module'() {
        when: 'get module definition request is performed with module name'
            def response = mvc.perform(get("$ncmpBasePathV1/ch/my-cm-handle/modules/definitions?module-name=sampleModuleName")).andReturn().response
        then: 'ncmp inventory facade returns a (list of one) module definition'
            mockNetworkCmProxyInventoryFacade.getModuleDefinitionsByCmHandleAndModule('my-cm-handle', 'sampleModuleName', _)
                >> [new ModuleDefinition('sampleModuleName', '2021-10-03','module sampleModuleName{ sample module content }')]
        and: 'response contains an array with the module name, revision and content'
            assert response.getContentAsString() == '[{"moduleName":"sampleModuleName","revision":"2021-10-03","content":"module sampleModuleName{ sample module content }"}]'
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Getting module definitions filtering on #scenario'() {
        when: 'get module definition request is performed'
            def response = mvc.perform(
                get("$ncmpBasePathV1/ch/my-cm-handle-reference/modules/definitions?module-name=$moduleName&revision=$revision")).andReturn().response
        then: 'the inventory facade method to get definitions by cm handle reference is only invoked when needed'
            numberOfCallsToByCmHandleId * mockNetworkCmProxyInventoryFacade.getModuleDefinitionsByCmHandleReference('my-cm-handle-reference') >> []
        and: 'the inventory facade method to get definitions by module is invoked when needed'
            numberOfCallsToByModule * mockNetworkCmProxyInventoryFacade.getModuleDefinitionsByCmHandleAndModule('my-cm-handle-reference', moduleName, revision) >> []
        and: 'the response returns is OK'
            assert response.status == HttpStatus.OK.value()
        and: 'the correct message is logged when needed'
            if (expectLogWarning) {
                def lastLoggingEvent = logger.list[0]
                assert lastLoggingEvent.level == Level.WARN
                assert lastLoggingEvent.formattedMessage.contains('Ignoring revision')
            }
        where: 'following parameters are used'
            scenario                   | moduleName  | revision      || numberOfCallsToByCmHandleId | numberOfCallsToByModule | expectLogWarning
            'module name'              | 'my-module' | ''            || 0                           | 1                       | false
            'module name and revision' | 'my-module' | 'my-revision' || 0                           | 1                       | false
            'no filtering'             | ''          | ''            || 1                           | 0                       | false
            'only revision'            | ''          | 'my-revision' || 1                           | 0                       | true
    }

    def 'Set the data sync enabled based on the cm handle id and the data sync flag is #scenario'() {
        when: 'the set data sync enabled request is invoked'
            def response = mvc.perform(put("$ncmpBasePathV1/ch/my-cm-handle/data-sync?dataSyncEnabled=$dataSyncEnabledFlag")).andReturn().response
        then: 'the inventory facade method to set data sync enabled is called'
            1 * mockNetworkCmProxyInventoryFacade.setDataSyncEnabled('my-cm-handle', dataSyncEnabledFlag)
        and: 'the response status is OK'
            assert response.status == HttpStatus.OK.value()
        where: 'the following parameters are used'
            scenario   | dataSyncEnabledFlag
            'enabled'  | true
            'disabled' | false
    }

    def 'Attempt execute #operation rest operation on resource data with #scenario'() {
        given: 'resource data url'
            def url = "$ncmpBasePathV1/ch/testCmHandle/data/ds/${datastoreInUrl}?resourceIdentifier=parent/child"
        when: 'selected request for data resource is performed on url'
            def response = mvc.perform(executeRestOperation(operation, url)).andReturn().response
        then: 'the response status is Bad Request'
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

    def 'Ensure URI-encoded alternateId with slashes is accepted for #operation - #scenario'() {
        given: 'A URI-encoded alternateId that includes slashes'
            def alternateIdWithSlashes = '/some/cps/path'
            def encodedAlternateId = URLEncoder.encode(alternateIdWithSlashes, 'UTF-8')
            def url = "$ncmpBasePathV1/ch/${encodedAlternateId}/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=some-resource"
        when: 'A passthrough operation is executed on the URL containing the encoded alternateId'
            def response = mvc.perform(executeRestOperation('POST', url)).andReturn().response
        then: 'The response status is Created'
            assert response.status == HttpStatus.CREATED.value()
    }

    def executeRestOperation(operation, url) {
        if (operation == 'POST') {
            return post(url).contentType(APPLICATION_JSON).content(validRequestBody)
        }
        if (operation == 'PUT') {
            return put(url).contentType(APPLICATION_JSON).content(validRequestBody)
        }
        if (operation == 'PATCH') {
            return patch(url).contentType(APPLICATION_JSON).content(validRequestBody)
        }
        if (operation == 'DELETE') {
            return delete(url).contentType(APPLICATION_JSON)
        }
    }

    def dataStores() {
        DataStores.builder().operationalDataStore(Operational.builder().dataStoreSyncState(DataStoreSyncState.NONE_REQUESTED)
                                                                       .lastSyncTime(formattedDateAndTime.toString()).build()).build()
    }

    def assertContainsAll(response, assertContent) {
        assertContent.forEach(string -> { assert (response.contentAsString.contains(string)) })
        return void
    }

    def assertContainsTestCompositeState(response) {
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
                '"my public property"'
        ]
        return assertContainsAll(response, expectedContent)
    }

    def createDataOperationRequest() {
        def dataOperationRequest = new DataOperationRequest()
        def dataOperationDefinition = new DataOperationDefinition([operation: 'read', operationId: 'operational-12', datastore: PASSTHROUGH_RUNNING])
        dataOperationRequest.addOperationsItem([dataOperationDefinition])
        return dataOperationRequest
    }

    def setupLogger() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(NetworkCmProxyController.class))
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
    }

}

