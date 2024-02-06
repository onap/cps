/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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
${createPayload}         {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","createdCmHandles":[{"cmHandle":"ietfYang-PNFDemo","cmHandleProperties":{"Book1":"Sci-Fi Book"},"publicCmHandleProperties":{"Contact":"storeemail@bookstore.com", "Contact2":"storeemail2@bookstore.com"}},{"cmHandle":"CmHandleForDelete"}]}
${updatePayload}         {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","updatedCmHandles":[{"cmHandle":"ietfYang-PNFDemo","cmHandleProperties":{"Book1":"Romance Book"},"publicCmHandleProperties":{"Contact":"newemailforstore@bookstore.com"}}]}
${deletePayload}         {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","removedCmHandles":["CmHandleForDelete"]}

*** Test Cases ***
Register data node and sync modules.
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${createPayload}
    Should Be Equal As Strings              ${response.status_code}   200

Get CM Handle details and confirm it has been registered.
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/ietfYang-PNFDemo
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    ${schemaCount}=      Get length         ${responseJson}
    Should Be Equal As Strings              ${response.status_code}   200
    IF    "${responseJson['cmHandle']}" == "ietfYang-PNFDemo"
           FOR   ${item}   IN  @{responseJson['publicCmHandleProperties']}
                   Should Be Equal As Strings              "${item['Contact']}"  "storeemail@bookstore.com"
           END
    END

Update data node and sync modules.
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${updatePayload}
    Should Be Equal As Strings              ${response.status_code}   200

Get CM Handle details and confirm it has been updated.
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/ietfYang-PNFDemo
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    ${schemaCount}=      Get length         ${responseJson}
    Should Be Equal As Strings              ${response.status_code}   200
    IF    "${responseJson['cmHandle']}" == "ietfYang-PNFDemo"
           FOR   ${item}   IN  @{responseJson['publicCmHandleProperties']}
                   Should Be Equal As Strings              "${item['Contact']}"  "newemailforstore@bookstore.com"
           END
    END

Delete cm handle
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${deletePayload}
    Should Be Equal As Strings              ${response.status_code}   200

Get cm handle details and confirm it has been deleted
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/CmHandleForDelete
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}   expected_status=404
    Should Be Equal As Strings              ${response}   <Response [404]>

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