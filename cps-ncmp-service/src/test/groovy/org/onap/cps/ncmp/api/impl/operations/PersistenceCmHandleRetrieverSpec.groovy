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
import org.onap.cps.ncmp.api.models.PersistenceCmHandle
import spock.lang.Shared

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class PersistenceCmHandleRetrieverSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new PersistenceCmHandleRetriever(mockCpsDataService)

    def cmHandleId = 'some cm handle'
    def leaves = ["dmi-service-name":"common service name","dmi-data-service-name":"data service name","dmi-model-service-name":"model service name"]
    def xpath = "/dmi-registry/cm-handles[@id='some cm handle']"

    @Shared
    def childDataNodesForCmHandleProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/additional-properties[@name='name1']", leaves: ["name":"name1","value":"value1"]),
                                               new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/public-properties[@name='name2']", leaves: ["name":"name2","value":"value2"])]

    def "Retrieve CmHandle using datanode #scenario."() {
        given: 'the cps data service returns a data node from the DMI registry'
            def dataNode = new DataNode(childDataNodes:childDataNodes, leaves: leaves)
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', xpath, INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'retrieving the persisted cm handle'
            def result = objectUnderTest.retrieveCmHandleDmiServiceNameAndDmiProperties(cmHandleId)
        then: 'the result has the correct id and service names'
            result.id == cmHandleId
            result.dmiServiceName == 'common service name'
            result.dmiDataServiceName == 'data service name'
            result.dmiModelServiceName == 'model service name'
        and: 'the expected DMI properties'
            result.dmiProperties == expectedCmHandleProperties
        where: 'the following parameters are used'
            scenario                 | childDataNodes                      || expectedCmHandleProperties
            'without DMI properties' | []                                  || []
            'with DMI properties'    | childDataNodesForCmHandleProperties || [new PersistenceCmHandle.Property("name1", "value1")]
    }
}
