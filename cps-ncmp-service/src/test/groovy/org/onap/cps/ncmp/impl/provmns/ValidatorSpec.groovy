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

import org.onap.cps.ncmp.api.exceptions.ProvMnSException
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED

class ValidatorSpec extends Specification{

    def objectUnderTest = new Validator()

    def 'Data Producer Identifier and Ready state validation.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID', compositeState: new CompositeState(cmHandleState: READY))
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkValidParameters(yangModelCmHandle)
        then: 'no exception thrown for yangModelCmHandle when a data producer is present'
            noExceptionThrown()
    }

    def 'Validator throws exception where #scenario.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: dataProducerId, compositeState: new CompositeState(cmHandleState: cmHandleState))
        when:'a data producer identifier is checked'
            def result = objectUnderTest.checkValidParameters(yangModelCmHandle)
        then: 'http status unprocessable entity is returned'
            thrown(ProvMnSException)
        where:
            scenario                     | dataProducerId          | cmHandleState
            'data producer id is null'   | null                    | READY
            'data producer id is empty'  | ''                      | READY
            'cm handle state is Advised' | 'some-data-producer-id' | ADVISED

    }
}
