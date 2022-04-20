/*
 * ============LICENSE_START=======================================================
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
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.operations

import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Shared

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class YangModelCmHandleRetrieverSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new YangModelCmHandleRetriever(mockCpsDataService)

    def cmHandleId = 'some-cm-handle'
    def leaves = ["dmi-service-name":"common service name","dmi-data-service-name":"data service name","dmi-model-service-name":"model service name"]
    def xpath = "/dmi-registry/cm-handles[@id='some-cm-handle']"

    @Shared
    def childDataNodesForCmHandleWithAllProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/additional-properties[@name='name1']", leaves: ["name":"name1", "value":"value1"]),
                                                      new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/public-properties[@name='name2']", leaves: ["name":"name2","value":"value2"])]

    @Shared
    def childDataNodesForCmHandleWithDMIProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some-cm-handle']/additional-properties[@name='name1']", leaves: ["name":"name1", "value":"value1"])]

    @Shared
    def childDataNodesForCmHandleWithPublicProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some-cm-handle']/public-properties[@name='name2']", leaves: ["name":"name2","value":"value2"])]

    def "Retrieve CmHandle using datanode with #scenario."() {
        given: 'the cps data service returns a data node from the DMI registry'
            def dataNode = new DataNode(childDataNodes:childDataNodes, leaves: leaves)
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', xpath, INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'retrieving the yang modelled cm handle'
            def result = objectUnderTest.getYangModelCmHandle(cmHandleId)
        then: 'the result has the correct id and service names'
            result.id == cmHandleId
            result.dmiServiceName == 'common service name'
            result.dmiDataServiceName == 'data service name'
            result.dmiModelServiceName == 'model service name'
        and: 'the expected DMI properties'
            result.dmiProperties == expectedDmiProperties
            result.publicProperties == expectedPublicProperties
        where: 'the following parameters are used'
            scenario                    | childDataNodes                                || expectedDmiProperties                               || expectedPublicProperties
            'no properties'             | []                                            || []                                                  || []
            'DMI and public properties' | childDataNodesForCmHandleWithAllProperties    || [new YangModelCmHandle.Property("name1", "value1")] || [new YangModelCmHandle.Property("name2", "value2")]
            'just DMI properties'       | childDataNodesForCmHandleWithDMIProperties    || [new YangModelCmHandle.Property("name1", "value1")] || []
            'just public properties'    | childDataNodesForCmHandleWithPublicProperties || []                                                  || [new YangModelCmHandle.Property("name2", "value2")]
    }

    def "Retrieve CmHandle using datanode with invalid CmHandle id."() {
        when: 'retrieving the yang modelled cm handle with an invalid id'
            def result = objectUnderTest.getYangModelCmHandle('cm handle id with spaces')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the result is not returned'
            result == null
    }
}
