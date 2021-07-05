# ============LICENSE_START=======================================================
# Copyright (c) 2021 Pantheon.
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

Suite Setup           Create Session    CPS_HOST    ${CPS_HOST}

*** Variables ***


*** Test Cases ***
Test Liveness Probe Endpoint
    ${response}=      GET On Session    CPS_HOST     /manage/health/liveness     expected_status=200
    Should Be Equal As Strings          ${response.json()['status']}      UP

Test Readiness Probe Endpoint
    ${response}=      GET On Session    CPS_HOST     /manage/health/readiness    expected_status=200
    Should Be Equal As Strings          ${response.json()['status']}      UP