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
Verify Kafka flow for Subscription Creation Notification
    ${group_id}=                    Create Consumer             auto_offset_reset=earliest
    Subscribe Topic                 group_id=${group_id}        topics=${RESPONSE_TOPIC}
    Wait Until Keyword Succeeds     10x  3s                     All Messages Are Produced and Consumed                  ${group_id}
    [Teardown]                      Basic Teardown              ${group_id}

*** Keywords ***
Starting Test
    Set Suite Variable              ${REQUEST_TOPIC}            cm-avc-subscription
    Set Suite Variable              ${RESPONSE_TOPIC}           dmi-ncmp-cm-avc-subscription
    ${avcEventRespJson}=            Get File                    ${DATADIR_SUBS_NOTIFICATION}${/}avcSubscriptionCreateEventResponse.json
    ${avcEventRespJson}=            Evaluate                    json.loads("""${avcEventRespJson}""")                   json
    Set Suite Variable              ${avcEventRespJsonGlobal}   ${avcEventRespJson}
    ${thread}=                      Start Consumer Threaded     topics=test
    Set Suite Variable              ${MAIN_THREAD}              ${thread}
    ${producer_group_id}=           Create Producer
    Set Suite Variable              ${PRODUCER_ID}              ${producer_group_id}
    ${avcEventRequest}=             Get File                    ${DATADIR_SUBS_NOTIFICATION}${/}avcSubscriptionCreationEvent.json         encoding=UTF-8
    Set Suite Variable              ${avcEventRequestVariable}  ${avcEventRequest}

All Messages Are Produced and Consumed
    [Arguments]                     ${GROUP_ID}
    Produce                         group_id=${PRODUCER_ID}     topic=${REQUEST_TOPIC}                                  value=${avcEventRequestVariable}
    Sleep                           10sec
    ${result}=                      Poll                        group_id=${GROUP_ID}                                    max_records=1        decode_format=utf_8
    ${resultAsDict}=                Evaluate                    json.loads("""${result[0]}""")                          json
    Log                             Retrying...                 console=yes
    Dictionaries Should Be Equal    ${resultAsDict}             ${avcEventRespJsonGlobal}

Basic Teardown
    [Arguments]                     ${group_id}
    Unsubscribe                     ${group_id}
    Close Consumer                  ${group_id}