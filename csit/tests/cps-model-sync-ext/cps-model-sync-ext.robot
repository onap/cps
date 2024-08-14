/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

Check if ietfYang-PNFDemo is READY
    ${uri}=        Set Variable       ${ncmpBasePath}/v1/ch/ietfYang-PNFDemo
    ${headers}=    Create Dictionary  Authorization=${auth}
    Wait Until Keyword Succeeds       20sec    200ms    Is CM Handle READY    ${uri}    ${headers}    ietfYang-PNFDemo

Get modules for registered data node
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/ietfYang-PNFDemo/modules
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   200
    FOR   ${item}   IN  @{response.json()}
            IF   "${item['moduleName']}" == "stores"
                Should Be Equal As Strings              "${item['revision']}"   "2020-09-15"
            END
    END

*** Keywords ***
Is CM Handle READY
    [Arguments]    ${uri}    ${headers}    ${cmHandle}
    ${response}=    GET On Session    CPS_URL    ${uri}    headers=${headers}
    Should Be Equal As Strings    ${response.status_code}    200
    FOR  ${item}  IN  ${response.json()}
            IF  "${item['cmHandle']}" == "${cmHandle}"
                Should Be Equal As Strings    ${item['state']['cmHandleState']}    READY
            END
    END