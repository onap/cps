/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration
import org.onap.cps.ncmp.rest.model.RestInputCmHandle
import org.onap.cps.spi.model.ModuleReference
import spock.lang.Specification

class RestInputMapperSpec extends Specification {

    def objectUnderTest = Mappers.getMapper(RestInputMapper.class)

    def 'Convert a created REST CM Handle Input to an NCMP Service CM Handle with #scenario'() {
        given: 'a rest cm handle input'
            def inputRestCmHandle = new RestInputCmHandle(cmHandle : 'example-id', cmHandleProperties: dmiProperties,
                publicCmHandleProperties: publicProperties)
            def restDmiPluginRegistration = new RestDmiPluginRegistration(
                createdCmHandles: [inputRestCmHandle])
        when: 'to plugin dmi registration is called'
            def result  = objectUnderTest.toDmiPluginRegistration(restDmiPluginRegistration)
        then: 'the result returns the correct number of cm handles'
            result.createdCmHandles.size() == 1
        and: 'the converted cm handle has the same id'
            result.createdCmHandles[0].cmHandleID == 'example-id'
        and: '(empty) properties are converted correctly'
            result.createdCmHandles[0].dmiProperties == expectedDmiProperties
            result.createdCmHandles[0].publicProperties == expectedPublicProperties
        where: 'the following parameters are used'
            scenario                    | dmiProperties                            | publicProperties                                         || expectedDmiProperties                     | expectedPublicProperties
            'dmi and public properties' | ['Property-Example': 'example property'] | ['Public-Property-Example': 'public example property']   || ['Property-Example': 'example property']  | ['Public-Property-Example': 'public example property']
            'no properties'             | null                                     | null                                                     || [:]                                       | [:]
    }

    def 'Handling empty dmi registration'() {
        given: 'a rest cm handle input without any cm handles'
            def restDmiPluginRegistration = new RestDmiPluginRegistration()
        when: 'to plugin dmi registration is called'
            def result  = objectUnderTest.toDmiPluginRegistration(restDmiPluginRegistration)
        then: 'unspecified lists remain as empty lists'
            assert result.createdCmHandles == []
            assert result.updatedCmHandles == []
            assert result.removedCmHandles == []
    }

    def 'Convert a CPS ModuleReference to an NCMP ModuleReference'() {
        given: 'a CPS ModuleReference'
            def cpsModuleReference = new ModuleReference(moduleName: 'some-module', revision: 'some-revision')
        when: 'toNCMPModuleReference is called'
            def result = objectUnderTest.toNcmpModuleReference(cpsModuleReference)
        then: 'the result is of the correct ModuleReference'
            result.class == org.onap.cps.ncmp.rest.model.ModuleReference.class
        and: 'the properties of the result are the same as the CPS ModuleReference'
            result.moduleName == cpsModuleReference.moduleName
            result.revision ==  cpsModuleReference.revision
    }
}
