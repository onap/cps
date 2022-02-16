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
    YangModelCmHandle yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle("dmiServiceName",
            "dmiDataServiceName", "dmiModuleServiceName", new NcmpServiceCmHandle())

    @Shared
    def sampleDmiServiceUrl = "dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery&topic=topicParamInQuery"

    @Shared
    def sampleDmiServiceUrlWithoutTopic = "dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery"

    @Shared
    def sampleDmiServiceUrlWithEmptyResourceId = "dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?options=optionsParamInQuery&topic=topicParamInQuery"

    NcmpConfiguration.DmiProperties dmiProperties = new NcmpConfiguration.DmiProperties();

    def objectUnderTest = new DmiServiceUrlBuilder(dmiProperties)

    def 'Create the dmi service url with #scenario.'() {
        given: 'uri variables'
            dmiProperties.dmiBasePath = basePath;
            def uriVars = objectUnderTest.populateUriVariables(yangModelCmHandle,
                    "cmHandle", PASSTHROUGH_RUNNING);
        and: 'query params'
            def uriQueries = objectUnderTest.populateQueryParams(resourceId,
                    optionsParamInQuery, topicParamInQuery);
        when: 'getDmiDatastoreUrl is called'
            def dmiServiceUrl = objectUnderTest.getDmiDatastoreUrl(uriQueries, uriVars)
        then: 'dmi service url is generated'
            assert dmiServiceUrl == expectedDmiServiceUrl
        where: 'the following parameters are used'
            scenario                       | topicParamInQuery   | optionsParamInQuery   | resourceId   | basePath || expectedDmiServiceUrl
            'With valid resourceId'        | 'topicParamInQuery' | 'optionsParamInQuery' | 'resourceId' | 'dmi'    || sampleDmiServiceUrl
            'With Empty resourceId'        | 'topicParamInQuery' | 'optionsParamInQuery' | ''           | 'dmi'    || sampleDmiServiceUrlWithEmptyResourceId
            'With Empty dmi base path'     | 'topicParamInQuery' | 'optionsParamInQuery' | 'resourceId' | ''       || sampleDmiServiceUrl
            'With Empty topicParamInQuery' | ''                  | 'optionsParamInQuery' | 'resourceId' | 'dmi'    || sampleDmiServiceUrlWithoutTopic
    }
}
