/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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
Documentation         Mount Additional Nodes

Library               Collections
Library               OperatingSystem
Library               RequestsLibrary
Library               BuiltIn

Suite Setup           Create Session     SDNC_URL    http://${SDNC_HOST}:${SDNC_PORT}

*** Variables ***

${auth}                 Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==
${sdncBasePath}         /restconf/config

*** Test Cases ***

mount node PNFDemo2
    ${uri}=              Set Variable       ${sdncBasePath}/network-topology:network-topology/topology/topology-netconf/node/PNFDemo2
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${jsonData}=         Get Binary File    ${DATADIR}${/}mountPNFDemo2.json
    ${response}=         PUT On Session     SDNC_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

mount node PNFDemo3
    ${uri}=              Set Variable       ${sdncBasePath}/network-topology:network-topology/topology/topology-netconf/node/PNFDemo3
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${jsonData}=         Get Binary File    ${DATADIR}${/}mountPNFDemo3.json
    ${response}=         PUT On Session     SDNC_URL   ${uri}   headers=${headers}   data=${jsonData}
    Should Be Equal As Strings              ${response.status_code}   201

get nodes
    ${uri}=              Set Variable       ${sdncBasePath}/network-topology:network-topology/topology/topology-netconf
    ${headers}=          Create Dictionary  Content-Type=application/json   Authorization=${auth}
    ${response}=         GET On Session     SDNC_URL   ${uri}   headers=${headers}
	${responseJson}=     Set Variable       ${response.json()}
    Should Be Equal As Strings              ${response.status_code}   200
	FOR   ${item}   IN  @{responseJson['topology']}
        IF   "${item['node']}[1][node-id]" == "PNFDemo"
            Should Be Equal As Strings              "${item['node']}[1][netconf-node-topology:port]"  "6512"
        END
		IF   "${item['node']}[2][node-id]" == "PNFDemo2"
            Should Be Equal As Strings              "${item['node']}[1][netconf-node-topology:port]"  "6512"
        END
		IF   "${item['node']}[0][node-id]" == "PNFDemo3"
            Should Be Equal As Strings              "${item['node']}[1][netconf-node-topology:port]"  "6512"
        END
    END