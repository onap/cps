/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada
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

import org.onap.cps.ncmp.api.impl.exception.InvalidTopicException
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import spock.lang.Shared

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.CREATE
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.READ
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.UPDATE

import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations
import org.onap.cps.utils.JsonObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.exception.ServerNcmpException
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class NetworkCmProxyDataServiceImplSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockDmiModelOperations = Mock(DmiModelOperations)
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def nullNetworkCmProxyDataServicePropertyHandler = null
    def mockYangModelCmHandleRetriever = Mock(YangModelCmHandleRetriever)
    def NO_TOPIC = null
    def NO_REQUEST_ID = null
    @Shared
    def OPTIONS_PARAM = '(a=1,b=2)'

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockCpsDataService, spiedJsonObjectMapper, mockDmiDataOperations, mockDmiModelOperations,
        mockCpsModuleService, mockCpsAdminService, nullNetworkCmProxyDataServicePropertyHandler, mockYangModelCmHandleRetriever)

    def cmHandleXPath = "/dmi-registry/cm-handles[@id='testCmHandle']"

    def dataNode = new DataNode(leaves: ['dmi-service-name': 'testDmiService'])

    def 'Write resource data for pass-through running from DMI using POST #scenario cm handle properties.'() {
        given: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                CREATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.CREATED) }
    }

    def 'Write resource data for pass-through running from DMI using POST "not found" response (from DMI).'() {
        given: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns a response with 404 status code'
            mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.NOT_FOUND) }
        when: 'write resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
        then: 'exception is thrown'
            def exceptionThrown = thrown(ServerNcmpException.class)
        and: 'details contains (not found) error code: 404'
            exceptionThrown.details.contains('404')
    }

    def 'Get resource data for pass-through operational from DMI.'() {
        given: 'get data node is called'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'get resource data from DMI is called'
            mockDmiDataOperations.getResourceDataFromDmi(
                    'testCmHandle',
                    'testResourceId',
                    OPTIONS_PARAM,
                    'testAcceptParam',
                    PASSTHROUGH_OPERATIONAL,
                    NO_REQUEST_ID,
                    NO_TOPIC) >> new ResponseEntity<>('dmi-response', HttpStatus.OK)
        when: 'get resource data operational for cm-handle is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    OPTIONS_PARAM,
                    NO_TOPIC)
        then: 'DMI returns a json response'
            response == 'dmi-response'
    }

    def 'Get resource data for pass-through operational from DMI with Json Processing Exception.'() {
        given: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'objectMapper not able to parse object'
            spiedJsonObjectMapper.asJsonString(_) >> { throw new JsonProcessingException('testException') }
        and: 'DMI returns NOK response'
            mockDmiDataOperations.getResourceDataFromDmi(*_)
                >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    OPTIONS_PARAM,
                    NO_TOPIC)
        then: 'exception is thrown with the expected details'
            def exceptionThrown = thrown(ServerNcmpException.class)
            exceptionThrown.details == 'DMI status code: 404, DMI response body: NOK-json'
    }

    def 'Get resource data for pass-through operational from DMI return NOK response.'() {
        given: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns NOK response'
            mockDmiDataOperations.getResourceDataFromDmi('testCmHandle',
                    'testResourceId',
                    OPTIONS_PARAM,
                    'testAcceptParam',
                    PASSTHROUGH_OPERATIONAL,
                    NO_REQUEST_ID,
                    NO_TOPIC)
                    >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    OPTIONS_PARAM,
                    NO_TOPIC)
        then: 'exception is thrown'
            def exceptionThrown = thrown(ServerNcmpException.class)
        and: 'details contains the original response'
            exceptionThrown.details.contains('NOK-json')
    }

    def 'Get resource data for pass-through running from DMI.'() {
        given: 'cpsDataService returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns valid response and data'
            mockDmiDataOperations.getResourceDataFromDmi('testCmHandle',
                    'testResourceId',
                    OPTIONS_PARAM,
                    'testAcceptParam',
                    PASSTHROUGH_RUNNING,
                    NO_REQUEST_ID,
                    NO_TOPIC) >> new ResponseEntity<>('{dmi-response}', HttpStatus.OK)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    OPTIONS_PARAM,
                    NO_TOPIC)
        then: 'get resource data returns expected response'
            response == '{dmi-response}'
    }

    def 'Get resource data for pass-through running from DMI return NOK response.'() {
        given: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns NOK response'
            mockDmiDataOperations.getResourceDataFromDmi('testCmHandle',
                    'testResourceId',
                    OPTIONS_PARAM,
                    'testAcceptParam',
                    PASSTHROUGH_RUNNING,
                    NO_REQUEST_ID,
                    NO_TOPIC)
                    >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    OPTIONS_PARAM,
                    NO_TOPIC)
        then: 'exception is thrown'
            def exceptionThrown = thrown(ServerNcmpException.class)
        and: 'details contains the original response'
            exceptionThrown.details.contains('NOK-json')
    }

    def 'DMI Operational data request with #scenario'() {
        given: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi data operation returns valid response and data'
            mockDmiDataOperations.getResourceDataFromDmi(_, _, _, _, _, NO_REQUEST_ID, NO_TOPIC)
                    >> new ResponseEntity<>('{dmi-response}', HttpStatus.OK)
        when: 'get resource data is called data operational with blank topic'
            def responseData = objectUnderTest.getResourceDataOperationalForCmHandle('', '',
                    '', '', emptyTopic)
        then: 'a invalid topic exception is thrown'
            thrown(InvalidTopicException)
        where: 'the following parameters are used'
            scenario                               | emptyTopic
            'no topic value in url'                | ''
            'empty topic value in url'             | '\"\"'
            'blank topic value in url'             | ' '
            'invalid non-empty topic value in url' | '1_5_*_#'
    }

    def 'Get resource data for data operational from DMI with valid topic i.e. async request.'() {
        given: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode(*_) >> dataNode
        and: 'dmi data operation returns valid response and data'
            mockDmiDataOperations.getResourceDataFromDmi(_, _, _, _, _, _, 'my-topic-name')
                    >> new ResponseEntity<>('{dmi-response}', HttpStatus.OK)
        when: 'get resource data is called for data operational with valid topic'
            def responseData = objectUnderTest.getResourceDataOperationalForCmHandle('', '', '', '', 'my-topic-name')
        then: 'non empty request id is generated'
            assert responseData.body.requestId.length() > 0
    }

    def 'Get resource data for pass through running from DMI with valid topic async request.'() {
        given: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi data operation returns valid response and data'
            mockDmiDataOperations.getResourceDataFromDmi(_, _, _, _, _, _, 'my-topic-name')
                    >> new ResponseEntity<>('{dmi-response}', HttpStatus.OK)
        when: 'get resource data is called for data operational with valid topic'
            def responseData = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('',
                    '', '', OPTIONS_PARAM, 'my-topic-name')
        then: 'non empty request id is generated'
            assert responseData.body.requestId.length() > 0
    }

    def 'DMI pass through  running data request with #scenario'() {
        given: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi data operation returns valid response and data'
            mockDmiDataOperations.getResourceDataFromDmi(_, _, _, _, _, NO_REQUEST_ID, NO_TOPIC)
                    >> new ResponseEntity<>('{dmi-response}', HttpStatus.OK)
        when: 'get resource data is called for data operational with valid topic'
            def responseData = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('',
                    '', '', '', emptyTopic)
        then: 'a invalid topic exception is thrown'
            thrown(InvalidTopicException)
        where: 'the following parameters are used'
            scenario                               | emptyTopic
            'no topic value in url'                | ''
            'empty topic value in url'             | '\"\"'
            'blank topic value in url'             | ' '
            'invalid non-empty topic value in url' | '1_5_*_#'
    }

    def 'Getting Yang Resources.'() {
        when: 'yang resources is called'
            objectUnderTest.getYangResourcesModuleReferences('some cm handle')
        then: 'CPS module services is invoked for the correct dataspace and cm handle'
            1 * mockCpsModuleService.getYangResourcesModuleReferences('NFP-Operational','some cm handle')
    }

    def 'Get cm handle identifiers for the given module names.'() {
        when: 'execute a cm handle search for the given module names'
            objectUnderTest.executeCmHandleHasAllModulesSearch(['some-module-name'])
        then: 'get anchor identifiers is invoked  with the expected parameters'
            1 * mockCpsAdminService.queryAnchorNames('NFP-Operational', ['some-module-name'])
    }

    def 'Get a cm handle.'() {
        given: 'the system returns a yang modelled cm handle'
            def dmiServiceName = 'some service name'
            def dmiProperties = [new YangModelCmHandle.Property('Book', 'Romance Novel')]
            def publicProperties = [new YangModelCmHandle.Property('Public Book', 'Public Romance Novel')]
            def yangModelCmHandle = new YangModelCmHandle(id:'Some-Cm-Handle', dmiServiceName: dmiServiceName, dmiProperties: dmiProperties, publicProperties: publicProperties)
            1 * mockYangModelCmHandleRetriever.getDmiServiceNamesAndProperties('Some-Cm-Handle') >> yangModelCmHandle
        when: 'getting cm handle details for a given cm handle id from ncmp service'
            def result = objectUnderTest.getNcmpServiceCmHandle('Some-Cm-Handle')
        then: 'the result returns the correct data'
            result.cmHandleID == 'Some-Cm-Handle'
            result.dmiProperties ==[ Book:'Romance Novel' ]
            result.publicProperties == [ "Public Book":'Public Romance Novel' ]

    }

    def 'Update resource data for pass-through running from dmi using POST #scenario DMI properties.'() {
        given: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', UPDATE,
                '{some-json}', 'application/json')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                UPDATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.OK) }
    }

    def 'Verify error message from handleResponse is correct for #scenario operation.'() {
        given: 'writeResourceDataPassThroughRunningFromDmi fails to return OK HttpStatus'
            mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi(*_)
                >> new ResponseEntity<>(HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle(
                'testCmHandle',
                'testResourceId',
                givenOperation,
                '{some-json}',
                'application/json')
        then: 'an exception is thrown with the expected error message details with correct operation'
            def exceptionThrown = thrown(ServerNcmpException.class)
            exceptionThrown.getMessage().contains(expectedResponseMessage)
        where:
            scenario | givenOperation || expectedResponseMessage
            'CREATE' | CREATE         || 'Not able to create resource data.'
            'READ'   | READ           || 'Not able to read resource data.'
            'UPDATE' | UPDATE         || 'Not able to update resource data.'
    }
}
