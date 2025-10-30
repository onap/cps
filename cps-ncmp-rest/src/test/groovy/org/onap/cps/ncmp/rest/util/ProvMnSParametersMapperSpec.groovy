/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
package org.onap.cps.ncmp.rest.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.ncmp.api.exceptions.NcmpException
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter
import org.onap.cps.ncmp.impl.provmns.model.ResourceOneOf
import org.onap.cps.ncmp.impl.provmns.model.Scope

import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpStatus
import spock.lang.Specification

class ProvMnSParametersMapperSpec extends Specification{

    def objectUnderTest = new ProvMnSParametersMapper(new ProvMnSErrorResponseBuilder(),new JsonObjectMapper(new ObjectMapper()), new ObjectMapper())

    def 'Extract url template parameters for GET'() {
        when:'a set of given parameters from a call are passed in'
            def result = objectUnderTest.getUrlTemplateParameters(new Scope(scopeLevel: 1, scopeType: 'BASE_ALL'),
                    'some-filter', ['some-attribute'], ['some-field'], new ClassNameIdGetDataNodeSelectorParameter(dataNodeSelector: 'some-dataSelector'),
                    new YangModelCmHandle(dmiServiceName: 'some-dmi-service'))
        then:'verify object has been mapped correctly'
            result.urlVariables().get('filter') == 'some-filter'
    }

    def 'Data Producer Identifier validation.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkDataProducerIdentifierAndReadyState(yangModelCmHandle, "DEFAULT")
        then: 'no exception thrown for yangModelCmHandle when a data producer is present'
            noExceptionThrown()
    }

    def 'Data Producer Identifier validation with #scenario.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: dataProducerId, compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        when:'a data producer identifier is checked'
            def result = objectUnderTest.checkDataProducerIdentifierAndReadyState(yangModelCmHandle, source)
        then: 'exception thrown'
            result.statusCode == HttpStatus.UNPROCESSABLE_ENTITY
        where:
            scenario      | dataProducerId | source
            'null GET'    | null           | "GET"
            'blank PUT'   | ''             | "PUT"
            'blank PATCH' | ''             | "PATCH"
    }

    def 'State validation with Ready state.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkDataProducerIdentifierAndReadyState(yangModelCmHandle, "DEFAULT")
        then: 'no exception thrown for yangModelCmHandle when state is ready'
            noExceptionThrown()
    }

    def 'State validation with Advised state from #scenario.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkDataProducerIdentifierAndReadyState(yangModelCmHandle, source)
        then: 'exception thrown for yangModelCmHandle when state is advised'
            result.statusCode == HttpStatus.NOT_ACCEPTABLE
        where:
            scenario    | source
            'GET'       | "GET"
            'PUT'       | "PUT"
            'PATCH'     | "PATCH"
            'DELETE'    | "DELETE"
    }

    def 'Convert a configurationManagementOperation to json string'() {
        given: 'a provMnsRequestParameter and a resource'
            def path = new ProvMnsRequestParameters(uriLdnFirstPart: 'someUriLdnFirstPart', className: 'someClassName', id: 'someId')
            def resource = new ResourceOneOf(id: 'someResourceId', attributes: ['someAttribute1:someValue1', 'someAttribute2:someValue2'])
        when: 'a configurationManagementOperation is created and converted to JSON'
            def result = objectUnderTest.policyExecutorOperationToJson('create', path, resource)
        then:
            String expectedJsonString = TestUtils.getResourceFileContent('sample-provmns-json-translation.json')
            result == expectedJsonString
    }
}
