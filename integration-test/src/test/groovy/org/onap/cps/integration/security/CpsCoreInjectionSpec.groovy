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

class CpsCoreInjectionSpec extends CpsIntegrationSpecBase {

    def 'Read data node with #scenario as dataspace name'() {
        given: 'a dataspace name containing injection characters to attempt breaking out of the query'
            def encodedDataspaceName = URLEncoder.encode(dataspaceName, 'UTF-8')
        when: 'a data node read is attempted with this injection payload'
            def response = performGet(
                    "/cps/api/v2/dataspaces/${encodedDataspaceName}/anchors/test-anchor/node",
                    ['xpath': '/']
            )
        then: 'the request is rejected with a client error'
            assert response.statusCode.is4xxClientError()
        where: 'the following injection payloads are used as dataspace name'
            scenario                                                        | dataspaceName
            'single quote to attempt SQL tautology matching all dataspaces' | "operational' OR 1=1--"
            'semicolon to attempt SQL statement termination and table drop' | "operational'; DROP TABLE dataspace;--"
    }

    def 'Read data node with #scenario as anchor name'() {
        given: 'an anchor name containing injection characters to attempt breaking out of the query'
            def encodedAnchorName = URLEncoder.encode(anchorName, 'UTF-8')
        when: 'a data node read is attempted with this injection payload'
            def response = performGet(
                    "/cps/api/v2/dataspaces/${GENERAL_TEST_DATASPACE}/anchors/${encodedAnchorName}/node",
                    ['xpath': '/']
            )
        then: 'the request is rejected with a client error'
            assert response.statusCode.is4xxClientError()
        where: 'the following injection payloads are used as anchor name'
            scenario                                                        | anchorName
            'single quote to attempt SQL tautology matching all anchors'    | "bookstore' OR 1=1--"
            'semicolon to attempt SQL statement termination and table drop' | "bookstore'; DROP TABLE anchor;--"
    }

    def 'Write data node with #scenario as dataspace name'() {
        given: 'a dataspace name containing injection characters to attempt unauthorized write'
            def encodedDataspaceName = URLEncoder.encode(dataspaceName, 'UTF-8')
        when: 'a write operation is attempted with this injection payload'
            def response = performPost(
                    "/cps/api/v2/dataspaces/${encodedDataspaceName}/anchors/test-anchor/nodes",
                    '{"bookstore":{"bookstore-name":"Hacked"}}',
                    ['xpath': '/']
            )
        then: 'the write is rejected with a client error'
            assert response.statusCode.is4xxClientError()
        where: 'the following injection payloads are used as dataspace name'
            scenario                                                          | dataspaceName
            'single quote to attempt SQL tautology bypassing write target'    | "operational' OR 1=1--"
            'semicolon to attempt SQL statement termination and data update'  | "operational'; UPDATE anchor SET name='hacked';--"
    }

    def 'Get module definitions with #scenario'() {
        given: 'dataspace or anchor names containing injection characters'
            def encodedDataspaceName = URLEncoder.encode(dataspaceName, 'UTF-8')
            def encodedAnchorName = URLEncoder.encode(anchorName, 'UTF-8')
        when: 'module definitions are requested with this injection payload'
            def response = performGet(
                    "/cps/api/v2/dataspaces/${encodedDataspaceName}/anchors/${encodedAnchorName}/module-definitions",
                    ['module-name': 'test-module', 'revision': '2024-01-01']
            )
        then: 'the request is rejected with a client error'
            assert response.statusCode.is4xxClientError()
        where: 'the following injection payloads are used as dataspace or anchor name'
            scenario                                                                  | dataspaceName           | anchorName
            'single quote in dataspace name to attempt SQL tautology'                 | "operational' OR 1=1--" | 'valid-anchor'
            'single quote in anchor name to attempt reading modules from all anchors' | GENERAL_TEST_DATASPACE  | "bookstore' OR 1=1--"
    }
}
