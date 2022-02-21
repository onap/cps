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
import org.onap.cps.ncmp.rest.model.RestCmHandle
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration
import spock.lang.Specification

class RestInputConverterSpec extends Specification {

    def objectUnderTest = Mappers.getMapper(RestInputMapper.class)

    def 'Convert service names without any CM Handles'() {
        given: 'an input with service names and no cm handle'
            def restDmiPluginRegistration = new RestDmiPluginRegistration(
                dmiPlugin: 'test dmi plugin',
                dmiDataPlugin: 'test dmi data plugin',
                dmiModelPlugin: 'test dmi model plugin',
            )
        when: 'to plugin dmi registration is called'
            def result  = objectUnderTest.toDmiPluginRegistration(restDmiPluginRegistration)
        then: 'the conversion returns the service names data'
            restDmiPluginRegistration.dmiPlugin == result.dmiPlugin
            restDmiPluginRegistration.dmiDataPlugin == result.dmiDataPlugin
            restDmiPluginRegistration.dmiModelPlugin == result.dmiModelPlugin
    }

    def 'Convert a created REST CM Handle Input to an NCMP Service CM Handle with'() {
        given: 'a rest cm handle input'
            def createdRestCmHandle = new RestCmHandle(cmHandle : 'example-name', cmHandleProperties: dmiProperties , publicCmHandleProperties: publicProperties)
            def restDmiPluginRegistration = new RestDmiPluginRegistration(
                dmiPlugin: 'test',
                dmiDataPlugin: '',
                dmiModelPlugin: '',
                createdCmHandles: [createdRestCmHandle])
        when: 'to plugin dmi registration is called'
            def result  = objectUnderTest.toDmiPluginRegistration(restDmiPluginRegistration)
        then: 'the conversion returns the service names data'
            restDmiPluginRegistration.dmiPlugin == result.dmiPlugin
            restDmiPluginRegistration.dmiDataPlugin == result.dmiDataPlugin
            restDmiPluginRegistration.dmiModelPlugin == result.dmiModelPlugin
        and: 'the conversion returns the correct cm handle data'
            restDmiPluginRegistration.createdCmHandles[0].cmHandle == result.createdCmHandles[0].cmHandleID
            restDmiPluginRegistration.createdCmHandles[0].cmHandleProperties == result.createdCmHandles[0].dmiProperties
            restDmiPluginRegistration.createdCmHandles[0].publicCmHandleProperties == result.createdCmHandles[0].publicProperties
        where: 'the following parameters are used'
            scenario                    | dmiProperties                            | publicProperties
            'dmi properties'            | ['Property-Example': 'example property'] | []
            'public properties'         | []                                       | ['Public-Property-Example': 'public example property']
            'dmi and public properties' | ['Property-Example': 'example property'] | ['Public-Property-Example': 'public example property']
            'no properties'             | []                                       | []
    }

    def 'Convert an updated REST CM Handle Input to an NCMP Service CM Handle'() {
        given: 'a rest cm handle input'
            def updatedRestCmHandle = new RestCmHandle(cmHandle : 'updated-example', cmHandleProperties: ['Updated-Property-Example': 'updated example property'], publicCmHandleProperties: ['Public-Updated-Property-Example': 'public updated example property'])
            def restDmiPluginRegistration = new RestDmiPluginRegistration(
                updatedCmHandles: [updatedRestCmHandle])
        when: 'to plugin dmi registration is called'
            def result  = objectUnderTest.toDmiPluginRegistration(restDmiPluginRegistration)
        then: 'the conversion returns the correct cm handle data'
            restDmiPluginRegistration.updatedCmHandles[0].cmHandle == result.updatedCmHandles[0].cmHandleID
            restDmiPluginRegistration.updatedCmHandles[0].cmHandleProperties == result.updatedCmHandles[0].dmiProperties
            restDmiPluginRegistration.updatedCmHandles[0].publicCmHandleProperties == result.updatedCmHandles[0].publicProperties
        where: 'the following parameters are used'
            scenario                    | dmiProperties                                            | publicProperties
            'dmi properties'            | ['Updated-Property-Example': 'updated example property'] | []
            'public properties'         | []                                                       | ['Public-Updated-Property-Example': 'public updated example property']
            'dmi and public properties' | ['Updated-Property-Example': 'updated example property'] | ['Public-Updated-Property-Example': 'public updated example property']
            'no properties'             | []                                                       | []

    }

    def 'Convert a removed REST CM Handle Input to an NCMP Service CM Handle'() {
        given: 'a rest cm handle input'
            def restDmiPluginRegistration = new RestDmiPluginRegistration(
                removedCmHandles: ['cmHandle1', 'cmHandle2'],
            )
        when: 'to plugin dmi registration is called'
            def result  = objectUnderTest.toDmiPluginRegistration(restDmiPluginRegistration)
        then: 'the conversion returns the correct cm handle data'
            restDmiPluginRegistration.removedCmHandles[0] == result.removedCmHandles[0]
            restDmiPluginRegistration.removedCmHandles[1] == result.removedCmHandles[1]
    }

}
