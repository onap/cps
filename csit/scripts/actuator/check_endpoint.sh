#!/usr/bin/env bash
#
#  ============LICENSE_START=======================================================
#  Copyright (C) 2021 Pantheon.tech
#  ================================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  ============LICENSE_END=========================================================
#

# $1 is endpoint for GET request

echo "Testing CPS actuator endpoint $1"

response=$(curl -o /dev/null -s -w "%{http_code}\n" -H "Accept: application/json" -H "Content-Type: application/json" -X GET $1)

if [ "$response" == "200" ]; then
    echo "CPS Actuator endpoint check successful."
    exit 0;
fi

echo "CPS Actuator endpoint check failed with response code ${response}."
exit 1
