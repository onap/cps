/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ncmp.impl.data


import org.onap.cps.ncmp.api.data.models.CmResourceAddress
import org.onap.cps.ncmp.api.data.models.DataOperationRequest
import org.onap.cps.api.model.DataNode
import reactor.core.publisher.Mono
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.DatastoreType.OPERATIONAL
import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE

class NetworkCmProxyFacadeSpec extends Specification {

    def mockDmiDataOperations = Mock(DmiDataOperations)
    def mockNcmpCachedResourceRequestHandler = Mock(NcmpCachedResourceRequestHandler)
    def mockNcmpPassthroughResourceRequestHandler = Mock(NcmpPassthroughResourceRequestHandler)

    def objectUnderTest = new NetworkCmProxyFacade(mockNcmpCachedResourceRequestHandler, mockNcmpPassthroughResourceRequestHandler, mockDmiDataOperations)

    def NO_TOPIC = null

    def 'Execute Data Operation for CM Handles (delegation).'() {
        given: 'a data operation request'
            def dataOperationRequest = Mock(DataOperationRequest)
        and: ' a response from the (mocked) pass-through request handler for the given parameters'
            def responseFromHandler = [attr:'value']
            mockNcmpPassthroughResourceRequestHandler.executeAsynchronousRequest('topic', dataOperationRequest, 'authorization') >> responseFromHandler
        expect: 'the response form the handler'
            assert objectUnderTest.executeDataOperationForCmHandles('topic', dataOperationRequest, 'authorization') == responseFromHandler
    }

    def 'Query Resource Data for cm handle (delegation).'() {
        given: 'a response from the (mocked) cached data handler for the given parameters'
            def responseFromHandler = [Mock(DataNode)]
            mockNcmpCachedResourceRequestHandler.executeRequest('ch-1', 'some cps path', true) >> responseFromHandler
        expect: 'the response form the handler'
            assert objectUnderTest.queryResourceDataForCmHandle('ch-1','some cps path',true) == responseFromHandler
    }

    def 'Choosing Data Request Handler.'() {
        expect: '(a mock of) #expectedHandler'
            assert objectUnderTest.getNcmpDatastoreRequestHandler(datastore.datastoreName).class.name.startsWith(expectedHandler.name)
        where:
            datastore               || expectedHandler
            OPERATIONAL             || NcmpCachedResourceRequestHandler.class
            PASSTHROUGH_RUNNING     || NcmpPassthroughResourceRequestHandler.class
            PASSTHROUGH_OPERATIONAL || NcmpPassthroughResourceRequestHandler.class
    }

    def 'Write resource data for pass-through running from DMI using POST (delegation).'() {
        when: 'write resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json', null)
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId', CREATE, '{some-json}', 'application/json', null)
    }

    def 'Get resource data from DMI (delegation).'() {
        given: 'a cm resource address for datastore operational'
            def cmResourceAddress = new CmResourceAddress('ncmp-datastore:operational', 'some CM Handle', 'some resource Id')
        and: 'get resource data from DMI is called'
            mockNcmpCachedResourceRequestHandler.executeRequest(cmResourceAddress, 'options', NO_TOPIC, false, 'authorization') >>
                    Mono.just('dmi response')
        when: 'get resource data operational for the given cm resource address is called'
            def response = objectUnderTest.getResourceDataForCmHandle(cmResourceAddress, 'options', NO_TOPIC, false, 'authorization').block()
        then: 'DMI returns a json response'
            assert response == 'dmi response'
    }

    def 'Update resource data for pass-through running from dmi (delegation).'() {
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', UPDATE,
                '{some-json}', 'application/json', 'authorization')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId', UPDATE, '{some-json}', 'application/json', 'authorization')
    }
}
