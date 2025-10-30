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
package org.onap.cps.ncmp.impl.provmns


import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.springframework.http.HttpStatus
import spock.lang.Specification

class RequestValidatorSpec extends Specification{

    def objectUnderTest = new RequestValidator(new ErrorResponseBuilder())

    def 'Data Producer Identifier validation.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkValidElseReturnNull(yangModelCmHandle, "DEFAULT")
        then: 'no exception thrown for yangModelCmHandle when a data producer is present'
            noExceptionThrown()
    }

    def 'Data Producer Identifier validation with #scenario.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: dataProducerId, compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        when:'a data producer identifier is checked'
            def result = objectUnderTest.checkValidElseReturnNull(yangModelCmHandle, requestType)
        then: 'http status unprocessable entity is returned'
            result.statusCode == HttpStatus.UNPROCESSABLE_ENTITY
        where:
            scenario      | dataProducerId | requestType
            'null GET'    | null           | 'GET'
            'blank PUT'   | ''             | 'PUT'
            'blank PATCH' | ''             | 'PATCH'
    }

    def 'State validation with Ready state.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkValidElseReturnNull(yangModelCmHandle, "DEFAULT")
        then: 'no exception thrown for yangModelCmHandle when state is ready'
            noExceptionThrown()
    }

    def 'State validation with Advised state from #requestType.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkValidElseReturnNull(yangModelCmHandle, requestType)
        then: 'http status not acceptable is expected'
            result.statusCode == HttpStatus.NOT_ACCEPTABLE
        where:
            requestType << ['GET', 'PUT', 'PATCH', 'DELETE']
    }
}
