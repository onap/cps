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
Documentation         Public Properties Query Test

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn

Suite Setup           Create Session      CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***

${auth}                                Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpBasePath}                        /ncmp/v1
${jsonMatchingPublicProperties}        {"publicCmHandleProperties": {"name": "Contact", "value": "newemailforstore@bookstore.com"}}
${jsonNonMatchingPublicProperties}     {"publicCmHandleProperties": {"name": "Contact", "value": "some other email"}}
${jsonNoPublicProperties}              {"publicCmHandleProperties": {"name": "", "value": ""}}
${jsonPublicPropertiesThrowsExc}       {"publicCmHandleProperties": {"wrong value": "Contact", "value": "some other email"}}

*** Test Cases ***
Retrieve CM Handles where Public Properties Match
    ${uri}=              Set Variable       ${ncmpBasePath}/data/ch/searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonMatchingPublicProperties}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    Should Contain       ${responseJson}    PNFDemo
    Should Contain       ${responseJson}    PNFDemo2

Retrieve All CM Handles where there are no Public Properties
    ${uri}=              Set Variable       ${ncmpBasePath}/data/ch/searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonNoPublicProperties}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    Should Contain       ${responseJson}    PNFDemo
    Should Contain       ${responseJson}    PNFDemo2
    Should Contain       ${responseJson}    PNFDemo3

Retrieve NO CM Handles where Public Properties don't Match
    ${uri}=              Set Variable       ${ncmpBasePath}/data/ch/searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonNonMatchingPublicProperties}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    Should Not Contain   ${responseJson}    PNFDemo
    Should Not Contain   ${responseJson}    PNFDemo2
    Should Not Contain   ${responseJson}    PNFDemo3

Throw 400 when Structure of Request is Incorrect
    ${uri}=              Set Variable       ${ncmpBasePath}/data/ch/searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonPublicPropertiesThrowsExc}    expected_status=400
    Should Be Equal As Strings              ${response}   <Response [400]>
