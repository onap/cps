/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
Documentation         NCMP

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn

Suite Setup           Create Session      CPS_URL    http://${CPS_CORE_HOST}:${CPS_CORE_PORT}

*** Variables ***

${auth}                 Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpBasePath}         /ncmp
${netconf}              NETCONF

*** Test Cases ***

Get for Passthrough Operational (CF, RO) with fields
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=ietf-netconf-monitoring:netconf-state&options=(fields=schemas/schema)
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=     Set Variable       ${response.json()}
    ${schemaCount}=      Get length         ${responseJson['ietf-netconf-monitoring:netconf-state']['schemas']}
    Should Be True                          ${schemaCount} >0
    Should Contain                          ${responseJson['ietf-netconf-monitoring:netconf-state']['schemas']['schema'][0]['location']}   ${netconf}

Create to bookstore using passthrough-running
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${jsonData}=         Get Binary File    ${DATADIR}${/}bookstoreCreateExample.json
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

Verify create to bookstore using passthrough-running
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    FOR   ${item}   IN  @{responseJson['stores:bookstore']['categories']}
        IF   "${item['code']}" == "01"
            Should Be Equal As Strings              "${item['name']}"  "Sci-Fi"
            Should Be Equal As Strings              "${item['books']}[0][title]"  "A Sci-Fi book"
        END
        IF   "${item['code']}" == "02"
            Should Be Equal As Strings              "${item['name']}"  "Horror"
            Should Be Equal As Strings              "${item['books']}[0][title]"  "A Horror book"
        END
    END

Update Bookstore using passthrough-running for Category 01
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore/categories=01
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${jsonData}=         Get Binary File    ${DATADIR}${/}bookstoreUpdateExample.json
    ${response}=         PUT On Session     CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   200

Verify update to bookstore using passthrough-running updated category 01
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore/categories=01
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    FOR   ${item}   IN  @{responseJson['stores:categories']}
        IF   "${item['code']}" == "01"
            Should Be Equal As Strings              "${item['name']}"  "Updated Sci-Fi Category Name"
        END
    END

Verify update to bookstore using passthrough-running did not remove category 02
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    ${schemaCount}=      Get length         ${responseJson['stores:bookstore']['categories']}
    Should Be Equal As Numbers              ${schemaCount}  2

Delete Bookstore using passthrough-running for Category 01
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore/categories=01
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         DELETE On Session  CPS_URL   ${uri}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   204

Verify delete to bookstore using passthrough-running removed only category 01
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}
    ${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
    ${schemaCount}=      Get length         ${responseJson['stores:bookstore']['categories']}
    Should Be Equal As Numbers              ${schemaCount}  1
    FOR   ${item}   IN  @{responseJson['stores:bookstore']['categories']}
        IF   "${item['code']}" == "02"
            Should Be Equal As Strings              "${item['name']}"  "Horror"
        END
    END

Patch will add new category with new book and add a new book to an existing category
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Content-Type=application/yang.patch+json   Authorization=${auth}
    ${jsonData}=         Get Binary File    ${DATADIR}${/}bookstorePatchExample.json
    ${response}=         PATCH On Session   CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   200
    ${verifyUri}=       Set Variable        ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore/categories=100
    ${verifyHeaders}=    Create Dictionary  Authorization=${auth}
    ${verifyResponse}=   Get On Session     CPS_URL   ${verifyUri}   headers=${verifyHeaders}
    ${responseJson}=    Set Variable        ${verifyResponse.json()}
    Should Be Equal As Strings              ${verifyResponse.status_code}   200
    FOR   ${item}   IN  @{responseJson['stores:categories']}
        IF   "${item['code']}" == "100"
            Should Be Equal As Strings              "${item['name']}"  "Category100"
        END
    END
    ${verifyUri}=       Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore/categories=02/books=A%20New%20book%20in%20existing%20category
    ${verifyResponse}=  Get On Session     CPS_URL   ${verifyUri}   headers=${verifyHeaders}
    ${responseJson}=    Set Variable       ${verifyResponse.json()}
    Should Be Equal As Strings             ${verifyResponse.status_code}   200

Get for Passthrough Operational (CF, RO) with fields and topic
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=ietf-netconf-monitoring:netconf-state&options=(fields=schemas/schema)&topic=my-topic-name
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=     Set Variable       ${response.json()}
    ${requestUuid}=      Get length         ${responseJson['requestId']}
    Should Be Equal As Strings              ${response.status_code}   200
    Should Be True                          ${requestUuid} >0