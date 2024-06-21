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

import { makeCustomSummaryReport } from './common/utils.js'
import { executeCmHandleSearch, executeCmHandleIdSearch } from './common/search-base.js';
import { passthroughRead } from './common/passthrough-read.js';

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
            vus: 4,
            duration: '1m',
        },
    },

    thresholds: {
        'http_req_failed{scenario:passthrough_read}': ['rate == 0'],
        'http_req_failed{scenario:id_search_module}': ['rate == 0'],
        'http_req_failed{scenario:cm_search_module}': ['rate == 0'],
        'http_req_duration{scenario:passthrough_read}': ['avg <= 2600'], // DMI delay + 100 ms
        'http_req_duration{scenario:id_search_module}': ['avg <= 625'],
        'http_req_duration{scenario:cm_search_module}': ['avg <= 13000'],
    },
};

export function passthrough_read() {
    passthroughRead();
}

export function id_search_module() {
    executeCmHandleIdSearch('module');
}

export function cm_search_module() {
    executeCmHandleSearch('module');
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}
