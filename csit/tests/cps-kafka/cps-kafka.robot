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

Suite Setup  Starting Test

*** Test Cases ***
Verify Topics
    ${group_id}=  Create Consumer  auto_offset_reset=earliest
    ${topics}=  List Topics  ${group_id}
    Dictionary Should Contain Key  ${topics}  ${TEST_TOPIC}
    [Teardown]  Close Consumer  ${group_id}

Consume Basic Record
    ${group_id}=  Create Consumer  auto_offset_reset=earliest
    Subscribe Topic  group_id=${group_id}  topics=${TEST_TOPIC}
    ${messages}=  Poll  group_id=${group_id}  max_records=2  decode_format=utf_8
    ${data}=  Create List  CPS  NCMP
    Lists Should Be Equal  ${messages}  ${data}
    [Teardown]  Basic Teardown  ${group_id}

Consume Complex Record
    ${group_id}=  Create Consumer  auto_offset_reset=earliest
    Subscribe Topic  group_id=${group_id}  topics=${TEST_TOPIC}
    ${messages}=  Poll  group_id=${group_id}  max_records=3  decode_format=utf_8
    ${data}=  Create List  CPS  NCMP  {'name': 'CPS', 'nest': {'node': 'left', 'birds':['Eagle','Falcon']}}
    Lists Should Be Equal  ${messages}  ${data}
    [Teardown]  Basic Teardown  ${group_id}

Publish Record Without Value
    ${topic_name}=  Set Variable  sometopic
    Produce  group_id=${PRODUCER_ID}  topic=${topic_name}
    Wait Until Keyword Succeeds  10x  0.5s  All Messages Are Delivered  ${PRODUCER_ID}  1
    ${group_id}=  Create Consumer  auto_offset_reset=earliest
    Subscribe Topic  group_id=${group_id}  topics=${topic_name}
    ${messages}=  Poll  group_id=${group_id}  max_records=1
    Should Be Equal As Strings  ${messages}  [None]

*** Keywords ***
Starting Test
    Set Suite Variable  ${TEST_TOPIC}  test
    ${thread}=  Start Consumer Threaded  topics=${TEST_TOPIC}
    Set Suite Variable  ${MAIN_THREAD}  ${thread}
    ${producer_group_id}=  Create Producer
    Set Suite Variable  ${PRODUCER_ID}  ${producer_group_id}
    Prepare Test Data

Prepare Test Data
    Produce  group_id=${PRODUCER_ID}  topic=${TEST_TOPIC}  value=CPS
    Produce  group_id=${PRODUCER_ID}  topic=${TEST_TOPIC}  value=NCMP
    Produce  group_id=${PRODUCER_ID}  topic=${TEST_TOPIC}  value={'name': 'CPS', 'nest': {'node': 'left', 'birds':['Eagle','Falcon']}}
    Wait Until Keyword Succeeds  10x  0.5s  All Messages Are Delivered  ${PRODUCER_ID}  3
    Sleep  2sec

All Messages Are Delivered
    [Arguments]  ${producer_id}  ${test_count}
    ${count}=  Flush  ${producer_id}
    Log  Reaming messages to be delivered: ${count}
    Should Be Equal As Integers  ${count}  ${test_count}

Basic Teardown
    [Arguments]  ${group_id}
    Unsubscribe  ${group_id}
    Close Consumer  ${group_id}