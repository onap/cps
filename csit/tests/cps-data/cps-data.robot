# ============LICENSE_START=======================================================
# Copyright (c) 2021 Pantheon.tech.
# Modifications Copyright (C) 2022 Bell Canada.
# Modifications Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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
Documentation         CPS Core - Data REST API

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary

Suite Setup           Create Session      CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***

${basePath}             /cps/api
${dataspaceName}        CSIT-Dataspace
${anchorName}           CSIT-Anchor

*** Test Cases ***
Create Data Node
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors/${anchorName}/nodes
    ${headers}          Create Dictionary   Content-Type=application/json
    ${jsonData}=        Get Binary File     ${DATADIR_CPS_CORE}${/}test-tree.json
    ${response}=        POST On Session     CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

Patch Data Node
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors/${anchorName}/nodes
    ${params}=          Create Dictionary   xpath=/test-tree/branch[@name='Right']
    ${headers}          Create Dictionary   Content-Type=application/json
    ${jsonData}=        Get Binary File     ${DATADIR_CPS_CORE}${/}testTreePatchExample.json
    ${response}=        PATCH On Session    CPS_URL   ${uri}  params=${params}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   200

Get Updated Data Node by XPath
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors/${anchorName}/node
    ${params}=          Create Dictionary   xpath=/test-tree/branch[@name='Right']/nest
    ${response}=        Get On Session      CPS_URL   ${uri}   params=${params}   expected_status=200
    ${responseJson}=    Set Variable        ${response.json()['tree:nest']}
    Should Be Equal As Strings              ${responseJson['name']}   Bigger
    ${length_birds}=    Get Length          ${responseJson['birds']}
    Should Be Equal As Integers             ${length_birds}   3
    Should Be Equal As Strings              ${responseJson['birds']}   ['Eagle', 'Falcon', 'Pigeon']

Get Data Node by XPath
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors/${anchorName}/node
    ${params}=          Create Dictionary   xpath=/test-tree/branch[@name='LEFT/left']/nest
    ${response}=        Get On Session      CPS_URL   ${uri}   params=${params}   expected_status=200
    Should Be Equal As Strings              ${response.json()['tree:nest']['name']}   SMALL/small


