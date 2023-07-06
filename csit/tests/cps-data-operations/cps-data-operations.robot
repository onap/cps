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
${ncmpBasePath}                         /ncmp
${CmHandleJsonReqBody}                  {"dmiPlugin":"some-dummy-plugin","createdCmHandles":[{"cmHandle":"NonReadyTest","cmHandleProperties":{"Book2":"Sci-Fi Book"}}]}

*** Test Cases ***
Receiving Data operation request at NCMP with non existing cmHandle then it publishes error message to client topic
    ${uri}=                         Set Variable               ${ncmpBasePath}/v1/data
    ${dataOperationReqBody}=        Get Binary File            ${DATADIR}${/}dataOperationNonAvailableCMHandle.json
    ${params}=                      Create Dictionary          topic=some-client-topic
    ${headers}=                     Create Dictionary          Content-Type=application/json   Authorization=${auth}
    ${response}=                    POST On Session    CPS_URL   ${uri}   params=${params}   headers=${headers}   data=${dataOperationReqBody}
    Should Be Equal As Strings      ${response.status_code}    200
    Sleep    5

Consume Kafka Messages for the scenario cmHandle not found
    ${expectedStatusMessage}=           Set Variable                                          cm handle id(s) not found
    ${expectedStatusCode}=              Set Variable                                          100
    ${consumedEventAsDict}=             Consume Kafka Messages and returns as dictionary      some-client-topic
    Fetch StatusMessage and StatusCode values from polled message and verify it with expected values   ${consumedEventAsDict}   ${expectedStatusMessage}      ${expectedStatusCode}

Receiving Data operation request at NCMP with non ready cmHandle then publishes error message to client topic
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/data
    ${dataOperationReqBody}=                Get Binary File     ${DATADIR}${/}dataOperationNonReadyCMHandle.json
    ${params}=           Create Dictionary  topic=test-client-topic
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
                         POST On Session    CPS_URL   ncmpInventory/v1/ch   headers=${headers}   data=${CmHandleJsonReqBody}
    ${response}=         POST On Session    CPS_URL   ${uri}   params=${params}   headers=${headers}   data=${dataOperationReqBody}
    Should Be Equal As Strings              ${response.status_code}   200
    Sleep    5

Consume Kafka Messages for the scenario cmHandle state non ready
    ${expectedStatusMessage}=           Set Variable                                           cm handle(s) not ready
    ${expectedStatusCode}=              Set Variable                                           101
    ${consumedEventAsDict}=             Consume Kafka Messages and returns as dictionary       test-client-topic
    Fetch StatusMessage and StatusCode values from polled message and verify it with expected values   ${consumedEventAsDict}   ${expectedStatusMessage}     ${expectedStatusCode}

*** Keywords ***
Consume Kafka Messages and returns as dictionary
    [Arguments]     ${topic}
    ${group_id}=         Create Consumer     port=19092              auto_offset_reset=earliest
    Subscribe Topic      topics=${topic}     group_id=${group_id}
    ${messages}=         Poll                group_id=${group_id}    decode_format=utf_8
    ${consumedEventAsDict}=   Evaluate            json.loads('''${messages[0]}''')
    [Teardown]           Basic Teardown      ${group_id}
    RETURN               ${consumedEventAsDict}

Fetch StatusMessage and StatusCode values from polled message and verify it with expected values
    [Arguments]     ${consumedEventAsDict}      ${expectedStatusMessage}        ${expectedStatusCode}
    ${statusMessage}=   Get From Dictionary         ${consumedEventAsDict}[data][responses][0]       statusMessage
    ${statusCode}=      Get From Dictionary         ${consumedEventAsDict}[data][responses][0]       statusCode
    Should Be Equal As Strings      ${statusMessage}          ${expectedStatusMessage}
    Should Be Equal As Strings      ${statusCode}             ${expectedStatusCode}

Basic Teardown
    [Arguments]  ${group_id}
    Unsubscribe  ${group_id}
    Close Consumer  ${group_id}

