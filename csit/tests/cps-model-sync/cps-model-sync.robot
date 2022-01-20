/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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
${jsonData}               {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":null,"dmiModelPlugin":null,"createdCmHandles":[{"cmHandle":"PNFDemo","cmHandleProperties":{"Book1":"Sci-Fi Book"},"publicCmHandleProperties":{"Contact":"storeemail@bookstore.com"}}],"updatedCmHandles":[{"cmHandle":"PNFDemo","cmHandleProperties":{"Book1":"Romance Book"},"publicCmHandleProperties":{"Contact":"newemailforstore@bookstore.com"}}]}

*** Test Cases ***
Register node, update data node and sync modules.
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

Get modules for registered data node
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/modules
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    FOR   ${item}   IN  @{responseJson}
            IF   "${item['moduleName']}" == "stores"
                Should Be Equal As Strings              "${item['revision']}"   "2020-09-15"
            END
        END
