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

import org.onap.cps.ncmp.api.exceptions.NcmpException
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter
import org.onap.cps.ncmp.impl.provmns.model.Scope
import spock.lang.Specification

class ProvMnSParametersMapperSpec extends Specification{

    def objectUnderTest = new ProvMnSParametersMapper()

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
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID')
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkDataProducerIdentifier(yangModelCmHandle)
        then: 'no exception thrown for yangModelCmHandle when a data producer is present'
            noExceptionThrown()
    }

    def 'Data Producer Identifier validation with #scenario.'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: dataProducerId)
        when:'a data producer identifier is checked'
            def result = objectUnderTest.checkDataProducerIdentifier(yangModelCmHandle)
        then: 'exception thrown'
            thrown(NcmpException)
        where:
            scenario    | dataProducerId
            'null'      | null
            'blank'     | ''

    }
}
