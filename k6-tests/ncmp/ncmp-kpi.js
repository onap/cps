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
import { Gauge, Trend } from 'k6/metrics';
import {
    TOTAL_CM_HANDLES, READ_DATA_FOR_CM_HANDLE_DELAY_MS, WRITE_DATA_FOR_CM_HANDLE_DELAY_MS,
    makeCustomSummaryReport, recordTimeInSeconds, makeBatchOfCmHandleIds, DATA_OPERATION_READ_BATCH_SIZE,
    TOPIC_DATA_OPERATIONS_BATCH_READ, KAFKA_BOOTSTRAP_SERVERS
} from './common/utils.js';
import { registerAllCmHandles, deregisterAllCmHandles } from './common/cmhandle-crud.js';
import { executeCmHandleSearch, executeCmHandleIdSearch } from './common/search-base.js';
import { passthroughRead, passthroughReadWithAltId, passthroughWrite, batchRead } from './common/passthrough-crud.js';
import {
    Reader,
} from 'k6/x/kafka';

let cmHandlesCreatedPerSecondGauge = new Gauge('cmhandles_created_per_second');
let cmHandlesDeletedPerSecondGauge = new Gauge('cmhandles_deleted_per_second');
let passthroughReadNcmpOverheadTrend = new Trend('ncmp_overhead_passthrough_read');
let passthroughReadNcmpOverheadTrendWithAlternateId = new Trend('ncmp_overhead_passthrough_read_alt_id');
let passthroughWriteNcmpOverheadTrend = new Trend('ncmp_overhead_passthrough_write');
let dataOperationsBatchReadCmHandlePerSecondTrend = new Trend('data_operations_batch_read_cmhandles_per_second');

const reader = new Reader({
    brokers: KAFKA_BOOTSTRAP_SERVERS,
    topic: TOPIC_DATA_OPERATIONS_BATCH_READ,
});

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
        passthrough_read_alt_id: {
            executor: 'constant-vus',
            exec: 'passthrough_read_alt_id',
            vus: 1,
            duration: DURATION,
        },
        passthrough_write: {
            executor: 'constant-vus',
            exec: 'passthrough_write',
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
        data_operation_send_async_http_request: {
            executor: 'constant-arrival-rate',
            exec: 'data_operation_send_async_http_request',
            duration: DURATION,
            rate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 1,
        },
        data_operation_async_batch_read: {
            executor: 'constant-arrival-rate',
            exec: 'data_operation_async_batch_read',
            duration: DURATION,
            rate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 1,
        }
    },
    thresholds: {
        'cmhandles_created_per_second': ['value >= 22'],
        'cmhandles_deleted_per_second': ['value >= 22'],
        'http_reqs{scenario:passthrough_write}': ['rate >= 13'],
        'http_reqs{scenario:passthrough_read}': ['rate >= 25'],
        'ncmp_overhead_passthrough_read': ['avg <= 100'],
        'ncmp_overhead_passthrough_read_alt_id': ['avg <= 100'],
        'ncmp_overhead_passthrough_write': ['avg <= 100'],
        'http_req_duration{scenario:id_search_module}': ['avg <= 625'],
        'http_req_duration{scenario:cm_search_module}': ['avg <= 13000'],
        'http_req_failed{scenario:id_search_module}': ['rate == 0'],
        'http_req_failed{scenario:cm_search_module}': ['rate == 0'],
        'http_req_failed{scenario:passthrough_read}': ['rate == 0'],
        'http_req_failed{scenario:passthrough_write}': ['rate == 0'],
        'http_req_failed{scenario:data_operation_send_async_http_request}': ['rate == 0'],
        'kafka_reader_error_count{scenario:data_operation_consume_kafka_responses}': ['count == 0'],
        'data_operations_batch_read_cmhandles_per_second': ['avg >= 150'],
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
    const overhead = response.timings.duration - READ_DATA_FOR_CM_HANDLE_DELAY_MS;
    passthroughReadNcmpOverheadTrend.add(overhead);
}

export function passthrough_read_alt_id() {
    const response = passthroughReadWithAltId();
    check(response, { 'passthrough read with alternate Id status equals 200': (r) => r.status === 200 });
    const overhead = response.timings.duration - READ_DATA_FOR_CM_HANDLE_DELAY_MS;
    passthroughReadNcmpOverheadTrendWithAlternateId.add(overhead);
}

export function passthrough_write() {
    const response = passthroughWrite();
    check(response, { 'passthrough write status equals 201': (r) => r.status === 201 });
    const overhead = response.timings.duration - WRITE_DATA_FOR_CM_HANDLE_DELAY_MS;
    passthroughWriteNcmpOverheadTrend.add(overhead);
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

export function data_operation_send_async_http_request() {
    const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(DATA_OPERATION_READ_BATCH_SIZE,1);
    const response = batchRead(nextBatchOfCmHandleIds)
    check(response, { 'data operation batch read status equals 200': (r) => r.status === 200 });
}

export function data_operation_async_batch_read() {
    try {
        let messages = reader.consume({ limit: DATA_OPERATION_READ_BATCH_SIZE });
        dataOperationsBatchReadCmHandlePerSecondTrend.add(messages.length);
    } catch (error) {
        dataOperationsBatchReadCmHandlePerSecondTrend.add(0);
        console.error(error);
    }
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}
