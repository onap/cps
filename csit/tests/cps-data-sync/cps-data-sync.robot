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

*** Settings ***
Documentation         NCMP

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn

Suite Setup           Create Session      CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***

${auth}                   Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpInventoryBasePath}  /ncmpInventory
${ncmpBasePath}           /ncmp
${dmiUrl}                 http://${DMI_HOST}:${DMI_PORT}

*** Test Cases ***
Get CM Handle with 'READY' state and confirm that initial dataSyncEnabled flag is set
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/state
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=    Set Variable        ${response.json()['state']}
    Should Be Equal As Strings              ${responseJson['cmHandleState']}     READY
    Should Be Equal As Strings              ${responseJson['dataSyncEnabled']}   False

Verify Operational state matches when dataSyncEnabled=false
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/state
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=    Set Variable        ${response.json()['state']['dataSyncState']['operational']}
    Should Be Equal As Strings              ${responseJson['syncState']}   NONE_REQUESTED

Set dataSyncEnabled flag to TRUE and verify operational state goes to UNSYNCHRONIZED
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data-sync
    ${params}=           Create Dictionary  dataSyncEnabled=true
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         PUT On Session     CPS_URL   ${uri}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   200
    ${verifyUri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/state
    ${verifyHeaders}=          Create Dictionary  Authorization=${auth}
    ${verifyResponse}=         GET On Session     CPS_URL   ${verifyUri}   headers=${verifyHeaders}
    ${verifyResponseJson}=    Set Variable        ${verifyResponse.json()['state']['dataSyncState']['operational']}
    Should Be Equal As Strings                    ${verifyResponseJson['syncState']}   UNSYNCHRONIZED
    Sleep    5

Verify successful synchronization after dataSyncEnabled flag is set to TRUE
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/state
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=    Set Variable        ${response.json()['state']['dataSyncState']['operational']}
    Should Be Equal As Strings              ${responseJson['syncState']}   SYNCHRONIZED