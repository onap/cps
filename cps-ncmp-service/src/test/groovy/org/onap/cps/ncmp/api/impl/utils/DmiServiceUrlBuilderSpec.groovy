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

package org.onap.cps.ncmp.api.impl.utils

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING

import org.onap.cps.ncmp.api.impl.config.DmiProperties
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService
import org.onap.cps.spi.utils.CpsValidator
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import spock.lang.Specification

class DmiServiceUrlBuilderSpec extends Specification {

    static YangModelCmHandle yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle('dmiServiceName',
        'dmiDataServiceName', 'dmiModuleServiceName', new NcmpServiceCmHandle(cmHandleId: 'some-cm-handle-id'),'my-module-set-tag', 'my-alternate-id', 'my-data-producer-identifier')

    DmiProperties dmiProperties = new DmiProperties()

    def mockCpsValidator = Mock(CpsValidator)

    def objectUnderTest = new DmiServiceUrlBuilder(dmiProperties, mockCpsValidator)

    def setup() {
        dmiProperties.dmiBasePath = 'dmi'
    }

    def 'Create the dmi service url with #scenario.'() {
        given: 'uri variables'
            def uriVars = objectUnderTest.populateUriVariables(PASSTHROUGH_RUNNING.datastoreName, yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA), 'cmHandle')
        and: 'query params'
            def uriQueries = objectUnderTest.populateQueryParams(resourceId, 'optionsParamInQuery', topic, moduleSetTag)
        when: 'a dmi datastore service url is generated'
            def dmiServiceUrl = objectUnderTest.getDmiDatastoreUrl(uriQueries, uriVars)
        then: 'service url is generated as expected'
            assert dmiServiceUrl == expectedDmiServiceUrl
        where: 'the following parameters are used'
            scenario                       | topic               | moduleSetTag      | resourceId   || expectedDmiServiceUrl
            'With valid resourceId'        | 'topicParamInQuery' | ''                | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery&topic=topicParamInQuery'
            'With Empty resourceId'        | 'topicParamInQuery' | ''                | ''           || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?options=optionsParamInQuery&topic=topicParamInQuery'
            'With valid moduleSetTag'      | 'topicParamInQuery' | 'module-set-tag1' | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery&topic=topicParamInQuery&moduleSetTag=module-set-tag1'
            'With Empty moduleSetTag'      | 'topicParamInQuery' | ''                | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery&topic=topicParamInQuery'
            'With Empty dmi base path'     | 'topicParamInQuery' | ''                | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery&topic=topicParamInQuery'
            'With Empty topicParamInQuery' | ''                  | ''                | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery'
    }

    def 'Populate dmi data store url #scenario.'() {
        given: 'uri variables are created'
            dmiProperties.dmiBasePath = dmiBasePath
            def uriVars = objectUnderTest.populateUriVariables(PASSTHROUGH_RUNNING.datastoreName, yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA), 'cmHandle')
        and: 'null query params'
            def uriQueries = objectUnderTest.populateQueryParams(null, null, null, null)
        when: 'a dmi datastore service url is generated'
            def dmiServiceUrl = objectUnderTest.getDmiDatastoreUrl(uriQueries, uriVars)
        then: 'the created dmi service url matches the expected'
            assert dmiServiceUrl == expectedDmiServiceUrl
        where: 'the following parameters are used'
            scenario                   | decription                          | dmiBasePath || expectedDmiServiceUrl
            'base path starts with  /' | 'Remove / from start of base path'  | '/dmi'      || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running'
            'base path ends with / '   | 'Remove / from end of base path'    | 'dmi/'      || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running'
            'base path without any / ' | 'base path does not contains any /' | 'dmi'       || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running'
    }

    def 'Bath request Url creation.'() {
        given: 'the required path parameters'
            def batchRequestUriVariables = [dmiServiceName: 'some-service', dmiBasePath: 'testBase', cmHandleId: '123']
        and: 'the relevant query parameters'
            def batchRequestQueryParams = objectUnderTest.getDataOperationRequestQueryParams('some topic', 'some id')
        when: 'a URL is created'
            def result = objectUnderTest.getDataOperationRequestUrl(batchRequestQueryParams, batchRequestUriVariables)
        then: 'it is formed correctly'
            assert result.toString() == 'some-service/testBase/v1/data?topic=some+topic&requestId=some+id'
    }

    def 'Populate batch uri variables.'() {
        expect: 'Populate batch uri variables returns a map with given service name and base path from setup'
            assert objectUnderTest.populateDataOperationRequestUriVariables('some service')  == [dmiServiceName: 'some service', dmiBasePath: 'dmi' ]
    }
}
