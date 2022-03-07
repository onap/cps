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

*** Keywords ***

Run Test
	[Arguments]    ${node}    ${value}
	${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/${node}
	${headers}=          Create Dictionary  Authorization=${auth}
	${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
	${responseJson}=     Set Variable       ${response.json()}
    Log to Console       ${\n}"${responseJson}"${\n}
	Should Be Equal As Strings              ${response.status_code}   200
	IF    "${responseJson['cmHandle']}" == "${node}"
		FOR   ${item}   IN  @{responseJson['publicCmHandleProperties']}
		    Should Be Equal As Strings              "${item['Contact']}"  "${value}"
		END
	END

*** Variables ***

${auth}                   Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpInventoryBasePath}  /ncmpInventory
${ncmpBasePath}           /ncmp
${dmiUrl}                 http://${DMI_HOST}:${DMI_PORT}

${jsonDataCreate}         {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","createdCmHandles":[{"cmHandle":"PNFDemo","cmHandleProperties":{"Book1":"Sci-Fi Book 1"},"publicCmHandleProperties":{"Contact":"storeemail@bookstore.com"}},{"cmHandle":"PNFDemo2","cmHandleProperties":{"Book2":"Sci-Fi Book 2"},	"publicCmHandleProperties":{"Contact":"newemailforstore@bookstore.com"}},{"cmHandle": "PNFDemo3","cmHandleProperties":{"Book3": "Sci-Fi Book 3"},"publicCmHandleProperties":{"Contact":"someotheremail@doesntexist.fake"}}]}

${jsonDataUpdate}         {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","updatedCmHandles":[{"cmHandle":"PNFDemo","cmHandleProperties":{"Book1":"Romance Book"},"publicCmHandleProperties":{"Contact":"newemailforstore@bookstore.com"}}]}

*** Test Cases ***
Register data node and sync modules.
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonDataCreate}
    Should Be Equal As Strings              ${response.status_code}   204

Get CM Handle details and confirm it has been registered.
    Run Test    PNFDemo    storeemail@bookstore.com
    Run Test    PNFDemo2   newemailforstore@bookstore.com
    Run Test    PNFDemo3   someotheremail@doesntexist.fake

Update data node and sync modules.
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonDataUpdate}
    Should Be Equal As Strings              ${response.status_code}   204

Get CM Handle details and confirm it has been updated.
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         GET On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    IF    "${responseJson['cmHandle']}" == "PNFDemo"
           FOR   ${item}   IN  @{responseJson['publicCmHandleProperties']}
                   Should Be Equal As Strings              "${item['Contact']}"  "newemailforstore@bookstore.com"
           END
    END

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
