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

${auth}                   Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpBasePath}           /ncmp

*** Test Cases ***
Operational state goes to UNSYNCHRONIZED when data sync (flag) is enabled
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/ietfYang-PNFDemo/data-sync
    ${params}=           Create Dictionary  dataSyncEnabled=true
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         PUT On Session     CPS_URL   ${uri}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   200
    ${verifyUri}=              Set Variable       ${ncmpBasePath}/v1/ch/ietfYang-PNFDemo/state
    ${verifyHeaders}=          Create Dictionary  Authorization=${auth}
    ${verifyResponse}=         GET On Session     CPS_URL   ${verifyUri}   headers=${verifyHeaders}
    Should Be Equal As Strings                    ${verifyResponse.json()['state']['dataSyncState']['operational']['syncState']}   UNSYNCHRONIZED
    Sleep    5

Operational state goes to SYNCHRONIZED after sometime when data sync (flag) is enabled
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/ietfYang-PNFDemo/state
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    Should Be Equal As Strings              ${response.json()['state']['dataSyncState']['operational']['syncState']}   SYNCHRONIZED