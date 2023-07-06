/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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
${dmipluginCsitStubUrL}                 http://${DMI_CSIT_STUB_HOST}:${DMI_CSIT_STUB_PORT}
${CmHandleReadyJsonReqBody}             {"dmiPlugin":"${dmipluginCsitStubUrL}","createdCmHandles":[{"cmHandle":"DataOperationTest"}]}

*** Test Cases ***

Process the received Data operation request at NCMP then forward it to a respective DMI plugin later DMI publishes a response to client topic
        ${uri}=                          Set Variable        ${ncmpBasePath}/v1/data
        ${dataOperationReqBody}=         Get Binary File     ${DATADIR}${/}dataOperationReadyStateCMHandle.json
        ${params}=                       Create Dictionary   topic=${topic}
        ${headers}=                      Create Dictionary   Content-Type=application/json         Authorization=${auth}
                                         POST On Session     CPS_URL   ncmpInventory/v1/ch         headers=${headers}     data=${CmHandleReadyJsonReqBody}
        Sleep                            5
        ${response}=                     POST On Session     CPS_URL   ${uri}   params=${params}   headers=${headers}     data=${dataOperationReqBody}
        Set Global Variable              ${expectedRequestId}       ${response.json()}[requestId]
        Should Be Equal As Strings       ${response.status_code}   200
        Sleep                            5

Consume Kafka Messages for the scenario cmHandle state Ready
    ${group_id}=         Create Consumer     port=19092               auto_offset_reset=earliest
    Subscribe Topic      topics=${topic}     group_id=${group_id}
    ${messages}=         Poll                group_id=${group_id}     only_value=false
    ${event}                        Set Variable                      ${messages}[0]
    ${headers}                      Set Variable                      ${event.headers()}
    ${specVersionHeaderValue}       Set Variable                      ${headers[1][1]}
    ${sourceHeaderValue}            Set Variable                      ${headers[3][1]}
    ${typeHeaderValue}              Set Variable                      ${headers[4][1]}
    ${correlationIdHeaderValue}     Set Variable                      ${headers[8][1]}
    Should Be Equal As Strings      ${specVersionHeaderValue}         1.0
    Should Be Equal As Strings      ${sourceHeaderValue}              DMI
    Should Be Equal As Strings      ${correlationIdHeaderValue}       ${expectedRequestId}
    Should Be Equal As Strings      ${typeHeaderValue}                org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
    [Teardown]                      Basic Teardown                    ${group_id}

*** Keywords ***

Basic Teardown
    [Arguments]     ${group_id}
    Unsubscribe     ${group_id}
    Close Consumer  ${group_id}

