/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.security

import org.onap.cps.integration.base.CpsIntegrationSpecBase

/**
 * Verifies that XPath injection via dataspace/anchor names in CPS core
 * service operations is rejected and cannot read, write, or query data.
 */
class CpsCoreInjectionSpec extends CpsIntegrationSpecBase {

    def 'Read data node with dataspace name as #scenario'() {
        when: 'a data node read is attempted with an injected dataspace name'
            def response = performGet(
                    "/cps/api/v2/dataspaces/${URLEncoder.encode(dataspaceName, 'UTF-8')}/anchors/test-anchor/node",
                    ['xpath': '/']
            )
        then: 'the request is rejected'
            assert response.statusCode.is4xxClientError()
        where:
            scenario                 | dataspaceName
            'XPath injection payload'| "ds' or '1'='1"
            'single quote in name'   | "test'ds"
            'SQL injection attempt'  | "'; DROP TABLE dataspace; --"
    }

    def 'Read data node with anchor name as #scenario'() {
        when: 'a data node read is attempted with an injected anchor name'
            def response = performGet(
                    "/cps/api/v2/dataspaces/${GENERAL_TEST_DATASPACE}/anchors/${URLEncoder.encode(anchorName, 'UTF-8')}/node",
                    ['xpath': '/']
            )
        then: 'the request is rejected'
            assert response.statusCode.is4xxClientError()
        where:
            scenario                 | anchorName
            'XPath injection payload'| "anc' or '1'='1"
            'single quote in name'   | "test'anchor"
            'SQL injection attempt'  | "'; DROP TABLE anchor; --"
    }

    def 'Write data node with dataspace name as #scenario'() {
        when: 'a write operation is attempted with an injected dataspace name'
            def response = performPost(
                    "/cps/api/v2/dataspaces/${URLEncoder.encode(dataspaceName, 'UTF-8')}/anchors/test-anchor/nodes",
                    '{"bookstore":{"bookstore-name":"Hacked"}}',
                    ['xpath': '/']
            )
        then: 'the write is rejected'
            assert response.statusCode.is4xxClientError()
        where:
            scenario                 | dataspaceName
            'XPath injection payload'| "ds' or '1'='1"
            'SQL injection attempt'  | "'; DROP TABLE dataspace; --"
    }

    def 'Get module definitions with dataspace or anchor name as #scenario'() {
        when: 'module definitions are requested with injected names'
            def response = performGet(
                    "/cps/api/v2/dataspaces/${URLEncoder.encode(dataspaceName, 'UTF-8')}/anchors/${URLEncoder.encode(anchorName, 'UTF-8')}/module-definitions",
                    ['module-name': 'test-module', 'revision': '2024-01-01']
            )
        then: 'the request is rejected'
            assert response.statusCode.is4xxClientError()
        where:
            scenario            | dataspaceName                | anchorName
            'injected dataspace'| "ds' or '1'='1"              | 'valid-anchor'
            'injected anchor'   | GENERAL_TEST_DATASPACE       | "anc' or '1'='1"
            'SQL in dataspace'  | "'; DROP TABLE dataspace; --"| 'valid-anchor'
    }
}
