/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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
Library               ConfluentKafkaLibrary

Suite Setup           Create Session      CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***

${auth}                                 Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${topic}                                data-operation-client-topic
${ncmpBasePath}                         /ncmp
${expectedRequestId}                    ${EMPTY}
${dmipluginCsitStubUrl}                 http://${DMI_CSIT_STUB_HOST}:${DMI_CSIT_STUB_PORT}
${newCmHandleRequestBody}               {"dmiPlugin":"${dmipluginCsitStubUrl}","createdCmHandles":[{"cmHandle":"CMHandle1"}]}

*** Test Cases ***

NCMP Data Operation, forwarded to DMI, response on Client Topic
        ${uri}=                          Set Variable        ${ncmpBasePath}/v1/data
        ${dataOperationReqBody}=         Get Binary File     ${DATADIR_CPS_CORE}${/}dataOperationRequest.json
        ${params}=                       Create Dictionary   topic=${topic}
        ${headers}=                      Create Dictionary   Content-Type=application/json         Authorization=${auth}
                                         POST On Session     CPS_URL   ncmpInventory/v1/ch         headers=${headers}     data=${newCmHandleRequestBody}
        Sleep                            8                   wait some time to get updated the cm handle state to READY
        ${response}=                     POST On Session     CPS_URL   ${uri}   params=${params}   headers=${headers}     data=${dataOperationReqBody}
        Set Global Variable              ${expectedRequestId}       ${response.json()}[requestId]
        Should Be Equal As Strings       ${response.status_code}   200
        Sleep                            5                         wait some time to get published a message to the client topic

Consume cloud event from client topic
    ${group_id}=         Create Consumer     auto_offset_reset=earliest
    Subscribe Topic      topics=${topic}     group_id=${group_id}
    ${messages}=         Poll                group_id=${group_id}     only_value=false
    ${event}                        Set Variable                      ${messages}[0]
    ${headers}                      Set Variable                      ${event.headers()}
    FOR   ${header_key_value_pair}   IN  @{headers}
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}      "ce_specversion"      "1.0"
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}      "ce_type"             "org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent"
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}      "ce_correlationid"    "${expectedRequestId}"
                Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}      "ce_source"           "DMI"
    END
    [Teardown]                      Basic Teardown                    ${group_id}

*** Keywords ***
Compare Header Values
    [Arguments]                    ${header_key}        ${header_value}     ${header_to_check}       ${expected_header_value}
    IF   "${header_key}" == ${header_to_check}
        Should Be Equal As Strings              "${header_value}"    ${expected_header_value}
    END

Basic Teardown
    [Arguments]     ${group_id}
    Unsubscribe     ${group_id}
    Close Consumer  ${group_id}

