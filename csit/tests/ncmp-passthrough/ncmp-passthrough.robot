/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

*** Settings ***
Documentation         NCMP

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn

Suite Setup           Create Session      CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***

${auth}                 Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpBasePath}         /ncmp
${netconf}              NETCONF

*** Test Cases ***

Get for Passthough Operational (CF, RO) with fields
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=ietf-netconf-monitoring:netconf-state&options=(fields=schemas/schema/location)
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=     Set Variable       ${response.json()}
    ${schemaCount}=      Get length         ${responseJson['ietf-netconf-monitoring:netconf-state']['schemas']}
    Should Be True                          ${schemaCount} >0
    Should Contain                          ${responseJson['ietf-netconf-monitoring:netconf-state']['schemas']['schema'][0]['location']}   ${netconf}

Write to bookstore using passthrough-running
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${jsonData}=         Get Binary File    ${DATADIR}${/}bookstoreAddEntry.json
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

Verify write to bookstore using passthrough-running
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    FOR   ${item}   IN  @{responseJson['stores:bookstore']['categories']}
        IF   "${item['code']}" == "ISBN200123"
            Should Be Equal As Strings              "${item['books']}[0][title]"  "A good book"
        END
    END

Update Bookstore using passthrough running
        ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
        ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
        ${jsonData}=         Get Binary File    ${DATADIR}${/}bookstoreUpdateEntry.json
        ${response}=         PUT On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
        Should Be Equal As Strings              ${response.status_code}   200

Verify update to bookstore using passthrough running
        ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
        ${headers}=          Create Dictionary  Authorization=${auth}
        ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}
        ${responseJson}=     Set Variable       ${response.json()}
        Should Be Equal As Strings              ${response.status_code}   200
        FOR   ${item}   IN  @{responseJson['stores:bookstore']['categories']}
            IF   "${item['code']}" == "Updated-ISBN200123"
                Should Be Equal As Strings              "${item['name']}"  "update-library"
            END
        END