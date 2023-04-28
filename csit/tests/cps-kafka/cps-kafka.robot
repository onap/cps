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
${avcEventResponse}     {"clientId":"SCO-9989752","subscriptionName":"cm-subscription-001","dmiName":"dminame1","cmHandleIdToStatus":{"CMHandle2":"ACCEPTED","CMHandle1":"ACCEPTED"}}

*** Test Cases ***
Create CM Handle
    Create Session                  CPS_URL                     http://${CPS_CORE_HOST}:${CPS_CORE_PORT}
    ${headers}                      Create Dictionary           Content-Type=application/json   Authorization=${auth}
    ${jsonData}=                    Get Binary File             ${DATADIR}${/}createCmHandle.json
    ${response}=                    POST On Session             CPS_URL   ${basePath}   headers=${headers}              data=${jsonData}
    Should Be Equal As Strings      ${response.status_code}     200

Verify Kafka flow for Subscription Creation Notification
    ${group_id}=                    Create Consumer             auto_offset_reset=earliest
    Subscribe Topic                 group_id=${group_id}        topics=${RESPONSE_TOPIC}
    ${result}=                      Poll                        group_id=${group_id}            max_records=1           decode_format=utf_8
    Should Be Equal As Strings      ${result[0]}                ${avcEventResponse}
    [Teardown]                      Basic Teardown              ${group_id}

*** Keywords ***
Starting Test
    Set Suite Variable              ${REQUEST_TOPIC}            cm-avc-subscription
    Set Suite Variable              ${RESPONSE_TOPIC}           dmi-ncmp-cm-avc-subscription
    ${thread}=                      Start Consumer Threaded     topics=${REQUEST_TOPIC}
    Set Suite Variable              ${MAIN_THREAD}              ${thread}
    ${producer_group_id}=           Create Producer
    Set Suite Variable              ${PRODUCER_ID}              ${producer_group_id}
    ${avcEventRequest}=             Get File                    ${DATADIR}${/}avcSubscriptionCreationEvent.json         encoding=UTF-8
    Set Suite Variable              ${avcEventRequestVariable}  ${avcEventRequest}
    Prepare Test Data

Prepare Test Data
    Produce                         group_id=${PRODUCER_ID}     topic=${REQUEST_TOPIC}             value=${avcEventRequestVariable}
    Wait Until Keyword Succeeds     10x  0.5s                   All Messages Are Delivered         ${PRODUCER_ID}       1
    Sleep  15sec

All Messages Are Delivered
    [Arguments]                     ${producer_id}              ${test_count}
    ${count}=                       Flush                       ${producer_id}
    Log  Reaming messages to be delivered: ${count}
    Should Be Equal As Integers     ${count}                    ${test_count}

Basic Teardown
    [Arguments]                     ${group_id}
    Unsubscribe                     ${group_id}
    Close Consumer                  ${group_id}