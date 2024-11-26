/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.impl.data

import org.onap.cps.api.CpsQueryService
import org.onap.cps.spi.api.FetchDescendantsOption
import org.onap.cps.spi.api.model.DataNode
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME

class NetworkCmProxyQueryServiceImplSpec extends Specification {

    def mockCpsQueryService = Mock(CpsQueryService)

    def objectUnderTest = new NetworkCmProxyQueryServiceImpl(mockCpsQueryService)

    def 'Query resource data for operational from DMI.'() {
        given: 'a list of datanodes'
            def dataNodes = [new DataNode(xpath: '/cps/path'), new DataNode(xpath: '/cps/path/child')]
        and: 'the list of datanodes is returned for query data node'
            1 * mockCpsQueryService.queryDataNodes(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                '//cps/path', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNodes
        when: 'query resource data operational for cm-handle is called'
            def response = objectUnderTest.queryResourceDataOperational(NCMP_DMI_REGISTRY_ANCHOR,
                '//cps/path', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the expected datanodes are returned from the DMI'
            response == dataNodes
    }
}
