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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.utils

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING

import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import spock.lang.Shared
import spock.lang.Specification

class DmiServiceUrlBuilderSpec extends Specification {

    @Shared
    YangModelCmHandle yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle('dmiServiceName',
            'dmiDataServiceName', 'dmiModuleServiceName', new NcmpServiceCmHandle(cmHandleID: 'some-cm-handle-id'))

    NcmpConfiguration.DmiProperties dmiProperties = new NcmpConfiguration.DmiProperties()

    def objectUnderTest = new DmiServiceUrlBuilder(dmiProperties)

    def 'Create the dmi service url with #scenario.'() {
        given: 'uri variables'
            dmiProperties.dmiBasePath = 'dmi'
            def uriVars = objectUnderTest.populateUriVariables(yangModelCmHandle,
                    "cmHandle", PASSTHROUGH_RUNNING)
        and: 'query params'
            def uriQueries = objectUnderTest.populateQueryParams(resourceId,
                    'optionsParamInQuery', topicParamInQuery)
        when: 'a dmi datastore service url is generated'
            def dmiServiceUrl = objectUnderTest.getDmiDatastoreUrl(uriQueries, uriVars)
        then: 'service url is generated as expected'
            assert dmiServiceUrl == expectedDmiServiceUrl
        where: 'the following parameters are used'
            scenario                       | topicParamInQuery   | resourceId   || expectedDmiServiceUrl
            'With valid resourceId'        | 'topicParamInQuery' | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery&topic=topicParamInQuery'
            'With Empty resourceId'        | 'topicParamInQuery' | ''           || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?options=optionsParamInQuery&topic=topicParamInQuery'
            'With Empty dmi base path'     | 'topicParamInQuery' | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery&topic=topicParamInQuery'
            'With Empty topicParamInQuery' | ''                  | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery'
    }

    def 'Populate dmi data store url #scenario.'() {
        given: 'uri variables are created'
            dmiProperties.dmiBasePath = dmiBasePath
            def uriVars = objectUnderTest.populateUriVariables(yangModelCmHandle,
                    "cmHandle", PASSTHROUGH_RUNNING)
        and: 'null query params'
            def uriQueries = objectUnderTest.populateQueryParams(null,
                    null, null)
        when: 'a dmi datastore service url is generated'
            def dmiServiceUrl = objectUnderTest.getDmiDatastoreUrl(uriQueries, uriVars)
        then: 'the created dmi service url matches the expected'
            assert dmiServiceUrl == expectedDmiServiceUrl
        where: 'the following parameters are used'
            scenario               | decription                                | dmiBasePath || expectedDmiServiceUrl
            'with base path  / '   | 'Invalid base path as it starts with /'   | '/dmi'      || 'dmiServiceName//dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running'
            'without base path / ' | 'Valid path as it does not starts with /' | 'dmi'       || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running'
    }
}
