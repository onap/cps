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
Documentation         Public Properties Query Test

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn

Suite Setup           Create Session      CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***

${auth}                                     Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpBasePath}                             /ncmp/v1
${jsonModuleAndPropertyQueryParameters}     {"cmHandleQueryParameters": [{"conditionName": "hasAllModules", "conditionParameters": [ {"moduleName": "iana-crypt-hash"} ]}, {"conditionName": "hasAllProperties", "conditionParameters": [ {"Contact": "newemailforstore@bookstore.com"} ]}]}
${jsonEmptyQueryParameters}                 {}
${jsonMissingPropertyQueryParameters}       {"cmHandleQueryParameters": [{"conditionName": "hasAllProperties", "conditionParameters": [{"" : "doesnt matter"}]}]}

*** Test Cases ***
Retrieve CM Handle ids where query parameters Match (module and property query)
    ${uri}=              Set Variable       ${ncmpBasePath}/ch/id-searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonModuleAndPropertyQueryParameters}
    Should Be Equal As Strings              ${response.status_code}   200
    Should Contain       ${response.json()}    ietfYang-PNFDemo

Retrieve CM Handle ids where query parameters Match (empty query)
    ${uri}=              Set Variable       ${ncmpBasePath}/ch/id-searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonEmptyQueryParameters}
    Should Be Equal As Strings              ${response.status_code}   200
    Should Contain       ${response.json()}    ietfYang-PNFDemo

Throw 400 when Structure of Request is Incorrect
    ${uri}=              Set Variable       ${ncmpBasePath}/ch/id-searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonMissingPropertyQueryParameters}    expected_status=400
    Should Be Equal As Strings              ${response}   <Response [400]>
