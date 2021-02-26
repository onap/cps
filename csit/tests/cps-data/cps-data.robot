*** Settings ***
Documentation         CPS Core - Data REST API

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary

Suite Setup           Create Session      CPS_HOST    ${CPS_HOST}

*** Variables ***

${basePath}             /cps/api
${dataspaceName}        CSIT-Dataspace
${anchorName}           CSIT-Anchor

*** Test Cases ***
Create Data Node
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors/${anchorName}/nodes
    ${headers}          Create Dictionary   Content-Type=application/json
    ${jsonData}=        Get Binary File     ${DATADIR}${/}test-tree.json
    ${response}=        POST On Session     CPS_HOST   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

Get Data Node by XPath
    ${uri}=             Set Variable        ${basePath}/v1/dataspaces/${dataspaceName}/anchors/${anchorName}/node
    ${params}=          Create Dictionary   xpath=/test-tree/branch[@name='Left']/nest
    ${response}=        Get On Session      CPS_HOST   ${uri}   params=${params}   expected_status=200
    ${responseJson}=    Set Variable        ${response.json()}
    Should Be Equal As Strings              ${responseJson['name']}   Small


