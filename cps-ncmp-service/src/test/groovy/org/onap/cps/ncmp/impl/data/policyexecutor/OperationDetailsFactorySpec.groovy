/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

    OperationDetailsFactory objectUnderTest = new OperationDetailsFactory(jsonObjectMapper)

    static def complexValueAsResource = new ResourceOneOf(id: 'myId', attributes: ['myAttribute1:myValue1', 'myAttribute2:myValue2'], objectClass: 'myClassName')
    static def simpleValueAsResource = new ResourceOneOf(id: 'myId', attributes: ['simpleAttribute:1'], objectClass: 'myClassName')

    def 'Build create operation details with all properties.'() {
        given: 'request parameters and resource'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'my uri', className: 'class in uri', id: 'my id')
            def resource = new ResourceOneOf(id: 'some resource id', objectClass: 'class in resource')
        when: 'create operation details are built'
            def result = objectUnderTest.buildCreateOperationDetails(CREATE, requestPathParameters, resource)
        then: 'all details are correct'
            assert result.targetIdentifier == 'my uri'
            assert result.changeRequest.keySet()[0] == 'class in resource'
            assert result.changeRequest['class in resource'][0].id == 'my id'
    }

    def 'Build replace operation details with all properties where class name in body is #scenario.'() {
        given: 'request parameters and resource'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'my uri', className: 'class in uri', id: 'some id')
            def resource = new ResourceOneOf(id: 'some resource id', objectClass: classNameInBody)
        when: 'replace operation details are built'
            def result = objectUnderTest.buildCreateOperationDetails(CREATE, requestPathParameters, resource)
        then: 'all details are correct'
            assert result.targetIdentifier == 'my uri'
            assert result.changeRequest.keySet()[0] == expectedChangeRequestKey
        where:
            scenario    | classNameInBody || expectedChangeRequestKey
            'populated' | 'class in body' || 'class in body'
            'empty'     | ''              || 'class in uri'
            'null'      | null            || 'class in uri'
    }

    def 'Build delete operation details with all properties'() {
        given: 'request parameters'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'my uri', className: 'classNameInUri', id: 'myId')
        when: 'delete operation details are built'
            def result = objectUnderTest.buildDeleteOperationDetails(requestPathParameters.toTargetFdn())
        then: 'all details are correct'
            assert result.targetIdentifier == 'my uri/classNameInUri=myId'
    }

    def 'Single patch operation with #patchOperationType checks correct operation type.'() {
        given: 'request parameters and single patch item'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'some uri', className: 'some class')
            def resource = new ResourceOneOf(id: 'some resource id')
            def patchItem = new PatchItem(op: patchOperationType, 'path':'some uri', value: resource)
        when: 'operation details is created'
            def result = objectUnderTest.buildOperationDetails(requestPathParameters, patchItem)
        then: 'it has the correct operation type (for Policy Executor check)'
            assert result.operation() == expectedPolicyExecutorOperationType.name()
        where: 'following operations are used'
            patchOperationType | expectedPolicyExecutorOperationType
            'ADD'              | CREATE
            'REPLACE'          | UPDATE
            'REMOVE'           | DELETE
    }

    def 'Build policy executor patch operation details with single replace operation and #scenario.'() {
        given: 'a requestParameter and a patchItem list'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'some uri', className: 'some class')
            def pathItem = new PatchItem(op: 'REPLACE', 'path':"some uri${suffix}", value: value)
        when: 'patch operation details are checked'
            def result = objectUnderTest.buildOperationDetails(requestPathParameters, pathItem)
        then: 'Attribute Value in operation is correct'
            result.changeRequest.values()[0].attributes[0] == expectedAttributesValueInOperation
        where: 'attributes are set using # or resource'
            scenario                           | suffix                         | value                  || expectedAttributesValueInOperation
            'set simple value using #'         | '#/attributes/simpleAttribute' | 1                      || [simpleAttribute:1]
            'set simple value using resource'  | ''                             | simpleValueAsResource  || ['simpleAttribute:1']
            'set complex value using resource' | ''                             | complexValueAsResource || ["myAttribute1:myValue1","myAttribute2:myValue2"]
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
        given: 'a provMnsRequestParameter and a patchItem'
            def path = new RequestParameters(uriLdnFirstPart: 'some uri', className: 'some class')
            def patchItem = new PatchItem(op: 'MOVE', 'path':'some uri')
        when: 'a build is attempted with an unsupported op'
            objectUnderTest.buildOperationDetails(path, patchItem)
        then: 'the result is as expected (exception thrown)'
            def exceptionThrown = thrown(ProvMnSException)
            assert exceptionThrown.title == 'Unsupported Patch Operation Type: move'
    }

    def 'Build policy executor create operation details from ProvMnS request parameters where objectClass in resource #scenario.'() {
        given: 'a provMnsRequestParameter and a resource'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'some uri', className: 'class in uri', id:'my id')
            def resource = new ResourceOneOf(id: 'some resource id', objectClass: objectInResouce)
        when: 'a configurationManagementOperation is created and converted to JSON'
            def result = objectUnderTest.buildCreateOperationDetails(CREATE, requestPathParameters, resource)
        then: 'the result is as expected (using json to compare)'
            String expectedJsonString = '{"operation":"CREATE","targetIdentifier":"some uri","changeRequest":{"' + changeRequestClassReference + '":[{"id":"my id","attributes":null}]}}'
            assert jsonObjectMapper.asJsonString(result) == expectedJsonString
        where:
            scenario    | objectInResouce     || changeRequestClassReference
            'populated' | 'class in resource' || 'class in resource'
            'empty'     | ''                  || 'class in uri'
            'null'      | null                || 'class in uri'
    }

}
