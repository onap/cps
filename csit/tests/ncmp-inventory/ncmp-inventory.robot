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

${auth}                 Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpBasePath}         /ncmpInventory
${dmiUrl}               http://${DMI_HOST}:${DMI_PORT}
${jsonData}             {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":null,"dmiModelPlugin":null,"createdCmHandles":[{"cmHandle":"BookStore","cmHandleProperties":{"Book1":"Sci-Fi Book","Book2":"Horror Book","Book3":"Crime Book"},"publicCmHandleProperties":{"Public-Book1":"Public Sci-Fi Book","Public-Book2":"Public Horror Book","Public-Book3":"Public Crime Book"}}],"updatedCmHandles":[{"cmHandle":"BookStore","cmHandleProperties":{"Book1":"Romance Book","Book2":"Thriller Book","Book3":"Comedy Book"},"publicCmHandleProperties":{"Public-Book1":"Public Romance Book","Public-Book2":"Public Thriller Book","Public-Book3":"Public Comedy Book"}}],"removedCmHandles":["BookStore"]}

*** Test Cases ***

Register a DMI Plugin with any new, updated or removed CM Handles.
    ${uri}=             Set Variable        ${ncmpBasePath}/v1/ch
    ${headers}=         Create Dictionary   Content-Type=application/json   Authorization=${auth}
    ${response}=        POST On Session     CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201