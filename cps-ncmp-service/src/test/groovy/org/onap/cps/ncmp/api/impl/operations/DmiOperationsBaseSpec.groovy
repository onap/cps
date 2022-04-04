/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.operations

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder
import org.spockframework.spring.SpringBean
import spock.lang.Shared
import spock.lang.Specification

abstract class DmiOperationsBaseSpec extends Specification {

    @Shared
    def yangModelCmHandleProperty = new YangModelCmHandle.Property('prop1', 'val1')

    @SpringBean
    DmiRestClient mockDmiRestClient = Mock()

    @SpringBean
    YangModelCmHandleRetriever mockCmHandlePropertiesRetriever = Mock()

    @SpringBean
    ObjectMapper spyObjectMapper = Spy()

    @SpringBean
    DmiServiceUrlBuilder dmiServiceUrlBuilder = new DmiServiceUrlBuilder(new NcmpConfiguration.DmiProperties())

    def yangModelCmHandle = new YangModelCmHandle()
    def static dmiServiceName = 'some service name'
    def static cmHandleId = 'some-cm-handle'
    def static resourceIdentifier = 'parent/child'

    def mockYangModelCmHandleRetrieval(dmiProperties) {
        yangModelCmHandle.dmiDataServiceName = dmiServiceName
        yangModelCmHandle.dmiServiceName = dmiServiceName
        yangModelCmHandle.dmiProperties = dmiProperties
        yangModelCmHandle.id = cmHandleId
        mockCmHandlePropertiesRetriever.getDmiServiceNamesAndProperties(cmHandleId) >> yangModelCmHandle
    }
}
