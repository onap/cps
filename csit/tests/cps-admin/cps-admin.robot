# ============LICENSE_START=======================================================
# Copyright (c) 2021 Pantheon.tech.
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

${auth}                 Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${basePath}             /cps/api
${dataspaceName}        CSIT_Dataspace
${schemaSetName}        CSIT_SchemaSet
${anchorName}           CSIT_Anchor
${ranDataspaceName}     NFP-Operational
${ranSchemaSetName}     cps-ran-schema-model

*** Test Cases ***
Create Dataspace
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces
    ${params}=          Create Dictionary   dataspace-name=${dataspaceName}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        POST On Session     CPS_URL   ${uri}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   201

Create Schema Set from YANG file
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets
    ${params}=          Create Dictionary   schema-set-name=${schemaSetName}
    ${fileData}=        Get Binary File     ${DATADIR}${/}test-tree.yang
    ${fileTuple}=       Create List         test.yang   ${fileData}   application/zip
    &{files}=           Create Dictionary   file=${fileTuple}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        POST On Session     CPS_URL   ${uri}   files=${files}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   201

Create Schema Set from ZIP file
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets
    ${params}=          Create Dictionary   schema-set-name=ZipTestSchemaSet
    ${fileData}=        Get Binary File     ${DATADIR}${/}yang-resources.zip
    ${fileTuple}=       Create List         test.zip   ${fileData}   application/zip
    &{files}=           Create Dictionary   file=${fileTuple}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        POST On Session     CPS_URL   ${uri}   files=${files}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   201

Get Schema Set info
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets/${schemaSetName}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        Get On Session      CPS_URL   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=    Set Variable        ${response.json()}
    Should Be Equal As Strings              ${responseJson['name']}   ${schemaSetName}
    Should Be Equal As Strings              ${responseJson['dataspaceName']}   ${dataspaceName}

Create Anchor
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors
    ${params}=          Create Dictionary   schema-set-name=${schemaSetName}   anchor-name=${anchorName}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        POST On Session     CPS_URL   ${uri}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   201

Get CPS RAN Schema Set info
    ${uri}=              Set Variable       ${basePath}/v1/dataspaces/${ranDataspaceName}/schema-sets/${ranSchemaSetName}
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${responseJson['name']}   ${ranSchemaSetName}
    Should Be Equal As Strings              ${responseJson['dataspaceName']}   ${ranDataspaceName}
