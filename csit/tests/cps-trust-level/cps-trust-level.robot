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
Documentation         Trust Level Test

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn
Library               ConfluentKafkaLibrary

Suite Setup           Starting Test

*** Variables ***

${auth}                                   Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpInventoryBasePath}                  /ncmpInventory
${ncmpBasePath}                           /ncmp/v1
${dmiUrl}                                 http://${DMI_HOST}:${DMI_PORT}
${jsonCH-1Create}                      {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","createdCmHandles":[{"cmHandle":"CH-1","trustLevel":"COMPLETE"}]}
${jsonCH-2Create}                      {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","createdCmHandles":[{"cmHandle":"CH-2","trustLevel":"COMPLETE"}]}
${jsonCH-3Create}                      {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","createdCmHandles":[{"cmHandle":"CH-3"}]}
${jsonCH-4Create}                      {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","createdCmHandles":[{"cmHandle":"CH-4","trustLevel":"NONE"}]}
${jsonTrustLevelPropertyQueryParameters}  {"cmHandleQueryParameters": [{"conditionName": "cmHandleWithTrustLevel", "conditionParameters": [ {"trustLevel": "COMPLETE"} ] }]}
${jsonTrustLevelQueryResponse}            {"data":{"attributeValueChange":[{"attributeName":"trustLevel","newAttributeValue":"NONE"}]}}

*** Test Cases ***
Register data node CH-1
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonCH-1Create}
    Should Be Equal As Strings              ${response.status_code}   200

Register data node CH-2
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonCH-2Create}
    Should Be Equal As Strings              ${response.status_code}   200

Register data node CH-3
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonCH-3Create}
    Should Be Equal As Strings              ${response.status_code}   200

Register data node CH-4
    ${uri}=              Set Variable       ${ncmpInventoryBasePath}/v1/ch
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonCH-4Create}
    Should Be Equal As Strings              ${response.status_code}   200

Retrieve CM Handle ids where query parameters Match (trust level query)
    ${uri}=              Set Variable       ${ncmpBasePath}/ch/id-searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonTrustLevelPropertyQueryParameters}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    Should Contain       ${responseJson}    CH-3, CH-2, CH-1, PNFDemo

Verify Kafka flow for Subscription Creation Notification
    ${group_id}=         Create Consumer
    Subscribe Topic      group_id=${group_id}        topics=cm-events
    Wait Until Keyword Succeeds     10x  3s                     All Messages Are Consumed                  ${group_id}
    [Teardown]                      Basic Teardown              ${group_id}

*** Keywords ***
Starting Test
    Create Session                  CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}
    ${thread}=                      Start Consumer Threaded     topics=cm-events
    Set Suite Variable              ${MAIN_THREAD}              ${thread}

All Messages Are Consumed
    [Arguments]                     ${GROUP_ID}
    ${result}=                      Poll                        group_id=${GROUP_ID}      only_value=False
    ${headers}                      Set Variable                ${result[0].headers()}
    ${payload}                      Set Variable                ${result[0].value()}
    ${payloadAsDict}=               Evaluate                    json.loads("""${payload}""")        json
    FOR   ${header_key_value_pair}   IN  @{headers}
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}     "ce_specversion"      "1.0"
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}     "ce_source"           "NCMP"
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}     "ce_type"             "org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent"
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}     "ce_correlationid"    "CH-4"
    END
    Dictionaries Should Be Equal    ${payloadAsDict}    ${jsonTrustLevelQueryResponse}

Compare Header Values
    [Arguments]                    ${header_key}        ${header_value}      ${header_to_check}       ${expected_header_value}
    IF   "${header_key}" == ${header_to_check}
        Should Be Equal As Strings              "${header_value}"    ${expected_header_value}
    END

Basic Teardown
    [Arguments]                     ${group_id}
    Unsubscribe                     ${group_id}
    Close Consumer                  ${group_id}
