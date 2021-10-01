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
Documentation         NCMP

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn

Suite Setup           Create Session      CPS_URL    http://${CPS_HOST}:${CPS_PORT}

*** Variables ***

${auth}                 Basic Y3BzdXNlcjpjcHNyMGNrcyE=
${ncmpBasePath}         /ncmp
${netconf}     			['NETCONF']

*** Test Cases ***

Get for Passthough Operational (CF, RO)
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=ietf-netconf-monitoring:netconf-state
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=     Set Variable       ${response.json()}
	${schemaCount}=      Get length         ${responseJson['ietf-netconf-monitoring:netconf-state']['schemas']}        
    Should Be True		 ${schemaCount} >0
	
Get for Passthough Operational (CF, RO) with fields
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=ietf-netconf-monitoring:netconf-state&fields=schemas/schema/location
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=     Set Variable       ${response.json()}
	${schemaCount}=      Get length         ${responseJson['ietf-netconf-monitoring:netconf-state']['schemas']}        
    Should Be True		 ${schemaCount} >0
	Should Be Equal As Strings 	 ${responseJson['ietf-netconf-monitoring:netconf-state']['schemas']['schema'][0]['location']}   ${netconf}   
	
Get for Passthough Operational (CF, RO) with Depth
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=ietf-netconf-monitoring:netconf-state&depth=4
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}   expected_status=200
    ${responseJson}=     Set Variable       ${response.json()}
	${schemaCount}=      Get length         ${responseJson['netconf-state']['schemas']}        
    Should Be True		 ${schemaCount} >0
	
Write to bookstore using passthrough-running
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
	${jsonData}=         Get Binary File    ${DATADIR}${/}bookstoreAddEntry.json
    ${response}=         POST On Session    CPS_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

Verify write to bookstore using passthrough-running
    ${uri}=              Set Variable       ${ncmpBasePath}/v1/ch/PNFDemo/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=stores:bookstore
    ${headers}=          Create Dictionary  Authorization=${auth}
    ${response}=         Get On Session     CPS_URL   ${uri}   headers=${headers}
    Should Be Equal As Strings              ${response.status_code}   200
	
	
	