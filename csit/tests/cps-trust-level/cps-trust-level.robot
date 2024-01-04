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

Suite Setup           Create Session    CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***
${auth}                                     Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpInventoryBasePath}                    /ncmpInventory
${ncmpBasePath}                             /ncmp/v1
${dmiUrl}                                   http://${DMI_HOST}:${DMI_PORT}
${jsonCreateCmHandles}                      {"dmiPlugin":"${dmiUrl}","dmiDataPlugin":"","dmiModelPlugin":"","createdCmHandles":[{"trustLevel":"COMPLETE","cmHandle":"ietfYang-CH-1"},{"trustLevel":"COMPLETE","cmHandle":"bookStore-CH-2"},{"cmHandle":"ietfYang-CH-3"},{"trustLevel":"NONE","cmHandle":"bookStore-CH-4"}]}
${jsonTrustLevelPropertyQueryParameters}    {"cmHandleQueryParameters": [{"conditionName": "cmHandleWithTrustLevel", "conditionParameters": [ {"trustLevel": "COMPLETE"} ] }]}
${jsonTrustLevelQueryResponse}              {"data":{"attributeValueChange":[{"attributeName":"trustLevel","newAttributeValue":"NONE"}]}}
${partitionId}                              ${0}

*** Test Cases ***
Register data node and verify notification
    ${group_id}=           Create Consumer
    ${topic_partition}=    Create Topic Partition    cm-events      ${partitionId}
    ${offset}=             Get Watermark Offsets     ${group_id}    ${topic_partition}
    ${tp}=                 Create Topic Partition    cm-events      ${partitionId}    ${offset[1]}
    Assign To Topic Partition  ${group_id}  ${tp}
    Sleep  5sec
    Register Data Nodes
    ${result}=      Poll                    group_id=${group_id}  only_value=False  poll_attempts=5
    ${headers}                      Set Variable                ${result[0].headers()}
    ${payload}                      Set Variable                ${result[0].value()}
    FOR   ${header_key_value_pair}   IN  @{headers}
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}     "ce_specversion"      "1.0"
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}     "ce_source"           "NCMP"
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}     "ce_type"             "org.onap.cps.ncmp.events.avc.ncmp_to_client.AvcEvent"
        Compare Header Values       ${header_key_value_pair[0]}   ${header_key_value_pair[1]}     "ce_correlationid"    "CH-4"
    END
    Should Be Equal As Strings      ${payload}    ${jsonTrustLevelQueryResponse}
    [Teardown]    Basic Teardown    ${group_id}

Retrieve CM Handle ids where query parameters Match (trust level query)
    ${uri}=              Set Variable       ${ncmpBasePath}/ch/id-searches
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonTrustLevelPropertyQueryParameters}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    Should Contain       ${responseJson}    ietfYang-CH-1
    Should Contain       ${responseJson}    bookStore-CH-2
    Should Contain       ${responseJson}    ietfYang-CH-3
    Should Not Contain   ${responseJson}    bookStore-CH-4

*** Keywords ***
Register Data Nodes
    ${uri}=         Set Variable         ${ncmpInventoryBasePath}/v1/ch
    ${headers}=     Create Dictionary    Content-Type=application/json    Authorization=${auth}
    ${response}=    POST On Session      CPS_URL   ${uri}    headers=${headers}    data=${jsonCreateCmHandles}
    Should Be Equal As Strings           ${response.status_code}    200

Compare Header Values
    [Arguments]    ${header_key}    ${header_value}    ${header_to_check}    ${expected_header_value}
    IF    "${header_key}" == ${header_to_check}
        Should Be Equal As Strings    "${header_value}"    ${expected_header_value}
    END

Basic Teardown
    [Arguments]       ${group_id}
    Unsubscribe       ${group_id}
    Close Consumer    ${group_id}