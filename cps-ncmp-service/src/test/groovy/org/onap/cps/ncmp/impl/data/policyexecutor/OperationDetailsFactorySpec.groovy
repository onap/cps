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
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.RequestPathParameters;
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

    def 'Build create operation details with all properties'() {
        given: 'request parameters and resource'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'classNameInUri', id: 'myId')
            def resource = new ResourceOneOf(id: 'someResourceId', attributes: ['attr1:val1'], objectClass: 'myClass')
        when: 'create operation details are built'
            def result = objectUnderTest.buildCreateOperationDetails(CREATE, path, resource)
        then: 'all details are correct'
            result.targetIdentifier == 'myUriLdnFirstPart'
            result.changeRequest.keySet()[0] == 'myClass'
            result.changeRequest['myClass'][0].id == 'myId'
    }

    def 'Build replace operation details with all properties where #scenario'() {
        given: 'request parameters and resource'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'classNameInUri', id: 'myId')
            def resource = new ResourceOneOf(id: 'someResourceId', attributes: ['attr1:val1'], objectClass: classNameInBody)
        when: 'replace operation details are built'
            def result = objectUnderTest.buildCreateOperationDetails(CREATE, path, resource)
        then: 'all details are correct'
            result.targetIdentifier == 'myUriLdnFirstPart'
            result.changeRequest.keySet()[0] == expectedChangeRequestKey
        where:
            scenario                          | classNameInBody || expectedChangeRequestKey
            'class name in body is populated' | 'myClass'       || 'myClass'
            'class name in body is empty'     | ''              || 'classNameInUri'
            'class name in body is null'      | null            || 'classNameInUri'
    }

    def 'Build delete operation details with all properties'() {
        given: 'request parameters'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'classNameInUri', id: 'myId')
        when: 'delete operation details are built'
            def result = objectUnderTest.buildDeleteOperationDetails(path.toAlternateId())
        then: 'all details are correct'
            assert result.targetIdentifier == 'myUriLdnFirstPart/classNameInUri=myId'
    }

    def 'Single patch operation with #operationType checks correct operation type'() {
        given: 'request parameters and single patch item'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'classNameInUri', id: 'myId')
            def resource = new ResourceOneOf(id: 'someResourceId', attributes: ['attr1:val1'], objectClass: 'myClass')
            def patchItemsList = [new PatchItem(op: operationType, 'path':'myUriLdnFirstPart', value: resource)]
        when: 'patch operation is processed'
            objectUnderTest.checkPermissionForEachPatchItem(path, patchItemsList, yangModelCmHandle)
        then: 'policy executor is called with correct operation type'
            1 * policyExecutor.checkPermission(yangModelCmHandle, expectedOperationType, _, _, _)
        where:
            operationType | expectedOperationType
            'ADD'         | CREATE
            'REPLACE'     | UPDATE
    }

    def 'Single patch operation with REMOVE checks correct operation type'() {
        given: 'request parameters and single remove patch item'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'classNameInUri', id: 'myId')
            def patchItemsList = [new PatchItem(op: 'REMOVE', 'path':'myUriLdnFirstPart')]
        when: 'patch operation is processed'
            objectUnderTest.checkPermissionForEachPatchItem(path, patchItemsList, yangModelCmHandle)
        then: 'policy executor is called with DELETE operation type'
            1 * policyExecutor.checkPermission(yangModelCmHandle, DELETE, _, _, _)
    }

    def 'Multiple patch operations invoke policy executor correct number of times in order'() {
        given: 'request parameters and multiple patch items'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'classNameInUri', id: 'myId')
            def resource = new ResourceOneOf(id: 'someResourceId', attributes: ['attr1:val1'], objectClass: 'myClass')
            def patchItemsList = [
                new PatchItem(op: 'ADD', 'path':'myUriLdnFirstPart', value: resource),
                new PatchItem(op: 'REPLACE', 'path':'myUriLdnFirstPart', value: resource),
                new PatchItem(op: 'REMOVE', 'path':'myUriLdnFirstPart')
            ]
        when: 'patch operations are processed'
            objectUnderTest.checkPermissionForEachPatchItem(path, patchItemsList, yangModelCmHandle)
        then: 'policy executor is called 3 times with correct operation types in order'
            1 * policyExecutor.checkPermission(yangModelCmHandle, CREATE, _, _, _)
            1 * policyExecutor.checkPermission(yangModelCmHandle, UPDATE, _, _, _)
            1 * policyExecutor.checkPermission(yangModelCmHandle, DELETE, _, _, _)
    }

    def 'Build policy executor patch operation details with single replace operation and #scenario.'() {
        given: 'a requestParameter and a patchItem list'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'myClassName', id: 'myId')
            def pathItems = [new PatchItem(op: 'REPLACE', 'path':"myUriLdnFirstPart${suffix}", value: value)]
        when: 'patch operation details are checked'
            objectUnderTest.checkPermissionForEachPatchItem(path, pathItems, yangModelCmHandle)
        then: 'policyExecutor is called with correct payload'
            1 * policyExecutor.checkPermission(
                    yangModelCmHandle,
                    UPDATE,
                    null,
                    path.toAlternateId(),
                    { String json ->
                        // check for more details eg. verify type
                        assert json.contains(attributesValueInOperation)
                    }
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
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'someClassName', id: 'someId')
            def patchItemsList = [new PatchItem(op: 'TEST', 'path':'myUriLdnFirstPart')]
        when: 'a build is attempted with an invalid op'
            objectUnderTest.checkPermissionForEachPatchItem(path, patchItemsList, yangModelCmHandle)
        then: 'the result is as expected (exception thrown)'
            thrown(ProvMnSException)
    }

    def 'Build policy executor create operation details from ProvMnS request parameters where #scenario.'() {
        given: 'a provMnsRequestParameter and a resource'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'someClassName', id: 'someId')
            def resource = new ResourceOneOf(id: 'someResourceId', attributes: ['someAttribute1:someValue1', 'someAttribute2:someValue2'], objectClass: objectClass)
        when: 'a configurationManagementOperation is created and converted to JSON'
            def result = objectUnderTest.buildCreateOperationDetails(CREATE, path, resource)
        then: 'the result is as expected (using json to compare)'
            String expectedJsonString = '{"operation":"CREATE","targetIdentifier":"myUriLdnFirstPart","changeRequest":{"' + changeRequestClassReference + '":[{"id":"someId","attributes":["someAttribute1:someValue1","someAttribute2:someValue2"]}]}}'
            assert jsonObjectMapper.asJsonString(result) == expectedJsonString
        where:
            scenario                   | objectClass        || changeRequestClassReference
            'objectClass is populated' | 'someObjectClass'  || 'someObjectClass'
            'objectClass is empty'     | ''                 || 'someClassName'
            'objectClass is null'      | null               || 'someClassName'
    }

    def 'Build Policy Executor Operation Details with a exception during conversion'() {
        given: 'a provMnsRequestParameter and a resource'
            def path = new RequestPathParameters(uriLdnFirstPart: 'myUriLdnFirstPart', className: 'someClassName', id: 'someId')
            def resource = new ResourceOneOf(id: 'myResourceId', attributes: ['someAttribute1:someValue1', 'someAttribute2:someValue2'])
        and: 'json object mapper throws an exception'
            def originalException = new JsonProcessingException('some-exception')
            spiedObjectMapper.readValue(*_) >> {throw originalException}
        when: 'a configurationManagementOperation is created and converted to JSON'
            objectUnderTest.buildCreateOperationDetails(CREATE, path, resource)
        then: 'the expected exception is throw and matches the original'
            def thrown = thrown(NcmpException)
            assert thrown.message.contains('Cannot convert Resource Object')
            assert thrown.details.contains('some-exception')
    }

}
