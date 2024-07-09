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

import { check } from 'k6';
import { Gauge } from 'k6/metrics';
import { TOTAL_CM_HANDLES, makeCustomSummaryReport, recordTimeInSeconds } from './common/utils.js';
import { registerAllCmHandles, deregisterAllCmHandles } from './common/cmhandle-crud.js';
import { executeCmHandleSearch, executeCmHandleIdSearch } from './common/search-base.js';
import { passthroughRead } from './common/passthrough-read.js';

let cmHandlesCreatedPerSecondGauge = new Gauge('cmhandles_created_per_second');
let cmHandlesDeletedPerSecondGauge = new Gauge('cmhandles_deleted_per_second');

const DURATION = '15m';

export const options = {
    setupTimeout: '6m',
    teardownTimeout: '6m',
    scenarios: {
        passthrough_read: {
            executor: 'constant-vus',
            exec: 'passthrough_read',
            vus: 10,
            duration: DURATION,
        },
        id_search_module: {
            executor: 'constant-vus',
            exec: 'id_search_module',
            vus: 3,
            duration: DURATION,
        },
        cm_search_module: {
            executor: 'constant-vus',
            exec: 'cm_search_module',
            vus: 3,
            duration: DURATION,
        },
    },
    thresholds: {
        'cmhandles_created_per_second': ['value >= 22'],
        'cmhandles_deleted_per_second': ['value >= 22'],
        'http_req_failed{scenario:passthrough_read}': ['rate == 0'],
        'http_req_failed{scenario:id_search_module}': ['rate == 0'],
        'http_req_failed{scenario:cm_search_module}': ['rate == 0'],
        'http_req_duration{scenario:passthrough_read}': ['avg <= 2600'], // DMI delay + 100 ms
        'http_req_duration{scenario:id_search_module}': ['avg <= 625'],
        'http_req_duration{scenario:cm_search_module}': ['avg <= 13000'],
    },
};

export function setup() {
    const totalRegistrationTimeInSeconds = recordTimeInSeconds(registerAllCmHandles);
    cmHandlesCreatedPerSecondGauge.add(TOTAL_CM_HANDLES / totalRegistrationTimeInSeconds);
}

export function teardown() {
    const totalDeregistrationTimeInSeconds = recordTimeInSeconds(deregisterAllCmHandles);
    cmHandlesDeletedPerSecondGauge.add(TOTAL_CM_HANDLES / totalDeregistrationTimeInSeconds);
}

export function passthrough_read() {
    const response = passthroughRead();
    check(response, { 'passthrough read status equals 200': (r) => r.status === 200 });
}

export function id_search_module() {
    const response = executeCmHandleIdSearch('module');
    check(response, { 'module ID search status equals 200': (r) => r.status === 200 });
    check(JSON.parse(response.body), { 'module ID search returned expected CM-handles': (arr) => arr.length === TOTAL_CM_HANDLES });
}

export function cm_search_module() {
    const response = executeCmHandleSearch('module');
    check(response, { 'module search status equals 200': (r) => r.status === 200 });
    check(JSON.parse(response.body), { 'module search returned expected CM-handles': (arr) => arr.length === TOTAL_CM_HANDLES });
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}
