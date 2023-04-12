# ============LICENSE_START=======================================================
# Copyright (C) 2023 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================


*** Settings ***
Library  ConfluentKafkaLibrary
Library  Collections
Library  OperatingSystem
Library  RequestsLibrary

Suite Setup  Starting Test

*** Variables ***

${auth}                 Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${basePath}             /ncmpInventory/v1/ch


*** Test Cases ***
Create CM Handle
    Create Session                  CPS_URL                     http://${CPS_CORE_HOST}:${CPS_CORE_PORT}
    ${headers}                      Create Dictionary           Content-Type=application/json   Authorization=${auth}
    ${jsonData}=                    Get Binary File             ${DATADIR_CM_HANDLE}${/}createCmHandle.json
    ${response}=                    POST On Session             CPS_URL   ${basePath}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings      ${response.status_code}     200

Verify Kafka flow for CM Handle Lifecycle Notification
    ${group_id}=                    Create Consumer             auto_offset_reset=earliest
    Subscribe Topic                 group_id=${group_id}        topics=${LCM_RESPONSE_TOPIC}
    Wait Until Keyword Succeeds     10x  3s                     All Messages Consumed                                   ${group_id}
    [Teardown]                      Basic Teardown              ${group_id}

*** Keywords ***
Starting Test
    Set Suite Variable              ${LCM_RESPONSE_TOPIC}       ncmp-events
    ${createCmHandleRespJson}=      Get File                    ${DATADIR_CM_HANDLE}${/}createCmHandleResponse.json
    ${createCmHandleRespJson}=      Evaluate                    json.loads("""${createCmHandleRespJson}""")             json
    Set Suite Variable              ${createCmHandleRespJsonGlobal}   ${createCmHandleRespJson}
    ${thread}=                      Start Consumer Threaded     topics=test
    Set Suite Variable              ${MAIN_THREAD}              ${thread}
    ${producer_group_id}=           Create Producer
    Set Suite Variable              ${PRODUCER_ID}              ${producer_group_id}

All Messages Consumed
    [Arguments]                     ${GROUP_ID}
    ${consumed}=                    Poll                        group_id=${GROUP_ID}                                    max_records=1        decode_format=utf_8
    ${consumedAsDict}=              Evaluate                    json.loads("""${consumed[0]}""")                        json
    ${eventSource}=                 Get From Dictionary         ${consumedAsDict}                                       eventSource
    ${eventSchema}=                 Get From Dictionary         ${consumedAsDict}                                       eventSchema
    ${eventType}=                   Get From Dictionary         ${consumedAsDict}                                       eventType
    ${payload}=                     Get From Dictionary         ${consumedAsDict}                                       event
    Log                             Retrying for LCM flow...    console=yes
    Should Be Equal As Strings      ${eventSource}              org.onap.ncmp
    Should Be Equal As Strings      ${eventSchema}              org.onap.ncmp:cmhandle-lcm-event
    Should Be Equal As Strings      ${eventType}                org.onap.ncmp.cmhandle-lcm-event.create
    Dictionaries Should Be Equal    ${payload}                  ${createCmHandleRespJsonGlobal}

Basic Teardown
    [Arguments]                     ${group_id}
    Unsubscribe                     ${group_id}
    Close Consumer                  ${group_id}