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
Documentation         NCMP-DMI - Registration & Model Sync

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn

Suite Setup           Create Session     DMI_URL    http://${DMI_HOST}:${DMI_PORT}

*** Variables ***

${auth}            Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${basePath}        /dmi

*** Test Cases ***
Register node & sync models
    ${uri}=              Set Variable       ${basePath}/v1/inventory/cmHandles
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${jsonData}=         Get Binary File    ${DATADIR}${/}cmHandleRegistration.json
    ${response}=         POST On Session    DMI_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

Verify Sync
    ${uri}=              Set Variable       ${basePath}/v1/ch/PNFDemo/modules
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${jsonData}=         Get Binary File    ${DATADIR}${/}postModuleRequestBody.json
    ${response}=         POST On Session    DMI_URL   ${uri}   headers=${headers}   data=${jsonData}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    FOR   ${item}   IN  @{responseJson['schemas']}
            IF   "${item['moduleName']}" == "stores"
                Should Be Equal As Strings              "${item['revision']}"   "2020-09-15"
                Should Be Equal As Strings              "${item['namespace']}"  "org:onap:ccsdk:sample"
            END
        END
