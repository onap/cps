# ============LICENSE_START=======================================================
# Copyright (c) 2021 Pantheon.tech.
# Modifications Copyright (c) 2021 Bell Canada.
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
Documentation         CPS - Actuator endpoints

Library               Collections
Library               RequestsLibrary

Suite Setup           Create Session    MANAGEMENT_URL    http://${CPS_CORE_HOST}:${CPS_CORE_MANAGEMENT_PORT}/manage

*** Variables ***


*** Test Cases ***
Test CPS Enhanced Healthcheck
    [Documentation]     Runs CPS Health Check. It will check for overall status update of CPS component like, Database and diskspace status along with liveliness and readiness check
    ${response}=      GET On Session    MANAGEMENT_URL     health    expected_status=200
    ${resp_body}=      Convert to string     ${response.text}
    Should Contain      ${resp_body}     UP
    Should Not Contain      ${resp_body}     DOWN
