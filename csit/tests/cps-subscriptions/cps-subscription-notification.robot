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
    ${jsonData}=                    Get Binary File             ${DATADIR_SUBS_NOTIFICATION}${/}createCmHandleRequestBody.json
    ${response}=                    POST On Session             CPS_URL   ${basePath}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings      ${response.status_code}     200
    Sleep                           5                           wait some time to get updated in the db

Verify Kafka flow for Subscription Creation Notification
    ${group_id}=                    Create Consumer
    Subscribe Topic                 group_id=${group_id}        topics=${RESPONSE_TOPIC}
    Wait Until Keyword Succeeds     10x  3s                     All Messages Are Produced and Consumed                  ${group_id}
    [Teardown]                      Basic Teardown              ${group_id}

*** Keywords ***
Starting Test
    Set Suite Variable              ${REQUEST_TOPIC}            subscription
    Set Suite Variable              ${RESPONSE_TOPIC}           subscription-response
    ${ncmpOutEventJson}=            Get File                    ${DATADIR_SUBS_NOTIFICATION}${/}cmSubscriptionNcmpOutEventForCsit.json
    ${ncmpOutEventJson}=            Evaluate                    json.loads("""${ncmpOutEventJson}""")                   json
    Set Suite Variable              ${ncmpOutEventJsonGlobal}   ${ncmpOutEventJson}
    ${thread}=                      Start Consumer Threaded     topics=test
    Set Suite Variable              ${MAIN_THREAD}              ${thread}
    ${producer_group_id}=           Create Producer
    Set Suite Variable              ${PRODUCER_ID}              ${producer_group_id}
    ${ncmpInEventJson}=             Get File                    ${DATADIR_SUBS_NOTIFICATION}${/}cmSubscriptionNcmpInEventForCsit.json         encoding=UTF-8
    Set Suite Variable              ${ncmpInEventJsonGlobal}    ${ncmpInEventJson}
    ${headers}=                     Create Dictionary           ce_specversion=1.0  ce_id=some-event-id  ce_source=some-resource  ce_type=subscriptionCreated  ce_correlationid=test-cmhandle1
    Set Suite Variable              ${headersGlobal}            ${headers}

All Messages Are Produced and Consumed
    [Arguments]                     ${GROUP_ID}
    Produce                         group_id=${PRODUCER_ID}     topic=${REQUEST_TOPIC}    value=${ncmpInEventJsonGlobal}    headers=${headersGlobal}
    Sleep                           10sec
    ${result}=                      Poll                        group_id=${GROUP_ID}      only_value=False
    ${headers}                      Set Variable                      ${result[0].headers()}
    ${value}                        Set Variable                      ${result[0].value()}
    ${valueAsDict}=                 Evaluate                          json.loads("""${value}""")                              json
    ${specVersionHeaderValue}       Set Variable                      ${headers.lastHeader('ce_specversion')}
    ${sourceHeaderValue}            Set Variable                      ${headers.lastHeader('ce_source')}
    ${typeHeaderValue}              Set Variable                      ${headers.lastHeader('ce_type')}
    ${correlationIdHeaderValue}     Set Variable                      ${headers.lastHeader('ce_correlationid')}
    Dictionaries Should Be Equal    ${valueAsDict}                    ${ncmpOutEventJsonGlobal}
    Should Be Equal As Strings      ${specVersionHeaderValue}         1.0
    Should Be Equal As Strings      ${sourceHeaderValue}              NCMP
    Should Be Equal As Strings      ${typeHeaderValue}                subscriptionCreatedStatus
    Should Be Equal As Strings      ${correlationIdHeaderValue}       SCO-9989752cm-subscription-001

Basic Teardown
    [Arguments]                     ${group_id}
    Unsubscribe                     ${group_id}
    Close Consumer                  ${group_id}