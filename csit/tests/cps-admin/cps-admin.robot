*** Settings ***
Documentation         CPS Core - Admin REST API

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary

Suite Setup           Create Session      CPS_HOST    ${CPS_HOST}

*** Variables ***

${auth}                 Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${basePath}             /cps/api
${dataspaceName}        CSIT-Dataspace
${schemaSetName}        CSIT-SchemaSet
${anchorName}           CSIT-Anchor

*** Test Cases ***
Create Dataspace
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces
    ${params}=          Create Dictionary   dataspace-name=${dataspaceName}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        POST On Session     CPS_HOST   ${uri}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   201

Create Schema Set from YANG file
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets
    ${params}=          Create Dictionary   schema-set-name=${schemaSetName}
    ${fileData}=        Get Binary File     ${DATADIR}${/}test-tree.yang
    ${fileTuple}=       Create List         test.yang   ${fileData}   application/zip
    &{files}=           Create Dictionary   file=${fileTuple}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        POST On Session     CPS_HOST   ${uri}   files=${files}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   201

Create Schema Set from ZIP file
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets
    ${params}=          Create Dictionary   schema-set-name=ZipTestSchemaSet
    ${fileData}=        Get Binary File     ${DATADIR}${/}yang-resources.zip
    ${fileTuple}=       Create List         test.zip   ${fileData}   application/zip
    &{files}=           Create Dictionary   file=${fileTuple}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        POST On Session     CPS_HOST   ${uri}   files=${files}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   201

Get Schema Set info
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/schema-sets/${schemaSetName}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        Get On Session      CPS_HOST   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=    Set Variable        ${response.json()}
    Should Be Equal As Strings              ${responseJson['name']}   ${schemaSetName}
    Should Be Equal As Strings              ${responseJson['dataspaceName']}   ${dataspaceName}

Create Anchor
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors
    ${params}=          Create Dictionary   schema-set-name=${schemaSetName}   anchor-name=${anchorName}
    ${headers}=         Create Dictionary   Authorization=${auth}
    ${response}=        POST On Session     CPS_HOST   ${uri}   params=${params}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   201