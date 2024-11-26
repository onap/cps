/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters
import org.onap.cps.ncmp.rest.model.ConditionProperties
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration
import org.onap.cps.ncmp.rest.model.RestInputCmHandle
import org.onap.cps.ncmp.rest.model.RestModuleDefinition
import org.onap.cps.ncmp.rest.model.RestModuleReference
import org.onap.cps.api.model.ModuleDefinition
import org.onap.cps.api.model.ModuleReference
import spock.lang.Specification

class NcmpRestInputMapperSpec extends Specification {

    def objectUnderTest = Mappers.getMapper(NcmpRestInputMapper.class)

    def 'Convert a created REST CM Handle Input to an NCMP Service CM Handle with #scenario'() {
        given: 'a rest cm handle input'
            def inputRestCmHandle = new RestInputCmHandle(cmHandle : 'example-id', cmHandleProperties: registrationDmiProperties,
                publicCmHandleProperties: registrationPublicProperties, trustLevel: registrationTrustLevel, alternateId: 'my-alternate-id', moduleSetTag: 'my-module-set-tag', dataProducerIdentifier: 'my-data-producer-identifier')
            def restDmiPluginRegistration = new RestDmiPluginRegistration(
                createdCmHandles: [inputRestCmHandle])
        when: 'to plugin dmi registration is called'
            def result  = objectUnderTest.toDmiPluginRegistration(restDmiPluginRegistration)
        then: 'the result returns the correct number of cm handles'
            result.createdCmHandles.size() == 1
        and: 'the converted cm handle has the same id'
            result.createdCmHandles[0].cmHandleId == 'example-id'
        and: '(empty) properties are converted correctly'
            result.createdCmHandles[0].dmiProperties == mappedDmiProperties
            result.createdCmHandles[0].publicProperties == mappedPublicProperties
        and: 'other fields are mapped correctly'
            result.createdCmHandles[0].alternateId == 'my-alternate-id'
            result.createdCmHandles[0].moduleSetTag == 'my-module-set-tag'
            result.createdCmHandles[0].registrationTrustLevel == mappedTrustLevel
            result.createdCmHandles[0].dataProducerIdentifier == 'my-data-producer-identifier'
        where: 'the following parameters are used'
            scenario                    | registrationDmiProperties                | registrationPublicProperties                           | registrationTrustLevel || mappedDmiProperties                      | mappedPublicProperties                                 | mappedTrustLevel
            'dmi and public properties' | ['Property-Example': 'example property'] | ['Public-Property-Example': 'public example property'] | 'COMPLETE'             || ['Property-Example': 'example property'] | ['Public-Property-Example': 'public example property'] | TrustLevel.COMPLETE
            'no properties'             | null                                     | null                                                   | null                   || [:]                                      | [:]                                                    | null
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

    def 'Handling non-empty dmi registration'() {
        given: 'a rest cm handle input with cm handles'
            def restDmiPluginRegistration = new RestDmiPluginRegistration(
                createdCmHandles: [new RestInputCmHandle()],
                updatedCmHandles: [new RestInputCmHandle()],
                removedCmHandles: ["some-cmHandle"]
            )
        when: 'to dmi plugin registration is called'
            def result  = objectUnderTest.toDmiPluginRegistration(restDmiPluginRegistration)
        then: 'Lists contain values'
            assert result.createdCmHandles[0].class == NcmpServiceCmHandle.class
            assert result.updatedCmHandles[0].class == NcmpServiceCmHandle.class
            assert result.removedCmHandles == ["some-cmHandle"]
    }

    def 'Convert a ModuleReference to a RestModuleReference'() {
        given: 'a ModuleReference'
            def moduleReference = new ModuleReference()
        when: 'toRestModuleReference is called'
            def result = objectUnderTest.toRestModuleReference(moduleReference)
        then: 'the result is of the correct class RestModuleReference'
            result.class == RestModuleReference.class
    }

    def 'Convert a ModuleDefinition to a RestModuleDefinition'() {
        given: 'a ModuleDefinition'
            def moduleDefinition = new ModuleDefinition('moduleName','revision', 'content')
        when: 'toRestModuleDefinition is called'
            def result = objectUnderTest.toRestModuleDefinition(moduleDefinition)
        then: 'the result is of the correct class RestModuleDefinition'
            result.class == RestModuleDefinition.class
        and: 'all contents are mapped correctly'
            result.toString()=='class RestModuleDefinition {\n' +
                    '    moduleName: moduleName\n' +
                    '    revision: revision\n' +
                    '    content: content\n' +
                    '}'
    }

    def 'Convert a CmHandle REST query to CmHandle query service parameters.'() {
        given: 'a CmHandle REST query with two conditions'
            def conditionParameter1 = new ConditionProperties(conditionName: 'some condition', conditionParameters: [[p1:1]] )
            def conditionParameter2 = new ConditionProperties(conditionName: 'other condition', conditionParameters: [[p2:2]] )
            def cmHandleQuery = new CmHandleQueryParameters()
            cmHandleQuery.cmHandleQueryParameters = [conditionParameter1, conditionParameter2]
        when: 'it is converted into CmHandle query service parameters'
            def result = objectUnderTest.toCmHandleQueryServiceParameters(cmHandleQuery)
        then: 'the result is of the correct class'
            assert result instanceof CmHandleQueryServiceParameters
        and: 'the result has the same conditions'
            assert result.cmHandleQueryParameters.size() == 2
            assert result.cmHandleQueryParameters[0].conditionName == 'some condition'
            assert result.cmHandleQueryParameters[0].conditionParameters == [[p1:1]]
            assert result.cmHandleQueryParameters[1].conditionName == 'other condition'
            assert result.cmHandleQueryParameters[1].conditionParameters == [[p2:2]]
    }
}
