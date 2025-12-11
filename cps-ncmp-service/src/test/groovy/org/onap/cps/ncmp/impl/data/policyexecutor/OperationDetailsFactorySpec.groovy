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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.exceptions.NcmpException
import org.onap.cps.ncmp.api.exceptions.ProvMnSException
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.RequestParameters;
import org.onap.cps.ncmp.impl.provmns.model.PatchItem;
import org.onap.cps.ncmp.impl.provmns.model.ResourceOneOf
import org.onap.cps.utils.JsonObjectMapper;
import spock.lang.Specification;

import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE;
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE;
import static org.onap.cps.ncmp.api.data.models.OperationType.DELETE;

class OperationDetailsFactorySpec extends Specification {

    def spiedObjectMapper = Spy(ObjectMapper)
    def jsonObjectMapper = new JsonObjectMapper(spiedObjectMapper)
    def policyExecutor = Mock(PolicyExecutor)

    OperationDetailsFactory objectUnderTest = new OperationDetailsFactory(jsonObjectMapper, spiedObjectMapper, policyExecutor)

    static def complexValueAsResource = new ResourceOneOf(id: 'myId', attributes: ['myAttribute1:myValue1', 'myAttribute2:myValue2'], objectClass: 'myClassName')
    static def simpleValueAsResource = new ResourceOneOf(id: 'myId', attributes: ['simpleAttribute:1'], objectClass: 'myClassName')
    static def yangModelCmHandle = new YangModelCmHandle(id: 'someId')

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
            def result = objectUnderTest.buildDeleteOperationDetails(requestPathParameters.toAlternateId())
        then: 'all details are correct'
            assert result.targetIdentifier == 'my uri/classNameInUri=myId'
    }

    def 'Single patch operation with #patchOperationType checks correct operation type.'() {
        given: 'request parameters and single patch item'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'some uri', className: 'some class')
            def resource = new ResourceOneOf(id: 'some resource id')
            def patchItem = new PatchItem(op: patchOperationType, 'path':'some uri', value: resource)
        when: 'patch operation is processed'
            objectUnderTest.checkPermissionForEachPatchItem(requestPathParameters, [patchItem], yangModelCmHandle)
        then: 'policy executor is called with correct operation type'
            1 * policyExecutor.checkPermission(yangModelCmHandle, expectedPolicyExecutorOperationType, _, _, _)
        where: 'following operations are used'
            patchOperationType | expectedPolicyExecutorOperationType
            'ADD'              | CREATE
            'REPLACE'          | UPDATE
    }

    def 'Single patch operation with REMOVE checks correct operation type.'() {
        given: 'request parameters and single remove patch item'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'some uri')
            def patchItem = new PatchItem(op: 'REMOVE')
        when: 'patch operation is processed'
            objectUnderTest.checkPermissionForEachPatchItem(requestPathParameters, [patchItem], yangModelCmHandle)
        then: 'policy executor is called with DELETE operation type'
            1 * policyExecutor.checkPermission(yangModelCmHandle, DELETE, _, _, _)
    }

    def 'Multiple patch operations invoke policy executor correct number of times in order.'() {
        given: 'request parameters and multiple patch items'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'some uri')
            def resource = new ResourceOneOf(id: 'some resource id', objectClass: 'some class')
            def patchItemsList = [
                new PatchItem(op: 'ADD', 'path':'some uri', value: resource),
                new PatchItem(op: 'REPLACE', 'path':'some uri', value: resource),
                new PatchItem(op: 'REMOVE', 'path':'some uri')
            ]
        when: 'patch operations are processed'
            objectUnderTest.checkPermissionForEachPatchItem(requestPathParameters, patchItemsList, yangModelCmHandle)
        then: 'policy executor is checked for create first'
            1 * policyExecutor.checkPermission(yangModelCmHandle, CREATE, _, _, _)
        then: 'update is next'
            1 * policyExecutor.checkPermission(yangModelCmHandle, UPDATE, _, _, _)
        then: 'and finally delete'
            1 * policyExecutor.checkPermission(yangModelCmHandle, DELETE, _, _, _)
    }

    def 'Build policy executor patch operation details with single replace operation and #scenario.'() {
        given: 'a requestParameter and a patchItem list'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'some uri', className: 'some class')
            def pathItems = [new PatchItem(op: 'REPLACE', 'path':"some uri${suffix}", value: value)]
        when: 'patch operation details are checked'
            objectUnderTest.checkPermissionForEachPatchItem(requestPathParameters, pathItems, yangModelCmHandle)
        then: 'policyExecutor is called with correct payload'
            1 * policyExecutor.checkPermission(
                    yangModelCmHandle,
                    UPDATE,
                    null,
                    requestPathParameters.toAlternateId(),
                    { json -> assert json.contains(attributesValueInOperation) } // check for more details eg. verify type
            )
        where: 'attributes are set using # or resource'
            scenario                           | suffix                         | value                  || attributesValueInOperation
            'set simple value using #'         | '#/attributes/simpleAttribute' | 1                      || '{"simpleAttribute":1}'
            'set simple value using resource'  | ''                             | simpleValueAsResource  || '["simpleAttribute:1"]'
            'set complex value using resource' | ''                             | complexValueAsResource || '["myAttribute1:myValue1","myAttribute2:myValue2"]'
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

    def 'Build policy executor patch operation details from ProvMnS request parameters with invalid op.'() {
        given: 'a provMnsRequestParameter and a patchItem list'
            def path = new RequestParameters(uriLdnFirstPart: 'some uri', className: 'some class')
            def patchItemsList = [new PatchItem(op: 'TEST', 'path':'some uri')]
        when: 'a build is attempted with an invalid op'
            objectUnderTest.checkPermissionForEachPatchItem(path, patchItemsList, yangModelCmHandle)
        then: 'the result is as expected (exception thrown)'
            thrown(ProvMnSException)
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

    def 'Build Policy Executor Operation Details with a exception during conversion.'() {
        given: 'a provMnsRequestParameter and a resource'
            def requestPathParameters = new RequestParameters(uriLdnFirstPart: 'some uri', className: 'some class')
            def resource = new ResourceOneOf(id: 'some resource id')
        and: 'json object mapper throws an exception'
            spiedObjectMapper.readValue(*_) >> { throw new JsonProcessingException('original exception message') }
        when: 'a configurationManagementOperation is created and converted to JSON'
            objectUnderTest.buildCreateOperationDetails(CREATE, requestPathParameters, resource)
        then: 'the expected exception is throw and contains the original message'
            def thrown = thrown(NcmpException)
            assert thrown.message.contains('Cannot convert Resource Object')
            assert thrown.details.contains('original exception message')
    }

}
