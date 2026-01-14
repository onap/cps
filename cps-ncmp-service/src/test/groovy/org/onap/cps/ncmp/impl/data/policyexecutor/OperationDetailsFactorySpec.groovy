/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.data.policyexecutor

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.exceptions.ProvMnSException
import org.onap.cps.ncmp.impl.provmns.RequestParameters
import org.onap.cps.ncmp.impl.provmns.model.PatchItem
import org.onap.cps.ncmp.impl.provmns.model.ResourceOneOf
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE
import static org.onap.cps.ncmp.api.data.models.OperationType.DELETE
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE

class OperationDetailsFactorySpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def requestPathParameters = new RequestParameters('some method', 'some authorization', '/parent=id1/someChild=someId','some uri', 'class from uri', 'id from uri')

    OperationDetailsFactory objectUnderTest = new OperationDetailsFactory(jsonObjectMapper)

    def 'Build replace (~create) operation details with all properties where class name in body is #scenario.'() {
        given: 'a resource'
            def resource = new ResourceOneOf(id: 'some resource id', objectClass: classNameInBody)
        when: 'replace operation details are built'
            def result = objectUnderTest.buildOperationDetails(CREATE, requestPathParameters, resource)
        then: 'all details are correct'
            assert result.parentFdn == '/parent=id1'
            assert result.className == 'class from uri'
            assert result.ClassInstances[0].id == 'id from uri'
        where:
            scenario    | classNameInBody
            'populated' | 'class in body'
            'empty'     | ''
            'null'      | null
    }

    def 'Single patch operation with #patchOperationType checks correct operation type.'() {
        given: 'a resource and single patch item'
            def resource = new ResourceOneOf(id: 'some resource id')
            def patchItem = new PatchItem(op: patchOperationType, 'path':'some uri', value: resource)
        when: 'operation details is created'
            def result = objectUnderTest.buildOperationDetails(requestPathParameters, patchItem)
        then: 'it has the correct operation type (for Policy Executor check)'
            assert result.operationType == expectedPolicyExecutorOperationType
        where: 'following operations are used'
            patchOperationType | expectedPolicyExecutorOperationType
            'ADD'              | CREATE
            'REPLACE'          | UPDATE
            'REMOVE'           | DELETE
    }

    def 'Build policy executor patch operation details with single replace operation and #scenario.'() {
        given: 'a patchItem'
            def patchItem = new PatchItem(op: 'REPLACE', 'path':"some uri${suffix}", value: value)
        when: 'patch operation details are checked'
            def result = objectUnderTest.buildOperationDetails(requestPathParameters, patchItem)
        then: 'Attribute value is correct'
            result.ClassInstances[0].attributes == [attr1:456]
        where: 'attributes are set using # or resource'
            scenario                            | suffix               | value
            'set simple value using #'          | '#/attributes/attr1' | 456
            'set complex value using resource'  | '/attributes'        | [attr1:456]
    }

    def 'Build an attribute map with different depths of hierarchy with #scenario.'() {
        given: 'a patch item with a path'
            def patchItem = new PatchItem(op: 'REPLACE', 'path':path, value: 123)
        when: 'transforming the attributes'
            def hierarchyMap = objectUnderTest.createNestedMap(patchItem)
        then: 'the map depth is equal to the expected number of attributes'
            assert hierarchyMap.get(expectedAttributeName).toString() == expectedAttributeValue
        where: 'simple and complex attributes are tested'
            scenario                                   | path                                                             || expectedAttributeName || expectedAttributeValue
            'set a simple attribute'                   | 'myUriLdnFirstPart#/attributes/simpleAttribute'                  || 'simpleAttribute'     || '123'
            'set a simple attribute with a trailing /' | 'myUriLdnFirstPart#/attributes/simpleAttribute/'                 || 'simpleAttribute'     || '123'
            'set a complex attribute'                  | 'myUriLdnFirstPart#/attributes/complexAttribute/simpleAttribute' || 'complexAttribute'    || '[simpleAttribute:123]'
    }

    def 'Attempt to Build Operation details with unsupported op (MOVE).'() {
        given: 'a patchItem'
            def patchItem = new PatchItem(op: 'MOVE', 'path':'some uri')
        when: 'a build is attempted with an unsupported op'
            objectUnderTest.buildOperationDetails(requestPathParameters, patchItem)
        then: 'the result is as expected (exception thrown)'
            def exceptionThrown = thrown(ProvMnSException)
            assert exceptionThrown.title == 'Unsupported Patch Operation Type: move'
    }

}
