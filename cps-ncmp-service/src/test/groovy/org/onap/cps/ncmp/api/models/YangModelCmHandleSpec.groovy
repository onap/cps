/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.models

import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.DATA
import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.MODEL

class YangModelCmHandleSpec extends Specification {

    def 'Creating yang model cm handle from a service api cm handle.'() {
        given: 'a cm handle with properties'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.dmiProperties = [myDmiProperty:'value1']
            ncmpServiceCmHandle.publicProperties = [myPublicProperty:'value2']
        when: 'it is converted to a yang model cm handle'
            def objectUnderTest = YangModelCmHandle.toYangModelCmHandle('','','', ncmpServiceCmHandle)
        then: 'the result has the right size'
            assert objectUnderTest.dmiProperties.size() == 1
        and: 'the DMI property in the result has the correct name and value'
            assert objectUnderTest.dmiProperties[0].name == 'myDmiProperty'
            assert objectUnderTest.dmiProperties[0].value == 'value1'
        and: 'the public property in the result has the correct name and value'
            assert objectUnderTest.publicProperties[0].name == 'myPublicProperty'
            assert objectUnderTest.publicProperties[0].value == 'value2'
    }

    def 'Resolve DMI service name: #scenario and #requiredService service require.'() {
        given: 'a yang model cm handle'
            def objectUnderTest = YangModelCmHandle.toYangModelCmHandle(dmiServiceName, dmiDataServiceName, dmiModelServiceName, new NcmpServiceCmHandle())
        expect:
            assert objectUnderTest.resolveDmiServiceName(requiredService) == expectedService
        where:
            scenario                        | dmiServiceName     | dmiDataServiceName | dmiModelServiceName | requiredService || expectedService
            'common service registered'     | 'common service'   | 'does not matter'  | 'does not matter'   | DATA            || 'common service'
            'common service registered'     | 'common service'   | 'does not matter'  | 'does not matter'   | MODEL           || 'common service'
            'common service empty'          | ''                 | 'data service'     | 'does not matter'   | DATA            || 'data service'
            'common service empty'          | ''                 | 'does not matter'  | 'model service'     | MODEL           || 'model service'
            'common service blank'          | '   '              | 'data service'     | 'does not matter'   | DATA            || 'data service'
            'common service blank'          | '   '              | 'does not matter'  | 'model service'     | MODEL           || 'model service'
            'common service null '          | null               | 'data service'     | 'does not matter'   | DATA            || 'data service'
            'common service null'           | null               | 'does not matter'  | 'model service'     | MODEL           || 'model service'
            'only model service registered' | null               | null               | 'does not matter'   | DATA            || null
            'only data service registered'  | null               | 'does not matter'  | null                | MODEL           || null
    }

}
