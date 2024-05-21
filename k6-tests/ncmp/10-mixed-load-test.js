/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
 *  ============LICENSE_END=========================================================
 */

import http from 'k6/http';
import { check } from 'k6';
import { NCMP_BASE_URL, getRandomCmHandleId } from './utils.js'
import { searchRequest } from './search-base.js';

export const options = {
  scenarios: {
    passthrough_read: {
      executor: 'constant-vus',
      exec: 'passthrough_read',
      vus: 10,
      duration: '1m',
    },
    id_search_module: {
      executor: 'constant-vus',
      exec: 'id_search_module',
      vus: 5,
      duration: '1m',
    },
    cm_search_module: {
      executor: 'constant-vus',
      exec: 'cm_search_module',
      vus: 5,
      duration: '1m',
    },
  },

  thresholds: {
    http_req_failed: ['rate==0'],
    'http_req_duration{scenario:passthrough_read}': ['avg <= 2540'], // DMI delay + 40 ms
    'http_req_duration{scenario:id_search_module}': ['avg <= 200'],
    'http_req_duration{scenario:cm_search_module}': ['avg <= 35_000'],
  },
};

export function passthrough_read() {
    const cmHandleId = getRandomCmHandleId();
    const datastoreName = 'ncmp-datastore%3Apassthrough-operational';
    const url = `${NCMP_BASE_URL}/ncmp/v1/ch/${cmHandleId}/data/ds/${datastoreName}?resourceIdentifier=x&include-descendants=true`
    const response = http.get(url);
    check(response, {
        'status equals 200': (r) => r.status === 200,
    });
}

export function id_search_module() {
    const search_filter = {
        "cmHandleQueryParameters": [
            {
                "conditionName": "hasAllModules",
                "conditionParameters": [ {"moduleName": "ietf-yang-types-1"} ]
            }
        ]
    };
    searchRequest('id-searches', JSON.stringify(search_filter));
}

export function cm_search_module() {
    const search_filter = {
        "cmHandleQueryParameters": [
            {
                "conditionName": "hasAllModules",
                "conditionParameters": [ {"moduleName": "ietf-yang-types-1"} ]
            }
        ]
    };
    searchRequest('searches', JSON.stringify(search_filter));
}
