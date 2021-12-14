/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.DATA
import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.MODEL

class PersistenceCmHandleSpec extends Specification {

    def 'Setting and getting additional & public properties.'() {
        given: 'a map of one property is added'
            def objectUnderTest = new PersistenceCmHandle()
            objectUnderTest.asAdditionalProperties([myAdditionalProperty: 'some value'])
            objectUnderTest.asPublicProperties([myPublicProperty: 'some value'])
        when: 'the additional properties are retrieved'
            def additionalProperties = objectUnderTest.getAdditionalProperties()
            def publicProperties = objectUnderTest.getPublicProperties()
        then: 'the result has the right size'
            assert additionalProperties.size() == 1
            assert publicProperties.size() == 1
        and: 'the property in the result has the correct name and value'
            def actualAdditionalProperty = additionalProperties.get(0)
            def actualPublicProperty = publicProperties.get(0)
            def expectedAdditionalProperty = new PersistenceCmHandle.AdditionalOrPublicProperty('myAdditionalProperty','some value')
            def expectedPublicProperty = new PersistenceCmHandle.AdditionalOrPublicProperty('myPublicProperty','some value')
            assert actualAdditionalProperty.name == expectedAdditionalProperty.name
            assert actualAdditionalProperty.value == expectedAdditionalProperty.value
            assert actualPublicProperty.name == expectedPublicProperty.name
            assert actualPublicProperty.value == expectedPublicProperty.value
    }

    def 'Resolve dmi service name: #scenario and #requiredService service require.'() {
        given: 'a Persistence CM Handle'
            def objectUnderTest = PersistenceCmHandle.toPersistenceCmHandle(dmiServiceName, dmiDataServiceName, dmiModelServiceName, new CmHandle('some id', null, null))
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
