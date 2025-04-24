# ============LICENSE_START=======================================================
# Copyright (c) 2021 Pantheon.tech.
# Modifications Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
Documentation         CPS Core - Admin REST API

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary

Suite Setup           Create Session      CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***

${basePath}             /cps/api
${dataspaceName}        CSIT-Dataspace
${schemaSetName}        CSIT-SchemaSet
${anchorName}           CSIT-Anchor
${ranDataspaceName}     NFP-Operational

*** Test Cases ***
Create Dataspace
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces
    ${params}=          Create Dictionary   dataspace-name=${dataspaceName}
    ${response}=        POST On Session     CPS_URL   ${uri}   params=${params}
    Should Be Equal As Strings              ${response.status_code}   201

Create Schema Set from YANG file
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets
    ${params}=          Create Dictionary   schema-set-name=${schemaSetName}
    ${fileData}=        Get Binary File     ${DATADIR_CPS_CORE}${/}test-tree.yang
    ${fileTuple}=       Create List         test.yang   ${fileData}   application/zip
    &{files}=           Create Dictionary   file=${fileTuple}
    ${response}=        POST On Session     CPS_URL   ${uri}   files=${files}   params=${params}
    Should Be Equal As Strings              ${response.status_code}   201

Create Schema Set from ZIP file
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets
    ${params}=          Create Dictionary   schema-set-name=ZipTestSchemaSet
    ${fileData}=        Get Binary File     ${DATADIR_CPS_CORE}${/}yang-resources.zip
    ${fileTuple}=       Create List         test.zip   ${fileData}   application/zip
    &{files}=           Create Dictionary   file=${fileTuple}
    ${response}=        POST On Session     CPS_URL   ${uri}   files=${files}   params=${params}
    Should Be Equal As Strings              ${response.status_code}   201

Get Schema Set info
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets/${schemaSetName}
    ${response}=        Get On Session      CPS_URL   ${uri}   expected_status=200
    ${responseJson}=    Set Variable        ${response.json()}
    Should Be Equal As Strings              ${responseJson['name']}   ${schemaSetName}
    Should Be Equal As Strings              ${responseJson['dataspaceName']}   ${dataspaceName}

Create Anchor
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors
    ${params}=          Create Dictionary   schema-set-name=${schemaSetName}   anchor-name=${anchorName}
    ${response}=        POST On Session     CPS_URL   ${uri}   params=${params}
    Should Be Equal As Strings              ${response.status_code}   201
